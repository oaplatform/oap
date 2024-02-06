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

import com.github.dockerjava.api.command.CreateContainerCmd;
import com.github.dockerjava.api.model.ExposedPort;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.PortBinding;
import com.github.dockerjava.api.model.Ports;
import lombok.extern.slf4j.Slf4j;
import oap.storage.dynamo.client.DynamodbClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.dynamodb.DynamoDbClient;

import java.net.URI;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Slf4j
public class TestContainerDynamodbFixture extends AbstractDynamodbFixture {
    private volatile GenericContainer<?> genericContainer;
    protected URI uri;
    protected StaticCredentialsProvider provider;

    @Override
    protected DynamodbClient createClient() {
        log.info( "Starting a test container's client with endpoint URL: {}",
            "http://localhost:" + genericContainer.getFirstMappedPort() );
        uri = URI.create( "http://localhost:" + genericContainer.getFirstMappedPort() );
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

    @BeforeClass
    public void beforeClass() {
        if( genericContainer == null ) {
            var portBinding = new PortBinding(
                Ports.Binding.bindPort( 8000 ),
                new ExposedPort( 8000 ) );
            Consumer<CreateContainerCmd> cmd = e -> e.withHostConfig( new HostConfig().withPortBindings( portBinding ) );
            GenericContainer<?> container = new GenericContainer<>( DockerImageName
                .parse( "amazon/dynamodb-local" ) )
                .withCommand( "-jar DynamoDBLocal.jar -inMemory -sharedDb" )
                .withExposedPorts( 8000 )
                .withCreateContainerCmdModifier( cmd );
            container.start();
            genericContainer = container;
            log.info( "Container {} started, listening to {}", genericContainer.getContainerId(), genericContainer.getFirstMappedPort() );
        }
    }

    @AfterClass
    public void afterClass() {
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
}
