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

package oap.metrics;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.Histogram;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.codahale.metrics.Timer;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;
import org.joda.time.DateTimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.ConnectException;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;

class InfluxDBReporter extends ScheduledReporter {
    private static final Logger logger = LoggerFactory.getLogger( InfluxDBReporter.class );

    private final InfluxDB influxDB;
    private final String database;
    private final HashMap<String, String> tags;

    protected InfluxDBReporter( InfluxDB influxDB, String database, HashMap<String, String> tags,
        MetricRegistry registry, String name,
        MetricFilter filter, TimeUnit rateUnit,
        TimeUnit durationUnit ) {
        super( registry, name, filter, rateUnit, durationUnit );
        this.influxDB = influxDB;
        this.database = database;
        this.tags = tags;
    }

    @Override
    public void report( SortedMap<String, Gauge> gauges, SortedMap<String, Counter> counters,
        SortedMap<String, Histogram> histograms, SortedMap<String, Meter> meters, SortedMap<String, Timer> timers ) {
        try {

            final long time = DateTimeUtils.currentTimeMillis();
            BatchPoints.Builder pointsBuilder = BatchPoints.database( database );

            BatchPoints points = pointsBuilder
                .consistency( InfluxDB.ConsistencyLevel.QUORUM )
                .time( time, TimeUnit.MILLISECONDS )
                .build();

            reportCounters( counters, points );
            reportMeters( meters, points );
            reportTimers( timers, points );
            reportGauges( gauges, points );

            influxDB.write( points );
        } catch( Exception e ) {
            if( e.getCause() instanceof ConnectException ) {
                logger.error( e.getMessage() );
            } else {
                logger.error( e.getMessage(), e );
            }
        }
    }

    private void reportCounters( SortedMap<String, Counter> counters, BatchPoints points ) {
        for( Map.Entry<String, Counter> entry : counters.entrySet() ) {
            Point.Builder builder = Point
                .measurement( entry.getKey() );

            tags.forEach( builder::tag );

            final Point point = builder
                .field( "value", entry.getValue().getCount() )
                .build();

            points.point( point );
        }
    }

    private void reportMeters( SortedMap<String, Meter> meters, BatchPoints points ) {
        for( Map.Entry<String, Meter> entry : meters.entrySet() ) {
            Point.Builder builder = Point
                .measurement( entry.getKey() );

            tags.forEach( builder::tag );

            final Point point = builder
                .field( "value", format( convertRate( entry.getValue().getOneMinuteRate() ) ) )
                .build();

            points.point( point );
        }
    }

    private void reportTimers( SortedMap<String, Timer> timers, BatchPoints points ) {
        for( Map.Entry<String, Timer> entry : timers.entrySet() ) {
            Point.Builder builder = Point
                .measurement( entry.getKey() );

            tags.forEach( builder::tag );

            final Point point = builder
                .field( "value", format( convertDuration( entry.getValue().getSnapshot().getMean() ) ) )
                .build();

            points.point( point );
        }
    }

    private void reportGauges( SortedMap<String, Gauge> gauges, BatchPoints points ) {
        for( Map.Entry<String, Gauge> entry : gauges.entrySet() ) {
            Point.Builder builder = Point
                .measurement( entry.getKey() );

            tags.forEach( builder::tag );

            final Point point = builder
                .field( "value", format( entry.getValue().getValue() ) )
                .build();

            points.point( point );
        }
    }


    public static Builder forRegistry( MetricRegistry registry ) {
        return new Builder( registry );
    }

    public static class Builder {
        private final MetricRegistry registry;
        private final HashMap<String, String> tags = new HashMap<>();
        private String host;
        private int port;
        private String database;
        private String login;
        private String password;
        private TimeUnit rateUnit;
        private TimeUnit durationUnit;

        public Builder( MetricRegistry registry ) {
            this.registry = registry;
        }

        public Builder withTag( String name, String value ) {
            tags.put( name, value );

            return this;
        }

        public Builder withConnect( String host, int port, String database, String login, String password ) {
            this.host = host;
            this.port = port;
            this.database = database;
            this.login = login;
            this.password = password;

            return this;
        }

        public Builder convertRatesTo( TimeUnit rateUnit ) {
            this.rateUnit = rateUnit;
            return this;
        }

        public Builder convertDurationsTo( TimeUnit durationUnit ) {
            this.durationUnit = durationUnit;
            return this;
        }

        public InfluxDBReporter build() {
            InfluxDB influxDB = InfluxDBFactory.connect( "http://" + host + ":" + port, login, password );

            if( logger.isTraceEnabled() )
                influxDB.setLogLevel( InfluxDB.LogLevel.FULL );

            return new InfluxDBReporter(
                influxDB,
                database,
                tags,
                registry,
                "influx-reporter",
                MetricFilter.ALL,
                rateUnit,
                durationUnit );
        }
    }

    private String format( double v ) {
        // the Carbon plaintext format is pretty underspecified, but it seems like it just wants
        // US-formatted digits
        return String.format( Locale.US, "%2.2f", v );
    }

    private String format( Object o ) {
        if( o instanceof Float ) {
            return format( ((Float) o).doubleValue() );
        } else if( o instanceof Double ) {
            return format( ((Double) o).doubleValue() );
        } else if( o instanceof Byte ) {
            return format( ((Byte) o).longValue() );
        } else if( o instanceof Short ) {
            return format( ((Short) o).longValue() );
        } else if( o instanceof Integer ) {
            return format( ((Integer) o).longValue() );
        } else if( o instanceof Long ) {
            return format( ((Long) o).longValue() );
        }
        return null;
    }
}
