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
import com.codahale.metrics.Metric;
import com.codahale.metrics.MetricFilter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.ScheduledReporter;
import com.codahale.metrics.Timer;
import lombok.SneakyThrows;
import oap.util.Lists;
import oap.util.Pair;
import oap.util.Stream;
import oap.util.Throwables;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.StringUtils;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;
import org.joda.time.DateTimeUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InterruptedIOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.SocketException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static oap.util.Pair.__;

class InfluxDBReporter extends ScheduledReporter {
    private static final Logger logger = LoggerFactory.getLogger( InfluxDBReporter.class );
    private final InfluxDB influxDB;
    private final String database;
    private final Map<String, String> tags;
    private final boolean resetTimersAfterReport;
    private final Collection<Pattern> aggregates;

    @SneakyThrows
    protected InfluxDBReporter( InfluxDB influxDB, String database, Map<String, String> tags,
                                MetricRegistry registry, String name,
                                MetricFilter filter, Collection<String> aggregates,
                                TimeUnit rateUnit, TimeUnit durationUnit,
                                boolean resetTimersAfterReport ) {
        super( registry, name, filter, rateUnit, durationUnit );
        this.influxDB = influxDB;
        this.database = database;
        this.tags = tags;
        this.resetTimersAfterReport = resetTimersAfterReport;

        final Field key_escaper = Point.class.getDeclaredField( "KEY_ESCAPER" );
        key_escaper.setAccessible( true );

        Field modifiersField = Field.class.getDeclaredField( "modifiers" );
        modifiersField.setAccessible( true );
        modifiersField.setInt( key_escaper, key_escaper.getModifiers() & ~Modifier.FINAL );

        key_escaper.set( null, ( Function<String, String> ) s -> s.replace( " ", "\\ " ) );

        this.aggregates = Lists.map( aggregates,
            a -> Pattern.compile( a.replace( ".", "\\." ).replace( "\\.*", "(\\.[^,\\s]+)([^\\s]*)" ) ) );

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

            BatchPoints points = pointsBuilder.build();

            final SortedMap<String, Point.Builder> builders = new TreeMap<>();

            reportCounters( counters, builders );
            reportMeters( meters, builders );
            reportTimers( timers, builders );
            reportGauges( gauges, builders );
            reportHistograms( histograms, builders );

            builders.values().forEach( b -> points.point( b.time( time, MILLISECONDS ).build() ) );

            logger.trace( "reporting {} counters, {} meters, {} timers, {} gauges, {} histograms",
                counters.size(), meters.size(), timers.size(), gauges.size(), histograms
            );
            influxDB.write( points );
        } catch( Exception e ) {
            Throwable rootCause = Throwables.getRootCause( e );
            if( rootCause instanceof SocketException || rootCause instanceof InterruptedIOException ) {
                logger.error( e.getMessage() );
            } else {
                logger.error( e.getMessage(), e );
            }
        }
    }

    private void reportCounters( SortedMap<String, Counter> counters, SortedMap<String, Point.Builder> builders ) {
        report( counters, builders, ( b, e ) -> b.addField( e.getKey(), e.getValue().getCount() ) );
    }

    private <T extends Metric> void report(
        SortedMap<String, T> counters,
        SortedMap<String, Point.Builder> builders,
        BiConsumer<Point.Builder, Map.Entry<String, T>>... funcs ) {

        final Map<String, SortedMap<String, T>> ap = aggregate( counters );

        ap.forEach( ( pointName, metrics ) -> {
            Point.Builder builder = builders.computeIfAbsent( pointName, ( p ) -> {
                final Point.Builder b = Point.measurement( pointName );
                tags.forEach( b::tag );
                return b;
            } );

            for( Map.Entry<String, T> entry : metrics.entrySet() ) {
                Stream.of( funcs ).forEach( func -> func.accept( builder, entry ) );
            }
        } );
    }

    private <T extends Metric> SortedMap<String, SortedMap<String, T>> aggregate( SortedMap<String, T> metrics ) {
        final SortedMap<String, SortedMap<String, T>> result = new TreeMap<>();

        for( final Map.Entry<String, T> entry : metrics.entrySet() ) {
            final Optional<Pair<Pattern, Matcher>> m = aggregates
                .stream()
                .map( a -> __( a, a.matcher( entry.getKey() ) ) )
                .filter( p -> p._2.find() )
                .findAny();

            if( m.isPresent() ) {
                final Matcher matcher = m.get()._2;

                if( matcher.groupCount() != 2 ) {
                    throw new IllegalArgumentException(
                        "Wrong reporter pattern '" + m.get()._1.pattern() + "' or input '" + entry.getKey() + "'"
                    );
                }

                final String field = matcher.group( 1 ).substring( 1 );
                final String tags = matcher.group( 2 );
                final String point = StringUtils.removeEnd( entry.getKey(), matcher.group( 1 ) + tags ) + tags;
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
        report( meters, builders,
            ( b, e ) -> {
                var key = e.getKey();
                var m = e.getValue();
                b.addField( key, convertRate( m.getOneMinuteRate() ) );
                b.addField( key + "_oneMinuteRate", convertRate( m.getOneMinuteRate() ) );
                b.addField( key + "_fiveMinuteRate", convertRate( m.getFiveMinuteRate() ) );
                b.addField( key + "_fifteenMinuteRate", convertRate( m.getFifteenMinuteRate() ) );
                b.addField( key + "_count", convertRate( m.getCount() ) );
                b.addField( key + "_meanRate", convertRate( m.getMeanRate() ) );
            }
        );
    }

    private void reportTimers( SortedMap<String, Timer> timers, SortedMap<String, Point.Builder> builders ) {
        report( timers, builders,
            ( b, e ) -> {
                var key = e.getKey();
                var t = e.getValue();
                var snapshot = t.getSnapshot();
                b.addField( key, convertDuration( snapshot.getMean() ) )
                    .addField( key + "_mean", convertDuration( snapshot.getMean() ) )
                    .addField( key + "_75th", convertDuration( snapshot.get75thPercentile() ) )
                    .addField( key + "_95th", convertDuration( snapshot.get95thPercentile() ) )
                    .addField( key + "_98th", convertDuration( snapshot.get98thPercentile() ) )
                    .addField( key + "_99th", convertDuration( snapshot.get99thPercentile() ) )
                    .addField( key + "_999th", convertDuration( snapshot.get999thPercentile() ) )
                    .addField( key + "_max", convertDuration( snapshot.getMax() ) )
                    .addField( key + "_min", convertDuration( snapshot.getMin() ) )
                    .addField( key + "_median", convertDuration( snapshot.getMedian() ) )
                    .addField( key + "_stddev", convertDuration( snapshot.getStdDev() ) )
                    .addField( key + "_count", t.getCount() )
                    .addField( key + "_oneMinuteRate", convertRate( t.getOneMinuteRate() ) )
                    .addField( key + "_fiveMinuteRate", convertRate( t.getFiveMinuteRate() ) )
                    .addField( key + "_fifteenMinuteRate", convertRate( t.getFifteenMinuteRate() ) )
                    .addField( key + "_meanRate", convertRate( t.getMeanRate() ) );
            }
        );

        if( resetTimersAfterReport ) {
            for( var timerName : timers.keySet() ) {
                if( Metrics2.hdrTimers.containsKey( timerName ) ) continue;

                Metrics.registry.remove( timerName );
            }
        }
    }

    private void reportGauges( SortedMap<String, Gauge> gauges, SortedMap<String, Point.Builder> builders ) {
        report( gauges, builders, ( b, e ) -> field( b, e.getKey(), e.getValue().getValue() ) );
    }

    private void field( Point.Builder point, String name, Object value ) {
        if( value instanceof Long ) {
            point.addField( name, ( Long ) value );
        } else if( value instanceof Integer ) {
            point.addField( name, ( Integer ) value );
        } else point.field( name, value );
    }

    private void reportHistograms( SortedMap<String, Histogram> histograms, SortedMap<String, Point.Builder> builders ) {
        report( histograms, builders,
            ( b, e ) -> {
                var key = e.getKey();
                var h = e.getValue();
                var snapshot = h.getSnapshot();
                b.addField( key, snapshot.getMean() );
                b.addField( key + "_mean", snapshot.getMean() );
                b.addField( key + "_75th", snapshot.get75thPercentile() );
                b.addField( key + "_95th", snapshot.get95thPercentile() );
                b.addField( key + "_98th", snapshot.get98thPercentile() );
                b.addField( key + "_99th", snapshot.get99thPercentile() );
                b.addField( key + "_999th", snapshot.get999thPercentile() );
                b.addField( key + "_max", snapshot.getMax() );
                b.addField( key + "_min", snapshot.getMin() );
                b.addField( key + "_median", snapshot.getMedian() );
                b.addField( key + "_stddev", snapshot.getStdDev() );
                b.addField( key + "_count", h.getCount() );
            }
        );
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
        private long connectionTimeout;
        private long readTimeout;
        private long writeTimeout;
        private boolean resetTimersAfterReport = false;

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
            final OkHttpClient.Builder builder = new OkHttpClient.Builder()
                .connectTimeout( connectionTimeout, MILLISECONDS )
                .readTimeout( readTimeout, MILLISECONDS )
                .writeTimeout( writeTimeout, MILLISECONDS );

            InfluxDB influxDB = InfluxDBFactory.connect( "http://" + host + ":" + port, login, password, builder );

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
                durationUnit,
                resetTimersAfterReport );
        }

        public Builder withFilter( ReporterFilter filter ) {
            this.filter = filter;
            return this;
        }

        public Builder withAggregates( ArrayList<String> aggregates ) {
            this.aggregates = aggregates;
            return this;
        }

        public Builder withConnectionTimeout( long connectionTimeout ) {
            this.connectionTimeout = connectionTimeout;
            return this;
        }

        public Builder withReadTimeout( long readTimeout ) {
            this.readTimeout = readTimeout;
            return this;
        }

        public Builder withWriteTimeout( long writeTimeout ) {
            this.writeTimeout = writeTimeout;
            return this;
        }

        public Builder withResetTimersAfterReport( boolean resetTimersAfterReport ) {
            this.resetTimersAfterReport = resetTimersAfterReport;
            return this;
        }
    }
}
