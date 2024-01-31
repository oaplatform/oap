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
        invoke( startWith, supervised, logger, true );
        started = true;
    }

    @SneakyThrows
    public void preStart() {
        invoke( preStartWith, supervised, logger, true );
    }

    @Override
    public void preStop() {
        if( started ) invoke( preStopWith, supervised, logger, false );
    }

    @Override
    public void stop() {
        if( started ) invoke( stopWith, supervised, logger, false );
        started = false;
    }

    protected static void invoke( List<String> methods, Object supervised, Logger logger, boolean rethrow ) {
        try {
            findMethod( methods, supervised ).ifPresent( m -> m.invoke( supervised ) );
        } catch( Exception e ) {
            logger.error( e.getMessage(), e );
            if( rethrow ) throw e;
        }
    }

    private static Optional<Reflection.Method> findMethod( List<String> names, Object supervised ) {
        return names
            .stream()
            .flatMap( m -> Optionals.toStream( getControlMethod( m, supervised ) ) )
            .findFirst();
    }

    private static Optional<Reflection.Method> getControlMethod( String name, Object supervised ) {
        return Reflect.reflect( supervised.getClass() ).methods
            .stream()
            .filter( m -> name.equals( m.name() ) && m.parameters.isEmpty() )
            .findAny();
    }

}
