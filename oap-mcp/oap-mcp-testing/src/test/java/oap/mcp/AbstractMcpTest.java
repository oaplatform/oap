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
import oap.application.testng.KernelFixture;
import oap.testng.Fixtures;
import oap.testng.TestDirectoryFixture;

import java.net.http.HttpRequest;
import java.time.Duration;

import static oap.io.Resources.urlOrThrow;

public abstract class AbstractMcpTest extends Fixtures {
    protected static final KernelFixture kernel;

    static {
        TestDirectoryFixture tdf = Fixtures.suiteFixture( new TestDirectoryFixture() );
        kernel = Fixtures.suiteFixture( new KernelFixture( tdf, urlOrThrow( AbstractMcpTest.class, "/application-mcp.test.conf" ) ) );
    }

    protected McpSyncClient createClient() {
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
}
