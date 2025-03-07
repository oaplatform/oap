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

package oap.io.content;

import lombok.SneakyThrows;
import oap.json.Binder;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;

import static java.nio.charset.StandardCharsets.UTF_8;

public interface ContentWriter<T> {
    @SneakyThrows
    default void write( OutputStream os, T object ) {
        os.write( write( object ) );
    }

    default byte[] write( T object ) {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        write( os, object );
        return os.toByteArray();
    }

    static <T> byte[] write( T object, ContentWriter<T> writer ) {
        return writer.write( object );
    }

    static <T> void write( OutputStream os, T object, ContentWriter<T> writer ) {
        writer.write( os, object );
    }

    static ContentWriter<String> ofString() {
        return new ContentWriter<>() {
            @Override
            public byte[] write( String object ) {
                return object.getBytes( UTF_8 );
            }
        };
    }

    static ContentWriter<byte[]> ofBytes() {
        return new ContentWriter<>() {
            @Override
            public byte[] write( byte[] object ) {
                return object;
            }
        };
    }

    static ContentWriter<Object> ofObject() {
        return new ContentWriter<>() {
            @Override
            @SneakyThrows
            public void write( OutputStream os, Object object ) {
                try( ObjectOutputStream oos = new ObjectOutputStream( os ) ) {
                    oos.writeObject( object );
                    oos.flush();
                }
            }
        };
    }

    static <T> ContentWriter<T> ofJson() {
        return new ContentWriter<>() {
            @Override
            public byte[] write( Object object ) {
                return Binder.json.marshal( object ).getBytes( UTF_8 );
            }

            @Override
            public void write( OutputStream os, Object object ) {
                Binder.json.marshal( os, object );
            }
        };
    }
}
