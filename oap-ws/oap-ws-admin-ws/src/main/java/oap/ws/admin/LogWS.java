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

package oap.ws.admin;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.joran.JoranConfigurator;
import ch.qos.logback.core.joran.spi.JoranException;
import ch.qos.logback.core.util.StatusPrinter;
import lombok.extern.slf4j.Slf4j;
import oap.ws.WsMethod;
import oap.ws.WsParam;
import org.slf4j.LoggerFactory;

import java.util.LinkedHashMap;
import java.util.Map;

import static oap.http.server.nio.HttpServerExchange.HttpMethod.GET;
import static oap.io.Resources.urlOrThrow;
import static oap.ws.WsParam.From.PATH;

@Slf4j
public class LogWS {
    @WsMethod( path = "/", method = GET )
    public Map<String, String> getAll() {
        log.debug( "get all" );

        var map = new LinkedHashMap<String, String>();

        var loggerContext = ( LoggerContext ) LoggerFactory.getILoggerFactory();
        for( var logger : loggerContext.getLoggerList() ) {
            if( logger.getLevel() != null ) {
                map.put( logger.getName(), logger.getLevel().toString() );
            }
        }

        return map;
    }

    @WsMethod( path = "/reset", method = GET )
    public void reset() {
        log.debug( "reset" );

        var loggerContext = ( LoggerContext ) LoggerFactory.getILoggerFactory();
        var url = urlOrThrow( getClass(), "/logback-test.xml" );

        try {
            var configurator = new JoranConfigurator();
            configurator.setContext( loggerContext );
            loggerContext.reset();
            configurator.doConfigure( url );
        } catch( JoranException e ) {
            log.error( e.getMessage(), e );
        }

        StatusPrinter.printInCaseOfErrorsOrWarnings( loggerContext );
    }

    @WsMethod( path = "/{level}/{packageName}" )
    public void setLevel(
        @WsParam( from = PATH ) String level,
        @WsParam( from = PATH ) String packageName
    ) {
        log.debug( "set {} for {}", level, packageName );

        var loggerContext = ( LoggerContext ) LoggerFactory.getILoggerFactory();
        var logger = loggerContext.getLogger( packageName );
        log.debug( "{} current logger level: {}", packageName, logger.getLevel() );
        logger.setLevel( Level.toLevel( level ) );
    }
}
