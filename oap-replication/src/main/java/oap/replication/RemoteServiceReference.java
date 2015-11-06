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

package oap.replication;

import oap.application.ApplicationException;
import oap.application.ServiceReference;

import java.lang.reflect.Proxy;
import java.util.Optional;

public class RemoteServiceReference implements ServiceReference {
    @Override
    public Optional<Object> getLink( String serviceName, Class<?> serviceClass ) {
        final int index = serviceName.indexOf( '@' );

        if( index <= 0 ) throw new ApplicationException( "remote service url: " + serviceName );

        return Optional.of(
            Proxy.newProxyInstance(
                getClass().getClassLoader(),
                new Class[]{ serviceClass },
                new HttpRemoteInvocationHandler( serviceName.substring( index + 1 ), serviceName.substring( 0, index ) )
            )
        );
    }
}
