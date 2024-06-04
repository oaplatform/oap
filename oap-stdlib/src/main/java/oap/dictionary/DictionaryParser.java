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

package oap.dictionary;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.util.DefaultIndenter;
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import oap.io.Resources;
import oap.io.content.ContentReader;
import oap.json.Binder;
import oap.util.Lists;
import oap.util.function.Try;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.lang.Math.max;
import static java.util.Collections.emptyList;
import static java.util.Collections.emptyMap;

public class DictionaryParser {
    private static final String NAME = "name";
    private static final String ID = "id";
    private static final String ENABLED = "enabled";
    private static final String EXTERNAL_ID = "eid";
    private static final String VALUES = "values";
    private static final Set<String> defaultFields = new HashSet<>();
    private static final Function<Object, Optional<Integer>> intFunc = str -> switch( str ) {
        case Long l -> Optional.of( l.intValue() );
        case Double v -> Optional.of( v.intValue() );
        case String s when s.length() == 1 -> Optional.of( ( int ) s.charAt( 0 ) );
        case null, default -> Optional.empty();
    };

    static {
        defaultFields.add( ID );
        defaultFields.add( ENABLED );
        defaultFields.add( EXTERNAL_ID );
    }

    @SuppressWarnings( "unchecked" )
    private static Dictionary parseAsDictionaryValue( Object value, String path, boolean valueAsRoot, IdStrategy idStrategy ) {
        if( value instanceof Map ) {
            Map<Object, Object> valueMap = ( Map<Object, Object> ) value;
            List<Dictionary> values = emptyList();

            HashMap<String, Object> properties = new HashMap<>();
            for( Map.Entry<Object, Object> e : valueMap.entrySet() ) {
                String propertyName = e.getKey().toString();
                if( !defaultFields.contains( propertyName ) && ( !valueAsRoot || !NAME.equals( propertyName ) ) ) {
                    Object propertyValue = e.getValue();

                    if( VALUES.equals( propertyName ) )
                        values = parseValues( ( List<?> ) propertyValue, path, idStrategy );
                    else
                        properties.put( propertyName, propertyValue );
                }
            }


            Map<String, Object> p = properties.isEmpty() ? emptyMap() : properties;

            if( valueAsRoot ) {
                String name = getString( valueMap, NAME );

                return new DictionaryRoot( name, values, p );
            }

            Extends anExtends = getExtendsOpt( valueMap ).orElse( null );
            if( anExtends != null ) return new DictionaryExtends( anExtends );

            String id = getString( valueMap, ID );
            boolean enabled = getBooleanOpt( valueMap, ENABLED ).orElse( true );
            int externalId = idStrategy.get( valueMap );

            return values.isEmpty()
                ? new DictionaryLeaf( id, enabled, externalId, p )
                : new DictionaryValue( id, enabled, externalId, values, p );
        } else {
            throw new DictionaryFormatError(
                "value " + path + " type "
                    + ( value == null ? "<NULL>" : value.getClass().toString() ) + " != " + Map.class
            );
        }
    }

    public static DictionaryRoot parse( Path path ) {
        return parse( path, new AutoIdStrategy() );
    }

    public static DictionaryRoot parse( Path path, IdStrategy idStrategy ) {
        Map<?, ?> map = Binder.hoconWithoutSystemProperties.unmarshal( Map.class, path );
        return parse( map, idStrategy );
    }

    public static DictionaryRoot parse( URL resource ) {
        return parse( resource, new AutoIdStrategy() );
    }

    public static DictionaryRoot parse( URL resource, IdStrategy idStrategy ) {
        Map<?, ?> map = Binder.Format.of( resource, false ).binder.unmarshal( Map.class, resource );
        return parse( map, idStrategy );
    }

    public static DictionaryRoot parse( String resource ) {
        return parse( resource, new AutoIdStrategy() );
    }

    public static DictionaryRoot parse( String resource, IdStrategy idStrategy ) {
        Map<?, ?> map = Binder.hoconWithoutSystemProperties.unmarshalResource( DictionaryParser.class, Map.class, resource );

        String baseName = FilenameUtils.getBaseName( resource );
        String extResources = FilenameUtils.removeExtension( resource ) + "/" + baseName + ".conf";
        if( extResources.startsWith( "/" ) ) extResources = extResources.substring( 1 );

        List<URL> urls = Resources.urls( extResources );

        List<String> maps = Lists.map( urls, url -> ContentReader.read( url, ContentReader.ofString() ) );

        Map<?, ?> extMap = Binder.hoconWithConfig( false, maps ).unmarshal( Map.class, Binder.json.marshal( map ) );

        return parse( extMap, idStrategy );
    }

    public static DictionaryRoot parse( Map<?, ?> map, IdStrategy idStrategy ) {
        DictionaryRoot dictionaryRoot = ( DictionaryRoot ) parseAsDictionaryValue( map, "", true, idStrategy );
        ArrayList<InvalidEntry> invalid = new ArrayList<>();

        int lastId = idStrategy.getMaxExtendsId( dictionaryRoot );

        resolveExtends( dictionaryRoot, dictionaryRoot, new AtomicInteger( lastId ) );
        validate( "", invalid, dictionaryRoot );

        if( !invalid.isEmpty() ) {
            invalid.sort( Comparator.comparing( l -> l.path ) );
            String msg = invalid
                .stream()
                .map( e -> "path: " + e.path + "; eid: " + e.one.getExternalId() + "; one: " + e.one.getId() + "; two: " + e.two.getId() )
                .collect( Collectors.joining( ", " ) );

            throw new DictionaryError( "duplicate eid: " + msg );

        }

        return dictionaryRoot;
    }

    public static DictionaryRoot parseFromString( String dictionary ) {
        Map<?, ?> map = Binder.hoconWithoutSystemProperties.unmarshal( Map.class, dictionary );
        return parse( map, new AutoIdStrategy() );
    }

    @SuppressWarnings( "unchecked" )
    private static void resolveExtends( DictionaryRoot dictionaryRoot, List<? extends Dictionary> values, AtomicInteger id ) {
        ListIterator<Dictionary> iterator = ( ListIterator<Dictionary> ) values.listIterator();
        int lastExtendsId = -1;
        while( iterator.hasNext() ) {
            Dictionary child = iterator.next();
            if( child instanceof DictionaryExtends ) {
                iterator.remove();

                Extends anExtends = ( ( DictionaryExtends ) child ).anExtends;
                List<? extends Dictionary> eValues = getValues( dictionaryRoot, anExtends );
                resolveExtends( dictionaryRoot, eValues, id );

                for( Dictionary v : eValues ) {
                    if( anExtends.filter.isEmpty() || anExtends.filter.filter( f -> v.getTags().contains( f ) ).isPresent() ) {
                        if( Lists.anyMatch( values, pv -> !( pv instanceof DictionaryExtends ) && pv.getId().equals( v.getId() ) ) ) {
                            if( !anExtends.ignoreDuplicate )
                                throw new DictionaryError( "duplicate id " + v.getId() + " from " + anExtends );
                        } else {
                            iterator.add( v );
                            lastExtendsId = v.getExternalId();
                        }
                    }
                }
            } else {
                if( lastExtendsId >= 0 ) {
                    iterator.remove();

                    if( child instanceof DictionaryLeaf ) {
                        iterator.add( new DictionaryLeaf( child.getId(), child.isEnabled(), id.incrementAndGet(), child.getProperties() ) );
                    } else {
                        iterator.add( new DictionaryValue( child.getId(), child.isEnabled(), id.incrementAndGet(), child.getValues(), child.getProperties() ) );
                    }
                }
                resolveExtends( dictionaryRoot, child, id );
            }
        }
    }

    private static void resolveExtends( DictionaryRoot dictionaryRoot, Dictionary parent, AtomicInteger id ) {
        List<? extends Dictionary> values = parent.getValues();

        resolveExtends( dictionaryRoot, values, id );
    }

    private static List<? extends Dictionary> getValues( DictionaryRoot dictionaryRoot, Extends anExtends ) {
        Dictionary value = dictionaryRoot;

        for( String id : StringUtils.split( anExtends.path, "/" ) ) {
            value = value.getValue( id );
        }
        return value.getValues();
    }

    private static void validate( String path, ArrayList<InvalidEntry> invalid, Dictionary dictionary ) {
        validate( path, invalid, dictionary.getValues() );
    }

    private static void validate( String path, ArrayList<InvalidEntry> invalid, List<? extends Dictionary> values ) {
        HashMap<Integer, Dictionary> eid = new HashMap<>();

        for( Dictionary dictionary : values ) {
            Dictionary found = eid.get( dictionary.getExternalId() );
            if( found != null ) invalid.add( new InvalidEntry( found, dictionary, path ) );
            else eid.put( dictionary.getExternalId(), dictionary );

            validate( path + "/" + dictionary.getId(), invalid, dictionary );
        }
    }

    private static ArrayList<Dictionary> parseValues( List<?> values, String path, IdStrategy idStrategy ) {
        ArrayList<Dictionary> dv = new ArrayList<>();

        for( int i = 0; i < values.size(); i++ ) {
            Object value = values.get( i );
            dv.add( parseAsDictionaryValue( value, path + "[" + i + "]", false, idStrategy ) );
        }

        return dv;
    }

    private static Optional<Extends> getExtendsOpt( Map<?, ?> map ) {
        Map<?, ?> m = getValueOpt( Map.class, map, "extends", _ -> Optional.empty() ).orElse( null );
        if( m == null ) return Optional.empty();

        String path = getString( m, "path" );
        Optional<String> filter = getStringOpt( m, "filter" );
        Boolean ignoreDuplicate = getBooleanOpt( m, "ignoreDuplicate" ).orElse( false );

        return Optional.of( new Extends( path, filter, ignoreDuplicate ) );
    }

    private static String getString( Map<?, ?> map, String field ) {
        return getValue( String.class, map, field, _ -> Optional.empty() );
    }

    private static Optional<String> getStringOpt( Map<?, ?> map, String field ) {
        return getValueOpt( String.class, map, field, _ -> Optional.empty() );
    }

    private static int getInt( Map<?, ?> map, String field ) {
        return getValue( Integer.class, map, field, intFunc );
    }

    private static Optional<Integer> getIntOpt( Map<?, ?> map, String field ) {
        return getValueOpt( Integer.class, map, field, intFunc );
    }

    private static Optional<Boolean> getBooleanOpt( Map<?, ?> map, String field ) {
        return getValueOpt( Boolean.class, map, field, _ -> Optional.empty() );
    }

    private static <T> T getValue( Class<T> clazz, Map<?, ?> map, String field, Function<Object, Optional<T>> func ) {
        return getValueOpt( clazz, map, field, func ).orElseThrow( () -> new DictionaryFormatError( "field '" + field + "' not found" ) );
    }

    @SuppressWarnings( "unchecked" )
    private static <T> Optional<T> getValueOpt( Class<T> clazz, Map<?, ?> map, String field, Function<Object, Optional<T>> func ) {
        Object f = map.get( field );

        if( f == null ) return Optional.empty();

        if( clazz.isInstance( f ) ) return Optional.of( ( T ) f );

        Optional<T> apply = func.apply( f );
        if( apply.isPresent() ) return apply;

        throw new DictionaryFormatError( "field '" + field + "' type " + f.getClass() + " != " + clazz );
    }

    public static void serialize( DictionaryRoot dictionary, Path path ) {
        serialize( dictionary, path, false );
    }

    public static void serialize( DictionaryRoot dictionary, JsonGenerator jsonGenerator ) throws IOException {
        jsonGenerator.writeStartObject();

        jsonGenerator.writeStringField( NAME, dictionary.name );

        writeProperties( jsonGenerator, dictionary );

        writeValues( jsonGenerator, dictionary.getValues() );

        jsonGenerator.writeEndObject();
    }

    public static void serialize( DictionaryRoot dictionary, Path path, boolean format ) {
        try( JsonGenerator jsonGenerator = getJsonGenerator( path, format ) ) {
            serialize( dictionary, jsonGenerator );
        } catch( IOException e ) {
            throw new UncheckedIOException( e );
        }
    }

    public static void serialize( DictionaryRoot dictionary, StringBuilder sb, boolean format ) {
        try( JsonGenerator jsonGenerator = getJsonGenerator( sb, format ) ) {
            serialize( dictionary, jsonGenerator );
        } catch( IOException e ) {
            throw new UncheckedIOException( e );
        }
    }

    public static JsonGenerator getJsonGenerator( Path path, boolean format ) {
        JsonGenerator jsonGenerator = Binder.json.getJsonGenerator( path );
        if( format ) jsonGenerator = jsonGenerator
            .setPrettyPrinter( new DefaultPrettyPrinter().withObjectIndenter( new DefaultIndenter().withLinefeed( "\n" ) ) );
        return jsonGenerator;
    }

    public static JsonGenerator getJsonGenerator( StringBuilder sb, boolean format ) {
        JsonGenerator jsonGenerator = Binder.json.getJsonGenerator( sb );
        if( format ) jsonGenerator = jsonGenerator
            .setPrettyPrinter( new DefaultPrettyPrinter().withObjectIndenter( new DefaultIndenter().withLinefeed( "\n" ) ) );
        return jsonGenerator;
    }

    private static void writeValues( JsonGenerator jsonGenerator, List<? extends Dictionary> values ) throws IOException {
        if( values.isEmpty() ) return;

        jsonGenerator.writeFieldName( VALUES );
        serializeValues( jsonGenerator, values );
    }

    public static void serializeValues( List<? extends Dictionary> values, StringBuilder sb, boolean format ) {
        try( JsonGenerator jsonGenerator = getJsonGenerator( sb, format ) ) {
            serializeValues( jsonGenerator, values );
        } catch( IOException e ) {
            throw new UncheckedIOException( e );
        }
    }

    private static void serializeValues( JsonGenerator jsonGenerator, List<? extends Dictionary> values ) throws IOException {
        jsonGenerator.writeStartArray();

        for( Dictionary value : values ) {
            serializeChild( jsonGenerator, value );
        }

        jsonGenerator.writeEndArray();
    }

    public static void serializeChild( Dictionary value, StringBuilder sb, boolean format ) {
        try( JsonGenerator jsonGenerator = getJsonGenerator( sb, format ) ) {
            serializeChild( jsonGenerator, value );
        } catch( IOException e ) {
            throw new UncheckedIOException( e );
        }
    }

    private static void serializeChild( JsonGenerator jsonGenerator, Dictionary value ) throws IOException {
        jsonGenerator.writeStartObject();
        jsonGenerator.writeStringField( ID, value.getId() );
        if( !value.isEnabled() ) jsonGenerator.writeBooleanField( ENABLED, false );
        jsonGenerator.writeNumberField( EXTERNAL_ID, value.getExternalId() );
        if( value instanceof DictionaryValue ) {
            writeValues( jsonGenerator, value.getValues() );
        }

        writeProperties( jsonGenerator, value );

        jsonGenerator.writeEndObject();
    }

    private static void writeProperties( JsonGenerator jsonGenerator, Dictionary value ) {
        value.getProperties().forEach( Try.consume( jsonGenerator::writeObjectField ) );
    }

    public interface IdStrategy {
        int get( Map<Object, Object> valueMap );

        int getMaxExtendsId( DictionaryRoot root );
    }

    private static class InvalidEntry {
        public final String path;
        final Dictionary one;
        final Dictionary two;

        InvalidEntry( Dictionary one, Dictionary two, String path ) {
            this.one = one;
            this.two = two;
            this.path = path;
        }
    }

    public static class IncrementalIdStrategy implements IdStrategy {
        private final AtomicInteger id = new AtomicInteger();

        @Override
        public int get( Map<Object, Object> valueMap ) {
            return id.incrementAndGet();
        }

        @Override
        public int getMaxExtendsId( DictionaryRoot root ) {
            return id.get() + 1;
        }
    }

    public static class PropertyIdStrategy implements IdStrategy {
        private static int maxExtendsId( Dictionary root ) {
            if( root instanceof DictionaryExtends ) return Integer.MIN_VALUE;

            int max = root.getExternalId();

            for( Dictionary child : root.getValues() ) max = max( max, maxExtendsId( child ) );

            return max;
        }

        @Override
        public int get( Map<Object, Object> valueMap ) {
            return getInt( valueMap, EXTERNAL_ID );
        }

        @Override
        public int getMaxExtendsId( DictionaryRoot root ) {
            return maxExtendsId( root );
        }
    }

    public static class AutoIdStrategy implements DictionaryParser.IdStrategy {
        private final AtomicInteger id = new AtomicInteger();

        public AutoIdStrategy() {
        }

        private static int maxExtendsId( Dictionary root ) {
            if( root instanceof DictionaryExtends ) return Integer.MIN_VALUE;

            int max = root.getExternalId();

            for( Dictionary child : root.getValues() ) max = max( max, maxExtendsId( child ) );

            return max;
        }

        @Override
        public int get( Map<Object, Object> valueMap ) {
            return getIntOpt( valueMap, EXTERNAL_ID ).orElseGet( id::incrementAndGet );
        }

        @Override
        public int getMaxExtendsId( DictionaryRoot root ) {
            return maxExtendsId( root );
        }
    }
}
