/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Volodymyr Kyrychenko <vladimir.kirichenko@gmail.com>
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

import com.google.common.base.Throwables;
import oap.reflect.Reflect;
import oap.reflect.Reflection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Optional;

public class SupervisedService implements Supervised {
    private final Logger logger;
    private final Object supervised;
    private boolean started;

    public SupervisedService( Object supervised ) {
        this.supervised = supervised;
        this.logger = LoggerFactory.getLogger( supervised.getClass() );
    }

    @Override
    public void start() {
        try {
            getControlMethod( "start" ).ifPresent( m -> m.invoke( supervised ) );
            started = true;
        } catch( Exception e ) {
            logger.error( e.getMessage(), e );
            Throwables.propagate( e );
        }
    }

    @Override
    public void stop() {
        try {
            if( started ) getControlMethod( "stop" )
                .ifPresent( m -> m.invoke( supervised ) );
        } catch( Exception e ) {
            logger.error( e.getMessage(), e );
        }
    }

    private Optional<Reflection.Method> getControlMethod( String name ) {
        return Reflect.reflect( supervised.getClass() ).methods
            .stream()
            .filter( m -> name.equals( m.name() ) && m.paramerers.isEmpty() )
            .findAny();
    }

}
