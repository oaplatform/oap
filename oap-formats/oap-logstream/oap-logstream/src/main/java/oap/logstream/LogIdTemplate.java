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

import oap.kubernetes.ReplicaUtils;
import oap.net.Inet;
import oap.reflect.TypeRef;
import oap.template.TemplateAccumulators;
import oap.template.TemplateEngine;
import org.joda.time.DateTime;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * The LogIdTemplate class is used to render templates associated with the LogId object.
 *
 * @see LogId
 * @see oap.logstream.disk.AbstractWriter
 */
public class LogIdTemplate {
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

    public String render( TemplateEngine templateEngine, String template, DateTime time, Timestamp timestamp, int version, String hostname ) {
        LinkedHashMap<String, String> context = new LinkedHashMap<>();
        init( context, time, timestamp, version, hostname );

        context.putAll( variables );

        return templateEngine.getRuntimeTemplate( "LogIdTemplate", new TypeRef<Map<String, String>>() {}, template, TemplateAccumulators.STRING, _ -> {} ).render( context ).get();
    }

    public void init( Map<String, String> context, DateTime time, Timestamp timestamp, int version, String hostname ) {
        context.put( "LOG_TYPE", logId.logType );
        context.put( "LOG_VERSION", getHashWithVersion( version, hostname ) );
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

        context.putAll( logId.properties );
    }

    public String getHashWithVersion( int version, String hostname ) {
        return "%x%d-%d".formatted( logId.getHash(), ReplicaUtils.getReplicaId( hostname ), version );
    }

    private String print2Chars( int v ) {
        return v > 9 ? String.valueOf( v ) : "0" + v;
    }
}
