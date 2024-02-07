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

package oap.logstream;

import lombok.SneakyThrows;
import oap.io.Closeables;
import oap.logstream.LogStreamProtocol.ProtocolVersion;
import oap.util.BiStream;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static java.util.stream.Collectors.toList;
import static oap.util.Pair.__;

public class MemoryLoggerBackend extends AbstractLoggerBackend {
    private final LinkedHashMap<LogId, ByteArrayOutputStream> outputs = new LinkedHashMap<>();

    @Override
    public synchronized void log( ProtocolVersion version, String hostName, String filePreffix, Map<String, String> properties, String logType,
                                  String[] headers, byte[][] types, byte[] buffer, int offset, int length ) {
        outputs
            .computeIfAbsent( new LogId( filePreffix, logType, hostName, properties, headers, types ), fn -> new ByteArrayOutputStream() )
            .write( buffer, offset, length );
    }

    @Deprecated( forRemoval = true )
    public List<String> getLines( LogId id ) {
        return loggedLines( id );
    }

    public synchronized List<String> loggedLines( LogId id ) {
        var log = logged( id );
        return new BufferedReader( new StringReader( log ) )
            .lines()
            .collect( toList() );
    }

    public synchronized List<String> loggedLines() {
        var ret = new ArrayList<String>();
        for( var id : outputs.keySet() ) ret.addAll( loggedLines( id ) );
        return ret;
    }

    public synchronized String logged() {
        var ret = new StringBuilder();
        for( var id : outputs.keySet() )
            ret.append( outputs.getOrDefault( id, new ByteArrayOutputStream() ).toString() );
        return ret.toString();
    }

    public synchronized byte[] loggedBytes() {
        return loggedBytes( logId -> true );
    }

    public synchronized byte[] loggedBytes( LogId id ) {
        return loggedBytes( logId -> logId.equals( id ) );
    }

    @SneakyThrows
    public synchronized byte[] loggedBytes( Predicate<LogId> filter ) {
        var ret = new ByteArrayOutputStream();
        for( var id : outputs.keySet() ) {
            if( filter.test( id ) )
                ret.write( outputs.getOrDefault( id, new ByteArrayOutputStream() ).toByteArray() );
        }
        return ret.toByteArray();
    }

    @SuppressWarnings( "checkstyle:OverloadMethodsDeclarationOrder" )
    public synchronized String logged( LogId id ) {
        return outputs.getOrDefault( id, new ByteArrayOutputStream() ).toString();
    }

    public synchronized Map<LogId, String> logs() {
        return BiStream.of( outputs )
            .map( ( logId, bytes ) -> __( logId, bytes.toString() ) )
            .toMap();
    }

    @Override
    public void close() {
        outputs.values().forEach( Closeables::close );
    }

    @Override
    public AvailabilityReport availabilityReport() {
        return new AvailabilityReport( AvailabilityReport.State.OPERATIONAL );
    }

    public void reset() {
        outputs.clear();
    }
}
