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
import com.google.common.escape.Escapers;
import lombok.SneakyThrows;
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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.stream.Collectors.toList;
import static oap.util.Pair.__;

class InfluxDBReporter extends ScheduledReporter {
    private static final Logger logger = LoggerFactory.getLogger( InfluxDBReporter.class );
    private final InfluxDB influxDB;
    private final String database;
    private final Map<String, String> tags;
    private final Collection<Pattern> aggregates;

    @SneakyThrows
    protected InfluxDBReporter( InfluxDB influxDB, String database, Map<String, String> tags,
                                MetricRegistry registry, String name,
                                MetricFilter filter, Collection<String> aggregates,
                                TimeUnit rateUnit, TimeUnit durationUnit ) {
        super( registry, name, filter, rateUnit, durationUnit );
        this.influxDB = influxDB;
        this.database = database;
        this.tags = tags;

        final Field key_escaper = Point.class.getDeclaredField( "KEY_ESCAPER" );
        key_escaper.setAccessible( true );

        Field modifiersField = Field.class.getDeclaredField( "modifiers" );
        modifiersField.setAccessible( true );
        modifiersField.setInt( key_escaper, key_escaper.getModifiers() & ~Modifier.FINAL );

        key_escaper.set( null, Escapers.builder().addEscape( ' ', "\\ " ).build() );

        this.aggregates = aggregates
            .stream()
            .map( a -> Pattern.compile( a.replace( ".", "\\." ).replace( "\\.*", "(\\.[^,\\s]+)([^\\s]*)" ) ) )
            .collect( toList() );

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
            ( b, e ) -> b.addField( e.getKey(), convertRate( e.getValue().getOneMinuteRate() ) ),
            ( b, e ) -> b.addField( e.getKey() + "_oneMinuteRate", convertRate( e.getValue().getOneMinuteRate() ) ),
            ( b, e ) -> b.addField( e.getKey() + "_fiveMinuteRate", convertRate( e.getValue().getFiveMinuteRate() ) ),
            ( b, e ) -> b.addField( e.getKey() + "_fifteenMinuteRate", convertRate( e.getValue().getFifteenMinuteRate() ) ),
            ( b, e ) -> b.addField( e.getKey() + "_count", convertRate( e.getValue().getCount() ) ),
            ( b, e ) -> b.addField( e.getKey() + "_meanRate", convertRate( e.getValue().getMeanRate() ) )
        );
    }

    private void reportTimers( SortedMap<String, Timer> timers, SortedMap<String, Point.Builder> builders ) {
        report( timers, builders,
            ( b, e ) -> b.addField( e.getKey(), convertDuration( e.getValue().getSnapshot().getMean() ) ),
            ( b, e ) -> b.addField( e.getKey() + "_mean", convertDuration( e.getValue().getSnapshot().getMean() ) ),
            ( b, e ) -> b.addField( e.getKey() + "_75th", convertDuration( e.getValue().getSnapshot().get75thPercentile() ) ),
            ( b, e ) -> b.addField( e.getKey() + "_95th", convertDuration( e.getValue().getSnapshot().get95thPercentile() ) ),
            ( b, e ) -> b.addField( e.getKey() + "_98th", convertDuration( e.getValue().getSnapshot().get98thPercentile() ) ),
            ( b, e ) -> b.addField( e.getKey() + "_99th", convertDuration( e.getValue().getSnapshot().get99thPercentile() ) ),
            ( b, e ) -> b.addField( e.getKey() + "_999th", convertDuration( e.getValue().getSnapshot().get999thPercentile() ) ),
            ( b, e ) -> b.addField( e.getKey() + "_max", convertDuration( e.getValue().getSnapshot().getMax() ) ),
            ( b, e ) -> b.addField( e.getKey() + "_min", convertDuration( e.getValue().getSnapshot().getMin() ) ),
            ( b, e ) -> b.addField( e.getKey() + "_median", convertDuration( e.getValue().getSnapshot().getMedian() ) ),
            ( b, e ) -> b.addField( e.getKey() + "_stddev", convertDuration( e.getValue().getSnapshot().getStdDev() ) ),
            ( b, e ) -> b.addField( e.getKey() + "_count", convertDuration( e.getValue().getCount() ) ),
            ( b, e ) -> b.addField( e.getKey() + "_oneMinuteRate", convertDuration( e.getValue().getOneMinuteRate() ) ),
            ( b, e ) -> b.addField( e.getKey() + "_fiveMinuteRate", convertDuration( e.getValue().getFiveMinuteRate() ) ),
            ( b, e ) -> b.addField( e.getKey() + "_fifteenMinuteRate", convertDuration( e.getValue().getFifteenMinuteRate() ) ),
            ( b, e ) -> b.addField( e.getKey() + "_meanRate", convertDuration( e.getValue().getMeanRate() ) )
        );
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
            ( b, e ) -> b.addField( e.getKey(), e.getValue().getSnapshot().getMean() ),
            ( b, e ) -> b.addField( e.getKey() + "_mean", e.getValue().getSnapshot().getMean() ),
            ( b, e ) -> b.addField( e.getKey() + "_75th", e.getValue().getSnapshot().get75thPercentile() ),
            ( b, e ) -> b.addField( e.getKey() + "_95th", e.getValue().getSnapshot().get95thPercentile() ),
            ( b, e ) -> b.addField( e.getKey() + "_98th", e.getValue().getSnapshot().get98thPercentile() ),
            ( b, e ) -> b.addField( e.getKey() + "_99th", e.getValue().getSnapshot().get99thPercentile() ),
            ( b, e ) -> b.addField( e.getKey() + "_999th", e.getValue().getSnapshot().get999thPercentile() ),
            ( b, e ) -> b.addField( e.getKey() + "_max", convertDuration( e.getValue().getSnapshot().getMax() ) ),
            ( b, e ) -> b.addField( e.getKey() + "_min", convertDuration( e.getValue().getSnapshot().getMin() ) ),
            ( b, e ) -> b.addField( e.getKey() + "_median", convertDuration( e.getValue().getSnapshot().getMedian() ) ),
            ( b, e ) -> b.addField( e.getKey() + "_stddev", convertDuration( e.getValue().getSnapshot().getStdDev() ) ),
            ( b, e ) -> b.addField( e.getKey() + "_count", convertDuration( e.getValue().getCount() ) )
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
    }
}
