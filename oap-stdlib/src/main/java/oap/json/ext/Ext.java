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

package oap.json.ext;

import lombok.EqualsAndHashCode;
import oap.util.function.Try;
import oap.util.serialization.OptionalSerializator;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.reflect.Constructor;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@SuppressWarnings( "checkstyle:AbstractClassName" )
@EqualsAndHashCode
public abstract class Ext implements Serializable {
    private static final ConcurrentMap<String, Optional<Constructor<? extends Ext>>> cons = new ConcurrentHashMap<>();

    @SuppressWarnings( "unchecked" )
    protected static Optional<Constructor<? extends Ext>> init( Class<?> parent, String field, Class<?>... params ) {
        return cons.computeIfAbsent( parent + field, id -> {
            try {
                var clazz = ExtDeserializer.extensionOf( parent, field );
                if( clazz == null ) return Optional.empty();

                return Optional.of( ( Constructor<? extends Ext> ) clazz.getConstructor( params ) );
            } catch( NoSuchMethodException e ) {
                e.printStackTrace();
                throw new RuntimeException( e.getMessage(), e );
            }
        } );
    }

    @SuppressWarnings( "unchecked" )
    protected static <T extends Ext> T newExt( Class<?> parent, String field, Class<?>[] cparams, Object[] params ) {
        return init( parent, field, cparams ).map( Try.map( c -> ( T ) c.newInstance( params ) ) ).orElse( null );
    }
}
