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

package oap.fs;

import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.AnonymousAWSCredentials;
import com.amazonaws.client.builder.AwsClientBuilder;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import io.findify.s3mock.S3Mock;
import oap.io.Files;
import oap.testng.Env;
import org.testng.annotations.AfterClass;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.ByteArrayInputStream;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class S3FileManagerTest {
    private static final Path TEST_FOLDER = Path.of( "test/testDir" );
    private static final String TEST_BUCKET = "default";

    private S3Mock api;
    private S3FileManager fileManager;
    private Path tmp = Env.tmpPath( "s3" );

    @BeforeClass
    public void setUp() throws Exception {
        var port = Env.port( "s3" );

        api = new S3Mock.Builder().withPort( 9090 ).withInMemoryBackend().build();
        api.start();
        var endpoint = new AwsClientBuilder.EndpointConfiguration( "http://localhost:" + port, "us-west-2" );
        var client = AmazonS3ClientBuilder
            .standard()
            .withPathStyleAccessEnabled( true )
            .withEndpointConfiguration( endpoint )
            .withCredentials( new AWSStaticCredentialsProvider( new AnonymousAWSCredentials() ) )
            .build();

        fileManager = new S3FileManager( Map.of( TEST_BUCKET, TEST_FOLDER ), client );
        client.createBucket( TEST_BUCKET );
    }

    @AfterClass
    public void tearDown() {
        Files.cleanDirectory( tmp );
        api.stop();
    }


    @Test
    public void readWrite() throws Exception {
        var inputBytes = Base64.getDecoder().decode( "dGVzdA==" );
        var data = new S3Data( "file.txt", new ByteArrayInputStream( inputBytes ), ( long ) inputBytes.length );
        var resultFile = tmp.resolve( "output.txt" ).toFile();

        var upload = fileManager.uploadStream( TEST_BUCKET, data ).waitForUploadResult();
        fileManager.download( TEST_BUCKET, upload.getKey(), resultFile ).waitForCompletion();

        assertThat( resultFile.exists() ).isTrue();
        assertThat( Files.readString( resultFile.toPath() ) ).isEqualTo( "test" );
    }
}
