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

package oap.jpath;

import lombok.ToString;
import lombok.extern.slf4j.Slf4j;
import oap.reflect.Reflection;

import java.util.List;

/**
 * Created by igor.petrenko on 2020-06-11.
 */
@ToString( callSuper = true )
@Slf4j
public class PathNodeMethod extends PathNode {
    private final List<Object> arguments;

    protected PathNodeMethod( String name, List<Object> arguments ) {
        super( PathType.METHOD, name );
        this.arguments = arguments;
    }

    @Override
    public Object evaluate( Object v, Reflection reflect ) {
        log.trace( "method -> {}", name );
        var method = reflect.method( m -> m.name().equals( name ) && equals( m.parameters, arguments ) ).orElse( null );
        if( method == null ) throw new PathNotFound();
        return method.invoke( v, arguments.toArray( new Object[0] ) );
    }

    private boolean equals( List<Reflection.Parameter> parameters, List<Object> arguments ) {
        if( parameters.size() != arguments.size() ) return false;

        for( var i = 0; i < parameters.size(); i++ ) {
            var arg = arguments.get( i );
            var parameter = parameters.get( i );

            if( arg == null ) continue;

            var parameterType = parameter.underlying.getType();
            Class<?> argType = arg.getClass();

            if( Number.class.isAssignableFrom( argType ) ) {
                if( int.class.equals( parameterType ) || Integer.class.equals( parameterType ) ) {
                    arguments.set( i, ( ( Number ) arg ).intValue() );
                } else if( long.class.equals( parameterType ) || Long.class.equals( parameterType ) ) {
                    arguments.set( i, ( ( Number ) arg ).longValue() );
                } else if( float.class.equals( parameterType ) || Float.class.equals( parameterType ) ) {
                    arguments.set( i, ( ( Number ) arg ).floatValue() );
                } else if( short.class.equals( parameterType ) || Short.class.equals( parameterType ) ) {
                    arguments.set( i, ( ( Number ) arg ).shortValue() );
                } else if( byte.class.equals( parameterType ) || Byte.class.equals( parameterType ) ) {
                    arguments.set( i, ( ( Number ) arg ).byteValue() );
                } else if( double.class.equals( parameterType ) || Double.class.equals( parameterType ) ) {
                    arguments.set( i, ( ( Number ) arg ).doubleValue() );
                } else {
                    return false;
                }
                
                continue;
            }

            if( !parameterType.equals( argType ) ) return false;
        }

        return true;
    }
}
