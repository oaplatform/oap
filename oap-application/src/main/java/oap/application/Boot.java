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

import oap.cli.Cli;
import oap.cli.Option;
import oap.io.Files;
import org.slf4j.Logger;

import static org.slf4j.LoggerFactory.getLogger;

public class Boot {
    private static Logger logger = getLogger( Boot.class );
    public static boolean terminated = false;
    public static final Kernel kernel = new Kernel( Module.fromClassPath() );

    public static void main( String[] args ) {
        Cli.create()
            .group( "Starting service",
                params -> Boot.start( (String) params.get( "config" ) ),
                Option.simple( "start" ).required(),
                Option.string( "config" ).required() )
            .act( args );
    }

    public static void start( String config ) {
        Runtime.getRuntime().addShutdownHook( new Thread( "shutdown-hook" ) {
            @Override
            public void run() {
                Boot.stop();
            }
        } );
        try {
            kernel.start( Files.path( config ) );
            logger.debug( "started" );
        } catch( Exception e ) {
            logger.error( e.getMessage(), e );
        }
    }

    public static synchronized void stop() {
        if( !terminated ) {
            terminated = true;
            try {
                kernel.stop();
                logger.debug( "stopped" );
            } catch( Exception e ) {
                logger.error( e.getMessage(), e );
            }
        }
    }
}
