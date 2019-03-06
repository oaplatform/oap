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

import lombok.val;
import oap.io.Closeables;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.StringReader;
import java.util.HashMap;
import java.util.List;

import static java.util.stream.Collectors.toList;

public class MemoryLoggerBackend extends LoggerBackend {
    private final HashMap<LogId, ByteArrayOutputStream> outputs = new HashMap<>();

    @Override
    public void log( String hostName, String fileName, String logType, int version, byte[] buffer, int offset, int length ) {
        outputs
            .computeIfAbsent( new LogId( fileName, logType, hostName, version ), ( fn ) -> new ByteArrayOutputStream() )
            .write( buffer, offset, length );
    }

    public List<String> getLines( LogId id ) {
        val s = outputs.getOrDefault( id, new ByteArrayOutputStream() ).toString();
        return new BufferedReader( new StringReader( s ) )
            .lines()
            .collect( toList() );
    }

    @Override
    public void close() {
        outputs.values().forEach( Closeables::close );
    }

    @Override
    public AvailabilityReport availabilityReport() {
        return new AvailabilityReport( AvailabilityReport.State.OPERATIONAL );
    }
}
