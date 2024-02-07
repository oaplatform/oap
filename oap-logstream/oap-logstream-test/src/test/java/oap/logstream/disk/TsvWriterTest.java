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

package oap.logstream.disk;

import oap.io.Files;
import oap.io.content.ContentWriter;
import oap.logstream.LogId;
import oap.template.BinaryUtils;
import oap.template.Types;
import oap.testng.Fixtures;
import oap.testng.TestDirectoryFixture;
import oap.util.Dates;
import oap.util.LinkedHashMaps;
import org.testng.annotations.Test;

import java.io.IOException;
import java.util.Map;

import static oap.io.IoStreams.Encoding.GZIP;
import static oap.io.IoStreams.Encoding.PLAIN;
import static oap.logstream.LogStreamProtocol.CURRENT_PROTOCOL_VERSION;
import static oap.logstream.LogStreamProtocol.ProtocolVersion.TSV_V1;
import static oap.logstream.Timestamp.BPH_12;
import static oap.testng.Asserts.assertFile;
import static oap.testng.TestDirectoryFixture.testPath;

public class TsvWriterTest extends Fixtures {
    private static final String FILE_PATTERN = "<p>-file-<INTERVAL>-<LOG_VERSION>-<if(ORGANIZATION)><ORGANIZATION><else>UNKNOWN<endif>.log.gz";

    public TsvWriterTest() {
        fixture( TestDirectoryFixture.FIXTURE );
    }

    @Test
    public void testEscape() throws IOException {
        var headers = new String[] { "RAW" };
        var types = new byte[][] { new byte[] { Types.STRING.id } };

        Dates.setTimeFixed( 2015, 10, 10, 1, 0 );
        var content = "1\n2\n\r3\t4";
        var bytes = BinaryUtils.line( content );
        var logs = testPath( "logs" );

        try( var writer = new TsvWriter( logs, FILE_PATTERN,
            new LogId( "", "type", "log", LinkedHashMaps.of( "p", "1" ), headers, types ),
            new WriterConfiguration.TsvConfiguration(), 10, BPH_12, 20 ) ) {

            writer.write( CURRENT_PROTOCOL_VERSION, bytes, msg -> {} );
        }

        assertFile( logs.resolve( "1-file-00-198163-1-UNKNOWN.log.gz" ) )
            .hasContent( "RAW\n1\\n2\\n\\r3\\t4\n", GZIP );
    }

    @Test
    public void metadataChanged() throws IOException {
        var headers = new String[] { "REQUEST_ID" };
        var types = new byte[][] { new byte[] { Types.STRING.id } };

        Dates.setTimeFixed( 2015, 10, 10, 1, 0 );
        var content = "1234567890";
        var bytes = BinaryUtils.line( content );
        var logs = testPath( "logs" );

        var writer = new TsvWriter( logs, FILE_PATTERN,
            new LogId( "", "type", "log", LinkedHashMaps.of( "p", "1" ), headers, types ),
            new WriterConfiguration.TsvConfiguration(), 10, BPH_12, 20 );

        writer.write( CURRENT_PROTOCOL_VERSION, bytes, msg -> {} );

        writer.close();

        writer = new TsvWriter( logs, FILE_PATTERN,
            new LogId( "", "type", "log", LinkedHashMaps.of( "p", "1", "p2", "2" ), headers, types ),
            new WriterConfiguration.TsvConfiguration(), 10, BPH_12, 20 );
        writer.write( CURRENT_PROTOCOL_VERSION, bytes, msg -> {} );

        writer.close();

        assertFile( logs.resolve( "1-file-00-80723ad6-1-UNKNOWN.log.gz" ) )
            .hasContent( "REQUEST_ID\n" + content + "\n", GZIP );
        assertFile( logs.resolve( "1-file-00-80723ad6-1-UNKNOWN.log.gz.metadata.yaml" ) )
            .hasContent( """
                ---
                filePrefixPattern: ""
                type: "type"
                clientHostname: "log"
                headers:
                - "REQUEST_ID"
                types:
                - - 11
                p: "1"
                VERSION: "80723ad6-1"
                """ );

        assertFile( logs.resolve( "1-file-00-80723ad6-2-UNKNOWN.log.gz" ) )
            .hasContent( "REQUEST_ID\n" + content + "\n", GZIP );
        assertFile( logs.resolve( "1-file-00-80723ad6-2-UNKNOWN.log.gz.metadata.yaml" ) )
            .hasContent( """
                ---
                filePrefixPattern: ""
                type: "type"
                clientHostname: "log"
                headers:
                - "REQUEST_ID"
                types:
                - - 11
                p: "1"
                p2: "2"
                VERSION: "80723ad6-2"
                """ );

    }

    @Test
    public void write() throws IOException {
        var headers = new String[] { "REQUEST_ID" };
        var types = new byte[][] { new byte[] { Types.STRING.id } };
        var newHeaders = new String[] { "REQUEST_ID", "H2" };
        var newTypes = new byte[][] { new byte[] { Types.STRING.id }, new byte[] { Types.STRING.id } };

        Dates.setTimeFixed( 2015, 10, 10, 1, 0 );
        var content = "1234567890";
        var bytes = BinaryUtils.line( content );
        var logs = testPath( "logs" );
        Files.write(
            logs.resolve( "1-file-00-80723ad6-1-UNKNOWN.log.gz" ),
            PLAIN, "corrupted file", ContentWriter.ofString() );
        Files.write(
            logs.resolve( "1-file-00-80723ad6-1-UNKNOWN.log.gz.metadata.yaml" ),
            PLAIN, """
                ---
                filePrefixPattern: ""
                type: "type"
                clientHostname: "log"
                headers: "REQUEST_ID"
                p: "1"
                VERSION: "80723ad6-1"
                """, ContentWriter.ofString() );

        var writer = new TsvWriter( logs, FILE_PATTERN,
            new LogId( "", "type", "log", Map.of( "p", "1" ), headers, types ),
            new WriterConfiguration.TsvConfiguration(), 10, BPH_12, 20 );

        writer.write( CURRENT_PROTOCOL_VERSION, bytes, msg -> {} );

        Dates.setTimeFixed( 2015, 10, 10, 1, 5 );
        writer.write( CURRENT_PROTOCOL_VERSION, bytes, msg -> {} );

        Dates.setTimeFixed( 2015, 10, 10, 1, 10 );
        writer.write( CURRENT_PROTOCOL_VERSION, bytes, msg -> {
        } );

        writer.close();

        writer = new TsvWriter( logs, FILE_PATTERN,
            new LogId( "", "type", "log", Map.of( "p", "1" ), headers, types ),
            new WriterConfiguration.TsvConfiguration(), 10, BPH_12, 20 );

        Dates.setTimeFixed( 2015, 10, 10, 1, 14 );
        writer.write( CURRENT_PROTOCOL_VERSION, bytes, msg -> {} );

        Dates.setTimeFixed( 2015, 10, 10, 1, 59 );
        writer.write( CURRENT_PROTOCOL_VERSION, bytes, msg -> {} );
        writer.close();

        writer = new TsvWriter( logs, FILE_PATTERN,
            new LogId( "", "type", "log", Map.of( "p", "1" ), newHeaders, newTypes ),
            new WriterConfiguration.TsvConfiguration(), 10, BPH_12, 20 );

        Dates.setTimeFixed( 2015, 10, 10, 1, 14 );
        writer.write( CURRENT_PROTOCOL_VERSION, bytes, msg -> {} );
        writer.close();


        assertFile( logs.resolve( "1-file-01-80723ad6-1-UNKNOWN.log.gz" ) )
            .hasContent( "REQUEST_ID\n" + content + "\n", GZIP );
        assertFile( logs.resolve( "1-file-01-80723ad6-1-UNKNOWN.log.gz.metadata.yaml" ) )
            .hasContent( """
                ---
                filePrefixPattern: ""
                type: "type"
                clientHostname: "log"
                headers:
                - "REQUEST_ID"
                types:
                - - 11
                p: "1"
                VERSION: "80723ad6-1"
                """ );

        assertFile( logs.resolve( "1-file-02-80723ad6-1-UNKNOWN.log.gz" ) )
            .hasContent( "REQUEST_ID\n" + content + "\n", GZIP );
        assertFile( logs.resolve( "1-file-02-80723ad6-2-UNKNOWN.log.gz" ) )
            .hasContent( "REQUEST_ID\n" + content + "\n", GZIP );
        assertFile( logs.resolve( "1-file-02-80723ad6-1-UNKNOWN.log.gz.metadata.yaml" ) )
            .hasContent( """
                ---
                filePrefixPattern: ""
                type: "type"
                clientHostname: "log"
                headers:
                - "REQUEST_ID"
                types:
                - - 11
                p: "1"
                VERSION: "80723ad6-1"
                """ );

        assertFile( logs.resolve( "1-file-11-80723ad6-1-UNKNOWN.log.gz" ) )
            .hasContent( "REQUEST_ID\n" + content + "\n", GZIP );

        assertFile( logs.resolve( "1-file-11-80723ad6-1-UNKNOWN.log.gz" ) )
            .hasContent( "REQUEST_ID\n" + content + "\n", GZIP );

        assertFile( logs.resolve( "1-file-00-80723ad6-1-UNKNOWN.log.gz" ) )
            .hasContent( "corrupted file" );
        assertFile( logs.resolve( "1-file-00-80723ad6-1-UNKNOWN.log.gz.metadata.yaml" ) )
            .hasContent( """
                ---
                filePrefixPattern: ""
                type: "type"
                clientHostname: "log"
                headers: "REQUEST_ID"
                p: "1"
                VERSION: "80723ad6-1"
                """ );

        assertFile( logs.resolve( "1-file-02-ab96b20e-1-UNKNOWN.log.gz" ) )
            .hasContent( "REQUEST_ID\tH2\n" + content + "\n", GZIP );
    }

    @Test
    public void testVersions() throws IOException {
        var headers = new String[] { "REQUEST_ID", "H2" };
        var types = new byte[][] { new byte[] { Types.STRING.id }, new byte[] { Types.STRING.id } };

        Dates.setTimeFixed( 2015, 10, 10, 1, 0 );

        var logs = testPath( "logs" );
        String metadata = """
            ---
            filePrefixPattern: ""
            type: "type"
            clientHostname: "log"
            headers:
            - "REQUEST_ID"
            types:
            - - 11
            p: "1"
            VERSION: "80723ad6-1"
            """;
        Files.write(
            logs.resolve( "1-file-00-80723ad6-1-UNKNOWN.log.gz" ),
            PLAIN, "1\t2", ContentWriter.ofString() );
        Files.write(
            logs.resolve( "1-file-00-80723ad6-1-UNKNOWN.log.gz.metadata.yaml" ),
            PLAIN, metadata, ContentWriter.ofString() );

        Files.write(
            logs.resolve( "1-file-00-80723ad6-2-UNKNOWN.log.gz" ),
            PLAIN, "11\t22", ContentWriter.ofString() );
        Files.write(
            logs.resolve( "1-file-00-80723ad6-2-UNKNOWN.log.gz.metadata.yaml" ),
            PLAIN, metadata, ContentWriter.ofString() );

        try( var writer = new TsvWriter( logs, FILE_PATTERN,
            new LogId( "", "type", "log", Map.of( "p", "1" ), headers, types ),
            new WriterConfiguration.TsvConfiguration(), 10, BPH_12, 20 ) ) {
            writer.write( CURRENT_PROTOCOL_VERSION, BinaryUtils.line( "111", "222" ), msg -> {} );
        }

        assertFile( logs.resolve( "1-file-00-ab96b20e-1-UNKNOWN.log.gz" ) )
            .hasContent( """
                REQUEST_ID\tH2
                111\t222
                """, GZIP );

        assertFile( logs.resolve( "1-file-00-ab96b20e-1-UNKNOWN.log.gz.metadata.yaml" ) )
            .hasContent( """
                ---
                filePrefixPattern: ""
                type: "type"
                clientHostname: "log"
                headers:
                - "REQUEST_ID"
                - "H2"
                types:
                - - 11
                - - 11
                p: "1"
                VERSION: "ab96b20e-1"
                """ );
    }

    @Test
    public void testProtocolVersion1() {
        var headers = "REQUEST_ID";
        var newHeaders = "REQUEST_ID\tH2";

        Dates.setTimeFixed( 2015, 10, 10, 1, 0 );

        var content = "1234567890";
        var bytes = content.getBytes();
        var logs = testPath( "logs" );
        Files.write(
            logs.resolve( "1-file-00-9042dc83-1-UNKNOWN.log.gz" ),
            PLAIN, "corrupted file", ContentWriter.ofString() );
        Files.write(
            logs.resolve( "1-file-00-9042dc83-1-UNKNOWN.log.gz.metadata.yaml" ),
            PLAIN, """
                ---
                filePrefixPattern: ""
                type: "type"
                clientHostname: "log"
                headers: "REQUEST_ID"
                p: "1"
                """, ContentWriter.ofString() );

        try( var writer = new TsvWriter( logs, FILE_PATTERN,
            new LogId( "", "type", "log", Map.of( "p", "1" ), new String[] { headers }, new byte[][] { { -1 } } ), new WriterConfiguration.TsvConfiguration(), 10, BPH_12, 10 ) ) {
            writer.write( TSV_V1, bytes, msg -> {} );

            Dates.setTimeFixed( 2015, 10, 10, 1, 5 );
            writer.write( TSV_V1, bytes, msg -> {} );

            Dates.setTimeFixed( 2015, 10, 10, 1, 10 );
            writer.write( TSV_V1, bytes, msg -> {
            } );
        }

        try( var writer = new TsvWriter( logs, FILE_PATTERN, new LogId( "", "type", "log", Map.of( "p", "1" ), new String[] { headers }, new byte[][] { { -1 } } ), new WriterConfiguration.TsvConfiguration(), 10, BPH_12, 10 ) ) {
            Dates.setTimeFixed( 2015, 10, 10, 1, 14 );
            writer.write( TSV_V1, bytes, msg -> {} );

            Dates.setTimeFixed( 2015, 10, 10, 1, 59 );
            writer.write( TSV_V1, bytes, msg -> {} );
        }

        try( var writer = new TsvWriter( logs, FILE_PATTERN, new LogId( "", "type", "log", Map.of( "p", "1" ), new String[] { newHeaders }, new byte[][] { { -1 } } ), new WriterConfiguration.TsvConfiguration(), 10, BPH_12, 10 ) ) {
            Dates.setTimeFixed( 2015, 10, 10, 1, 14 );
            writer.write( TSV_V1, bytes, msg -> {} );
        }

        assertFile( logs.resolve( "1-file-01-9042dc83-1-UNKNOWN.log.gz" ) )
            .hasContent( "REQUEST_ID\n" + content, GZIP );
        assertFile( logs.resolve( "1-file-01-9042dc83-1-UNKNOWN.log.gz.metadata.yaml" ) )
            .hasContent( """
                ---
                filePrefixPattern: ""
                type: "type"
                clientHostname: "log"
                headers:
                - "REQUEST_ID"
                types:
                - - -1
                p: "1"
                VERSION: "9042dc83-1"
                """ );

        assertFile( logs.resolve( "1-file-02-9042dc83-1-UNKNOWN.log.gz" ) )
            .hasContent( "REQUEST_ID\n" + content, GZIP );
        assertFile( logs.resolve( "1-file-02-9042dc83-2-UNKNOWN.log.gz" ) )
            .hasContent( "REQUEST_ID\n" + content, GZIP );
        assertFile( logs.resolve( "1-file-02-9042dc83-1-UNKNOWN.log.gz.metadata.yaml" ) )
            .hasContent( """
                ---
                filePrefixPattern: ""
                type: "type"
                clientHostname: "log"
                headers:
                - "REQUEST_ID"
                types:
                - - -1
                p: "1"
                VERSION: "9042dc83-1"
                """ );

        assertFile( logs.resolve( "1-file-11-9042dc83-1-UNKNOWN.log.gz" ) )
            .hasContent( "REQUEST_ID\n" + content, GZIP );

        assertFile( logs.resolve( "1-file-11-9042dc83-1-UNKNOWN.log.gz" ) )
            .hasContent( "REQUEST_ID\n" + content, GZIP );

        assertFile( logs.resolve( "1-file-00-9042dc83-1-UNKNOWN.log.gz" ) )
            .hasContent( "corrupted file" );
        assertFile( logs.resolve( "1-file-00-9042dc83-1-UNKNOWN.log.gz.metadata.yaml" ) )
            .hasContent( """
                ---
                filePrefixPattern: ""
                type: "type"
                clientHostname: "log"
                headers: "REQUEST_ID"
                p: "1"
                """ );

        assertFile( logs.resolve( "1-file-02-e56ba426-1-UNKNOWN.log.gz" ) )
            .hasContent( "REQUEST_ID\tH2\n" + content, GZIP );
    }
}
