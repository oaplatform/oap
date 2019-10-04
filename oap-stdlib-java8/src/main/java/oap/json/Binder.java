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
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.introspect.JacksonAnnotationIntrospector;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.xml.ser.ToXmlGenerator;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.joda.JodaModule;
import com.fasterxml.jackson.datatype.joda.cfg.JacksonJodaDateFormat;
import com.fasterxml.jackson.datatype.joda.deser.DateTimeDeserializer;
import com.fasterxml.jackson.module.afterburner.AfterburnerModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import com.jasonclawson.jackson.dataformat.hocon.HoconFactory;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import oap.io.Files;
import oap.io.IoStreams;
import oap.io.Resources;
import oap.io.StringBuilderWriter;
import oap.json.ext.ExtModule;
import oap.reflect.Reflection;
import oap.reflect.TypeRef;
import oap.util.Dates;
import oap.util.Try;
import org.joda.time.ReadableInstant;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.Writer;
import java.lang.reflect.Type;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static oap.io.IoStreams.DEFAULT_BUFFER;
import static oap.io.IoStreams.Encoding.from;

@Slf4j
public class Binder {
    public static final Binder hocon;
    public static final Binder hoconWithoutSystemProperties;
    public static final Binder json;
    public static final Binder jsonWithTyping;
    public static final Binder xml;
    public static final Binder xmlWithTyping;
    public static final Binder yaml;
    private static final JacksonJodaDateFormat JACKSON_DATE_FORMAT = new JacksonJodaDateFormat( Dates.PARSER_FULL );
    private static final byte[] BEGIN_ARRAY = "[".getBytes();
    private static final byte[] END_ARRAY = "]".getBytes();
    private static final byte[] ITEM_SEP = ",".getBytes();
    private static Set<Module> modules;

    static {
        modules = Resources
            .lines( "META-INF/jackson.modules" )
            .map( Try.map( clazz -> ( ( Module ) Class.forName( clazz ).newInstance() ) ) )
            .toSet();

        json = new Binder( initialize( new ObjectMapper(), false, false ) );
        jsonWithTyping = new Binder( initialize( new ObjectMapper(), true, false ) );
        xml = new Binder( initialize( new XmlMapper(), false, false ) );
        xmlWithTyping = new Binder( initialize( new XmlMapper(), true, false ) );
        hoconWithoutSystemProperties =
            new Binder( initialize( new ObjectMapper( new HoconFactory() ), false, false ) );
        hocon =
            new Binder( initialize( new ObjectMapper( new HoconFactoryWithSystemProperties( log ) ), false, false ) );

        yaml = new Binder( initialize( new ObjectMapper( new YAMLFactory() ), false, false ) );
    }

    private ObjectMapper mapper;

    public Binder( ObjectMapper mapper ) {
        this.mapper = mapper;
    }

    public static Binder getBinder( URL url ) {
        return getBinder( url, true );
    }

    public static Binder getBinder( URL url, boolean withSystemProperties ) {
        val strUrl = url.toString().toLowerCase();
        if( strUrl.endsWith( "json" ) ) return Binder.json;
        else if( strUrl.endsWith( "conf" ) )
            return withSystemProperties ? Binder.hocon : Binder.hoconWithoutSystemProperties;
        else if( strUrl.endsWith( "yaml" ) ) return Binder.yaml;
        else if( strUrl.endsWith( "yml" ) ) return Binder.yaml;
        else return withSystemProperties ? Binder.hocon : Binder.hoconWithoutSystemProperties;
    }

    public static Binder hoconWithConfig( List<String> config ) {
        return hoconWithConfig( true, config );
    }

    public static Binder hoconWithConfig( boolean withSystemProperties, List<String> config ) {
        return new Binder( initialize( new ObjectMapper( new HoconFactoryWithFallback( withSystemProperties, log, config ) ), false, false ) );
    }

    public static Binder hoconWithConfig( Map<String, Object> config ) {
        return new Binder( initialize( new ObjectMapper( new HoconFactoryWithFallback( true, log, config ) ), false, false ) );
    }

    private static Binder hoconWithConfigWithNullInclusion( Map<String, Object> config ) {
        return new Binder( initialize( new ObjectMapper( new HoconFactoryWithFallback( true, log, config ) ), false, true ) );
    }

    private static Binder hoconWithConfigWithNullInclusion( List<String> config ) {
        return new Binder( initialize( new ObjectMapper( new HoconFactoryWithFallback( true, log, config ) ), false, true ) );
    }

    private static ObjectMapper initialize( ObjectMapper mapper, boolean defaultTyping, boolean nonNullInclusion ) {
        if( mapper instanceof XmlMapper ) {
            ( ( XmlMapper ) mapper ).setDefaultUseWrapper( false );
            ( ( XmlMapper ) mapper ).configure( ToXmlGenerator.Feature.WRITE_XML_DECLARATION, true );
        }
        AnnotationIntrospector introspector = new JacksonAnnotationIntrospector();
        mapper.getDeserializationConfig().with( introspector );
        mapper.getSerializationConfig().with( introspector );
        mapper.registerModule( new AfterburnerModule() );
        mapper.registerModule( new Jdk8Module().configureAbsentsAsNulls( true ) );
        mapper.registerModule( new JodaModule() );
        mapper.registerModule( new ExtModule() );
        mapper.registerModule( new ParameterNamesModule( JsonCreator.Mode.DEFAULT ) );
        mapper.enable( DeserializationFeature.USE_LONG_FOR_INTS );
        mapper.enable( JsonParser.Feature.ALLOW_SINGLE_QUOTES );
        mapper.disable( DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES );
        mapper.disable( SerializationFeature.WRITE_DATES_AS_TIMESTAMPS );
        mapper.disable( SerializationFeature.WRITE_EMPTY_JSON_ARRAYS );
        mapper.disable( SerializationFeature.FAIL_ON_EMPTY_BEANS );
        mapper.configure( JsonGenerator.Feature.AUTO_CLOSE_TARGET, false );
        mapper.enable( MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES );
        mapper.setVisibility( PropertyAccessor.FIELD, JsonAutoDetect.Visibility.ANY );

        if( !nonNullInclusion ) mapper.setSerializationInclusion( JsonInclude.Include.NON_NULL );

        modules.forEach( mapper::registerModule );

        if( defaultTyping )
            mapper.enableDefaultTyping( ObjectMapper.DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY );

        return mapper;
    }

    @SuppressWarnings( "unchecked" )
    private static <T extends ReadableInstant> JsonDeserializer<T> forType( Class<T> cls ) {
        return ( JsonDeserializer<T> ) new DateTimeDeserializer( cls, JACKSON_DATE_FORMAT );
    }

    private static <T> TypeReference<T> toTypeReference( TypeRef<T> ref ) {
        return new TypeReference<T>() {
            @Override
            public Type getType() {
                return ref.type();
            }
        };
    }

    public static void update( Object obj, Map<String, Object> values ) {
        try {
            String marshal = json.marshal( obj );
            hoconWithConfigWithNullInclusion( values ).mapper.readerForUpdating( obj ).readValue( marshal );
        } catch( IOException e ) {
            throw new JsonException( e );
        }
    }

    public static void update( Object obj, String json ) {
        try {
            String marshal = Binder.json.marshal( obj );
            hoconWithConfigWithNullInclusion( List.of( json ) ).mapper.readerForUpdating( obj ).readValue( marshal );
        } catch( IOException e ) {
            throw new JsonException( e );
        }
    }

    public ObjectMapper getMapper() {
        return mapper;
    }

    public final JsonGenerator getJsonGenerator( Path path ) {
        try {
            return mapper.getFactory().createGenerator( path.toFile(), JsonEncoding.UTF8 );
        } catch( IOException e ) {
            throw new JsonException( e );
        }
    }

    public final JsonGenerator getJsonGenerator( StringBuilder sb ) {
        try {
            return mapper.getFactory().createGenerator( new StringBuilderWriter( sb ) );
        } catch( IOException e ) {
            throw new JsonException( e );
        }
    }

    public String canonicalize( Class<?> clazz, String json ) {
        return marshal( unmarshal( clazz, json ) );
    }

    public String marshal( Object value ) {
        try {
            return mapper.writeValueAsString( value );
        } catch( IOException e ) {
            throw new JsonException( e );
        }
    }

    public void marshal( Object value, Writer writer ) {
        try {
            mapper.writeValue( writer, value );
        } catch( IOException e ) {
            throw new JsonException( e );
        }
    }

    public <T> String marshal( TypeRef<T> ref, Object value ) {
        try {
            return mapper.writerFor( toTypeReference( ref ) ).writeValueAsString( value );
        } catch( IOException e ) {
            throw new JsonException( e );
        }
    }

    @Deprecated
    public <T> String marshal( TypeReference<T> ref, Object value ) {
        try {
            return mapper.writerFor( ref ).writeValueAsString( value );
        } catch( IOException e ) {
            throw new JsonException( e );
        }
    }

    public void marshal( OutputStream os, Object value ) {
        try {
            mapper.writeValue( os, value );
        } catch( IOException e ) {
            throw new JsonException( e );
        }
    }

    public void marshal( Path path, Object object ) {
        Files.ensureFile( path );

        try( final OutputStream os = IoStreams.out( path ) ) {
            marshal( os, object );
        } catch( IOException e ) {
            throw new JsonException( e );
        }
    }

    public <T> T unmarshal( Class<T> clazz, Path path ) {
        try( val in = IoStreams.in( path ) ) {
            return unmarshal( clazz, in );
        } catch( IOException e ) {
            throw new JsonException( e );
        }
    }

    public <T> T unmarshal( Class<T> clazz, URL url ) {
        try( val in = url.openStream() ) {
            return unmarshal( clazz, in );
        } catch( IOException e ) {
            throw new JsonException( e );
        }
    }

    @SuppressWarnings( "unchecked" )
    public <T> T unmarshal( TypeRef<T> ref, String string ) {
        try {
            return ( T ) mapper.readValue( string, toTypeReference( ref ) );
        } catch( IOException e ) {
            log.trace( "json: " + string );
            throw new JsonException( "json error: " + e.getMessage(), e );
        }
    }

    @SuppressWarnings( "unchecked" )
    public <T> T unmarshal( Reflection type, String string ) {
        try {
            return ( T ) mapper.readValue( string, mapper.getTypeFactory().constructType( type.getType() ) );
        } catch( IOException e ) {
            log.trace( "json: " + string );
            throw new JsonException( "json error: " + e.getMessage(), e );
        }
    }

    @Deprecated
    public <T> T unmarshal( TypeReference<T> ref, String string ) {
        try {
            return ( T ) mapper.readValue( string, ref );
        } catch( IOException e ) {
            log.trace( "json: " + string );
            throw new JsonException( "json error: " + e.getMessage(), e );
        }
    }

    public ObjectReader readerFor( TypeRef<?> ref ) {
        return mapper.readerFor( toTypeReference( ref ) );
    }

    @Deprecated
    public ObjectReader readerFor( TypeReference<?> ref ) {
        return mapper.readerFor( ref );
    }

    public ObjectReader readerForUpdating( Object obj ) {
        return mapper.readerForUpdating( obj );
    }

    public ObjectWriter writerFor( TypeRef<?> ref ) {
        return mapper.writerFor( toTypeReference( ref ) );
    }

    @Deprecated
    public ObjectWriter writerFor( TypeReference<?> ref ) {
        return mapper.writerFor( ref );
    }

    public <T> Optional<T> unmarshal( TypeRef<T> ref, Path path ) {
        if( path != null && Files.exists( path ) ) {
            return Optional.of( unmarshal( ref, IoStreams.in( path ) ) );
        }
        log.warn( "File \"{}\" doesn't exist ", path );
        return Optional.empty();
    }

    @Deprecated
    public <T> T unmarshal( TypeReference<T> ref, Path path ) {
        try( val in = IoStreams.in( path ) ) {
            return unmarshal( ref, in );
        } catch( IOException e ) {
            throw new JsonException( e );
        }
    }

    public <T> T unmarshal( TypeRef<T> ref, URL url ) {
        try( val in = url.openStream() ) {
            return unmarshal( ref, in );
        } catch( IOException e ) {
            throw new JsonException( e );
        }
    }

    @Deprecated
    public <T> T unmarshal( TypeReference<T> ref, URL url ) {
        try( val in = url.openStream() ) {
            return unmarshal( ref, in );
        } catch( IOException e ) {
            throw new JsonException( e );
        }
    }

    @SuppressWarnings( "unchecked" )
    public <T> T unmarshal( TypeRef<T> ref, InputStream is ) {
        try {
            return ( T ) mapper.readValue( is, toTypeReference( ref ) );
        } catch( IOException e ) {
            throw new JsonException( e.getMessage(), e );
        }
    }

    @Deprecated
    @SuppressWarnings( "unchecked" )
    public <T> T unmarshal( TypeReference<T> ref, InputStream is ) {
        try {
            return ( T ) mapper.readValue( is, ref );
        } catch( IOException e ) {
            throw new JsonException( e.getMessage(), e );
        }
    }

    @SuppressWarnings( "unchecked" )
    public <T> T unmarshal( Class<?> clazz, String string ) {
        try {
            return ( T ) mapper.readValue( string, clazz );
        } catch( Exception e ) {
            log.trace( string );
            throw new JsonException( e.getMessage(), e );
        }
    }

    @SuppressWarnings( "unchecked" )
    public <T> T unmarshal( Class<?> clazz, Map<String, Object> map ) {
        try {
            return ( T ) mapper.convertValue( map, clazz );
        } catch( Exception e ) {
            log.trace( String.valueOf( map ) );
            throw new JsonException( e.getMessage(), e );
        }
    }

    @SuppressWarnings( "unchecked" )
    public <T> T unmarshal( TypeRef<T> ref, Object fromValue ) {
        try {
            return ( T ) mapper.convertValue( fromValue, toTypeReference( ref ) );
        } catch( Exception e ) {
            log.trace( String.valueOf( fromValue ) );
            throw new JsonException( e.getMessage(), e );
        }
    }

    @SuppressWarnings( "unchecked" )
    public <T> T unmarshal( Class<?> clazz, List<Object> map ) {
        try {
            return ( T ) mapper.convertValue( map, clazz );
        } catch( Exception e ) {
            log.trace( String.valueOf( map ) );
            throw new JsonException( e.getMessage(), e );
        }
    }

    @SuppressWarnings( "unchecked" )
    public <T> T unmarshal( TypeRef<T> ref, Map<String, Object> map ) {
        try {
            return ( T ) mapper.convertValue( map, toTypeReference( ref ) );
        } catch( Exception e ) {
            log.trace( String.valueOf( map ) );
            throw new JsonException( e.getMessage(), e );
        }
    }

    @Deprecated
    @SuppressWarnings( "unchecked" )
    public <T> T unmarshal( TypeReference<T> ref, Map<String, Object> map ) {
        try {
            return ( T ) mapper.convertValue( map, ref );
        } catch( Exception e ) {
            log.trace( String.valueOf( map ) );
            throw new JsonException( e.getMessage(), e );
        }
    }

    @SuppressWarnings( "unchecked" )
    public <T> T unmarshal( TypeRef<T> ref, List<Object> list ) {
        try {
            return ( T ) mapper.convertValue( list, toTypeReference( ref ) );
        } catch( Exception e ) {
            log.trace( String.valueOf( list ) );
            throw new JsonException( e.getMessage(), e );
        }
    }

    @SuppressWarnings( "unchecked" )
    public <T> T unmarshal( Class<?> clazz, InputStream json ) {
        try {
            return ( T ) mapper.readValue( json, clazz );
        } catch( IOException e ) {
            throw new JsonException( e.getMessage(), e );
        }
    }

    public <T> T unmarshalResource( Class<?> context, Class<T> clazz, String resourceJsonPath ) {
        try( InputStream is = context.getResourceAsStream( resourceJsonPath ) ) {
            if( is == null ) throw new JsonException( "not found " + resourceJsonPath );
            return this.unmarshal( clazz, is );
        } catch( IOException e ) {
            throw new JsonException( e );
        }

    }

    @SuppressWarnings( "unchecked" )
    public <T> T clone( T object ) {
        return unmarshal( object.getClass(), marshal( object ) );
    }

    @SneakyThrows
    public void marshal( Path path, Iterable<?> iterable ) {
        try( OutputStream out = IoStreams.out( path, from( path ), DEFAULT_BUFFER, false, true ) ) {
            out.write( BEGIN_ARRAY );
            val it = iterable.iterator();
            while( it.hasNext() ) {
                marshal( out, it.next() );
                if( it.hasNext() ) out.write( ITEM_SEP );
            }
            out.write( END_ARRAY );
        }
    }

}

