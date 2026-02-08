package oap.http;

import com.fasterxml.jackson.databind.MappingIterator;
import com.google.common.io.ByteStreams;
import lombok.SneakyThrows;
import lombok.ToString;
import oap.io.Closeables;
import oap.json.Binder;
import oap.reflect.TypeRef;
import oap.util.BiStream;
import oap.util.Pair;
import oap.util.Stream;
import oap.util.function.Try;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Closeable;
import java.io.InputStream;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import static java.nio.charset.StandardCharsets.UTF_8;
import static oap.http.Http.ContentType.APPLICATION_OCTET_STREAM;

@ToString( exclude = { "inputStream", "content" }, doNotUseGetters = true )
public class Response implements Closeable, AutoCloseable {
    public final String url;
    public final int code;
    public final String reasonPhrase;
    public final String contentType;
    public final List<Pair<String, String>> headers;
    private InputStream inputStream;
    private volatile byte[] content = null;

    public Response( String url, int code, String reasonPhrase, List<Pair<String, String>> headers, @Nonnull String contentType, InputStream inputStream ) {
        this.url = url;
        this.code = code;
        this.reasonPhrase = reasonPhrase;
        this.headers = headers;
        this.contentType = Objects.requireNonNull( contentType );
        this.inputStream = inputStream;
    }

    public Response( String url, int code, String reasonPhrase, List<Pair<String, String>> headers ) {
        this( url, code, reasonPhrase, headers, BiStream.of( headers )
            .filter( ( name, value ) -> "Content-type".equalsIgnoreCase( name ) )
            .mapToObj( ( name, value ) -> value )
            .findAny()
            .orElse( APPLICATION_OCTET_STREAM ), null );
    }

    public Optional<String> header( @Nonnull String headerName ) {
        return BiStream.of( headers )
            .filter( ( name, value ) -> headerName.equalsIgnoreCase( name ) )
            .mapToObj( ( name, value ) -> value )
            .findAny();
    }

    @Nullable
    @SneakyThrows
    public byte[] content() {
        if( content == null && inputStream == null ) return null;
        if( content == null ) synchronized( this ) {
            if( content == null ) {
                content = ByteStreams.toByteArray( inputStream );
                close();
            }
        }
        return content;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public String contentString() {
        var text = content();

        if( text == null ) return null;

        return new String( text, UTF_8 );
    }

    public <T> Optional<T> unmarshal( Class<T> clazz ) {
        if( inputStream != null ) synchronized( this ) {
            if( inputStream != null )
                return Optional.of( Binder.json.unmarshal( clazz, inputStream ) );
        }

        var contentString = contentString();
        if( contentString == null ) return Optional.empty();

        return Optional.of( Binder.json.unmarshal( clazz, contentString ) );
    }

    public <T> Optional<T> unmarshal( TypeRef<T> ref ) {
        if( inputStream != null ) synchronized( this ) {
            if( inputStream != null )
                return Optional.of( Binder.json.unmarshal( ref, inputStream ) );
        }

        var contentString = contentString();
        if( contentString == null ) return Optional.empty();

        return Optional.of( Binder.json.unmarshal( ref, contentString ) );
    }

    @SneakyThrows
    public <T> Stream<T> unmarshalStream( TypeRef<T> ref ) {
        MappingIterator<Object> objectMappingIterator = null;

        if( inputStream != null ) {
            synchronized( this ) {
                if( inputStream != null ) {
                    objectMappingIterator = Binder.json.readerFor( ref ).readValues( inputStream );
                }
            }
        }

        if( objectMappingIterator == null ) {
            var contentString = contentString();
            if( contentString == null )
                return Stream.empty();

            objectMappingIterator = Binder.json.readerFor( ref ).readValues( contentString );
        }

        var finalObjectMappingIterator = objectMappingIterator;

        var it = new Iterator<T>() {
            @Override
            public boolean hasNext() {
                return finalObjectMappingIterator.hasNext();
            }

            @Override
            @SuppressWarnings( "unchecked" )
            public T next() {
                return ( T ) finalObjectMappingIterator.next();
            }
        };

        var stream = Stream.of( it );
        if( inputStream != null ) stream = stream.onClose( Try.run( () -> inputStream.close() ) );
        return stream;
    }

    @Override
    public void close() {
        Closeables.close( inputStream );
        inputStream = null;
    }

    public Map<String, String> getHeaders() {
        return headers.stream().collect( Collectors.toMap( p -> p._1, p -> p._2 ) );
    }
}
