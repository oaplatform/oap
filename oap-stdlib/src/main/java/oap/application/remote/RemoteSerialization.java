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

package oap.application.remote;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by igor.petrenko on 26.03.2019.
 */
public final class RemoteSerialization<T> implements InvocationHandler {
    private final Class<?> interfaze;
    private final T master;
    private final FST fst;

    private RemoteSerialization( Class<T> interfaze, T master, FST.SerializationMethod serialization ) {
        this.interfaze = interfaze;
        this.master = master;
        this.fst = new FST( serialization );
    }

    private RemoteSerialization( Class<T> interfaze, T master ) {
        this( interfaze, master, FST.SerializationMethod.DEFAULT );
    }

    public static <P> P proxy( Class<P> clazz, P master ) {
        return new RemoteSerialization<>( clazz, master ).proxy();
    }

    @SuppressWarnings( "unchecked" )
    private T proxy() {
        return ( T ) Proxy.newProxyInstance( interfaze.getClassLoader(), new Class[] { interfaze }, this );
    }

    @Override
    public Object invoke( Object proxy, Method method, Object[] args ) throws Throwable {
        if( method.getDeclaringClass() == Object.class ) {
            return method.invoke( this, args );
        }
        Parameter[] parameters = method.getParameters();
        List<RemoteInvocation.Argument> arguments = new ArrayList<>();

        for( int i = 0; i < parameters.length; i++ ) {
            arguments.add( new RemoteInvocation.Argument( parameters[i].getName(),
                parameters[i].getType(), args[i] ) );
        }

        final byte[] content = fst.conf.asByteArray( new RemoteInvocation( "service", method.getName(), arguments ) );
        var ri = ( RemoteInvocation ) fst.conf.asObject( content );

        var result = master.getClass()
            .getMethod( ri.method, ri.types() )
            .invoke( master, ri.values() );


        var resultContent = fst.conf.asByteArray( result );

        return fst.conf.asObject( resultContent );
    }
}
