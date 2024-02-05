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

package oap.mail.velocity;

import lombok.SneakyThrows;
import org.apache.velocity.runtime.parser.node.AbstractExecutor;
import org.apache.velocity.util.introspection.Info;
import org.apache.velocity.util.introspection.UberspectImpl;
import org.apache.velocity.util.introspection.VelPropertyGet;

import java.lang.reflect.Field;

public class Uberspector extends UberspectImpl {
    @SneakyThrows
    public VelPropertyGet getPropertyGet( Object object, String name, Info i ) {
        VelPropertyGet getter = super.getPropertyGet( object, name, i );
        try {
            getter.getMethodName();
            return getter;
        } catch( NullPointerException notfound ) {
            Field field = object.getClass().getField( name );
            return new VelGetterImpl( new AbstractExecutor() {
                public Object execute( Object o ) throws IllegalAccessException {
                    return field.get( o );
                }
            } );
        }
    }
}
