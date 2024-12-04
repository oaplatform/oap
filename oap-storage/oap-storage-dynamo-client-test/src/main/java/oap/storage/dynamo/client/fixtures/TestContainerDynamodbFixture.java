/*
 * The MIT License (MIT)
 *
 * Copyright (c) Open Application Platform Authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package oap.storage.dynamo.client.fixtures;

import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import oap.storage.dynamo.client.DynamodbClient;
import oap.system.Env;
import oap.testng.AbstractFixture;
import oap.util.Result;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.net.URI;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Env:*
 * DYNAMODB_PROTOCOL
 * DYNAMODB_HOSTS
 * DYNAMODB_PORT
 * AWS_ACCESS_KEY_ID
 * AWS_SECRET_ACCESS_KEY
 * AWS_REGION
 */
@Slf4j
public class TestContainerDynamodbFixture extends AbstractFixture<TestContainerDynamodbFixture> {
    public static final String DYNAMODB_LOCAL_VERSION = "2.5.3";

    public static final String DYNAMODB_PROTOCOL = Env.get( "DYNAMODB_PROTOCOL", "http" );
    public static final String DYNAMODB_HOSTS = Env.get( "DYNAMODB_HOSTS", "localhost" );

    public static final String DYNAMODB_PORT = "" + Integer.parseInt( Env.get( "DYNAMODB_PORT", "8000" ) );
    public static final String AWS_ACCESS_KEY_ID = Env.get( "AWS_ACCESS_KEY_ID", "dummy" );
    public static final String AWS_SECRET_ACCESS_KEY = Env.get( "AWS_SECRET_ACCESS_KEY", "dummy" );
    public static final String AWS_REGION = Env.get( "AWS_REGION", "US_EAST_1" );
    public final int maxConnsPerNode;
    public final int connPoolsPerNode;
    @Getter
    private final int port;
    protected URI uri;
    protected StaticCredentialsProvider provider;
    private DynamodbClient dynamodbClient = null;
    private boolean skipBeforeAndAfter = false;
    private volatile GenericContainer<?> genericContainer;

    public TestContainerDynamodbFixture() {
        this( 300, 1, false );
    }

    public TestContainerDynamodbFixture( boolean skipBeforeAndAfter ) {
        this( 300, 1, skipBeforeAndAfter );
    }

    public TestContainerDynamodbFixture( int maxConnsPerNode, int connPoolsPerNode, boolean skipBeforeAndAfter ) {
        this.maxConnsPerNode = maxConnsPerNode;
        this.connPoolsPerNode = connPoolsPerNode;
        this.skipBeforeAndAfter = skipBeforeAndAfter;
        log.info( "Setting up environment variables: \n\t{} = {}\n\t{} = {}\n\t{} = {}\n\t{} = {}\n\t{} = {}\n\t{} = {}",
            "DYNAMODB_PROTOCOL", DYNAMODB_PROTOCOL,
            "DYNAMODB_HOSTS", DYNAMODB_HOSTS,
            "DYNAMODB_PORT", DYNAMODB_PORT,
            "AWS_ACCESS_KEY_ID", AWS_ACCESS_KEY_ID,
            "AWS_SECRET_ACCESS_KEY", AWS_SECRET_ACCESS_KEY,
            "AWS_REGION", AWS_REGION
        );
        define( "DYNAMODB_PROTOCOL", DYNAMODB_PROTOCOL );
        define( "DYNAMODB_HOSTS", DYNAMODB_HOSTS );
        define( "DYNAMODB_PORT", DYNAMODB_PORT );
        define( "AWS_ACCESS_KEY_ID", AWS_ACCESS_KEY_ID );
        define( "AWS_SECRET_ACCESS_KEY", AWS_SECRET_ACCESS_KEY );
        define( "AWS_REGION", AWS_REGION );

        port = definePort( "PORT" );
    }

    protected DynamodbClient createClient() {
        log.info( "Starting a test container's client with endpoint URL: http://localhost:{}", port );
        uri = URI.create( "http://localhost:" + port );
        provider = StaticCredentialsProvider.create(
            AwsBasicCredentials.create( AWS_ACCESS_KEY_ID, AWS_SECRET_ACCESS_KEY )
        );
        log.info( "AWS region: {}", AWS_REGION );
        log.info( "Creating DynamoDB client..." );
        DynamoDbClient dynamoDbAsyncClient = DynamoDbClient.builder()
            .region( Region.of( AWS_REGION ) )
            .endpointOverride( uri )
            .credentialsProvider( provider )
            .build();
        log.info( "DynamoDbClient is ready" );
        DynamodbClient dynamodbClient = new DynamodbClient( dynamoDbAsyncClient );
        dynamodbClient.setStreamClient( dynamodbClient.createStreamClient( uri, provider, Region.US_EAST_1 ) );
        return dynamodbClient;
    }

    public void before() {
        PortBinding portBinding = new PortBinding(
            Ports.Binding.bindPort( port ),
            new ExposedPort( 8000 ) );
        GenericContainer<?> container = new GenericContainer<>( DockerImageName
            .parse( "amazon/dynamodb-local:" + DYNAMODB_LOCAL_VERSION ) )
            .withCommand( "-jar DynamoDBLocal.jar -inMemory -sharedDb" )
            .withExposedPorts( 8000 )
            .withCreateContainerCmdModifier( cmd -> cmd.getHostConfig().withPortBindings( portBinding ) );
        container.start();
        genericContainer = container;
        log.info( "Container {} started, listening to {}", genericContainer.getContainerId(), port );

        if( skipBeforeAndAfter ) return;
        try {
            dynamodbClient = createClient();
            asDeleteAll();
        } catch( Exception ex ) {
            throw new RuntimeException( ex );
        }
    }

    public void after() {
        try {
            if( skipBeforeAndAfter ) return;
            asDeleteAll();
            dynamodbClient.close();
        } catch( Exception e ) {
            throw new RuntimeException( e );
        } finally {
            super.after();
        }

        if( genericContainer != null ) {
            genericContainer.stop();
            log.info( "Container stopped" );
            try {
                TimeUnit.SECONDS.sleep( 10 );
            } catch( InterruptedException e ) {
                Thread.currentThread().interrupt();
                throw new RuntimeException( e );
            }
            genericContainer = null;
            uri = null;
        }
    }

    public void asDeleteAll() {
        Result<List<String>, DynamodbClient.State> ret = dynamodbClient.getTables();
        ret.ifSuccess( tables -> {
            for( var table : tables ) {
                dynamodbClient.deleteTable( table );
            }
        } );
    }

    public DynamodbClient getDynamodbClient() {
        return dynamodbClient;
    }
}
