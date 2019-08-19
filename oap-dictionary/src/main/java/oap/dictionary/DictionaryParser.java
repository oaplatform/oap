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
import oap.json.Binder;
import oap.util.Lists;
import oap.util.Try;
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
    public static final IdStrategy PROPERTY_ID_STRATEGY = new PropertyIdStrategy();
    public static final IdStrategy INCREMENTAL_ID_STRATEGY = new IncrementalIdStrategy();

    private static final String NAME = "name";
    private static final String ID = "id";
    private static final String ENABLED = "enabled";
    private static final String EXTERNAL_ID = "eid";
    private static final String VALUES = "values";
    private static final Set<String> defaultFields = new HashSet<>();
    private static final Function<Object, Optional<Integer>> intFunc =
        ( str ) -> {
            if( str instanceof Long ) return Optional.of( ( ( Long ) str ).intValue() );
            else if( str instanceof Double ) return Optional.of( ( ( Double ) str ).intValue() );
            else if( str instanceof String && ( ( String ) str ).length() == 1 )
                return Optional.of( ( int ) ( ( String ) str ).charAt( 0 ) );
            else return Optional.empty();
        };

    static {
        defaultFields.add( ID );
        defaultFields.add( ENABLED );
        defaultFields.add( EXTERNAL_ID );
    }

    @SuppressWarnings( "unchecked" )
    private static Dictionary parseAsDictionaryValue( Object value, String path, boolean valueAsRoot, IdStrategy idStrategy ) {
        if( value instanceof Map ) {
            final Map<Object, Object> valueMap = ( Map<Object, Object> ) value;
            List<Dictionary> values = emptyList();

            final HashMap<String, Object> properties = new HashMap<>();
            for( Map.Entry e : valueMap.entrySet() ) {
                final String propertyName = e.getKey().toString();
                if( !defaultFields.contains( propertyName ) && ( !valueAsRoot || !NAME.equals( propertyName ) ) ) {
                    final Object propertyValue = e.getValue();

                    if( VALUES.equals( propertyName ) )
                        values = parseValues( ( List ) propertyValue, path, idStrategy );
                    else
                        properties.put( propertyName, propertyValue );
                }
            }


            final Map<String, Object> p = properties.isEmpty() ? emptyMap() : properties;

            if( valueAsRoot ) {
                final String name = getString( valueMap, NAME );

                final ExternalIdType externalIdAs = getStringOpt( valueMap, "externalIdAs" )
                    .map( ExternalIdType::valueOf )
                    .orElse( ExternalIdType.integer );

                return new DictionaryRoot( name, externalIdAs, values, p );
            }

            var anExtends = getExtendsOpt( valueMap ).orElse( null );
            if( anExtends != null ) return new DictionaryExtends( anExtends );

            final String id = getString( valueMap, ID );
            final boolean enabled = getBooleanOpt( valueMap, ENABLED ).orElse( true );
            final int externalId = idStrategy.get( valueMap );

            return values.isEmpty() ?
                new DictionaryLeaf( id, enabled, externalId, p ) :
                new DictionaryValue( id, enabled, externalId, values, p );
        } else {
            throw new DictionaryFormatError(
                "value " + path + " type "
                    + ( value == null ? "<NULL>" : value.getClass().toString() ) + " != " + Map.class
            );
        }
    }

    public static DictionaryRoot parse( Path resource ) {
        final Map map = Binder.hoconWithoutSystemProperties.unmarshal( Map.class, resource );
        return parse( map, PROPERTY_ID_STRATEGY );
    }

    public static DictionaryRoot parse( URL resource ) {
        return parse( resource, PROPERTY_ID_STRATEGY );
    }

    public static DictionaryRoot parse( URL resource, IdStrategy idStrategy ) {
        final Map map = Binder.getBinder( resource, false ).unmarshal( Map.class, resource );
        return parse( map, idStrategy );
    }

    public static DictionaryRoot parse( String resource ) {
        return parse( resource, PROPERTY_ID_STRATEGY );
    }

    public static DictionaryRoot parse( String resource, IdStrategy idStrategy ) {
        final Map map = Binder.hoconWithoutSystemProperties.unmarshalResource( DictionaryParser.class, Map.class, resource );

        return parse( map, idStrategy );
    }

    public static DictionaryRoot parseFromString( String dictionary ) {
        final Map map = Binder.hoconWithoutSystemProperties.unmarshal( Map.class, dictionary );

        return parse( map, PROPERTY_ID_STRATEGY );
    }

    private static DictionaryRoot parse( Map map, IdStrategy idStrategy ) {
        var dictionaryRoot = ( DictionaryRoot ) parseAsDictionaryValue( map, "", true, idStrategy );
        var invalid = new ArrayList<InvalidEntry>();

        var lastId = idStrategy.getMaxExtendsId( dictionaryRoot );

        resolveExtends( dictionaryRoot, dictionaryRoot, new AtomicInteger( lastId ) );
        validate( "", invalid, dictionaryRoot );

        if( !invalid.isEmpty() ) {
            invalid.sort( Comparator.comparing( l -> l.path ) );
            final String msg = invalid
                .stream()
                .map( e -> "path: " + e.path + "; eid: " + e.one.getExternalId() + "; one: " + e.one.getId() + "; two: " + e.two.getId() )
                .collect( Collectors.joining( ", " ) );

            throw new DictionaryError( "duplicate eid: " + msg );

        }

        return dictionaryRoot;
    }

    @SuppressWarnings( "unchecked" )
    private static void resolveExtends( DictionaryRoot dictionaryRoot, Dictionary parent, AtomicInteger id ) {
        var values = parent.getValues();
        var iterator = ( ListIterator<Dictionary> ) values.listIterator();
        var lastExtendsId = -1;
        while( iterator.hasNext() ) {
            var child = iterator.next();
            if( child instanceof DictionaryExtends ) {
                iterator.remove();

                var anExtends = ( ( DictionaryExtends ) child ).anExtends;
                for( var v : getValues( dictionaryRoot, anExtends ) ) {
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

    private static List<? extends Dictionary> getValues( DictionaryRoot dictionaryRoot, Extends anExtends ) {
        Dictionary value = dictionaryRoot;

        for( var id : StringUtils.split( anExtends.path, "/" ) ) {
            value = value.getValue( id );
        }
        return value.getValues();
    }

    private static void validate( String path, ArrayList<InvalidEntry> invalid, Dictionary dictionary ) {
        validate( path, invalid, dictionary.getValues() );
    }

    private static void validate( String path, ArrayList<InvalidEntry> invalid, List<? extends Dictionary> values ) {
        final HashMap<Integer, Dictionary> eid = new HashMap<>();

        for( Dictionary dictionary : values ) {
            final Dictionary found = eid.get( dictionary.getExternalId() );
            if( found != null ) {
                invalid.add( new InvalidEntry( found, dictionary, path ) );
            } else {
                eid.put( dictionary.getExternalId(), dictionary );
            }

            validate( path + "/" + dictionary.getId(), invalid, dictionary );
        }
    }

    private static ArrayList<Dictionary> parseValues( List values, String path, IdStrategy idStrategy ) {
        final ArrayList<Dictionary> dv = new ArrayList<>();

        for( int i = 0; i < values.size(); i++ ) {
            final Object value = values.get( i );
            dv.add( parseAsDictionaryValue( value, path + "[" + i + "]", false, idStrategy ) );
        }

        return dv;
    }

    private static Optional<Extends> getExtendsOpt( Map map ) {
        var m = getValueOpt( Map.class, map, "extends", o -> Optional.empty() ).orElse( null );
        if( m == null ) return Optional.empty();

        var path = getString( m, "path" );
        var filter = getStringOpt( m, "filter" );
        var ignoreDuplicate = getBooleanOpt( m, "ignoreDuplicate" ).orElse( false );

        return Optional.of( new Extends( path, filter, ignoreDuplicate ) );
    }

    private static String getString( Map map, String field ) {
        return getValue( String.class, map, field, str -> Optional.empty() );
    }

    private static Optional<String> getStringOpt( Map map, String field ) {
        return getValueOpt( String.class, map, field, str -> Optional.empty() );
    }

    private static int getInt( Map map, String field ) {
        return getValue( Integer.class, map, field, intFunc );
    }

    private static Optional<Boolean> getBooleanOpt( Map map, String field ) {
        return getValueOpt( Boolean.class, map, field, str -> Optional.empty() );
    }

    private static <T> T getValue( Class<T> clazz, Map map, String field, Function<Object, Optional<T>> func ) {
        return getValueOpt( clazz, map, field, func ).orElseThrow( () -> new DictionaryFormatError( "field '" + field + "' not found" ) );
    }

    @SuppressWarnings( "unchecked" )
    private static <T> Optional<T> getValueOpt( Class<T> clazz, Map map, String field, Function<Object, Optional<T>> func ) {
        final Object f = map.get( field );

        if( f == null ) return Optional.empty();

        if( clazz.isInstance( f ) ) {
            return Optional.of( ( T ) f );
        }

        final Optional<T> apply = func.apply( f );
        if( apply.isPresent() ) return Optional.of( apply.get() );

        throw new DictionaryFormatError( "field '" + field + "' type " + f.getClass() + " != " + clazz );
    }

    public static void serialize( DictionaryRoot dictionary, Path path ) {
        serialize( dictionary, path, false );
    }

    public static void serialize( DictionaryRoot dictionary, Path path, boolean format ) {
        try( JsonGenerator jsonGenerator = format
            ? Binder.json.getJsonGenerator( path )
            .setPrettyPrinter( new DefaultPrettyPrinter().withObjectIndenter( new DefaultIndenter().withLinefeed( "\n" ) ) )
            : Binder.json.getJsonGenerator( path ) ) {

            jsonGenerator.writeStartObject();

            jsonGenerator.writeStringField( NAME, dictionary.name );

            writeProperties( jsonGenerator, dictionary );

            writeValues( jsonGenerator, dictionary.getValues() );

            jsonGenerator.writeEndObject();
        } catch( IOException e ) {
            throw new UncheckedIOException( e );
        }
    }

    private static void writeValues( JsonGenerator jsonGenerator, List<? extends Dictionary> values ) throws IOException {
        if( values.isEmpty() ) return;

        jsonGenerator.writeFieldName( VALUES );
        jsonGenerator.writeStartArray();

        for( var value : values ) {
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

        jsonGenerator.writeEndArray();
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
        private AtomicInteger id = new AtomicInteger();

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
        private static int _getMaxExtendsId( Dictionary root ) {
            if( root instanceof DictionaryExtends ) return Integer.MIN_VALUE;

            int max = root.getExternalId();

            for( var child : root.getValues() ) {
                max = max( max, _getMaxExtendsId( child ) );
            }

            return max;
        }

        @Override
        public int get( Map<Object, Object> valueMap ) {
            return getInt( valueMap, EXTERNAL_ID );
        }

        @Override
        public int getMaxExtendsId( DictionaryRoot root ) {
            return _getMaxExtendsId( root );
        }
    }
}
