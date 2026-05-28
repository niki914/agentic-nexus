package com.niki914.nexus.agentic.app.ui.infra.component

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class SettingsDetailComposebaseSourceTest {
    private val defaultsFile = File(
        "src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/SettingsDetailPageDefaults.kt",
    )
    private val scaffoldFile = File(
        "src/main/java/com/niki914/nexus/agentic/app/ui/infra/component/SettingsDetailFormScaffold.kt",
    )

    @Test
    fun settings_detail_defaults_and_scaffold_follow_t01_t02_contract() {
        assertTrue(
            "缺少 T-01 文件: ${defaultsFile.path}",
            defaultsFile.exists(),
        )
        assertTrue(
            "缺少 T-02 文件: ${scaffoldFile.path}",
            scaffoldFile.exists(),
        )

        val defaultsSource = defaultsFile.readText()
        val scaffoldSource = scaffoldFile.readText()

        assertTrue(defaultsSource.contains("object SettingsDetailPageDefaults"))
        assertTrue(defaultsSource.contains("val HorizontalPadding"))
        assertTrue(defaultsSource.contains("val VerticalPadding"))
        assertTrue(defaultsSource.contains("val RootVerticalSpacing"))
        assertTrue(defaultsSource.contains("val ContentVerticalSpacing"))
        assertTrue(defaultsSource.contains("val InlineErrorHorizontalPadding"))
        assertTrue(defaultsSource.contains("val BottomSpacerHeight"))
        assertTrue(defaultsSource.contains("val DividerHorizontalPadding"))
        assertTrue(defaultsSource.contains("val DividerThickness"))
        assertTrue(defaultsSource.contains("@Composable"))
        assertTrue(
            defaultsSource.contains(
                "fun SettingsItemDivider(modifier: Modifier = Modifier)",
            ),
        )

        assertTrue(
            scaffoldSource.contains(
                "fun SettingsDetailFormScaffold(",
            ),
        )
        assertTrue(scaffoldSource.contains("topPadding: Dp"))
        assertTrue(scaffoldSource.contains("hazeState: HazeState"))
        assertTrue(scaffoldSource.contains("actionText: String"))
        assertTrue(scaffoldSource.contains("onActionClick: () -> Unit"))
        assertTrue(scaffoldSource.contains("description: String? = null"))
        assertTrue(scaffoldSource.contains("inlineErrorText: String? = null"))
        assertTrue(scaffoldSource.contains("actionEnabled: Boolean = true"))
        assertTrue(scaffoldSource.contains("onBackgroundTap: (() -> Unit)? = null"))
        assertTrue(
            scaffoldSource.contains(
                "content: @Composable ColumnScope.() -> Unit",
            ),
        )
        assertTrue(scaffoldSource.contains("TintLiquidButton("))
        assertTrue(scaffoldSource.contains("detectTapGestures"))
    }
}
