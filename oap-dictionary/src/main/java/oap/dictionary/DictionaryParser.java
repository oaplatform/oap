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

import oap.json.Binder;

import java.util.*;
import java.util.function.Function;

/**
 * Created by Igor Petrenko on 15.04.2016.
 */
public class DictionaryParser {
    public static final String NAME = "name";
    public static final String VALUES = "values";
    public static final String ID = "id";
    public static final String ENABLED = "enabled";
    public static final String EXTERNAL_ID = "eid";

    private static final Set<String> defaultFields = new HashSet<>();

    static {
        defaultFields.add( ID );
        defaultFields.add( ENABLED );
        defaultFields.add( EXTERNAL_ID );
    }

    public static final Dictionary parse( String resource ) {
        final Map map = Binder.json.unmarshalResource( DictionaryParser.class, Map.class, resource ).get();

        final String name = getString( map, NAME );

        final List values = getList( map, VALUES );
        final ArrayList<Dictionary.DictionaryValue> dv = new ArrayList<>();

        for( int i = 0; i < values.size(); i++ ) {
            final Object value = values.get( i );

            if( value instanceof Map ) {
                final Map<Object, Object> valueMap = ( Map<Object, Object> ) value;
                final String id = getString( valueMap, ID );
                final boolean enabled = getBoolean( valueMap, ENABLED );
                final long externalId = getLong( valueMap, EXTERNAL_ID, true );

                final HashMap<String, Object> properties = new HashMap<>();
                for( Map.Entry e : valueMap.entrySet() ) {
                    if( !defaultFields.contains( e.getKey().toString() ) ) {
                        properties.put( e.getKey().toString(), e.getValue() );
                    }
                }

                dv.add( new Dictionary.DictionaryValue( id, enabled, externalId, properties.isEmpty() ? Collections.emptyMap() : properties ) );
            } else {
                throw new DictionaryFormatError(
                    "value '" + i + "' type " +
                        ( value == null ? "<NULL>" : value.getClass().toString() ) + " != " + Map.class
                );
            }
        }

        return new Dictionary( name, dv );
    }

    private static String getString( Map map, String field ) {
        return getValue( String.class, map, field, str -> Optional.empty() );
    }

    private static long getLong( Map map, String field, boolean convert ) {
        return getValue( Long.class, map, field, ( str ) -> {
            if( !convert ) return Optional.empty();
            else if( str instanceof Integer ) return Optional.of( ( ( Integer ) str ).longValue() );
            else if( str instanceof Double ) return Optional.of( ( ( Double ) str ).longValue() );
            else if( str instanceof String && ( ( String ) str ).length() == 1 )
                return Optional.of( ( long ) ( ( String ) str ).charAt( 0 ) );
            else return Optional.empty();
        } );
    }

    private static boolean getBoolean( Map map, String field ) {
        return getValue( Boolean.class, map, field, str -> Optional.empty() );
    }

    private static List getList( Map map, String field ) {
        return getValue( List.class, map, field, ( str ) -> Optional.empty() );
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
        if( apply.isPresent() ) return Optional.ofNullable( apply.get() );

        throw new DictionaryFormatError( "field '" + field + "' type " + f.getClass() + " != " + clazz );
    }
}
