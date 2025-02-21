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
import org.apache.velocity.runtime.resource.loader.ClasspathResourceLoader;
import org.joda.time.DateTime;

import java.io.StringWriter;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.apache.velocity.runtime.RuntimeConstants.RESOURCE_LOADER;

/**
 * The LogIdTemplate class is used to render templates associated with the LogId object.
 *
 * @see LogId
 * @see oap.logstream.disk.AbstractWriter
 */
public class LogIdTemplate {
    private final LogId logId;
    private final LinkedHashMap<String, String> variables = new LinkedHashMap<>();
    private final VelocityEngine engine = new VelocityEngine();

    public LogIdTemplate( LogId logId ) {
        this.logId = logId;

        engine.setProperty( RESOURCE_LOADER, "classpath" );
        engine.setProperty( "classpath.resource.loader.class", ClasspathResourceLoader.class.getName() );

        engine.init();
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
        engine.evaluate( context, writer, "cl", template );
        Closeables.close( writer );

        return writer.toString();
    }

    public void init( VelocityContext velocityContext, DateTime time, Timestamp timestamp, int version ) {
        velocityContext.put( "LOG_TYPE", logId.logType );
        velocityContext.put( "LOG_VERSION", getHashWithVersion( version ) );
        velocityContext.put( "SERVER_HOST", Inet.HOSTNAME );
        velocityContext.put( "CLIENT_HOST", logId.clientHostname );
        velocityContext.put( "YEAR", String.valueOf( time.getYear() ) );
        velocityContext.put( "MONTH", print2Chars( time.getMonthOfYear() ) );
        velocityContext.put( "DAY", print2Chars( time.getDayOfMonth() ) );
        velocityContext.put( "HOUR", print2Chars( time.getHourOfDay() ) );
        velocityContext.put( "MINUTE", print2Chars( time.getMinuteOfHour() ) );
        velocityContext.put( "SECOND", print2Chars( time.getSecondOfMinute() ) );
        velocityContext.put( "INTERVAL", print2Chars( timestamp.currentBucket( time ) ) );
        velocityContext.put( "LOG_TIME_INTERVAL", String.valueOf( 60 / timestamp.bucketsPerHour ) );
        velocityContext.put( "REGION", System.getenv( "REGION" ) );

        logId.properties.forEach( velocityContext::put );
    }

    public String getHashWithVersion( int version ) {
        return "%x-%d".formatted( logId.getHash(), version );
    }

    private String print2Chars( int v ) {
        return v > 9 ? String.valueOf( v ) : "0" + v;
    }
}
