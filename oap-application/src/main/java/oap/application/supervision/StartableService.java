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
package oap.application.supervision;

import oap.reflect.Reflect;
import oap.reflect.Reflection;
import oap.util.Throwables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class StartableService implements Supervised {
    private final Logger logger;
    private final Object supervised;
    private final String startWith;
    private final String stopWith;
    private boolean started;

    public StartableService( Object supervised, String startWith, String stopWith ) {
        this.supervised = supervised;
        this.startWith = startWith;
        this.stopWith = stopWith;
        this.logger = LoggerFactory.getLogger( supervised.getClass() );
    }

    @Override
    public void start() {
        try {
            getControlMethod( startWith ).ifPresent( m -> m.invoke( supervised ) );
            started = true;
        } catch( Exception e ) {
            logger.error( e.getMessage(), e );
            throw Throwables.propagate( e );
        }
    }

    @Override
    public void stop() {
        try {
            if( started ) getControlMethod( stopWith )
                .ifPresent( m -> m.invoke( supervised ) );
        } catch( Exception e ) {
            logger.error( e.getMessage(), e );
        }
    }

    private Optional<Reflection.Method> getControlMethod( String name ) {
        return Reflect.reflect( supervised.getClass() ).methods
            .stream()
            .filter( m -> name.equals( m.name() ) && m.parameters.isEmpty() )
            .findAny();
    }

}
