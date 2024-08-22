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

package oap.util;

import oap.io.IoStreams;
import oap.io.Resources;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;

public class Properties {
    @Nonnull
    public static java.util.Properties read( URL url ) throws UncheckedIOException {
        try( InputStream stream = url.openStream() ) {
            return read( stream );
        } catch( IOException e ) {
            throw new UncheckedIOException( e );
        }
    }

    @Nonnull
    public static java.util.Properties read( String resource ) throws UncheckedIOException {
        try {
            java.util.Properties properties = new java.util.Properties();

            List<URL> urls = Resources.urls( resource );
            for( URL url : urls ) {
                try( InputStream stream = url.openStream() ) {
                    properties.load( stream );
                }
            }

            return properties;
        } catch( IOException e ) {
            throw new UncheckedIOException( e );
        }
    }

    @Nonnull
    public static java.util.Properties read( Path path ) throws UncheckedIOException {
        try( InputStream stream = IoStreams.in( path ) ) {
            return read( stream );
        } catch( IOException e ) {
            throw new UncheckedIOException( e );
        }
    }

    @Nonnull
    public static java.util.Properties read( InputStream stream ) throws UncheckedIOException {
        try {
            java.util.Properties properties = new java.util.Properties();
            properties.load( stream );
            return properties;
        } catch( IOException e ) {
            throw new UncheckedIOException( e );
        }
    }
}
