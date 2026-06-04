package com.niki914.nexus.agentic.chat.agentic.shell

data class ShellCommandPolicyDecision(
    // TODO P0 接入真的安全逻辑
    val allowed: Boolean,
    val code: String = "OK",
    val reason: String = "",
)

class ShellCommandSafetyPolicy {
    fun evaluate(command: String): ShellCommandPolicyDecision {
        val normalizedTokens = command.shellLikeTokens()
            .map { it.normalizedShellToken() }
            .filter { it.isNotBlank() }
        val blocked = normalizedTokens.containsDangerousCommand(depth = 0)
        return if (blocked) {
            ShellCommandPolicyDecision(
                allowed = false,
                code = "UNSAFE_COMMAND",
                reason = "Command blocked by safety policy.",
            )
        } else {
            ShellCommandPolicyDecision(allowed = true)
        }
    }

    private fun String.shellLikeTokens(): List<String> {
        val tokens = mutableListOf<String>()
        val current = StringBuilder()
        var quote: Char? = null
        var escaped = false

        fun flush() {
            if (current.isNotEmpty()) {
                tokens += current.toString()
                current.clear()
            }
        }

        for (char in this) {
            when {
                escaped -> {
                    current.append(char)
                    escaped = false
                }

                char == '\\' -> escaped = true
                quote != null -> {
                    if (char == quote) {
                        quote = null
                    } else {
                        current.append(char)
                    }
                }

                char == '\'' || char == '"' -> quote = char
                char.isWhitespace() || char in SHELL_TOKEN_SEPARATORS -> flush()
                else -> current.append(char)
            }
        }
        if (escaped) {
            current.append('\\')
        }
        flush()
        return tokens
    }

    private fun String.normalizedShellToken(): String {
        return lowercase()
            .trim()
            .trim('"', '\'')
    }

    private fun List<String>.containsDangerousCommand(depth: Int): Boolean {
        if (depth > MAX_SHELL_PAYLOAD_DEPTH) {
            return false
        }
        for (index in indices) {
            val executable = this[index].executableName()
            if (executable in DANGEROUS_STANDALONE_COMMANDS) {
                return true
            }
            if (executable == "rm" && hasRecursiveForceRmFlags(startIndex = index + 1)) {
                return true
            }
            if (executable == "pm" && getOrNull(index + 1)?.executableName() == "uninstall") {
                return true
            }
            if (
                executable == "cmd" &&
                getOrNull(index + 1)?.executableName() == "package" &&
                getOrNull(index + 2)?.executableName() == "uninstall"
            ) {
                return true
            }
            if (containsDangerousNestedShellPayload(executable, index, depth)) {
                return true
            }
        }
        return false
    }

    private fun List<String>.containsDangerousNestedShellPayload(
        executable: String,
        index: Int,
        depth: Int,
    ): Boolean {
        val payload = when {
            executable in SHELL_COMMANDS -> shellCommandPayloadAfterC(startIndex = index + 1)
            executable == "eval" -> drop(index + 1).joinToString(" ").takeIf { it.isNotBlank() }
            else -> null
        } ?: return false

        val nestedTokens = payload.shellLikeTokens()
            .map { it.normalizedShellToken() }
            .filter { it.isNotBlank() }
        return nestedTokens.containsDangerousCommand(depth = depth + 1)
    }

    private fun List<String>.shellCommandPayloadAfterC(startIndex: Int): String? {
        for (index in startIndex until size) {
            val token = this[index]
            if (token == "-c") {
                return getOrNull(index + 1)
            }
            if (!token.startsWith("-")) {
                return null
            }
        }
        return null
    }

    private fun List<String>.hasRecursiveForceRmFlags(startIndex: Int): Boolean {
        var hasRecursive = false
        var hasForce = false
        for (index in startIndex until size) {
            val token = this[index]
            if (!token.startsWith("-")) {
                continue
            }
            when {
                token == "--recursive" -> hasRecursive = true
                token == "--force" -> hasForce = true
                token.startsWith("--") -> Unit
                else -> {
                    hasRecursive = hasRecursive || token.contains('r')
                    hasForce = hasForce || token.contains('f')
                }
            }
            if (hasRecursive && hasForce) {
                return true
            }
        }
        return false
    }

    private fun String.executableName(): String {
        return substringAfterLast('/')
    }

    companion object {
        private const val MAX_SHELL_PAYLOAD_DEPTH = 8
        private val SHELL_TOKEN_SEPARATORS = setOf(';', '&', '|', '`', '$', '(', ')', '<', '>')
        private val SHELL_COMMANDS = setOf("sh", "bash", "mksh")
        private val DANGEROUS_STANDALONE_COMMANDS = setOf("reboot", "su", "setprop", "dd")
    }
}
