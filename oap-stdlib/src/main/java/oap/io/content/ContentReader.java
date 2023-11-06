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

import com.google.common.io.ByteStreams;
import lombok.SneakyThrows;
import oap.io.IoStreams;
import oap.json.Binder;
import oap.reflect.TypeRef;
import oap.util.Lists;
import oap.util.Stream;
import oap.util.Throwables;
import oap.util.function.Try;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.AutoCloseInputStream;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.function.Function;

import static java.nio.charset.StandardCharsets.UTF_8;

@SuppressWarnings( "checkstyle:AbstractClassName" )
public abstract class ContentReader<R> {
    public final boolean closeable;

    protected ContentReader( boolean closeable ) {
        this.closeable = closeable;
    }

    protected ContentReader() {
        this( false );
    }

    public static <R> R read( String data, ContentReader<R> reader ) {
        return reader.read( data.getBytes( UTF_8 ) );
    }

    public static <R> R read( byte[] bytes, ContentReader<R> reader ) {
        return reader.read( bytes );
    }

    public static <R> R read( byte[] bytes, int offset, int length, ContentReader<R> reader ) {
        return reader.read( new ByteArrayInputStream( bytes, offset, length ) );
    }

    public static <R> R read( InputStream is, ContentReader<R> reader ) {
        return reader.read( is );
    }

    @SneakyThrows
    public static <R> R read( URL url, ContentReader<R> reader ) {
        return read( new AutoCloseInputStream( url.openStream() ), reader );
    }

    public R read( byte[] bytes ) {
        return read( new ByteArrayInputStream( bytes ) );
    }

    @SneakyThrows
    public R read( InputStream is ) {
        return read( ByteStreams.toByteArray( is ) );
    }

    public <T> ContentReader<T> andThen( Function<R, T> then ) {
        return new ContentReader<T>( closeable ) {
            @Override
            public T read( byte[] bytes ) {
                return then.apply( ContentReader.this.read( bytes ) );
            }

            @Override
            public T read( InputStream is ) {
                return then.apply( ContentReader.this.read( is ) );
            }
        };
    }


    public static ContentReader<String> ofString() {
        return new ContentReader<>() {
            @Override
            public String read( byte[] bytes ) {
                return new String( bytes, UTF_8 );
            }
        };
    }

    public static ContentReader<List<String>> ofLines() {
        return ofLines( new ArrayList<>() );
    }

    public static ContentReader<List<String>> ofLines( List<String> lines ) {
        return new ContentReader<>() {
            @Override
            public List<String> read( InputStream is ) {
                return Lists.concat( lines, ofLinesStream().read( is ).toList() );
            }
        };
    }

    public static ContentReader<Stream<String>> ofLinesStream() {
        return new ContentReader<>( true ) {
            @Override
            public Stream<String> read( InputStream is ) {
                return IoStreams.lines( is, true );
            }
        };
    }

    public static ContentReader<Try.ThrowingConsumer<String>> ofLinesConsumer( Try.ThrowingConsumer<String> consumer ) {
        return new ContentReader<>() {
            @Override
            public Try.ThrowingConsumer<String> read( InputStream is ) {
                IoStreams.lines( is, true ).forEach( Try.consume( consumer ) );

                return consumer;
            }
        };
    }

    public static ContentReader<InputStream> ofInputStream() {
        return new ContentReader<>( true ) {
            @Override
            public InputStream read( InputStream is ) {
                return new AutoCloseInputStream( is );
            }
        };
    }

    public static ContentReader<byte[]> ofBytes() {
        return new ContentReader<>() {
            @Override
            public byte[] read( byte[] bytes ) {
                return bytes;
            }
        };
    }

    public static ContentReader<Try.ThrowingIntConsumer> ofBytes( byte[] buffer, Try.ThrowingIntConsumer consumer ) {
        return new ContentReader<>( true ) {
            @Override
            public Try.ThrowingIntConsumer read( InputStream inputStream ) {
                try {
                    int size;
                    do {
                        size = IOUtils.read( inputStream, buffer );
                        consumer.accept( size );
                    } while( size > 0 );

                    return consumer;
                } catch( Exception e ) {
                    throw Throwables.propagate( e );
                }
            }
        };
    }

    public static <R> ContentReader<R> ofObject() {
        return new ContentReader<>() {
            @Override
            @SuppressWarnings( "unchecked" )
            @SneakyThrows
            public R read( InputStream is ) {
                try( ObjectInputStream ois = new ObjectInputStream( is ) ) {
                    return ( R ) ois.readObject();
                }
            }
        };
    }

    public static ContentReader<Properties> ofProperties() {
        return ofProperties( new Properties() );
    }

    public static ContentReader<Properties> ofProperties( Properties properties ) {
        return new ContentReader<>() {
            @SneakyThrows
            @Override
            public Properties read( InputStream is ) {
                properties.load( is );
                return properties;
            }
        };
    }

    public static <R> ContentReader<R> ofJson( Class<R> clazz ) {
        return new ContentReader<>() {
            @Override
            public R read( InputStream is ) {
                return Binder.json.unmarshal( clazz, is );
            }
        };
    }

    public static <R> ContentReader<R> ofJson( TypeRef<R> typeRef ) {
        return new ContentReader<>() {
            @Override
            public R read( InputStream is ) {
                return Binder.json.unmarshal( typeRef, is );
            }
        };
    }

    public static <R> ContentReader<R> ofYaml( Class<R> clazz ) {
        return new ContentReader<>() {
            @Override
            public R read( InputStream is ) {
                return Binder.yaml.unmarshal( clazz, is );
            }
        };
    }

    public static <R> ContentReader<R> ofYaml( TypeRef<R> typeRef ) {
        return new ContentReader<>() {
            @Override
            public R read( InputStream is ) {
                return Binder.yaml.unmarshal( typeRef, is );
            }
        };
    }

    public static <R> ContentReader<R> ofHocon( Class<R> clazz ) {
        return new ContentReader<>() {
            @Override
            public R read( InputStream is ) {
                return Binder.hocon.unmarshal( clazz, is );
            }
        };
    }

    public static <R> ContentReader<R> ofHocon( TypeRef<R> typeRef ) {
        return new ContentReader<>() {
            @Override
            public R read( InputStream is ) {
                return Binder.hocon.unmarshal( typeRef, is );
            }
        };
    }
}
