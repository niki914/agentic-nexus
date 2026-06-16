package com.niki914.nexus.agentic.chat.agentic.shell

import com.niki914.libterm.OpenResult
import com.niki914.libterm.OutputStream
import com.niki914.libterm.TerminalFailure
import com.niki914.libterm.TerminalIdentity
import com.niki914.libterm.runtime.CommandResult
import com.niki914.libterm.runtime.LibTerm
import com.niki914.libterm.runtime.LibTermRuntime
import com.niki914.libterm.runtime.LibTermSession
import com.niki914.libterm.runtime.TermResult
import com.niki914.nexus.h.util.ContextProvider
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex

object TerminalSessionPool {
    private const val CUSTOM_TOOL_SESSION = "__custom_user"
    private val lock = Any()
    private var runtimeHolder: RuntimeHolder? = null
    private val sessions: MutableMap<String, TerminalSessionEntry> = linkedMapOf()
    private val asyncStates: MutableMap<String, AsyncState> = linkedMapOf()
    private val executionLocks: MutableMap<String, Mutex> = linkedMapOf()

    suspend fun open(identity: String, cwd: String? = null): TerminalOpenOutcome {
        return openSession(handle = identity.trim(), identity = identity, cwd = cwd)
    }

    suspend fun openAndExecute(
        identity: String,
        cwd: String?,
        command: String,
        timeoutMs: Long,
    ): TerminalCommandOutcome {
        return when (val openOutcome = open(identity = identity, cwd = cwd)) {
            is TerminalOpenOutcome.Success -> executeBlocking(
                session = openOutcome.session,
                command = command,
                timeoutMs = timeoutMs,
            )

            is TerminalOpenOutcome.Failure -> TerminalCommandOutcome.Failure(
                failure = openOutcome.failure,
                elapsedSeconds = openOutcome.elapsedSeconds,
            )

            is TerminalOpenOutcome.InvalidRequest -> TerminalCommandOutcome.UnexpectedError(
                throwable = IllegalArgumentException(openOutcome.message),
                elapsedSeconds = 0L,
            )
        }
    }

    suspend fun executeCustomCommand(command: String, timeoutMs: Long): TerminalCommandOutcome {
        return when (val openOutcome = openSession(
            handle = CUSTOM_TOOL_SESSION,
            identity = "user",
            cwd = null,
        )) {
            is TerminalOpenOutcome.Success -> executeBlocking(
                session = openOutcome.session,
                command = command,
                timeoutMs = timeoutMs,
            )

            is TerminalOpenOutcome.Failure -> TerminalCommandOutcome.Failure(
                failure = openOutcome.failure,
                elapsedSeconds = openOutcome.elapsedSeconds,
            )

            is TerminalOpenOutcome.InvalidRequest -> TerminalCommandOutcome.UnexpectedError(
                throwable = IllegalArgumentException(openOutcome.message),
                elapsedSeconds = 0L,
            )
        }
    }

    private suspend fun openSession(handle: String, identity: String, cwd: String? = null): TerminalOpenOutcome {
        val startTimeMs = System.currentTimeMillis()
        val mappedIdentity = try {
            mapIdentity(identity)
        } catch (error: IllegalArgumentException) {
            return TerminalOpenOutcome.InvalidRequest(error.message.orEmpty())
        }
        synchronized(lock) {
            sessions[handle]?.let { existing ->
                return TerminalOpenOutcome.Success(
                    session = existing.handle,
                    identity = existing.identity,
                )
            }
        }

        val holder = runtime()
        return when (val result = holder.runtime.open {
            this.identity = mappedIdentity
            this.cwd = cwd
        }) {
            is OpenResult.Success -> {
                val entry = TerminalSessionEntry(
                    handle = handle,
                    identity = handle,
                    session = result.value,
                    libTermSessionId = result.value.id,
                )
                val existing = synchronized(lock) {
                    val current = sessions[handle]
                    if (current == null) {
                        sessions[handle] = entry
                        executionLocks.getOrPut(handle) { Mutex() }
                        null
                    } else {
                        current
                    }
                }
                if (existing != null) {
                    runCatching { holder.runtime.close(result.value.id) }
                    TerminalOpenOutcome.Success(
                        session = existing.handle,
                        identity = existing.identity,
                    )
                } else {
                    TerminalOpenOutcome.Success(
                        session = entry.handle,
                        identity = entry.identity,
                    )
                }
            }

            is OpenResult.Failure -> TerminalOpenOutcome.Failure(
                failure = result.failure,
                elapsedSeconds = elapsedSeconds(startTimeMs),
            )
        }
    }

    fun get(session: String): TerminalSessionEntry? {
        return synchronized(lock) {
            sessions[session]
        }
    }

    suspend fun executeBlocking(
        session: String,
        command: String,
        timeoutMs: Long,
    ): TerminalCommandOutcome {
        val startTimeMs = System.currentTimeMillis()
        val (entry, executeLock) = synchronized(lock) {
            val entry = sessions[session] ?: return TerminalCommandOutcome.SessionNotFound(session)
            val executeLock = executionLocks.getOrPut(session) { Mutex() }
            entry to executeLock
        }
        if (!executeLock.tryLock()) {
            return TerminalCommandOutcome.Busy(
                session = session,
                asyncId = currentAsyncId(session),
            )
        }

        return try {
            when (val result = entry.session.exec(command = command, timeoutMillis = timeoutMs)) {
                is TermResult.Success -> {
                    if (result.value.timedOut) {
                        TerminalCommandOutcome.Timeout(
                            result = result.value,
                            elapsedSeconds = elapsedSeconds(startTimeMs),
                        )
                    } else {
                        TerminalCommandOutcome.Success(
                            result = result.value,
                            elapsedSeconds = elapsedSeconds(startTimeMs),
                        )
                    }
                }

                is TermResult.Failure -> TerminalCommandOutcome.Failure(
                    failure = result.failure,
                    elapsedSeconds = elapsedSeconds(startTimeMs),
                )
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            TerminalCommandOutcome.UnexpectedError(
                throwable = error,
                elapsedSeconds = elapsedSeconds(startTimeMs),
            )
        } finally {
            executeLock.unlock()
        }
    }

    suspend fun startAsync(
        session: String,
        command: String,
        timeoutMs: Long,
    ): TerminalAsyncStartOutcome {
        val startTimeMs = System.currentTimeMillis()
        val (entry, executeLock, holder) = synchronized(lock) {
            val entry = sessions[session] ?: return TerminalAsyncStartOutcome.SessionNotFound(session)
            val executeLock = executionLocks.getOrPut(session) { Mutex() }
            val holder = runtimeHolder ?: return TerminalAsyncStartOutcome.InvalidRequest(
                "Terminal runtime is not initialized. Use open or open_and_exec first.",
            )
            Triple(entry, executeLock, holder)
        }
        if (!executeLock.tryLock()) {
            return TerminalAsyncStartOutcome.Busy(
                session = session,
                asyncId = currentAsyncId(session),
            )
        }

        val asyncId = UUID.randomUUID().toString()
        val stdoutPartial = StringBuilder()
        val stderrPartial = StringBuilder()
        val stateLock = Any()
        lateinit var state: AsyncState
        val collectorJob = holder.scope.launch(start = CoroutineStart.UNDISPATCHED) {
            entry.session.stream.collect { chunk ->
                synchronized(stateLock) {
                    when (chunk.stream) {
                        OutputStream.STDOUT -> stdoutPartial.append(chunk.text)
                        OutputStream.STDERR -> stderrPartial.append(chunk.text)
                    }
                }
            }
        }
        val execJob = holder.scope.launch(start = CoroutineStart.LAZY) {
            try {
                when (val result = entry.session.exec(command = command, timeoutMillis = timeoutMs)) {
                    is TermResult.Success -> synchronized(state.lock) {
                        state.result = result.value
                    }

                    is TermResult.Failure -> synchronized(state.lock) {
                        state.failure = result.failure
                    }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                synchronized(state.lock) {
                    state.unexpectedError = error
                }
            }
        }
        state = AsyncState(
            asyncId = asyncId,
            execJob = execJob,
            collectorJob = collectorJob,
            startTimeMs = startTimeMs,
            stdoutPartial = stdoutPartial,
            stderrPartial = stderrPartial,
            lock = stateLock,
        )
        synchronized(lock) {
            asyncStates[session] = state
        }
        execJob.start()

        return TerminalAsyncStartOutcome.Accepted(
            asyncId = asyncId,
            elapsedSeconds = elapsedSeconds(startTimeMs),
        )
    }

    suspend fun readAsyncResult(session: String, asyncId: String): TerminalAsyncReadOutcome {
        val state = synchronized(lock) {
            if (!sessions.containsKey(session)) {
                return TerminalAsyncReadOutcome.SessionNotFound(session)
            }
            val state = asyncStates[session]
                ?: return TerminalAsyncReadOutcome.AsyncNotFound(session = session, asyncId = asyncId)
            if (state.asyncId != asyncId) {
                return TerminalAsyncReadOutcome.AsyncNotFound(session = session, asyncId = asyncId)
            }
            state
        }

        val snapshot = synchronized(state.lock) {
            AsyncSnapshot(
                stdoutPartial = state.stdoutPartial.toString(),
                stderrPartial = state.stderrPartial.toString(),
                result = state.result,
                failure = state.failure,
                unexpectedError = state.unexpectedError,
                execCompleted = state.execJob.isCompleted,
            )
        }
        val elapsedSeconds = elapsedSeconds(state.startTimeMs)
        snapshot.result?.let { result ->
            completeAsync(session = session, state = state)
            return if (result.timedOut) {
                TerminalAsyncReadOutcome.TimedOut(
                    result = result,
                    elapsedSeconds = elapsedSeconds,
                )
            } else {
                TerminalAsyncReadOutcome.Completed(
                    result = result,
                    elapsedSeconds = elapsedSeconds,
                )
            }
        }
        snapshot.failure?.let { failure ->
            completeAsync(session = session, state = state)
            return TerminalAsyncReadOutcome.Failure(
                failure = failure,
                elapsedSeconds = elapsedSeconds,
            )
        }
        snapshot.unexpectedError?.let { error ->
            completeAsync(session = session, state = state)
            return TerminalAsyncReadOutcome.UnexpectedError(
                throwable = error,
                elapsedSeconds = elapsedSeconds,
            )
        }
        if (snapshot.execCompleted) {
            completeAsync(session = session, state = state)
            return TerminalAsyncReadOutcome.UnexpectedError(
                throwable = IllegalStateException("Async command completed without result."),
                elapsedSeconds = elapsedSeconds,
            )
        }
        return TerminalAsyncReadOutcome.Running(
            stdoutPartial = snapshot.stdoutPartial,
            stderrPartial = snapshot.stderrPartial,
            elapsedSeconds = elapsedSeconds,
        )
    }

    suspend fun close(session: String): TerminalCloseOutcome {
        val removed = synchronized(lock) {
            RemovedSession(
                entry = sessions.remove(session),
                asyncState = asyncStates.remove(session),
                executionLock = executionLocks.remove(session),
            )
        }
        removed.asyncState?.let { state ->
            state.execJob.cancel()
            state.collectorJob.cancel()
            unlockIfLocked(removed.executionLock)
        }
        val entry = removed.entry ?: return TerminalCloseOutcome.Closed
        return try {
            entry.session.close()
            TerminalCloseOutcome.Closed
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            TerminalCloseOutcome.UnexpectedError(error)
        }
    }

    suspend fun closeAll(): TerminalCloseAllOutcome {
        val removed = synchronized(lock) {
            val removed = RemovedAll(
                holder = runtimeHolder,
                sessionCount = sessions.size,
                asyncStates = asyncStates.toMap(),
                executionLocks = executionLocks.toMap(),
            )
            runtimeHolder = null
            sessions.clear()
            asyncStates.clear()
            executionLocks.clear()
            removed
        }
        removed.asyncStates.forEach { (session, state) ->
            state.execJob.cancel()
            state.collectorJob.cancel()
            unlockIfLocked(removed.executionLocks[session])
        }
        val runtimeClosedCount = removed.holder?.let { holder ->
            runCatching { holder.runtime.closeAll() }.getOrDefault(0)
        } ?: 0
        removed.holder?.scopeJob?.cancel()
        return TerminalCloseAllOutcome(
            closedCount = maxOf(removed.sessionCount, runtimeClosedCount),
        )
    }

    private suspend fun runtime(): RuntimeHolder {
        synchronized(lock) {
            runtimeHolder?.let { return it }
        }

        val context = ContextProvider.await().applicationContext
        val scopeJob = SupervisorJob()
        val scope = CoroutineScope(scopeJob + Dispatchers.IO)
        val created = RuntimeHolder(
            runtime = LibTerm.runtime(context = context, scope = scope) {},
            scopeJob = scopeJob,
            scope = scope,
        )
        val existing = synchronized(lock) {
            runtimeHolder ?: created.also { runtimeHolder = it }
        }
        if (existing !== created) {
            created.scopeJob.cancel()
        }
        return existing
    }

    private fun mapIdentity(identity: String?): TerminalIdentity {
        return when (identity?.trim()) {
            "user" -> TerminalIdentity.User
            "root" -> TerminalIdentity.Su
            else -> throw IllegalArgumentException("Field 'identity' must be one of user, root.")
        }
    }

    private fun currentAsyncId(session: String): String? {
        return synchronized(lock) {
            asyncStates[session]?.asyncId
        }
    }

    private fun completeAsync(session: String, state: AsyncState) {
        val executeLock = synchronized(lock) {
            if (asyncStates[session] === state) {
                asyncStates.remove(session)
            }
            executionLocks[session]
        }
        state.collectorJob.cancel()
        unlockIfLocked(executeLock)
    }

    private fun unlockIfLocked(executeLock: Mutex?) {
        if (executeLock?.isLocked == true) {
            runCatching { executeLock.unlock() }
        }
    }

    private fun elapsedSeconds(startTimeMs: Long, nowMs: Long = System.currentTimeMillis()): Long {
        return ((nowMs - startTimeMs).coerceAtLeast(0L) / 1000L)
    }
}

private data class RuntimeHolder(
    val runtime: LibTermRuntime,
    val scopeJob: Job,
    val scope: CoroutineScope,
)

data class TerminalSessionEntry(
    val handle: String,
    val identity: String,
    val session: LibTermSession,
    val libTermSessionId: String,
)

private data class AsyncState(
    val asyncId: String,
    val execJob: Job,
    val collectorJob: Job,
    val startTimeMs: Long,
    val stdoutPartial: StringBuilder,
    val stderrPartial: StringBuilder,
    val lock: Any,
    var result: CommandResult? = null,
    var failure: TerminalFailure? = null,
    var unexpectedError: Throwable? = null,
)

sealed interface TerminalOpenOutcome {
    data class Success(val session: String, val identity: String) : TerminalOpenOutcome
    data class Failure(val failure: TerminalFailure, val elapsedSeconds: Long) : TerminalOpenOutcome
    data class InvalidRequest(val message: String) : TerminalOpenOutcome
}

sealed interface TerminalCommandOutcome {
    data class Success(val result: CommandResult, val elapsedSeconds: Long) : TerminalCommandOutcome
    data class Timeout(val result: CommandResult, val elapsedSeconds: Long) : TerminalCommandOutcome
    data class Failure(val failure: TerminalFailure, val elapsedSeconds: Long) : TerminalCommandOutcome
    data class SessionNotFound(val session: String) : TerminalCommandOutcome
    data class Busy(val session: String, val asyncId: String?) : TerminalCommandOutcome
    data class UnexpectedError(val throwable: Throwable, val elapsedSeconds: Long) : TerminalCommandOutcome
}

sealed interface TerminalAsyncStartOutcome {
    data class Accepted(val asyncId: String, val elapsedSeconds: Long) : TerminalAsyncStartOutcome
    data class SessionNotFound(val session: String) : TerminalAsyncStartOutcome
    data class Busy(val session: String, val asyncId: String?) : TerminalAsyncStartOutcome
    data class InvalidRequest(val message: String) : TerminalAsyncStartOutcome
}

sealed interface TerminalAsyncReadOutcome {
    data class Running(
        val stdoutPartial: String,
        val stderrPartial: String,
        val elapsedSeconds: Long,
    ) : TerminalAsyncReadOutcome

    data class Completed(val result: CommandResult, val elapsedSeconds: Long) : TerminalAsyncReadOutcome
    data class TimedOut(val result: CommandResult, val elapsedSeconds: Long) : TerminalAsyncReadOutcome
    data class Failure(val failure: TerminalFailure, val elapsedSeconds: Long) : TerminalAsyncReadOutcome
    data class AsyncNotFound(val session: String, val asyncId: String) : TerminalAsyncReadOutcome
    data class SessionNotFound(val session: String) : TerminalAsyncReadOutcome
    data class UnexpectedError(val throwable: Throwable, val elapsedSeconds: Long) : TerminalAsyncReadOutcome
}

sealed interface TerminalCloseOutcome {
    data object Closed : TerminalCloseOutcome
    data class UnexpectedError(val throwable: Throwable) : TerminalCloseOutcome
}

data class TerminalCloseAllOutcome(
    val closedCount: Int,
)

private data class AsyncSnapshot(
    val stdoutPartial: String,
    val stderrPartial: String,
    val result: CommandResult?,
    val failure: TerminalFailure?,
    val unexpectedError: Throwable?,
    val execCompleted: Boolean,
)

private data class RemovedSession(
    val entry: TerminalSessionEntry?,
    val asyncState: AsyncState?,
    val executionLock: Mutex?,
)

private data class RemovedAll(
    val holder: RuntimeHolder?,
    val sessionCount: Int,
    val asyncStates: Map<String, AsyncState>,
    val executionLocks: Map<String, Mutex>,
)
