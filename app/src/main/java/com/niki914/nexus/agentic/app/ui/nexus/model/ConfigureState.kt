package com.niki914.nexus.agentic.app.ui.nexus.model

import com.niki914.nexus.agentic.mod.LocalSettings
import com.niki914.nexus.cb.ComposeMVIViewModel
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

data class ConfigureUiState(
    val providerSpec: ProviderSpec = ProviderSpecs.default,
    val endpointOverrideEnabled: Boolean = false,
    val endpointInput: String = ProviderSpecs.default.officialEndpoint,
    val lastCustomEndpointInput: String = "",
    val modelInput: String = "",
    val apiKeyInput: String = "",
    val apiKeyVisible: Boolean = false,
    val isSaving: Boolean = false,
    val saveEnabled: Boolean = false,
    val inlineError: ConfigureInlineError? = null,
)

sealed interface ConfigureInlineError {
    data class LoadFailed(val reason: ConfigureErrorReason.LoadSettingsFailed) : ConfigureInlineError
    data class SaveFailed(val reason: ConfigureErrorReason.SaveSettingsFailed) : ConfigureInlineError
}

sealed interface ConfigureErrorReason {
    data class LoadSettingsFailed(val message: String) : ConfigureErrorReason
    data class SaveSettingsFailed(val message: String) : ConfigureErrorReason
}

sealed interface ConfigureIntent {
    data class Initialize(val providerId: String?) : ConfigureIntent
    data class SetEndpointOverride(val enabled: Boolean) : ConfigureIntent
    data class UpdateEndpoint(val value: String) : ConfigureIntent
    data class UpdateModel(val value: String) : ConfigureIntent
    data class UpdateApiKey(val value: String) : ConfigureIntent
    data object ToggleApiKeyVisibility : ConfigureIntent
    data object Save : ConfigureIntent
}

sealed interface ConfigureEffect {
    data object SaveSucceeded : ConfigureEffect
    data class SaveFailed(val reason: ConfigureErrorReason) : ConfigureEffect
    data object FocusModel : ConfigureEffect
    data object FocusApiKey : ConfigureEffect
    data object FocusEndpoint : ConfigureEffect
}

class ConfigureViewModel internal constructor(
    private val loadSettings: suspend () -> LocalSettings,
    private val saveSettings: suspend (LocalSettings) -> Unit,
) : ComposeMVIViewModel<ConfigureIntent, ConfigureUiState, ConfigureEffect>() {

    override fun initUiState(): ConfigureUiState = ConfigureUiState()

    override suspend fun handleIntent(intent: ConfigureIntent) {
        when (intent) {
            is ConfigureIntent.Initialize -> initialize(intent.providerId)
            is ConfigureIntent.SetEndpointOverride -> setEndpointOverride(intent.enabled)
            is ConfigureIntent.UpdateEndpoint -> updateEndpoint(intent.value)
            is ConfigureIntent.UpdateModel -> updateModel(intent.value)
            is ConfigureIntent.UpdateApiKey -> updateApiKey(intent.value)
            ConfigureIntent.ToggleApiKeyVisibility -> toggleApiKeyVisibility()
            ConfigureIntent.Save -> save()
        }
    }

    private suspend fun initialize(initialProviderId: String?) {
        try {
            val settings = loadSettings()
            val savedProviderId = settings.provider.takeIf { it.isNotBlank() }
            val resolvedProviderId = initialProviderId
                ?.takeIf { it.isNotBlank() }
                ?: savedProviderId
            val providerSpec = ProviderSpecs.find(resolvedProviderId)
            val shouldReuseSavedValues = if (initialProviderId.isNullOrBlank()) {
                savedProviderId == null || savedProviderId == providerSpec.id
            } else {
                savedProviderId == providerSpec.id
            }
            val savedEndpoint = if (shouldReuseSavedValues) settings.endpoint.trim() else ""
            val savedModel = if (shouldReuseSavedValues) settings.model else ""
            val savedApiKey = if (shouldReuseSavedValues) settings.apiKey else ""
            val endpointOverrideEnabled = savedEndpoint.isNotBlank() &&
                savedEndpoint != providerSpec.officialEndpoint
            val endpointInput = if (endpointOverrideEnabled) {
                savedEndpoint
            } else {
                providerSpec.officialEndpoint
            }
            val lastCustomEndpointInput = if (endpointOverrideEnabled) {
                savedEndpoint
            } else {
                providerSpec.officialEndpoint
            }
            updateState {
                copy(
                    providerSpec = providerSpec,
                    endpointOverrideEnabled = endpointOverrideEnabled,
                    endpointInput = endpointInput,
                    lastCustomEndpointInput = lastCustomEndpointInput,
                    modelInput = savedModel,
                    apiKeyInput = savedApiKey,
                    apiKeyVisible = false,
                    isSaving = false,
                    saveEnabled = canSave(
                        endpointOverrideEnabled = endpointOverrideEnabled,
                        endpointInput = endpointInput,
                        modelInput = savedModel,
                        apiKeyInput = savedApiKey,
                    ),
                    inlineError = null,
                )
            }
        } catch (throwable: Throwable) {
            if (throwable is CancellationException) throw throwable
            val message = throwable.message ?: throwable::class.java.simpleName
            val reason = ConfigureErrorReason.LoadSettingsFailed(message)
            updateState {
                copy(
                    isSaving = false,
                    saveEnabled = false,
                    inlineError = ConfigureInlineError.LoadFailed(reason),
                )
            }
        }
    }

    private fun setEndpointOverride(enabled: Boolean) {
        updateState {
            val nextEndpointInput = if (enabled) {
                lastCustomEndpointInput
            } else {
                providerSpec.officialEndpoint
            }
            val nextLastCustomEndpointInput = if (enabled) {
                lastCustomEndpointInput
            } else {
                endpointInput
            }
            copy(
                endpointOverrideEnabled = enabled,
                endpointInput = nextEndpointInput,
                lastCustomEndpointInput = nextLastCustomEndpointInput,
                saveEnabled = !isSaving && canSave(
                    endpointOverrideEnabled = enabled,
                    endpointInput = nextEndpointInput,
                ),
                inlineError = null,
            )
        }
    }

    private fun updateEndpoint(value: String) {
        updateState {
            copy(
                endpointInput = value,
                lastCustomEndpointInput = if (endpointOverrideEnabled) {
                    value
                } else {
                    lastCustomEndpointInput
                },
                saveEnabled = !isSaving && canSave(endpointInput = value),
                inlineError = null,
            )
        }
    }

    private fun updateModel(value: String) {
        updateState {
            copy(
                modelInput = value,
                saveEnabled = !isSaving && canSave(modelInput = value),
                inlineError = null,
            )
        }
    }

    private fun updateApiKey(value: String) {
        updateState {
            copy(
                apiKeyInput = value,
                saveEnabled = !isSaving && canSave(apiKeyInput = value),
                inlineError = null,
            )
        }
    }

    private fun toggleApiKeyVisibility() {
        updateState {
            copy(
                apiKeyVisible = !apiKeyVisible,
                inlineError = null,
            )
        }
    }

    private suspend fun save() {
        if (currentState.isSaving) {
            return
        }
        when (currentState.firstIncompleteField()) {
            ConfigureFieldTarget.Model -> {
                updateState { copy(inlineError = null) }
                sendEffect(ConfigureEffect.FocusModel)
                return
            }
            ConfigureFieldTarget.ApiKey -> {
                updateState { copy(inlineError = null) }
                sendEffect(ConfigureEffect.FocusApiKey)
                return
            }
            ConfigureFieldTarget.Endpoint -> {
                updateState { copy(inlineError = null) }
                sendEffect(ConfigureEffect.FocusEndpoint)
                return
            }
            null -> Unit
        }
        updateState {
            copy(
                isSaving = true,
                saveEnabled = false,
                inlineError = null,
            )
        }
        try {
            val latestSettings = loadSettings()
            saveSettings(buildUpdatedLocalSettings(latestSettings, currentState))
            updateState {
                copy(
                    isSaving = false,
                    saveEnabled = canSave(),
                    inlineError = null,
                )
            }
            sendEffect(ConfigureEffect.SaveSucceeded)
        } catch (throwable: Throwable) {
            if (throwable is CancellationException) throw throwable
            val message = throwable.message ?: throwable::class.java.simpleName
            val reason = ConfigureErrorReason.SaveSettingsFailed(message)
            updateState {
                copy(
                    isSaving = false,
                    saveEnabled = canSave(),
                    inlineError = ConfigureInlineError.SaveFailed(reason),
                )
            }
            sendEffect(ConfigureEffect.SaveFailed(reason))
        }
    }
}

internal fun buildUpdatedLocalSettings(
    settings: LocalSettings,
    state: ConfigureUiState,
): LocalSettings {
    val updatedProps = settings.props.toMutableMap()
    updatedProps["provider"] = JsonPrimitive(state.providerSpec.id)
    updatedProps["endpoint"] = JsonPrimitive(state.resolvedEndpoint())
    updatedProps["model"] = JsonPrimitive(state.modelInput)
    updatedProps["api_key"] = JsonPrimitive(state.apiKeyInput)
    return LocalSettings(JsonObject(updatedProps))
}

private fun ConfigureUiState.resolvedEndpoint(): String {
    return if (endpointOverrideEnabled) {
        endpointInput.trim().ifBlank { providerSpec.officialEndpoint }
    } else {
        providerSpec.officialEndpoint
    }
}

private fun ConfigureUiState.canSave(
    endpointOverrideEnabled: Boolean = this.endpointOverrideEnabled,
    endpointInput: String = this.endpointInput,
    modelInput: String = this.modelInput,
    apiKeyInput: String = this.apiKeyInput,
): Boolean {
    val endpointValid = !endpointOverrideEnabled || endpointInput.trim().isNotBlank()
    return endpointValid &&
        modelInput.trim().isNotBlank() &&
        apiKeyInput.trim().isNotBlank()
}

private enum class ConfigureFieldTarget {
    Endpoint,
    Model,
    ApiKey,
}

private fun ConfigureUiState.firstIncompleteField(): ConfigureFieldTarget? {
    return when {
        modelInput.trim().isBlank() -> ConfigureFieldTarget.Model
        apiKeyInput.trim().isBlank() -> ConfigureFieldTarget.ApiKey
        endpointOverrideEnabled && endpointInput.trim().isBlank() -> ConfigureFieldTarget.Endpoint
        else -> null
    }
}
