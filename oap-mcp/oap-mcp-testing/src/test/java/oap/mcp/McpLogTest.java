/*
 *
 *  * Copyright (c) Xenoss
 *  * Unauthorized copying of this file, via any medium is strictly prohibited
 *  * Proprietary and confidential
 *
 *
 */

package oap.mcp;

import io.modelcontextprotocol.client.McpClient;
import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.HttpClientSseClientTransport;
import io.modelcontextprotocol.spec.McpSchema;
import oap.application.testng.KernelFixture;
import oap.testng.Fixtures;
import oap.testng.TestDirectoryFixture;
import org.testng.annotations.Test;

import java.net.http.HttpRequest;
import java.time.Duration;
import java.util.Map;

import static oap.io.Resources.urlOrThrow;
import static org.assertj.core.api.Assertions.assertThat;

public class McpLogTest extends Fixtures {
    private final KernelFixture kernel;

    public McpLogTest() {
        TestDirectoryFixture tdf = fixture( new TestDirectoryFixture() );
        kernel = fixture( new KernelFixture( tdf, urlOrThrow( getClass(), "/application-mcp.test.conf" ) ) );
    }

    private McpSyncClient createClient() {
        HttpClientSseClientTransport transport = HttpClientSseClientTransport
            .builder( kernel.httpUrl( "" ) )
            .sseEndpoint( "/mcp/sse" )
            .requestBuilder( HttpRequest.newBuilder()
                .header( "X-Access-Token", TestUserProvider.ACCESS_KEY )
                .header( "X-API-Key", TestUserProvider.API_KEY ) )
            .build();
        McpSyncClient client = McpClient.sync( transport )
            .requestTimeout( Duration.ofSeconds( 10 ) )
            .build();
        client.initialize();
        return client;
    }

    @Test
    public void listTools() {
        try( McpSyncClient client = createClient() ) {
            McpSchema.ListToolsResult result = client.listTools();
            assertThat( result.tools() )
                .extracting( McpSchema.Tool::name )
                .containsExactlyInAnyOrder( "getLoggers", "resetLogging", "setLogLevel" );
        }
    }

    @Test
    public void callGetLoggers() {
        try( McpSyncClient client = createClient() ) {
            McpSchema.CallToolResult result = client.callTool( new McpSchema.CallToolRequest( "getLoggers", Map.of() ) );
            assertThat( result.isError() ).isNotEqualTo( Boolean.TRUE );
            assertThat( ( ( McpSchema.TextContent ) result.content().get( 0 ) ).text() ).isNotBlank();
        }
    }

    @Test
    public void callGetLoggersAll() {
        try( McpSyncClient client = createClient() ) {
            McpSchema.CallToolResult result = client.callTool(
                new McpSchema.CallToolRequest( "getLoggers", Map.of( "all", "true" ) ) );
            assertThat( result.isError() ).isNotEqualTo( Boolean.TRUE );
            assertThat( ( ( McpSchema.TextContent ) result.content().get( 0 ) ).text() ).contains( "ROOT" );
        }
    }

    @Test
    public void callSetLogLevel() {
        try( McpSyncClient client = createClient() ) {
            McpSchema.CallToolResult result = client.callTool( new McpSchema.CallToolRequest( "setLogLevel",
                Map.of( "package", "oap", "level", "DEBUG" ) ) );
            assertThat( result.isError() ).isNotEqualTo( Boolean.TRUE );
        }
    }

    @Test
    public void listPrompts() {
        try( McpSyncClient client = createClient() ) {
            McpSchema.ListPromptsResult result = client.listPrompts();
            assertThat( result.prompts() )
                .extracting( McpSchema.Prompt::name )
                .containsExactlyInAnyOrder( "analyzeLogging", "diagnosePackage" );
        }
    }

    @Test
    public void getPromptAnalyzeLogging() {
        try( McpSyncClient client = createClient() ) {
            McpSchema.GetPromptResult result = client.getPrompt(
                new McpSchema.GetPromptRequest( "analyzeLogging", Map.of() ) );
            assertThat( ( ( McpSchema.TextContent ) result.messages().get( 0 ).content() ).text() ).isNotBlank();
        }
    }

    @Test
    public void getPromptDiagnosePackage() {
        try( McpSyncClient client = createClient() ) {
            McpSchema.GetPromptResult result = client.getPrompt(
                new McpSchema.GetPromptRequest( "diagnosePackage", Map.of( "package", "oap.ws" ) ) );
            assertThat( ( ( McpSchema.TextContent ) result.messages().get( 0 ).content() ).text() ).contains( "oap.ws" );
        }
    }
}
