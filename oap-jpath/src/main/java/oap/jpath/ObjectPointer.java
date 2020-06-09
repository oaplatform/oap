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
import oap.reflect.Reflect;

/**
 * Created by igor.petrenko on 2020-06-09.
 */
@ToString
@Slf4j
public class ObjectPointer implements Pointer {
    private final Object v;

    public ObjectPointer( Object v ) {
        this.v = v;
    }

    @Override
    public Pointer resolve( PathNode n ) {
        var reflect = Reflect.reflect( v.getClass() );
        return Pointer.get( switch( n.type ) {
            case FIELD -> {
                log.trace( "field -> {}", n );
                var field = reflect.field( n.name ).orElse( null );
                if( field == null ) throw new PathNotFound();
                yield field.get( v );
            }
            case METHOD -> {
                log.trace( "method -> {}", n );
                var method = reflect.method( n.name ).orElse( null );
                if( method == null ) throw new PathNotFound();
                yield method.invoke( v );
            }
        } );
    }

    @Override
    public Object get() {
        return v;
    }
}