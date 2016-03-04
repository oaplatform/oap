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

import com.fasterxml.jackson.core.type.TypeReference;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import oap.json.schema.SchemaAST;
import oap.json.schema._array.ArraySchemaAST;
import oap.json.schema._object.ObjectSchemaAST;

import java.util.*;

import static java.util.Collections.unmodifiableList;
import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.collections4.CollectionUtils.subtract;

public class JsonDiff {
    private final ArrayList<Line> diff;

    private JsonDiff( ArrayList<Line> diff ) {
        this.diff = diff;
    }

    public static JsonDiff diff( String oldJson, String newJson, SchemaAST schema ) {
        final ArrayList<Line> result = new ArrayList<>();

        final Map<String, Object> to = Binder.json.unmarshal( new TypeReference<Map<String, Object>>() {
        }, newJson );
        final Map<String, Object> from = Binder.json.unmarshal( new TypeReference<Map<String, Object>>() {
        }, oldJson );

        diff( "", schema, result, to, from );

        return new JsonDiff( result );
    }

    private static void diff( String prefix, SchemaAST schema, ArrayList<Line> result, Object to, Object from ) {
        if( schema instanceof ObjectSchemaAST ) {
            diffObject( prefix, ( ObjectSchemaAST ) schema, result, to, from );
        } else if( schema instanceof ArraySchemaAST ) {
            diffArray( prefix, ( ArraySchemaAST ) schema, result, to, from );
        } else {
            diffField( prefix, schema, result, to, from );
        }
    }

    private static void diffField( String prefix, SchemaAST schema, ArrayList<Line> result, Object to, Object from ) {
        if( !Objects.equals( to, from ) ) {
            result.add( new Line(
                prefix,
                toLineType( schema ),
                Optional.ofNullable( from ).map( Binder.json::marshal ),
                Optional.ofNullable( to ).map( Binder.json::marshal )
            ) );
        }
    }

    private static void diffArray( String prefix, ArraySchemaAST schema, ArrayList<Line> result, Object to, Object from ) {
        if( !( to instanceof List ) || !( from instanceof List ) )
            throw new IllegalArgumentException( prefix + ": invalid json" );
        final List<?> toList = ( List<?> ) to;
        final List<?> fromList = ( List<?> ) from;

        final Collection<?> diffDel = subtract( fromList, toList );
        final Collection<?> diffAdd = subtract( toList, fromList );

        final SchemaAST items = schema.items;
        if( items instanceof ObjectSchemaAST ) {
            final String idField = schema.idField
                .orElseThrow( () -> new IllegalArgumentException( prefix + ": schema: id field is required" ) );

            List added = unique( toList, fromList, idField );
            List removed = unique( fromList, toList, idField );

            for( Object item : added ) {
                diffField( prefixWithIndex( prefix, getId( idField, item ) ), items, result, item, null );
            }

            for( Object item : removed ) {
                diffField( prefixWithIndex( prefix, getId( idField, item ) ), items, result, null, item );
            }


            for( Object fromItem : fromList ) {
                final Map fromItemMap = ( Map ) fromItem;
                final Object id = fromItemMap.get( idField );
                if( id == null )
                    throw new IllegalArgumentException( prefix + ": id field " + idField + ": not found" );

                toList
                    .stream()
                    .filter( toItem -> {
                        final Map toItemMap = ( Map ) toItem;
                        final Object toId = toItemMap.get( idField );

                        return Objects.equals( toId, id );
                    } )
                    .findAny()
                    .ifPresent( toItem -> diff( prefixWithIndex( prefix, id ), items, result, toItem, fromItemMap ) );
            }
        } else if( items instanceof ArraySchemaAST ) {
            throw new IllegalArgumentException( prefix + ": sub-array" );
        } else {

            diffField( prefix, schema, result, diffAdd.isEmpty() ? null : diffAdd, diffDel.isEmpty() ? null : diffDel );
        }
    }

    private static String prefixWithIndex( String prefix, Object id ) {
        return prefix.isEmpty() ? "[" + id + "]" : prefix + "[" + id + "]";
    }

    private static List unique( List<?> l1, List<?> l2, String idField ) {
        final Set<Object> ids2 = l2.stream().map( i -> getId( idField, i ) ).collect( toSet() );

        return l1
            .stream()
            .filter( i -> {
                final Object id = getId( idField, i );

                return !ids2.contains( id );
            } )
            .collect( toList() );
    }

    private static Object getId( String idField, Object i ) {
        return ( ( Map<?, ?> ) i ).get( idField );
    }

    private static void diffObject( String prefix, ObjectSchemaAST schema, ArrayList<Line> result, Object to, Object from ) {

        final Map toMap = ( Map ) to;
        final Map fromMap = ( Map ) from;

        for( Map.Entry<String, SchemaAST> child : schema.properties.entrySet() ) {
            final String property = child.getKey();
            final Object fromProperty = fromMap.get( property );
            final Object toProperty = toMap.get( property );

            final String newPrefix = prefix.length() > 0 ? prefix + "." + property : property;

            final SchemaAST schemaAST = child.getValue();

            if( ( fromProperty == null && toProperty != null ) || ( toProperty == null && fromProperty != null ) ) {
                diffField( newPrefix, schemaAST, result, toProperty, fromProperty );
            } else {
                diff( newPrefix, schemaAST, result, toProperty, fromProperty );
            }
        }
    }

    private static Line.LineType toLineType( SchemaAST schema ) {
        if( schema instanceof ObjectSchemaAST ) return Line.LineType.OBJECT;
        else if( schema instanceof ArraySchemaAST ) return Line.LineType.ARRAY;
        return Line.LineType.FIELD;
    }

    public List<Line> getDiff() {
        return unmodifiableList( diff );
    }

    @EqualsAndHashCode
    @ToString
    public static class Line {
        public final String path;
        public final Optional<Object> oldValue;
        public final LineType lineType;
        public final Optional<Object> newValue;

        Line( String path, LineType lineType, Optional<Object> oldValue, Optional<Object> newValue ) {
            this.path = path;
            this.lineType = lineType;
            this.newValue = newValue;
            this.oldValue = oldValue;
        }

        public enum LineType {
            ARRAY, FIELD, OBJECT
        }
    }
}
