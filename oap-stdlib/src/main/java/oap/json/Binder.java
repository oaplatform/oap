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
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.Nulls;
import de.undercouch.bson4jackson.BsonFactory;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import oap.io.Files;
import oap.io.IoStreams;
import oap.io.Resources;
import oap.io.StringBuilderWriter;
import oap.json.ext.ExtModule;
import oap.reflect.Coercions;
import oap.reflect.Reflect;
import oap.reflect.Reflection;
import oap.reflect.TypeRef;
import oap.util.Dates;
import oap.util.FastByteArrayOutputStream;
import oap.util.function.Try;
import org.joda.time.ReadableInstant;
import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonEncoding;
import tools.jackson.core.JsonGenerator;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.core.StreamWriteFeature;
import tools.jackson.core.json.JsonFactory;
import tools.jackson.core.json.JsonReadFeature;
import tools.jackson.core.type.TypeReference;
import tools.jackson.core.util.DefaultIndenter;
import tools.jackson.core.util.DefaultPrettyPrinter;
import tools.jackson.databind.DefaultTyping;
import tools.jackson.databind.DeserializationFeature;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.MapperFeature;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.ObjectReader;
import tools.jackson.databind.ObjectWriter;
import tools.jackson.databind.SerializationFeature;
import tools.jackson.databind.ValueDeserializer;
import tools.jackson.databind.cfg.DateTimeFeature;
import tools.jackson.databind.cfg.MapperBuilder;
import tools.jackson.databind.json.JsonMapper;
import tools.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import tools.jackson.dataformat.xml.XmlFactory;
import tools.jackson.dataformat.xml.XmlMapper;
import tools.jackson.dataformat.xml.XmlWriteFeature;
import tools.jackson.dataformat.yaml.YAMLFactory;
import tools.jackson.dataformat.yaml.YAMLMapper;
import tools.jackson.datatype.joda.JodaModule;
import tools.jackson.datatype.joda.cfg.JacksonJodaDateFormat;
import tools.jackson.datatype.joda.deser.DateTimeDeserializer;
import tools.jackson.module.afterburner.AfterburnerModule;

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

import static java.nio.charset.StandardCharsets.UTF_8;
import static oap.io.IoStreams.DEFAULT_BUFFER;
import static oap.io.IoStreams.Encoding.from;

@Slf4j
public class Binder {
    public static final Binder hocon;
    public static final Binder hoconWithoutSystemProperties;
    public static final Binder json;
    public static final Binder bson;
    public static final Binder jsonWithTyping;
    public static final Binder xml;
    public static final Binder xmlWithTyping;
    public static final Binder yaml;
    private static final JacksonJodaDateFormat JACKSON_DATE_FORMAT = new JacksonJodaDateFormat( Dates.PARSER_FULL );
    private static final byte[] BEGIN_ARRAY = "[".getBytes();
    private static final byte[] END_ARRAY = "]".getBytes();
    private static final byte[] ITEM_SEP = ",".getBytes();
    private static final Set<JacksonModule> modules;

    static {
        modules = Resources
            .lines( "META-INF/jackson.modules" )
            .map( Try.map( clazz -> ( JacksonModule ) Class.forName( clazz ).getDeclaredConstructor().newInstance() ) )
            .toSet();

        BsonFactory bsonFactory = new BsonFactory();

        JsonFactory jsonFactory = JsonFactory.builder()
            .enable( StreamReadFeature.USE_FAST_DOUBLE_PARSER )
            .enable( StreamReadFeature.USE_FAST_BIG_NUMBER_PARSER )
            .build();

        XmlFactory xmlFactory = XmlFactory.builder()
            .enable( StreamReadFeature.USE_FAST_DOUBLE_PARSER )
            .enable( StreamReadFeature.USE_FAST_BIG_NUMBER_PARSER )
            .build();

        OapHoconFactory hoconFactory = new OapHoconFactory();
        HoconFactoryWithSystemProperties hoconFactoryWithSystemProperties = new HoconFactoryWithSystemProperties( log );

        YAMLFactory yamlFactory = YAMLFactory.builder()
            .enable( StreamReadFeature.USE_FAST_DOUBLE_PARSER )
            .enable( StreamReadFeature.USE_FAST_BIG_NUMBER_PARSER )
            .build();

        json = new Binder( initialize( JsonMapper.builder( jsonFactory ), false, false, true ) );
        jsonWithTyping = new Binder( initialize( JsonMapper.builder( jsonFactory ), true, false, true ) );
        bson = new Binder( initialize( JsonMapper.builder( bsonFactory ), false, false, true ) );
        xml = new Binder( initialize( XmlMapper.builder( xmlFactory ), false, false, true ) );
        xmlWithTyping = new Binder( initialize( XmlMapper.builder( xmlFactory ), true, false, true ) );
        hoconWithoutSystemProperties =
            new Binder( initialize( JsonMapper.builder( hoconFactory ), false, false, true ) );
        hocon =
            new Binder( initialize( JsonMapper.builder( hoconFactoryWithSystemProperties ), false, false, true ) );
        yaml = new Binder( initialize( YAMLMapper.builder( yamlFactory ), false, false, true ) );
    }

    private final ObjectMapper mapper;

    public Binder( ObjectMapper mapper ) {
        this.mapper = mapper;
    }

    /**
     * @see Format#of(URL, boolean)
     */
    @Deprecated( forRemoval = true )
    public static Binder getBinder( URL url ) {
        return getBinder( url, true );
    }

    /**
     * @see Format#of(URL, boolean)
     */
    @Deprecated( forRemoval = true )
    public static Binder getBinder( URL url, boolean withSystemProperties ) {
        return Format.of( url, withSystemProperties ).binder;
    }

    public static Binder hoconWithConfig( List<String> config ) {
        return hoconWithConfig( true, config );
    }

    public static Binder hoconWithConfig( boolean withSystemProperties, List<String> config ) {
        return new Binder( initialize( JsonMapper.builder( new HoconFactoryWithFallback( withSystemProperties, log, config ) ), false, false, true ) );
    }

    public static Binder hoconWithConfig( Map<String, Object> config ) {
        return new Binder( initialize( JsonMapper.builder( new HoconFactoryWithFallback( true, log, config ) ), false, false, true ) );
    }

    private static Binder hoconWithConfigWithNullInclusion( Map<String, Object> config ) {
        return hoconWithConfigWithNullInclusion( config, true );
    }

    private static Binder hoconWithConfigWithNullInclusion( Map<String, Object> config, boolean skipInputNulls ) {
        return new Binder( initialize( JsonMapper.builder( new HoconFactoryWithFallback( true, log, config ) ), false, true, skipInputNulls ) );
    }

    private static Binder hoconWithConfigWithNullInclusion( List<String> config ) {
        return hoconWithConfigWithNullInclusion( config, true );
    }

    private static Binder hoconWithConfigWithNullInclusion( List<String> config, boolean skipInputNulls ) {
        return new Binder( initialize( JsonMapper.builder( new HoconFactoryWithFallback( true, log, config ) ), false, true, skipInputNulls ) );
    }

    private static ObjectMapper initialize( MapperBuilder<?, ?> builder, boolean defaultTyping, boolean nonNullInclusion, boolean skipInputNulls ) {
        if( builder instanceof XmlMapper.Builder xmlBuilder ) {
            xmlBuilder.defaultUseWrapper( false );
            xmlBuilder.enable( XmlWriteFeature.WRITE_XML_DECLARATION );
        }
        builder.addModule( new AfterburnerModule() );
        builder.addModule( new JodaModule() );
        builder.addModule( new ExtModule() );
        builder.enable( DeserializationFeature.USE_LONG_FOR_INTS );
        if( builder instanceof JsonMapper.Builder jsonBuilder ) jsonBuilder.enable( JsonReadFeature.ALLOW_SINGLE_QUOTES );
        builder.disable( DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES );
        builder.disable( DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES );
        builder.disable( DeserializationFeature.FAIL_ON_TRAILING_TOKENS );
        builder.disable( DateTimeFeature.WRITE_DATES_AS_TIMESTAMPS );
        builder.disable( SerializationFeature.FAIL_ON_EMPTY_BEANS );
        builder.disable( MapperFeature.SORT_PROPERTIES_ALPHABETICALLY );
        builder.disable( StreamWriteFeature.AUTO_CLOSE_TARGET );
        builder.enable( MapperFeature.ACCEPT_CASE_INSENSITIVE_PROPERTIES );
        builder.enable( MapperFeature.ALLOW_FINAL_FIELDS_AS_MUTATORS );
        builder.constructorDetector( tools.jackson.databind.cfg.ConstructorDetector.DEFAULT.withAllowImplicitWithDefaultConstructor( false ) );
        builder.polymorphicTypeValidator( BasicPolymorphicTypeValidator.builder().allowIfSubType( Object.class ).build() );
        builder.changeDefaultVisibility( v -> v.withFieldVisibility( JsonAutoDetect.Visibility.ANY ).withCreatorVisibility( JsonAutoDetect.Visibility.ANY ) );
        builder.enable( DeserializationFeature.ACCEPT_SINGLE_VALUE_AS_ARRAY );
        if( skipInputNulls ) builder.changeDefaultNullHandling( v -> JsonSetter.Value.forValueNulls( Nulls.SKIP ) );
        if( !nonNullInclusion ) builder.changeDefaultPropertyInclusion( v -> JsonInclude.Value.construct( JsonInclude.Include.NON_NULL, JsonInclude.Include.ALWAYS ) );
        // Preserve WRITE_EMPTY_JSON_ARRAYS=false for all container implementations,
        // while keeping empty strings and explicit inclusion annotations.
        builder.annotationIntrospector( tools.jackson.databind.introspect.AnnotationIntrospectorPair.pair(
            builder.annotationIntrospector(), new tools.jackson.databind.introspect.NopAnnotationIntrospector() {
                @Override
                public JsonInclude.Value findPropertyInclusion( tools.jackson.databind.cfg.MapperConfig<?> config, tools.jackson.databind.introspect.Annotated annotated ) {
                    Class<?> type = annotated.getRawType();
                    return type.isArray() || java.util.Collection.class.isAssignableFrom( type ) || Map.class.isAssignableFrom( type )
                        ? JsonInclude.Value.construct( JsonInclude.Include.NON_EMPTY, JsonInclude.Include.ALWAYS ) : JsonInclude.Value.empty();
                }
            } ) );
        builder.withConfigOverride( Optional.class, c -> c.setInclude( JsonInclude.Value.construct( JsonInclude.Include.NON_ABSENT, JsonInclude.Include.ALWAYS ) ) );
        modules.forEach( builder::addModule );
        if( defaultTyping )
            builder.activateDefaultTyping( BasicPolymorphicTypeValidator.builder().allowIfSubType( Object.class ).build(), DefaultTyping.NON_FINAL, JsonTypeInfo.As.PROPERTY );
        return builder.build();
    }

    @SuppressWarnings( "unchecked" )
    private static <T extends ReadableInstant> ValueDeserializer<T> forType( Class<T> clazz ) {
        return ( ValueDeserializer<T> ) new DateTimeDeserializer( clazz, JACKSON_DATE_FORMAT );
    }

    private static <T> TypeReference<T> toTypeReference( TypeRef<T> ref ) {
        return new TypeReference<>() {
            @Override
            public Type getType() {
                return ref.type();
            }
        };
    }

    public static void update( Object obj, Map<String, Object> values ) {
        try {
            var marshal = json.marshal( obj );
            hoconWithConfigWithNullInclusion( values, false ).mapper.readerForUpdating( obj ).readValue( marshal );
        } catch( JacksonException e ) {
            throw new JsonException( e );
        }
    }

    public static void update( Object obj, String json ) {
        try {
            String marshal = Binder.json.marshal( obj );
            hoconWithConfigWithNullInclusion( List.of( json ), false ).mapper.readerForUpdating( obj ).readValue( marshal );
        } catch( JacksonException e ) {
            throw new JsonException( e );
        }
    }

    private static String getLimitation( String json ) {
        if( json != null && json.length() > 20 ) return json.substring( 0, 20 ) + "...";
        return json;
    }

    private static String getLimitation( byte[] json ) {
        if( json != null && json.length > 20 ) return new String( json, UTF_8 ).substring( 0, 20 ) + "...";
        return json != null ? new String( json, UTF_8 ) : null;
    }

    public ObjectMapper getMapper() {
        return mapper;
    }

    public final JsonGenerator getJsonGenerator( Path path ) {
        try {
            return mapper.createGenerator( path.toFile(), JsonEncoding.UTF8 );
        } catch( JacksonException e ) {
            throw new JsonException( e );
        }
    }

    public final JsonGenerator getJsonGenerator( StringBuilder sb ) {
        try {
            return mapper.createGenerator( new StringBuilderWriter( sb ) );
        } catch( JacksonException e ) {
            throw new JsonException( e );
        }
    }

    public String canonicalize( Class<?> clazz, String json ) {
        return marshal( unmarshal( clazz, json ) );
    }

    public String canonicalizeWithDefaultPrettyPrinter( Class<?> clazz, String json ) {
        return marshalWithDefaultPrettyPrinter( unmarshal( clazz, json ) );
    }

    public String canonicalizeWithDefaultPrettyPrinter( TypeRef<?> typeRef, String json ) {
        return marshalWithDefaultPrettyPrinter( unmarshal( typeRef, json ) );
    }

    public String marshalWithDefaultPrettyPrinter( Object value ) {
        return marshal( value, true );
    }

    public String marshal( Object value ) {
        return marshal( value, false );
    }

    public String marshal( Object value, boolean prettyPrinter ) {
        try {
            if( prettyPrinter )
                return mapper.writerWithDefaultPrettyPrinter().with( new DefaultPrettyPrinter().withObjectIndenter( new DefaultIndenter().withLinefeed( "\n" ) ) ).writeValueAsString( value );
            else return mapper.writeValueAsString( value );
        } catch( JacksonException e ) {
            throw new JsonException( e );
        }
    }

    public void marshal( Object value, StringBuilder sb ) {
        try( StringBuilderWriter writer = new StringBuilderWriter( sb ) ) {
            marshal( value, writer, false );
        }
    }

    public void marshal( Object value, StringBuilder sb, boolean prettyPrinter ) {
        try( StringBuilderWriter writer = new StringBuilderWriter( sb ) ) {
            marshal( value, writer, prettyPrinter );
        }
    }

    public void marshal( Object value, Writer writer ) {
        marshal( value, writer, false );
    }

    public void marshal( Object value, Writer writer, boolean prettyPrinter ) {
        try {
            if( prettyPrinter ) {
                DefaultPrettyPrinter defaultPrettyPrinter = new DefaultPrettyPrinter();
                defaultPrettyPrinter.indentArraysWith( DefaultIndenter.SYSTEM_LINEFEED_INSTANCE );
                mapper.writer().with( defaultPrettyPrinter ).writeValue( writer, value );
            } else {
                mapper.writeValue( writer, value );
            }
        } catch( JacksonException e ) {
            throw new JsonException( e );
        }
    }

    public void marshal( Object value, OutputStream outputStream ) {
        marshal( value, outputStream, false );
    }

    public void marshal( Object value, OutputStream outputStream, boolean prettyPrinter ) {
        try {
            if( prettyPrinter ) mapper.writerWithDefaultPrettyPrinter().writeValue( outputStream, value );
            else
                mapper.writeValue( outputStream, value );
        } catch( JacksonException e ) {
            throw new JsonException( "Cannot serialize from class: " + value.getClass().getCanonicalName(), e );
        }
    }

    public <T> String marshal( TypeRef<T> ref, Object value ) {
        try {
            return mapper.writerFor( toTypeReference( ref ) ).writeValueAsString( value );
        } catch( JacksonException e ) {
            throw new JsonException( "Cannot serialize from class: " + ref.type().getClass().getCanonicalName(), e );
        }
    }

    @Deprecated
    public <T> String marshal( TypeReference<T> ref, Object value ) {
        try {
            return mapper.writerFor( ref ).writeValueAsString( value );
        } catch( JacksonException e ) {
            throw new JsonException( "Cannot serialize from class: " + value.getClass().getCanonicalName(), e );
        }
    }

    public void marshal( OutputStream os, Object value ) {
        try {
            mapper.writeValue( os, value );
        } catch( JacksonException e ) {
            throw new JsonException( "Cannot serialize from class: " + value.getClass().getCanonicalName(), e );
        }
    }

    public void marshal( Path path, Object value ) {
        Files.ensureFile( path );

        try( OutputStream os = IoStreams.out( path ) ) {
            marshal( os, value );
        } catch( IOException | JacksonException e ) {
            throw new JsonException( "Cannot serialize from class: " + value.getClass().getCanonicalName(), e );
        }
    }

    @SneakyThrows
    public void marshal( Path path, Iterable<?> iterable ) {
        try( OutputStream out = IoStreams.out( path, from( path ), DEFAULT_BUFFER, false, true ) ) {
            out.write( BEGIN_ARRAY );
            var it = iterable.iterator();
            while( it.hasNext() ) {
                marshal( out, it.next() );
                if( it.hasNext() ) out.write( ITEM_SEP );
            }
            out.write( END_ARRAY );
        }
    }

    @SuppressWarnings( "unchecked" )
    public <T> T unmarshalFromAny( Class<T> clazz, Object any ) throws JsonException {
        if( any instanceof URL ) {
            return unmarshal( clazz, ( URL ) any );
        } else if( any instanceof Path ) {
            return unmarshal( clazz, ( Path ) any );
        } else if( any instanceof String ) {
            try {
                return unmarshal( clazz, ( String ) any );
            } catch( JsonException e ) {
                return unmarshal( clazz, ( URL ) Coercions.basic().cast( Reflect.reflect( URL.class ), any ) );
            }
        } else {
            return unmarshal( clazz, json.marshal( any ) );
        }
    }

    public <T> T unmarshal( Class<T> clazz, Path path ) throws JsonException {
        try( var in = IoStreams.in( path ) ) {
            return unmarshal( clazz, in );
        } catch( JsonException e ) {
            throw new JsonException( "Cannot deserialize to class: " + clazz.getCanonicalName() + " from path " + path, e.getCause() );
        } catch( IOException e ) {
            throw new JsonException( "Cannot deserialize to class: " + clazz.getCanonicalName() + " from path " + path, e );
        }
    }

    public <T> T unmarshal( Class<T> clazz, URL url ) throws JsonException {
        try( var reader = OapHoconFactory.open( url ) ) {
            return mapper.readValue( reader, clazz );
        } catch( IOException | JacksonException e ) {
            throw new JsonException( "Cannot deserialize to class: " + clazz.getCanonicalName(), e );
        }
    }

    public <T> T unmarshal( TypeRef<T> ref, String string ) {
        try {
            return mapper.readValue( string, toTypeReference( ref ) );
        } catch( JacksonException e ) {
            log.trace( "json: {}", string );
            throw new JsonException( "Cannot deserialize to class: " + ref.type().getClass().getCanonicalName(), e );
        }
    }

    public <T> T unmarshal( Reflection type, String string ) throws JsonException {
        try {
            return mapper.readValue( string, mapper.getTypeFactory().constructType( type.getType() ) );
        } catch( JacksonException e ) {
            log.trace( "json: {}", string );
            throw new JsonException( "Cannot deserialize to class: " + type.getType().getClass().getCanonicalName(), e );
        }
    }

    public <T> T unmarshal( Reflection type, URL url ) throws JsonException {
        try( var reader = OapHoconFactory.open( url ) ) {
            return mapper.readValue( reader, mapper.getTypeFactory().constructType( type.getType() ) );
        } catch( IOException | JacksonException e ) {
            log.trace( "url: {}", url );
            throw new JsonException( "Cannot deserialize to class: " + type.getType().getClass().getCanonicalName(), e );
        }
    }

    public <T> T unmarshal( Reflection type, Path path ) throws JsonException {
        try( var is = java.nio.file.Files.newInputStream( path ) ) {
            return mapper.readValue( is, mapper.getTypeFactory().constructType( type.getType() ) );
        } catch( IOException | JacksonException e ) {
            log.trace( "path: {}", path );
            throw new JsonException( "Cannot deserialize to class: " + type.getType().getClass().getCanonicalName(), e );
        }
    }

    @Deprecated
    public <T> T unmarshal( TypeReference<T> ref, String string ) throws JsonException {
        try {
            return mapper.readValue( string, ref );
        } catch( JacksonException e ) {
            log.trace( "json: " + string );
            throw new JsonException( "Cannot deserialize to class: " + ref.getType().getClass().getCanonicalName(), e );
        }
    }

    public <T> Optional<T> unmarshal( TypeRef<T> ref, Path path ) throws JsonException {
        if( path != null && Files.exists( path ) ) {
            return Optional.of( unmarshal( ref, IoStreams.in( path ) ) );
        }
        log.warn( "File \"{}\" doesn't exist ", path );
        return Optional.empty();
    }

    @Deprecated
    public <T> T unmarshal( TypeReference<T> ref, Path path ) throws JsonException {
        try( var in = IoStreams.in( path ) ) {
            return unmarshal( ref, in );
        } catch( IOException | JacksonException e ) {
            throw new JsonException( "Cannot deserialize to class: " + ref.getType().getClass().getCanonicalName(), e );
        }
    }

    public <T> T unmarshal( TypeRef<T> ref, URL url ) throws JsonException {
        try( var in = url.openStream() ) {
            return unmarshal( ref, in );
        } catch( IOException | JacksonException e ) {
            throw new JsonException( "Cannot deserialize to class: " + ref.type().getClass().getCanonicalName(), e );
        }
    }

    @Deprecated
    public <T> T unmarshal( TypeReference<T> ref, URL url ) throws JsonException {
        try( var in = url.openStream() ) {
            return unmarshal( ref, in );
        } catch( IOException | JacksonException e ) {
            throw new JsonException( "Cannot deserialize to class: " + ref.getType().getClass().getCanonicalName(), e );
        }
    }

    public <T> T unmarshal( TypeRef<T> ref, InputStream is ) throws JsonException {
        try {
            return mapper.readValue( is, toTypeReference( ref ) );
        } catch( JacksonException e ) {
            throw new JsonException( "Cannot deserialize to class: " + ref.type().getClass().getCanonicalName(), e );
        }
    }

    @Deprecated
    public <T> T unmarshal( TypeReference<T> ref, InputStream is ) throws JsonException {
        try {
            return mapper.readValue( is, ref );
        } catch( JacksonException e ) {
            throw new JsonException( "Cannot deserialize to class: " + ref.getType().getClass().getCanonicalName(), e );
        }
    }

    public <T> T unmarshal( Class<T> clazz, String json ) throws JsonException {
        try {
            return mapper.readValue( json, clazz );
        } catch( Exception e ) {
            log.trace( "Cannot deserialize [{}] into {}", json, clazz.getCanonicalName() );
            throw new JsonException( "Cannot deserialize [" + getLimitation( json ) + "] to class: " + clazz.getCanonicalName(), e );
        }
    }

    public <T> T unmarshal( Class<T> clazz, byte[] json ) throws JsonException {
        try {
            return mapper.readValue( json, clazz );
        } catch( Exception e ) {
            log.trace( "Cannot deserialize [{}] into {}", json, clazz.getCanonicalName() );
            throw new JsonException( "Cannot deserialize [" + getLimitation( json ) + "] to class: " + clazz.getCanonicalName(), e );
        }
    }

    public <T> T unmarshal( Class<T> clazz, Map<String, Object> map ) throws JsonException {
        try {
            return mapper.convertValue( map, clazz );
        } catch( Exception e ) {
            log.trace( String.valueOf( map ) );
            throw new JsonException( "Cannot deserialize [" + getLimitation( map.toString() ) + "] to class: " + clazz.getCanonicalName(), e );
        }
    }

    public <T> T unmarshal( TypeRef<T> ref, byte[] bytes ) throws JsonException {
        try {
            return mapper.readValue( bytes, toTypeReference( ref ) );
        } catch( Exception e ) {
            throw new JsonException( "Cannot deserialize to class: " + ref.type().getClass().getCanonicalName(), e );
        }
    }

    public <T> T unmarshal( TypeRef<T> ref, byte[] bytes, int offset, int len ) throws JsonException {
        try {
            return mapper.readValue( bytes, offset, len, toTypeReference( ref ) );
        } catch( Exception e ) {
            throw new JsonException( "Cannot deserialize to class: " + ref.type().getClass().getCanonicalName(), e );
        }
    }

    public <T> T unmarshal( TypeRef<T> ref, Object fromValue ) throws JsonException {
        try {
            return mapper.convertValue( fromValue, toTypeReference( ref ) );
        } catch( Exception e ) {
            log.trace( String.valueOf( fromValue ) );
            throw new JsonException( "Cannot deserialize to class: " + ref.type().getClass().getCanonicalName(), e );
        }
    }

    public <T> T unmarshal( Class<T> clazz, List<Object> map ) throws JsonException {
        try {
            return mapper.convertValue( map, clazz );
        } catch( Exception e ) {
            log.trace( String.valueOf( map ) );
            throw new JsonException( "Cannot deserialize to class: " + clazz.getCanonicalName(), e );
        }
    }

    public <T> T unmarshal( TypeRef<T> ref, Map<String, Object> map ) throws JsonException {
        try {
            return mapper.convertValue( map, toTypeReference( ref ) );
        } catch( Exception e ) {
            log.trace( String.valueOf( map ) );
            throw new JsonException( "Cannot deserialize to class: " + ref.type().getClass().getCanonicalName(), e );
        }
    }

    @Deprecated
    public <T> T unmarshal( TypeReference<T> ref, Map<String, Object> map ) throws JsonException {
        try {
            return mapper.convertValue( map, ref );
        } catch( Exception e ) {
            log.trace( String.valueOf( map ) );
            throw new JsonException( "Cannot deserialize to class: " + ref.getType().getClass().getCanonicalName(), e );
        }
    }

    public <T> T unmarshal( TypeRef<T> ref, List<Object> list ) throws JsonException {
        try {
            return mapper.convertValue( list, toTypeReference( ref ) );
        } catch( Exception e ) {
            log.trace( String.valueOf( list ) );
            throw new JsonException( "Cannot deserialize to class: " + ref.type().getClass().getCanonicalName(), e );
        }
    }

    public <T> T unmarshal( Class<T> clazz, InputStream json ) throws JsonException {
        try {
            return mapper.readValue( json, clazz );
        } catch( JacksonException e ) {
            throw new JsonException( "Cannot deserialize to class: " + clazz.getCanonicalName(), e );
        }
    }

    public <T> T unmarshalResource( Class<?> context, Class<T> clazz, String resourceJsonPath ) throws JsonException {
        try( InputStream is = context.getResourceAsStream( resourceJsonPath ) ) {
            if( is == null ) throw new JsonException( "not found " + resourceJsonPath );
            return this.unmarshal( clazz, is );
        } catch( IOException | JacksonException e ) {
            throw new JsonException( "Cannot deserialize to class: " + clazz.getCanonicalName(), e );
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

    @SuppressWarnings( "unchecked" )
    public <T> T clone( T object ) {
        FastByteArrayOutputStream baos = new FastByteArrayOutputStream();
        marshal( object, baos );
        return unmarshal( ( Class<T> ) object.getClass(), baos.getInputStream() );
    }

    public enum Format {
        JSON( Binder.json ),
        HOCON( Binder.hocon ),
        HOCON_WO_SYSTEM_PROPERTIES( Binder.hoconWithoutSystemProperties ),
        YAML( Binder.yaml );

        public final Binder binder;

        Format( Binder binder ) {
            this.binder = binder;
        }

        public static Format of( URL url, boolean withSystemProperties ) {
            var path = url.toString().toLowerCase();
            return of( path, withSystemProperties );
        }

        public static Format of( String path, boolean withSystemProperties ) {
            if( path.endsWith( "json" ) ) return JSON;
            else if( path.endsWith( "yaml" ) || path.endsWith( "yml" ) ) return YAML;
            return withSystemProperties ? HOCON : HOCON_WO_SYSTEM_PROPERTIES;
        }
    }
}
