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

package oap.remote.application;

import lombok.extern.slf4j.Slf4j;
import oap.remote.RemoteInvocationException;

import java.util.Optional;
import java.util.stream.Stream;

@Slf4j
public class RemoteService implements RemoteClient {
    int transportErrors = 3;

    @Override
    public boolean accessible() {
        return true;
    }


    @Override
    public void erroneous() {
        throw new IllegalStateException( "this method always produces exception" );
    }

    @Override
    public void testRetry() {
        log.debug( "errors {}", --transportErrors );
        if( transportErrors > 0 ) throw new RemoteInvocationException( "transport error" );
    }

    @Override
    public Stream<Optional<String>> testStream( String... values ) {
        return Stream.of( values ).map( Optional::ofNullable );
    }
}
