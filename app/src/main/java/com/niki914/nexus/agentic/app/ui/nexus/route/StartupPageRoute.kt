package com.niki914.nexus.agentic.app.ui.nexus.route

import android.content.res.Configuration
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.niki914.nexus.agentic.app.R
import com.niki914.nexus.agentic.app.ui.infra.ConfirmationLiquidDialog
import com.niki914.nexus.agentic.app.ui.infra.ProvideLiquidScreenContentForPreview
import com.niki914.nexus.agentic.app.ui.nexus.content.StartupPageContent
import com.niki914.nexus.agentic.app.ui.nexus.model.StartupAssistantUi
import com.niki914.nexus.agentic.app.ui.nexus.nav.HomePage
import com.niki914.nexus.agentic.app.ui.nexus.nav.NexusPage
import com.niki914.nexus.agentic.app.ui.nexus.nav.ProviderPickPage
import com.niki914.nexus.agentic.repo.WebSettingsFailureReason
import com.niki914.nexus.agentic.repo.WebSettingsResult
import com.niki914.nexus.agentic.repo.XRepo
import com.niki914.nexus.cb.BaseTheme
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

internal typealias WebSettingsLoader = suspend (forceRetry: Boolean) -> WebSettingsResult

@Composable
internal fun StartupPageRoute(
    startupAssistantUi: StartupAssistantUi,
    onPush: (NexusPage) -> Unit,
    loadWebSettings: WebSettingsLoader = { forceRetry ->
        if (forceRetry) {
            XRepo.web.retry()
        } else {
            XRepo.web.await()
        }
    },
    initialLoading: Boolean = false,
    initialDialog: StartupWebSettingsDialog? = null,
) {
    val scope = rememberCoroutineScope()
    var isCheckingWebSettings by rememberSaveable {
        mutableStateOf(initialLoading)
    }
    var webSettingsDialog by rememberSaveable {
        mutableStateOf(initialDialog)
    }

    fun nextPage(): NexusPage {
        return when (startupAssistantUi) {
            StartupAssistantUi.Breeno,
            StartupAssistantUi.XiaoAi -> ProviderPickPage

            StartupAssistantUi.ChatOnly -> HomePage
        }
    }

    fun enterNextPage() {
        webSettingsDialog = null
        onPush(nextPage())
    }

    fun handleWebSettingsResult(result: WebSettingsResult) {
        isCheckingWebSettings = false
        when (result) {
            is WebSettingsResult.Success -> {
                webSettingsDialog = when {
                    result.isFallbackVersion -> StartupWebSettingsDialog.UnsupportedVersion
                    result.settings.isBeta -> StartupWebSettingsDialog.Beta
                    else -> null
                }
                if (webSettingsDialog == null) {
                    enterNextPage()
                }
            }

            is WebSettingsResult.RequestFailed -> {
                webSettingsDialog = when (result.reason) {
                    WebSettingsFailureReason.NetworkUnavailable -> StartupWebSettingsDialog.NetworkError
                    WebSettingsFailureReason.ServerError,
                    WebSettingsFailureReason.UnsupportedVersion,
                    WebSettingsFailureReason.InvalidConfig -> StartupWebSettingsDialog.FetchFailed
                }
            }
        }
    }

    fun requestWebSettings(forceRetry: Boolean) {
        if (isCheckingWebSettings) {
            return
        }
        isCheckingWebSettings = true
        webSettingsDialog = null
        scope.launch {
            val result = loadWebSettings(forceRetry)
            handleWebSettingsResult(result)
        }
    }

    StartupPageContent(
        assistantUi = startupAssistantUi,
        isLoading = isCheckingWebSettings,
        onContinue = {
            if (startupAssistantUi == StartupAssistantUi.ChatOnly) {
                enterNextPage()
            } else {
                requestWebSettings(forceRetry = false)
            }
        },
    )

    webSettingsDialog?.let { dialog ->
        StartupWebSettingsDialogContent(
            dialog = dialog,
            onEnterNextPage = ::enterNextPage,
            onRetry = {
                requestWebSettings(forceRetry = true)
            },
            onDismiss = {
                webSettingsDialog = null
            },
        )
    }
}

@Composable
private fun StartupWebSettingsDialogContent(
    dialog: StartupWebSettingsDialog,
    onEnterNextPage: () -> Unit,
    onRetry: () -> Unit,
    onDismiss: () -> Unit,
) {
    val titleRes = when (dialog) {
        StartupWebSettingsDialog.Beta -> R.string.ui_onboard_web_beta_title
        StartupWebSettingsDialog.FetchFailed -> R.string.ui_onboard_web_fetch_failed_title
        StartupWebSettingsDialog.UnsupportedVersion -> R.string.ui_onboard_web_unsupported_title
        StartupWebSettingsDialog.NetworkError -> R.string.ui_onboard_web_network_error_title
    }
    val bodyRes = when (dialog) {
        StartupWebSettingsDialog.Beta -> R.string.ui_onboard_web_beta_body
        StartupWebSettingsDialog.FetchFailed -> R.string.ui_onboard_web_fetch_failed_body
        StartupWebSettingsDialog.UnsupportedVersion -> R.string.ui_onboard_web_unsupported_body
        StartupWebSettingsDialog.NetworkError -> R.string.ui_onboard_web_network_error_body
    }
    val positiveTextRes = when (dialog) {
        StartupWebSettingsDialog.Beta,
        StartupWebSettingsDialog.UnsupportedVersion -> R.string.ui_onboard_web_confirm

        StartupWebSettingsDialog.FetchFailed,
        StartupWebSettingsDialog.NetworkError -> R.string.ui_onboard_web_enter_directly
    }
    val negativeTextRes = when (dialog) {
        StartupWebSettingsDialog.Beta,
        StartupWebSettingsDialog.UnsupportedVersion,
        StartupWebSettingsDialog.FetchFailed,
        StartupWebSettingsDialog.NetworkError -> R.string.ui_onboard_web_retry
    }
    val onPositiveClick = when (dialog) {
        StartupWebSettingsDialog.Beta,
        StartupWebSettingsDialog.UnsupportedVersion,
        StartupWebSettingsDialog.FetchFailed,
        StartupWebSettingsDialog.NetworkError -> onEnterNextPage
    }
    val onNegativeClick = when (dialog) {
        StartupWebSettingsDialog.Beta,
        StartupWebSettingsDialog.UnsupportedVersion,
        StartupWebSettingsDialog.FetchFailed,
        StartupWebSettingsDialog.NetworkError -> onRetry
    }

    ConfirmationLiquidDialog(
        visible = true,
        onDismissRequest = onDismiss,
        title = stringResource(titleRes),
        text = stringResource(bodyRes),
        positiveButtonText = stringResource(positiveTextRes),
        negativeButtonText = stringResource(negativeTextRes),
        onPositiveClick = onPositiveClick,
        onNegativeClick = onNegativeClick,
        dismissOnBackgroundTap = false,
    )
}

internal enum class StartupWebSettingsDialog {
    Beta,
    FetchFailed,
    UnsupportedVersion,
    NetworkError,
}

@Preview(name = "Startup Normal", showBackground = true, widthDp = 420, heightDp = 900)
@Composable
private fun StartupPageRouteNormalPreview() {
    StartupPageRoutePreview()
}

@Preview(name = "Startup Loading", showBackground = true, widthDp = 420, heightDp = 900)
@Composable
private fun StartupPageRouteLoadingPreview() {
    StartupPageRoutePreview(initialLoading = true)
}

@Preview(name = "Startup Dialog Beta", showBackground = true, widthDp = 420, heightDp = 900)
@Composable
private fun StartupPageRouteBetaDialogPreview() {
    StartupPageRoutePreview(initialDialog = StartupWebSettingsDialog.Beta)
}

@Preview(name = "Startup Dialog Unsupported", showBackground = true, widthDp = 420, heightDp = 900)
@Composable
private fun StartupPageRouteUnsupportedDialogPreview() {
    StartupPageRoutePreview(initialDialog = StartupWebSettingsDialog.UnsupportedVersion)
}

@Preview(name = "Startup Dialog Fetch Failed", showBackground = true, widthDp = 420, heightDp = 900)
@Composable
private fun StartupPageRouteFetchFailedDialogPreview() {
    StartupPageRoutePreview(initialDialog = StartupWebSettingsDialog.FetchFailed)
}

@Preview(name = "Startup Dialog Network Error", showBackground = true, widthDp = 420, heightDp = 900)
@Composable
private fun StartupPageRouteNetworkErrorDialogPreview() {
    StartupPageRoutePreview(initialDialog = StartupWebSettingsDialog.NetworkError)
}

@Preview(
    name = "Startup Dialog Network Error Dark",
    showBackground = true,
    widthDp = 420,
    heightDp = 900,
    uiMode = Configuration.UI_MODE_NIGHT_YES,
)
@Composable
private fun StartupPageRouteNetworkErrorDarkDialogPreview() {
    StartupPageRoutePreview(
        darkTheme = true,
        initialDialog = StartupWebSettingsDialog.NetworkError,
    )
}

@Composable
private fun StartupPageRoutePreview(
    darkTheme: Boolean = false,
    initialLoading: Boolean = false,
    initialDialog: StartupWebSettingsDialog? = null,
) {
    BaseTheme(darkTheme = darkTheme, dynamicColor = false) {
        Surface {
            ProvideLiquidScreenContentForPreview(topPadding = 0.dp) {
                StartupPageRoute(
                    startupAssistantUi = StartupAssistantUi.XiaoAi,
                    onPush = {},
                    loadWebSettings = previewLoadingWebSettingsLoader(),
                    initialLoading = initialLoading,
                    initialDialog = initialDialog,
                )
            }
        }
    }
}

private fun previewLoadingWebSettingsLoader(): WebSettingsLoader = {
    delay(60_000L)
    WebSettingsResult.RequestFailed(WebSettingsFailureReason.NetworkUnavailable)
}
