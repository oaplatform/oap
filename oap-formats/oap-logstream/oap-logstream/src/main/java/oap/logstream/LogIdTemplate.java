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

package oap.logstream;

import oap.io.Closeables;
import oap.net.Inet;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.joda.time.DateTime;

import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The LogIdTemplate class is used to render templates associated with the LogId object.
 *
 * @see LogId
 * @see oap.logstream.disk.AbstractWriter
 */
public class LogIdTemplate {
    private final VelocityEngine engine = new VelocityEngine();

    private final LogId logId;
    private final LinkedHashMap<String, String> variables = new LinkedHashMap<>();

    public LogIdTemplate( LogId logId ) {
        this.logId = logId;
    }

    public LogIdTemplate addVariable( String name, String value ) {
        variables.put( name, value );

        return this;
    }

    public LogIdTemplate addVariables( Map<String, String> variables ) {
        this.variables.putAll( variables );

        return this;
    }

    public String render( String template, DateTime time, Timestamp timestamp, int version ) {
        VelocityContext context = new VelocityContext();

        init( context, time, timestamp, version );

        variables.forEach( context::put );

        StringWriter writer = new StringWriter();
        engine.evaluate( context, writer, "log-id-template", template );
        Closeables.close( writer );

        return writer.toString();
    }

    public void init( VelocityContext context, DateTime time, Timestamp timestamp, int version ) {
        context.put( "LOG_TYPE", logId.logType );
        context.put( "LOG_VERSION", getHashWithVersion( version ) );
        context.put( "SERVER_HOST", Inet.HOSTNAME );
        context.put( "CLIENT_HOST", logId.clientHostname );
        context.put( "YEAR", String.valueOf( time.getYear() ) );
        context.put( "MONTH", print2Chars( time.getMonthOfYear() ) );
        context.put( "DAY", print2Chars( time.getDayOfMonth() ) );
        context.put( "HOUR", print2Chars( time.getHourOfDay() ) );
        context.put( "MINUTE", print2Chars( time.getMinuteOfHour() ) );
        context.put( "SECOND", print2Chars( time.getSecondOfMinute() ) );
        context.put( "INTERVAL", print2Chars( timestamp.currentBucket( time ) ) );
        context.put( "LOG_TIME_INTERVAL", String.valueOf( 60 / timestamp.bucketsPerHour ) );
        context.put( "REGION", System.getenv( "REGION" ) );

        logId.properties.forEach( context::put );
    }

    public String getHashWithVersion( int version ) {
        return "%x-%d".formatted( logId.getHash(), version );
    }

    private String print2Chars( int v ) {
        return v > 9 ? String.valueOf( v ) : "0" + v;
    }
}
