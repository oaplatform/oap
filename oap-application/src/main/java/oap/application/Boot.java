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
package oap.application;

import lombok.extern.slf4j.Slf4j;
import oap.cli.Cli;
import oap.cli.Option;
import sun.misc.Signal;
import sun.misc.SignalHandler;

import java.nio.file.Path;

@Slf4j
public class Boot {
    public static boolean terminated = false;
    private static Kernel kernel;

    public static void main( String[] args ) {
        Cli.create()
            .group( "Starting service",
                params -> {
                    Path config = ( Path ) params.get( "config" );
                    Boot.start( config, ( Path ) params.getOrDefault( "config-directory", config.getParent().resolve( "conf.d" ) ) );
                },
                Option.simple( "start" ).required(),
                Option.path( "config" ).required(),
                Option.path( "config-directory" )
            )
            .act( args );
    }

    public static void start( Path config, Path confd ) {
        installSignals();

        try {
            kernel = new Kernel( Module.CONFIGURATION.urlsFromClassPath(), Plugin.CONFIGURATION.urlsFromClassPath() );
            kernel.start( config, confd );
            log.debug( "started" );
        } catch( Exception e ) {
            log.error( e.getMessage(), e );
            exit( 13 );
        }
    }

    private static void installSignals() {
        SignalHandler handler = signal -> {
            log.info( "cought signal: {}", signal.getName() );
            System.out.println( "cought signal: {}" + signal.getName() );
            System.out.flush();
            exit( 0 );
        };
        Signal.handle( new Signal( "INT" ), handler );
        Signal.handle( new Signal( "TERM" ), handler );
    }

    public static synchronized void stop() {
        if( !terminated ) {
            terminated = true;
            try {
                kernel.stop();
                log.debug( "stopped" );
            } catch( Exception e ) {
                log.error( e.getMessage(), e );
            }
        }
    }

    public static void exit( int status ) {
        log.info( "exit status = " + status );
        System.out.println( "exit status = " + status );
        try {
            stop();
        } catch( Throwable e ) {
            log.error( e.getMessage(), e );
        }

        System.exit( status );
    }
}
