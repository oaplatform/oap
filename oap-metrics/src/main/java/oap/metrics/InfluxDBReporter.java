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
import java.util.Objects;
import java.util.SortedMap;
import java.util.concurrent.TimeUnit;

class InfluxDBReporter extends ScheduledReporter {
    private static final Logger logger = LoggerFactory.getLogger( InfluxDBReporter.class );

    private final InfluxDB influxDB;
    private final String database;
    private final HashMap<String, String> tags;

    private HashMap<String, Object> lastReport = new HashMap<>();

    protected InfluxDBReporter( InfluxDB influxDB, String database, HashMap<String, String> tags,
                                MetricRegistry registry, String name,
                                MetricFilter filter, TimeUnit rateUnit,
                                TimeUnit durationUnit ) {
        super( registry, name, filter, rateUnit, durationUnit );
        this.influxDB = influxDB;
        this.database = database;
        this.tags = tags;
    }

    public static Builder forRegistry( MetricRegistry registry ) {
        return new Builder( registry );
    }

    @Override
    public synchronized void report( SortedMap<String, Gauge> gauges, SortedMap<String, Counter> counters,
                                     SortedMap<String, Histogram> histograms, SortedMap<String, Meter> meters, SortedMap<String, Timer> timers ) {
        try {

            final long time = DateTimeUtils.currentTimeMillis();
            BatchPoints.Builder pointsBuilder = BatchPoints.database( database );

            BatchPoints points = pointsBuilder
                .consistency( InfluxDB.ConsistencyLevel.QUORUM )
                .time( time, TimeUnit.MILLISECONDS )
                .build();

            int c = reportCounters( counters, points );
            int m = reportMeters( meters, points );
            int t = reportTimers( timers, points );
            int g = reportGauges( gauges, points );
            int h = reportHistograms( histograms, points );

            logger.debug( "reporting {} counters, {} meters, {} timers, {} gauges, {} histograms", c, m, t, g, h );
            influxDB.write( points );
        } catch( Exception e ) {
            if( e.getCause() instanceof ConnectException ) {
                logger.error( e.getMessage() );
            } else {
                logger.error( e.getMessage(), e );
            }
        }
    }

    private int reportCounters( SortedMap<String, Counter> counters, BatchPoints points ) {
        int before = points.getPoints().size();
        for( Map.Entry<String, Counter> entry : counters.entrySet() ) {
            final long value = entry.getValue().getCount();
            final Object lastValue = lastReport.computeIfAbsent( entry.getKey(), k -> value );

            if( !Objects.equals( value, lastValue ) ) {
                lastReport.put( entry.getKey(), value );

                Point.Builder builder = Point
                    .measurement( entry.getKey() );

                tags.forEach( builder::tag );

                final Point point = builder
                    .field( "value", value )
                    .build();

                points.point( point );
            }

        }
        return points.getPoints().size() - before;
    }

    private int reportMeters( SortedMap<String, Meter> meters, BatchPoints points ) {
        int before = points.getPoints().size();
        for( Map.Entry<String, Meter> entry : meters.entrySet() ) {
            final double value = entry.getValue().getOneMinuteRate();
            final Object lastValue = lastReport.computeIfAbsent( entry.getKey(), k -> value );

            if( !Objects.equals( value, lastValue ) ) {
                lastReport.put( entry.getKey(), value );

                Point.Builder builder = Point
                    .measurement( entry.getKey() );

                tags.forEach( builder::tag );

                final Point point = builder
                    .field( "value", format( convertRate( value ) ) )
                    .build();

                points.point( point );
            }
        }  return points.getPoints().size() - before;
    }

    private int reportTimers( SortedMap<String, Timer> timers, BatchPoints points ) {
        int before = points.getPoints().size();
        for( Map.Entry<String, Timer> entry : timers.entrySet() ) {
            double value = entry.getValue().getSnapshot().getMean();
            makePoint( entry.getKey(), value, points, format( convertDuration( value ) ) );
        }
        return points.getPoints().size() - before;
    }

    private int reportGauges( SortedMap<String, Gauge> gauges, BatchPoints points ) {
        int before = points.getPoints().size();
        for( Map.Entry<String, Gauge> entry : gauges.entrySet() ) {
            Object value = entry.getValue().getValue();
            makePoint( entry.getKey(), value, points, format( value ) );
        }
        return points.getPoints().size() - before;
    }

    private int reportHistograms( SortedMap<String, Histogram> gauges, BatchPoints points ) {
        int before = points.getPoints().size();
        for( Map.Entry<String, Histogram> entry : gauges.entrySet() ) {
            double mean = entry.getValue().getSnapshot().getMean();
            makePoint( entry.getKey(), mean, points, format( mean ) );
        }
        return points.getPoints().size() - before;
    }

    private void makePoint( String key, Object value, BatchPoints points, Object formatted ) {
        final Object lastValue = lastReport.computeIfAbsent( key, k -> value );

        if( !Objects.equals( value, lastValue ) ) {
            lastReport.put( key, value );

            Point.Builder builder = Point.measurement( key );
            tags.forEach( builder::tag );
            builder.field( "value", formatted );

            points.point( builder.build() );
        }
    }


    private String format( double v ) {
        // the Carbon plaintext format is pretty underspecified, but it seems like it just wants
        // US-formatted digits
        return String.format( Locale.US, "%2.2f", v );
    }

    private String format( Object o ) {
        if( o instanceof Float ) {
            return format( ( ( Float ) o ).doubleValue() );
        } else if( o instanceof Double ) {
            return format( ( ( Double ) o ).doubleValue() );
        } else if( o instanceof Byte ) {
            return format( ( ( Byte ) o ).longValue() );
        } else if( o instanceof Short ) {
            return format( ( ( Short ) o ).longValue() );
        } else if( o instanceof Integer ) {
            return format( ( ( Integer ) o ).longValue() );
        } else if( o instanceof Long ) {
            return format( ( ( Long ) o ).longValue() );
        }
        return null;
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
        private ReporterFilter filter;

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
                filter,
                rateUnit,
                durationUnit );
        }

        public Builder withFilter( ReporterFilter filter ) {
            this.filter = filter;
            return this;
        }
    }
}
