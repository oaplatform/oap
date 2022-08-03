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

package oap;

import lombok.extern.slf4j.Slf4j;
import oap.util.Pair;
import org.slf4j.Logger;
import org.slf4j.event.Level;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Slf4j
public class LogConsolidated {
    private static final ConcurrentMap<String, TimeAndCount> lastLoggedTime = new ConcurrentHashMap<>();

    public static void log( Logger logger, Level level, long timeBetweenLogs, String message, Throwable t ) {
        if( isEnabledFor( logger, level ) ) {
            String uniqueIdentifier = getFileAndLine();
            var lastTimeAndCountNewValue = new TimeAndCount();
            var lastTimeAndCountOldValue= lastLoggedTime.putIfAbsent( uniqueIdentifier, lastTimeAndCountNewValue );
            if ( lastTimeAndCountOldValue == null ) {
                log( logger, level, message, t );
                return;
            }
            Pair<Boolean, Integer> count = lastTimeAndCountOldValue.tick( timeBetweenLogs );
            if ( count._1 ) {
                log( logger, level, "|x" + count._2 + "| " + message, t );
            }
        }
    }

    private static void log( Logger logger, Level level, String message, Throwable t ) {
        if( t == null ) {
            switch( level ) {
                case DEBUG -> logger.debug( message );
                case ERROR -> logger.error( message );
                case INFO -> logger.info( message );
                case TRACE -> logger.trace( message );
                default -> logger.warn( message );
            }
        } else {
            switch( level ) {
                case DEBUG -> logger.debug( message, t );
                case ERROR -> logger.error( message, t );
                case INFO -> logger.info( message, t );
                case TRACE -> logger.trace( message, t );
                default -> logger.warn( message, t );
            }
        }
    }

    private static String getFileAndLine() {
        var stackTrace = Thread.currentThread().getStackTrace();
        var enteredLogConsolidated = false;
        for( var ste : stackTrace ) {
            if( ste.getClassName().equals( LogConsolidated.class.getName() ) ) {
                enteredLogConsolidated = true;
            } else if( enteredLogConsolidated ) {
                // We have now file/line before entering LogConsolidated.
                return ste.getFileName() + ":" + ste.getLineNumber();
            }
        }
        return "?";
    }

    private static boolean isEnabledFor( Logger logger, Level level ) {
        return switch( level ) {
            case DEBUG -> logger.isDebugEnabled();
            case ERROR -> logger.isErrorEnabled();
            case INFO -> logger.isInfoEnabled();
            case TRACE -> logger.isTraceEnabled();
            case WARN -> logger.isWarnEnabled();
        };
    }

    private static class TimeAndCount {
        long time;
        int count;

        TimeAndCount() {
            this.time = System.currentTimeMillis();
            this.count = 0;
        }

        synchronized Pair<Boolean, Integer> tick( long timeBetweenLogs ) {
            count++;
            long now = System.currentTimeMillis();
            if( now - time < timeBetweenLogs ) {
                return Pair.__( false, count );
            }
            time = now;
            return Pair.__( true, count );
        }
    }
}
