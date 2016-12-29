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
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.core.JsonEncoding;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.AnnotationIntrospector;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.fasterxml.jackson.datatype.joda.cfg.JacksonJodaDateFormat;
import com.fasterxml.jackson.datatype.joda.deser.DateTimeDeserializer;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.jasonclawson.jackson.dataformat.hocon.HoconFactory;
import lombok.extern.slf4j.Slf4j;
import oap.io.Files;
import oap.io.IoStreams;
import oap.io.Resources;
import oap.util.Dates;
import oap.util.Strings;
import org.joda.time.ReadableInstant;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.nio.file.Path;
import java.util.Map;

@Slf4j
public class Binder {
    private static final JacksonJodaDateFormat JACKSON_DATE_FORMAT = new JacksonJodaDateFormat( Dates.FORMAT_FULL );

    public static final Binder hocon =
        new Binder( initialize( new ObjectMapper( new HoconFactoryWithSystemProperties( log ) ), false ) );
    public static final Binder hoconWithoutSystemProperties =
        new Binder( initialize( new ObjectMapper( new HoconFactory() ), false ) );
    public static final Binder json = new Binder( initialize( new ObjectMapper(), false ) );
    public static final Binder jsonWithTyping = new Binder( initialize( new ObjectMapper(), true ) );
    private ObjectMapper mapper;

    public Binder( ObjectMapper mapper ) {
        this.mapper = mapper;
    }

    public static Binder hoconWithConfig( String... config ) {
        return new Binder( initialize( new ObjectMapper( new HoconFactoryWithFallback( log, config ) ), false ) );
    }

    public static Binder hoconWithConfig( Map<String, Object> config ) {
        return new Binder( initialize( new ObjectMapper( new HoconFactoryWithFallback( log, config ) ), false ) );
    }

    private static ObjectMapper initialize( ObjectMapper mapper, boolean defaultTyping ) {
        AnnotationIntrospector introspector = new JacksonAnnotationIntrospector();
        mapper.getDeserializationConfig().with( introspector );
        mapper.getSerializationConfig().with( introspector );
        mapper.registerModule( new AfterburnerModule() );
        mapper.registerModule( new Jdk8Module().configureAbsentsAsNulls( true ) );
        mapper.registerModule( new JodaModule() );
        mapper.registerModule( new ParameterNamesModule( JsonCreator.Mode.DEFAULT ) );
        mapper.enable( DeserializationFeature.USE_LONG_FOR_INTS );
        mapper.enable( JsonParser.Feature.ALLOW_SINGLE_QUOTES );
        mapper.disable( DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES );
        mapper.disable( SerializationFeature.WRITE_DATES_AS_TIMESTAMPS );
        mapper.disable( SerializationFeature.WRITE_EMPTY_JSON_ARRAYS );
        mapper.disable( SerializationFeature.FAIL_ON_EMPTY_BEANS );
        mapper.setVisibility( PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY );
        mapper.setSerializationInclusion( JsonInclude.Include.NON_NULL );
        mapper.registerModule( new OapJsonModule() );

        if( defaultTyping )
            mapper.enableDefaultTyping( ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY );

        return mapper;
    }

    @SuppressWarnings( "unchecked" )
    private static <T extends ReadableInstant> JsonDeserializer<T> forType( Class<T> cls ) {
        return ( JsonDeserializer<T> ) new DateTimeDeserializer( cls, JACKSON_DATE_FORMAT );
    }

    public final JsonGenerator getJsonGenerator( Path path ) throws JsonException {
        try {
            return mapper.getFactory().createGenerator( path.toFile(), JsonEncoding.UTF8 );
        } catch( IOException e ) {
            throw new JsonException( e );
        }
    }

    public String canonicalize( Class<?> clazz, String json ) throws JsonException {
        return marshal( unmarshal( clazz, json ) );
    }

    public String marshal( Object value ) throws JsonException {
        try {
            return mapper.writeValueAsString( value );
        } catch( IOException e ) {
            throw new JsonException( e );
        }
    }

    public <T> String marshal( TypeReference<T> ref, Object value ) throws JsonException {
        try {
            return mapper.writerFor( ref ).writeValueAsString( value );
        } catch( IOException e ) {
            throw new JsonException( e );
        }
    }

    public void marshal( OutputStream os, Object value ) throws JsonException {
        try {
            mapper.writeValue( os, value );
        } catch( IOException e ) {
            throw new JsonException( e );
        }
    }


    public void marshal( Path path, Object object ) throws JsonException {
        Files.ensureFile( path );

        try( final OutputStream os = IoStreams.out( path ) ) {
            marshal( os, object );
        } catch( IOException e ) {
            throw new JsonException( e );
        }
    }

    public <T> T unmarshal( Class<T> clazz, Path path ) throws JsonException {
        return unmarshal( clazz, Files.readString( path ) );
    }

    public <T> T unmarshal( Class<T> clazz, URL url ) throws JsonException {
        return unmarshal( clazz, Strings.readString( url ) );
    }

    @SuppressWarnings( "unchecked" )
    public <T> T unmarshal( TypeReference<T> ref, String string ) throws JsonException {
        try {
            return ( T ) mapper.readValue( string, ref );
        } catch( IOException e ) {
            log.debug( "json: " + string );
            throw new JsonException( "json error: " + e.getMessage(), e );
        }
    }

    public ObjectReader readerFor( TypeReference<?> ref ) {
        return mapper.readerFor( ref );
    }

    public ObjectWriter writerFor( TypeReference<?> ref ) {
        return mapper.writerFor( ref );
    }

    public <T> T unmarshal( TypeReference<T> ref, Path path ) throws JsonException {
        return unmarshal( ref, Files.readString( path ) );
    }

    public <T> T unmarshal( TypeReference<T> ref, URL url ) {
        return unmarshal( ref, Strings.readString( url ) );
    }

    @SuppressWarnings( "unchecked" )
    public <T> T unmarshal( TypeReference<T> ref, InputStream is ) throws JsonException {
        try {
            return ( T ) mapper.readValue( is, ref );
        } catch( IOException e ) {
            throw new JsonException( e.getMessage(), e );
        }
    }

    @SuppressWarnings( "unchecked" )
    public <T> T unmarshal( Class<?> clazz, String string ) throws JsonException {
        try {
            return ( T ) mapper.readValue( string, clazz );
        } catch( Exception e ) {
            log.trace( string );
            throw new JsonException( e.getMessage(), e );
        }
    }

    @SuppressWarnings( "unchecked" )
    public <T> T unmarshal( Class<?> clazz, Map<String, Object> map ) throws JsonException {
        try {
            return ( T ) mapper.convertValue( map, clazz );
        } catch( Exception e ) {
            log.trace( String.valueOf( map ) );
            throw new JsonException( e.getMessage(), e );
        }
    }

    @SuppressWarnings( "unchecked" )
    public <T> T unmarshal( TypeReference<T> ref, Map<String, Object> map ) throws JsonException {
        try {
            return ( T ) mapper.convertValue( map, ref );
        } catch( Exception e ) {
            log.trace( String.valueOf( map ) );
            throw new JsonException( e.getMessage(), e );
        }
    }

    @SuppressWarnings( "unchecked" )
    public <T> T unmarshal( Class<?> clazz, InputStream json ) throws JsonException {
        try {
            return ( T ) mapper.readValue( json, clazz );
        } catch( IOException e ) {
            throw new JsonException( e.getMessage(), e );
        }
    }

    public <T> T unmarshalResource( Class<?> context, Class<T> clazz,
                                    String resourceJsonPath ) throws JsonException {
        return Resources.readString( context, resourceJsonPath )
            .map( json -> this.<T>unmarshal( clazz, json ) )
            .orElseThrow( () -> new JsonException( "not found " + resourceJsonPath ) );

    }

    @SuppressWarnings( "unchecked" )
    public <T> T clone( T object ) throws JsonException {
        return unmarshal( ( Class<T> ) object.getClass(), marshal( object ) );
    }

    public void update( Object obj, Map<String, Object> values ) throws JsonException {
        try {
            final String marshal = json.marshal( obj );
            final String vs = json.marshal( values );

            hoconWithConfig( vs ).mapper.readerForUpdating( obj ).readValue( marshal );
        } catch( IOException e ) {
            throw new JsonException( e );
        }
    }

    public void update( Object obj, String json ) throws JsonException {
        try {
            mapper.readerForUpdating( obj ).readValue( json );
        } catch( IOException e ) {
            throw new JsonException( e );
        }
    }
}

