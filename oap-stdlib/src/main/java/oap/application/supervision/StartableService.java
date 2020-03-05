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

import lombok.SneakyThrows;
import oap.reflect.Reflect;
import oap.reflect.Reflection;
import oap.util.Optionals;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;
import java.util.Optional;

public class StartableService implements Supervised {
    private final Logger logger;
    private final Object supervised;
    private final List<String> startWith;
    private final List<String> stopWith;
    private final List<String> preStartWith;
    private final List<String> preStopWith;
    private boolean started;

    public StartableService( Object supervised,
                             List<String> preStartWith, List<String> startWith,
                             List<String> preStopWith, List<String> stopWith ) {
        this.supervised = supervised;
        this.preStartWith = preStartWith;
        this.startWith = startWith;
        this.preStopWith = preStopWith;
        this.stopWith = stopWith;
        this.logger = LoggerFactory.getLogger( supervised.getClass() );
    }

    @SneakyThrows
    @Override
    public void start() {
        try {
            findMethod( startWith ).ifPresent( m -> m.invoke( supervised ) );
            started = true;
        } catch( Exception e ) {
            logger.error( e.getMessage(), e );
            throw e;
        }
    }

    @SneakyThrows
    public void preStart() {
        try {
            findMethod( preStartWith ).ifPresent( m -> m.invoke( supervised ) );
        } catch( Exception e ) {
            logger.error( e.getMessage(), e );
            throw e;
        }
    }

    public void preStop() {
        try {
            if( started ) {
                findMethod( preStopWith ).ifPresent( m -> m.invoke( supervised ) );
            }
        } catch( Exception e ) {
            logger.error( e.getMessage(), e );
        }
    }

    @Override
    public void stop() {
        try {
            if( started ) {
                findMethod( stopWith ).ifPresent( m -> m.invoke( supervised ) );
                started = false;
            }
        } catch( Exception e ) {
            logger.error( e.getMessage(), e );
        }
    }

    private Optional<Reflection.Method> findMethod( List<String> names ) {
        return names
            .stream()
            .flatMap( m -> Optionals.toStream( getControlMethod( m ) ) )
            .findFirst();
    }

    private Optional<Reflection.Method> getControlMethod( String name ) {
        return Reflect.reflect( supervised.getClass() ).methods
            .stream()
            .filter( m -> name.equals( m.name() ) && m.parameters.isEmpty() )
            .findAny();
    }

}
