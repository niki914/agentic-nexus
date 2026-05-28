package com.niki914.nexus.agentic.app.ui.nexus

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

class McpStringsResourceFileTest {
    @Test
    fun stringsXml_containsRequiredMcpResourceKeys() {
        val xml = File("src/main/res/values/strings.xml").readText()

        assertTrue(xml.contains("""name="mcp_field_headers""""))
        assertTrue(xml.contains("""name="mcp_field_name_hint""""))
        assertTrue(xml.contains("""name="mcp_field_url_hint""""))
        assertTrue(xml.contains("""name="mcp_field_headers_hint""""))
        assertTrue(xml.contains("""name="mcp_error_name_required""""))
        assertTrue(xml.contains("""name="mcp_error_name_duplicate""""))
        assertTrue(xml.contains("""name="mcp_error_url_required""""))
        assertTrue(xml.contains("""name="mcp_error_url_invalid""""))
        assertTrue(xml.contains("""name="mcp_error_headers_invalid_json""""))
        assertTrue(xml.contains("""name="mcp_error_headers_not_object""""))
        assertTrue(xml.contains("""name="mcp_error_headers_empty_key""""))
        assertTrue(xml.contains("""name="mcp_error_headers_non_string""""))
        assertTrue(xml.contains("""name="mcp_field_enabled""""))
    }
}
