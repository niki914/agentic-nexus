package com.niki914.zafiro.app.trigger

import com.niki914.zafiro.repo.TextAction
import com.niki914.zafiro.repo.TextActionsCodec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TextActionPromptComposerTest {

    @Test
    fun compose_quotesSelectionAndAppendsTemplate() {
        val result = TextActionPromptComposer.compose(
            template = "Translate the following text into English.",
            selectedText = "Hello world",
        )

        assertEquals(
            """
            > [NOTE]: The text below was copied from another app.
            >
            > Hello world

            Translate the following text into English.
            """.trimIndent(),
            result,
        )
    }

    @Test
    fun compose_quotesEveryLineOfMultilineSelection() {
        val result = TextActionPromptComposer.compose(
            template = "Summarize.",
            selectedText = "line one\nline two",
        )

        assertEquals(
            """
            > [NOTE]: The text below was copied from another app.
            >
            > line one
            > line two

            Summarize.
            """.trimIndent(),
            result,
        )
    }

    @Test
    fun compose_withoutSelectionReturnsTemplateOnly() {
        assertEquals(
            "Summarize.",
            TextActionPromptComposer.compose("Summarize.", null),
        )
        assertEquals(
            "Summarize.",
            TextActionPromptComposer.compose("Summarize.", "   "),
        )
    }
}

class TextSelectionInputTest {

    @Test
    fun parse_nullIsRejected() {
        assertNull(TextSelectionInput.parse(null))
    }

    @Test
    fun parse_blankIsRejected() {
        assertNull(TextSelectionInput.parse("   \n\t "))
    }

    @Test
    fun parse_invisibleCharactersOnlyIsRejected() {
        // BOM + zero-width space + word joiner
        assertNull(TextSelectionInput.parse("\uFEFF\u200B\u2060"))
    }

    @Test
    fun parse_stripsBomAndNormalizesLineEndings() {
        assertEquals("a\nb", TextSelectionInput.parse("\uFEFFa\r\nb"))
    }

    @Test
    fun parse_preservesLeadingWhitespaceOfRealContent() {
        // 段落缩进是内容，trim 只用于判空
        assertEquals("    indented", TextSelectionInput.parse("    indented"))
    }
}

class TextActionsCodecTest {

    @Test
    fun roundTripPreservesFields() {
        val actions = listOf(
            TextAction(id = "explain", name = "Explain", promptTemplate = "Explain it."),
            TextAction(
                id = "custom-1",
                name = "Custom",
                promptTemplate = "Do the thing.",
                enabled = false,
            ),
        )

        assertEquals(actions, TextActionsCodec.parse(TextActionsCodec.encode(actions)))
    }

    @Test
    fun parse_skipsEntriesWithoutIdNameOrPrompt() {
        val json = """
            {
              "actions": [
                {"id": "ok", "name": "Ok", "prompt": "Fine."},
                {"id": "", "name": "NoId", "prompt": "x"},
                {"id": "noName", "name": "  ", "prompt": "x"},
                {"id": "noPrompt", "name": "NoPrompt", "prompt": "  "}
              ]
            }
        """.trimIndent()

        assertEquals(
            listOf(TextAction(id = "ok", name = "Ok", promptTemplate = "Fine.")),
            TextActionsCodec.parse(json),
        )
    }

    @Test
    fun parse_malformedJsonFallsBackToEmpty() {
        assertEquals(emptyList<TextAction>(), TextActionsCodec.parse("not json"))
    }
}
