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

package oap.json.schema;

import oap.util.Lists;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.function.BiFunction;

import static oap.json.schema.OperationFunction.Condition.ANY;

public class OperationFunction {
    public static final String EQ_OP = "eq";
    public static final String NE_OP = "ne";
    public static final String IN_OP = "in";
    public static final String NIN_OP = "nin";

    private final Condition condition;
    private final BiFunction<Object, Optional<String>, List<Object>> func;

    public OperationFunction( Condition condition, BiFunction<Object, Optional<String>, List<Object>> func ) {
        this.condition = condition;
        this.func = func;
    }

    public static OperationFunction parse( Map<?, ?> map ) {
        OperationFunction.Condition condition = getCondition( map );
        Object value = getValue( condition, map );

        if( value instanceof Map ) {
            Map valueMap = ( Map ) value;
            String jsonPath = ( String ) valueMap.get( "json-path" );

            return new OperationFunction( condition, ( rootJson, currentPath ) -> new JsonPath( jsonPath, currentPath ).traverse( rootJson ) );
        } else {
            return new OperationFunction( condition, ( rootJson, currentPath ) -> Collections.singletonList( value ) );
        }
    }

    private static Condition getCondition( Map<?, ?> map ) {
        if( map.containsKey( EQ_OP ) ) return Condition.EQ;
        else if( map.containsKey( NE_OP ) ) return Condition.NE;
        else if( map.containsKey( IN_OP ) ) return Condition.IN;
        else if( map.containsKey( NIN_OP ) ) return Condition.NIN;
        return ANY;
    }


    private static Object getValue( Condition condition, Map<?, ?> map ) {
        return switch( condition ) {
            case EQ -> map.get( EQ_OP );
            case NE -> map.get( NE_OP );
            case IN -> map.get( IN_OP );
            case NIN -> map.get( NIN_OP );
            case ANY -> null;
        };
    }

    private Optional<Object> getValue( Object rootJson, Optional<String> currentPath ) {
        return Lists.headOf( func.apply( rootJson, currentPath ) );
    }

    public final boolean apply( Object rootJson, Optional<String> currentPath, Object value ) {
        if( condition == ANY ) return true;

        Optional<Object> foundOpt = getValue( rootJson, currentPath );

        return switch( condition ) {
            case EQ -> foundOpt.map( v -> Objects.equals( v, value ) ).orElse( false );
            case NE -> foundOpt.map( v -> !Objects.equals( v, value ) ).orElse( true );
            case IN -> {
                Object found = foundOpt.orElse( List.of() );
                for( Object item : ( List ) found ) {
                    if( Objects.equals( item, value ) ) yield true;
                }
                yield false;
            }
            case NIN -> {
                Object found = foundOpt.orElse( List.of() );
                for( Object item : ( List ) found ) {
                    if( Objects.equals( item, value ) ) yield false;
                }
                yield true;
            }
            default -> throw new IllegalStateException( "Unknown condition " + condition );
        };
    }

    public enum Condition {
        EQ, NE, ANY, IN, NIN
    }
}
