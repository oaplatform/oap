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

package oap.storage.dynamo.client;


import ch.qos.logback.classic.LoggerContext;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.LoggerFactory;
import ch.qos.logback.classic.Level;

@Slf4j
public class DynamodbLog {

    public DynamodbLog( String level, String clientLevel ) {
        LoggerContext loggerContext = ( LoggerContext ) LoggerFactory.getILoggerFactory();
        Level logLevel = getLevel( level );
        log.info( "DynamoDB server log (software.amazon.awssdk) level set to {}", logLevel );
        loggerContext.getLogger( "software.amazon.awssdk" ).setLevel( logLevel );
        logLevel = getLevel( clientLevel );
        log.info( "DynamoDB client log (oap.storage.dynamo.client) level set to {}", logLevel );
        loggerContext.getLogger( "oap.storage.dynamo.client" ).setLevel( logLevel );
    }

    private Level getLevel( String level ) {
        return switch( level ) {
            case "ALL" -> Level.ALL;
            case "DEBUG" -> Level.DEBUG;
            case "INFO" -> Level.INFO;
            case "WARN" -> Level.WARN;
            case "ERROR" -> Level.ERROR;
            case "OFF" -> Level.OFF;
            default -> Level.TRACE;
        };
    }
}
