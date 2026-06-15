/*
 *
 *  * Copyright (c) Xenoss
 *  * Unauthorized copying of this file, via any medium is strictly prohibited
 *  * Proprietary and confidential
 *
 *
 */

package oap.mcp;

import io.modelcontextprotocol.spec.McpSchema;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class McpJPathTest extends AbstractMcpTest {
    @Test
    public void listTools() {
        try( var client = createClient() ) {
            McpSchema.ListToolsResult result = client.listTools();
            assertThat( result.tools() )
                .extracting( McpSchema.Tool::name )
                .containsAll( List.of( "listServices", "queryService" ) );
        }
    }

    @Test
    public void callListServices() {
        try( var client = createClient() ) {
            McpSchema.CallToolResult result = client.callTool( new McpSchema.CallToolRequest( "listServices", Map.of() ) );
            assertThat( result.isError() ).isFalse();
            assertThat( ( ( McpSchema.TextContent ) result.content().getFirst() ).text() ).isNotBlank();
        }
    }

    @Test
    public void callListServicesWithPattern() {
        try( var client = createClient() ) {
            McpSchema.CallToolResult result = client.callTool(
                new McpSchema.CallToolRequest( "listServices", Map.of( "pattern", "*mcp*" ) ) );
            assertThat( result.isError() ).isFalse();
            assertThat( ( ( McpSchema.TextContent ) result.content().getFirst() ).text() ).contains( "oap-mcp-admin.mcp-jpath" );
        }
    }

    @Test
    public void callQueryService() {
        try( var client = createClient() ) {
            McpSchema.CallToolResult result = client.callTool(
                new McpSchema.CallToolRequest( "queryService", Map.of( "query", "oap-mcp-admin.mcp-jpath" ) ) );
            assertThat( result.isError() ).isFalse();
        }
    }

    @Test
    public void listPrompts() {
        try( var client = createClient() ) {
            McpSchema.ListPromptsResult result = client.listPrompts();
            assertThat( result.prompts() )
                .extracting( McpSchema.Prompt::name )
                .containsAll( List.of( "inspectService" ) );
        }
    }

    @Test
    public void getPromptInspectService() {
        try( var client = createClient() ) {
            McpSchema.GetPromptResult result = client.getPrompt(
                new McpSchema.GetPromptRequest( "inspectService", Map.of( "service", "jpath" ) ) );
            assertThat( ( ( McpSchema.TextContent ) result.messages().getFirst().content() ).text() ).contains( "jpath" );
        }
    }
}
