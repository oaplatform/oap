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
package oap.json;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.fasterxml.jackson.datatype.joda.cfg.JacksonJodaDateFormat;
import com.fasterxml.jackson.datatype.joda.deser.DateTimeDeserializer;
import com.fasterxml.jackson.datatype.joda.ser.DateTimeSerializer;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;
import oap.io.Files;
import oap.io.Resources;
import oap.util.Dates;
import oap.util.Strings;
import org.joda.time.DateTime;
import org.joda.time.ReadableInstant;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.Optional;

import static org.slf4j.LoggerFactory.getLogger;

public class Binder {
    public static final ObjectMapper mapper = new ObjectMapper();
    private static final org.slf4j.Logger logger = getLogger( Binder.class );
    private static final JacksonJodaDateFormat jodaDateFormat = new JacksonJodaDateFormat( Dates.FORMAT_SIMPLE );

    static {
        mapper.registerModule( new AfterburnerModule() );

        mapper.registerModule( new Jdk8Module() );
        final JodaModule module = new JodaModule();
        module.addDeserializer( DateTime.class, forType( DateTime.class ) );
        module.addSerializer( DateTime.class, new DateTimeSerializer( jodaDateFormat ) );
        mapper.registerModule( module );
        mapper.enable( DeserializationFeature.USE_LONG_FOR_INTS );
        mapper.disable( DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES );
        mapper.disable( SerializationFeature.WRITE_DATES_AS_TIMESTAMPS );
        mapper.disable( SerializationFeature.WRITE_EMPTY_JSON_ARRAYS );
        mapper.setVisibility( PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY );
        mapper.setSerializationInclusion( JsonInclude.Include.NON_NULL );
        mapper.registerModule( new PathModule() );
    }

    @SuppressWarnings( "unchecked" )
    private static <T extends ReadableInstant> JsonDeserializer<T> forType( Class<T> cls ) {
        return (JsonDeserializer<T>) new DateTimeDeserializer( cls, jodaDateFormat );
    }

    public static String canonicalize( Class<?> clazz, String json ) {
        return marshal( unmarshal( clazz, json ) );
    }

    public static String marshal( Object value ) {
        try {
            return mapper.writeValueAsString( value );
        } catch( IOException e ) {
            throw new UncheckedIOException( e );
        }
    }

    public static <T> String marshal( TypeReference<T> ref, Object value ) {
        try {
            return mapper.writerFor( ref ).writeValueAsString( value );
        } catch( IOException e ) {
            throw new UncheckedIOException( e );
        }
    }


    public static void marshal( Path path, Object object ) {
        Files.writeString( path, marshal( object ) );
    }

    public static <T> T unmarshal( Class<T> clazz, Path path ) {
        return unmarshal( clazz, Files.readString( path ) );
    }

    public static <T> T unmarshal( Class<T> clazz, URL url ) {
        return unmarshal( clazz, Strings.readString( url ) );
    }

    @SuppressWarnings( "unchecked" )
    public static <T> T unmarshal( TypeReference<T> ref, String json ) {
        try {
            return (T) mapper.readValue( json, ref );
        } catch( IOException e ) {
            throw new JsonException( "json error", e );
        }
    }

    public static <T> T unmarshal( TypeReference<T> ref, Path path ) {
        return unmarshal( ref, Files.readString( path ) );
    }

    @SuppressWarnings( "unchecked" )
    public static <T> T unmarshal( TypeReference<T> ref, InputStream json ) {
        try {
            return (T) mapper.readValue( json, ref );
        } catch( IOException e ) {
            throw new JsonException( "json error", e );
        }
    }

    @SuppressWarnings( "unchecked" )
    public static <T> T unmarshal( Class<?> clazz, String json ) {
        try {
            return (T) mapper.readValue( json, clazz );
        } catch( IOException e ) {
            throw new JsonException( "json error", e );
        }
    }

    @SuppressWarnings( "unchecked" )
    public static <T> T unmarshal( Class<?> clazz, InputStream json ) {
        try {
            return (T) mapper.readValue( json, clazz );
        } catch( IOException e ) {
            throw new JsonException( "json error", e );
        }
    }

    public static <T> Optional<T> unmarshalResource( Class<?> context, Class<T> clazz,
        String resourceJsonPath ) {
        return Resources.readString( context, resourceJsonPath ).
            map( json -> Binder.unmarshal( clazz, json ) );

    }

    @SuppressWarnings( "unchecked" )
    public static <T> T clone( T object ) {
        return unmarshal( (Class<T>) object.getClass(), marshal( object ) );
    }

}

