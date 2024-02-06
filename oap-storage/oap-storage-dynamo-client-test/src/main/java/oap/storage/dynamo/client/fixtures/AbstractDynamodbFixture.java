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

import lombok.extern.slf4j.Slf4j;
import oap.storage.dynamo.client.DynamodbClient;
import oap.system.Env;
import oap.testng.AbstractEnvFixture;

import java.net.MalformedURLException;
import java.net.URISyntaxException;

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
public abstract class AbstractDynamodbFixture extends AbstractEnvFixture<AbstractDynamodbFixture> {

    public static final String DYNAMODB_PROTOCOL = Env.get( "DYNAMODB_PROTOCOL", "http" );
    public static final String DYNAMODB_HOSTS = Env.get( "DYNAMODB_HOSTS", "localhost" );

    public static final String DYNAMODB_PORT = "" + Integer.parseInt( Env.get( "DYNAMODB_PORT", "8000" ) );
    public static final String AWS_ACCESS_KEY_ID = Env.get( "AWS_ACCESS_KEY_ID", "dummy" );
    public static final String AWS_SECRET_ACCESS_KEY = Env.get( "AWS_SECRET_ACCESS_KEY", "dummy" );
    public static final String AWS_REGION = Env.get( "AWS_REGION", "US_EAST_1" );
    public final int maxConnsPerNode;
    public final int connPoolsPerNode;
    private DynamodbClient dynamodbClient = null;
    private boolean skipBeforeAndAfter = false;

    public AbstractDynamodbFixture( ) {
        this( 300, 1, false );
    }

    public AbstractDynamodbFixture( boolean skipBeforeAndAfter ) {
        this( 300, 1, skipBeforeAndAfter );
    }

    public AbstractDynamodbFixture( int maxConnsPerNode, int connPoolsPerNode, boolean skipBeforeAndAfter ) {
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
    }

    @Override
    protected void before() throws RuntimeException {
        super.before();
        if ( skipBeforeAndAfter ) return;
        try {
            dynamodbClient = createClient();
            asDeleteAll();
        } catch( Exception ex ) {
            throw new RuntimeException( ex );
        }
    }

    protected abstract DynamodbClient createClient() throws URISyntaxException, MalformedURLException;

    @Override
    protected void after() {
        try {
            if ( skipBeforeAndAfter ) return;
            asDeleteAll();
            dynamodbClient.close();
        } catch( Exception e ) {
            throw new RuntimeException( e );
        } finally {
            super.after();
        }
    }

    public void asDeleteAll( ) {
        var ret = dynamodbClient.getTables();
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
