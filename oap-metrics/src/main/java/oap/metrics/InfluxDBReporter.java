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

import com.codahale.metrics.*;
import com.codahale.metrics.Timer;
import org.apache.commons.lang3.StringUtils;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.ConnectException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.stream.Collectors.toList;
import static org.joda.time.DateTimeUtils.currentTimeMillis;

class InfluxDBReporter extends ScheduledReporter {
    private static final Logger logger = LoggerFactory.getLogger( InfluxDBReporter.class );

    private final InfluxDB influxDB;
    private final String database;
    private final Map<String, String> tags;
    private final Collection<Pattern> aggregates;

    protected InfluxDBReporter( InfluxDB influxDB, String database, Map<String, String> tags,
                                MetricRegistry registry, String name,
                                MetricFilter filter, Collection<String> aggregates,
                                TimeUnit rateUnit, TimeUnit durationUnit ) {
        super( registry, name, filter, rateUnit, durationUnit );
        this.influxDB = influxDB;
        this.database = database;
        this.tags = tags;

        assert !aggregates.stream().filter( a -> !a.endsWith( ".*" ) ).findAny().isPresent();

        this.aggregates = aggregates
            .stream()
            .map( a -> Pattern.compile( a.replace( ".", "\\." ).replace( "\\.*", "(\\.[^.]+)" ) ) )
            .collect( toList() );

    }

    public static Builder forRegistry( MetricRegistry registry ) {
        return new Builder( registry );
    }

    @Override
    public synchronized void report( SortedMap<String, Gauge> gauges, SortedMap<String, Counter> counters,
                                     SortedMap<String, Histogram> histograms, SortedMap<String, Meter> meters, SortedMap<String, Timer> timers ) {
        try {

            final long time = currentTimeMillis();
            BatchPoints.Builder pointsBuilder = BatchPoints.database( database );

            BatchPoints points = pointsBuilder
                .consistency( InfluxDB.ConsistencyLevel.QUORUM )
                .time( time, MILLISECONDS )
                .build();

            final SortedMap<String, Point.Builder> builders = new TreeMap<>();

            reportCounters( counters, builders );
            reportMeters( meters, builders );
            reportTimers( timers, builders );
            reportGauges( gauges, builders );
            reportHistograms( histograms, builders );

            builders.values().forEach( b -> points.point( b.build() ) );

            logger.trace( "reporting {} counters, {} meters, {} timers, {} gauges, {} histograms",
                counters.size(), meters.size(), timers.size(), gauges.size(), histograms
            );
            influxDB.write( points );
        } catch( Exception e ) {
            if( e.getCause() instanceof ConnectException ) {
                logger.error( e.getMessage() );
            } else {
                logger.error( e.getMessage(), e );
            }
        }
    }

    private void reportCounters( SortedMap<String, Counter> counters, SortedMap<String, Point.Builder> builders ) {
        report( counters, builders, Counter::getCount );
    }

    private <T extends Metric> void report(
        SortedMap<String, T> counters,
        SortedMap<String, Point.Builder> builders,
        Function<T, Object> func ) {

        final Map<String, SortedMap<String, T>> ap = aggregate( counters );

        ap.forEach( ( pointName, metrics ) -> {
            Point.Builder builder = builders.computeIfAbsent( pointName, ( p ) -> {
                final Point.Builder b = Point.measurement( pointName ).time( currentTimeMillis(), MILLISECONDS );
                tags.forEach( b::tag );
                return b;
            } );

            for( Map.Entry<String, T> entry : metrics.entrySet() ) {
                builder.field( entry.getKey(), func.apply( entry.getValue() ) );
            }
        } );
    }

    private <T extends Metric> SortedMap<String, SortedMap<String, T>> aggregate( SortedMap<String, T> metrics ) {
        final SortedMap<String, SortedMap<String, T>> result = new TreeMap<>();

        for( final Map.Entry<String, T> entry : metrics.entrySet() ) {
            final Optional<Matcher> m = aggregates
                .stream()
                .map( a -> a.matcher( entry.getKey() ) )
                .filter( Matcher::find )
                .findAny();

            if( m.isPresent() ) {
                final String field = m.get().group( 1 ).substring( 1 );
                final String point = StringUtils.removeEnd( entry.getKey(), m.get().group( 1 ) );
                final SortedMap<String, T> map = result.computeIfAbsent( point, ( p ) -> new TreeMap<>() );
                map.put( field, entry.getValue() );
            } else {
                final SortedMap<String, T> map = new TreeMap<>();
                map.put( "value", entry.getValue() );
                result.put( entry.getKey(), map );
            }

        }

        return result;
    }

    private void reportMeters( SortedMap<String, Meter> meters, SortedMap<String, Point.Builder> builders ) {
        report( meters, builders, ( m ) -> format( convertRate( m.getOneMinuteRate() ) ) );
    }

    private void reportTimers( SortedMap<String, Timer> timers, SortedMap<String, Point.Builder> builders ) {
        report( timers, builders, ( t ) -> format( convertDuration( t.getSnapshot().getMean() ) ) );
    }

    private void reportGauges( SortedMap<String, Gauge> gauges, SortedMap<String, Point.Builder> builders ) {
        report( gauges, builders, ( g ) -> format( g.getValue() ) );
    }

    private void reportHistograms( SortedMap<String, Histogram> histograms, SortedMap<String, Point.Builder> builders ) {
        report( histograms, builders, ( h ) -> format( h.getSnapshot().getMean() ) );
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
        private ArrayList<String> aggregates;

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
                aggregates,
                rateUnit,
                durationUnit );
        }

        public Builder withFilter( ReporterFilter filter ) {
            this.filter = filter;
            return this;
        }

        public Builder withAggregates( ArrayList<String> aggregates ) {
            this.aggregates = aggregates;
            return this;
        }
    }
}
