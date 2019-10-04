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
import lombok.extern.slf4j.Slf4j;
import lombok.val;
import oap.util.Lists;
import oap.util.Pair;
import oap.util.Throwables;
import okhttp3.OkHttpClient;
import org.apache.commons.lang3.StringUtils;
import org.influxdb.InfluxDB;
import org.influxdb.InfluxDBFactory;
import org.influxdb.dto.BatchPoints;
import org.influxdb.dto.Point;
import org.joda.time.DateTimeUtils;

import java.io.InterruptedIOException;
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
import static oap.util.Pair.__;

@Slf4j
class InfluxDBReporter extends ScheduledReporter {
    private final InfluxDB influxDB;
    private final String database;
    private final Map<String, String> tags;
    private final boolean resetTimersAfterReport;
    private final boolean skipEmpty;
    private final Collection<Pattern> aggregates;
    private final InfluxMetricsConfiguration imc;

    @SneakyThrows
    protected InfluxDBReporter( InfluxDB influxDB, String database, Map<String, String> tags,
                                MetricRegistry registry, String name,
                                MetricFilter filter, Collection<String> aggregates,
                                TimeUnit rateUnit, TimeUnit durationUnit,
                                boolean resetTimersAfterReport,
                                boolean skipEmpty,
                                InfluxMetricsConfiguration imc ) {
        super( registry, name, filter, rateUnit, durationUnit );
        this.influxDB = influxDB;
        this.database = database;
        this.tags = tags;
        this.resetTimersAfterReport = resetTimersAfterReport;
        this.skipEmpty = skipEmpty;

        this.aggregates = Lists.map( aggregates,
            a -> Pattern.compile( a.replace( ".", "\\." ).replace( "\\.*", "(\\.[^,\\s]+)([^\\s]*)" ) ) );
        this.imc = imc;

        log.info( "aggregates = {}", aggregates );

    }

    public static Builder forRegistry( MetricRegistry registry ) {
        return new Builder( registry );
    }

    private static Point.Builder toPointBuilder( String pointName ) {
        val a = StringUtils.split( pointName, ',' );
        val point = Point.measurement( a[0] );

        for( int i = 1; i < a.length; i++ ) {
            val t = a[i];
            val idx = t.indexOf( '=' );
            point.tag( t.substring( 0, idx ), t.substring( idx + 1 ) );
        }

        return point;
    }

    @Override
    public synchronized void report( SortedMap<String, Gauge> gauges, SortedMap<String, Counter> counters,
                                     SortedMap<String, Histogram> histograms, SortedMap<String, Meter> meters, SortedMap<String, Timer> timers ) {
        log.trace( "report..." );
        try {

            val time = DateTimeUtils.currentTimeMillis();

            val pointsBuilder = BatchPoints.database( database );

            val points = pointsBuilder.build();

            val builders = new TreeMap<String, Point.Builder>();

            reportCounters( counters, builders );
            reportMeters( meters, builders );
            reportTimers( timers, builders );
            reportGauges( gauges, builders );
            reportHistograms( histograms, builders );

            builders.entrySet().removeIf( entry -> !entry.getValue().hasFields() );
            builders.values().forEach( b -> points.point( b.time( time, MILLISECONDS ).build() ) );

            if( log.isTraceEnabled() )
                log.trace( "reporting {} counters, {} meters, {} timers, {} gauges, {} histograms...",
                    counters.size(), meters.size(), timers.size(), gauges.size(), histograms
                );
            influxDB.write( points );
            if( log.isTraceEnabled() )
                log.trace( "reporting {} counters, {} meters, {} timers, {} gauges, {} histograms... Done",
                    counters.size(), meters.size(), timers.size(), gauges.size(), histograms
                );
        } catch( Throwable e ) {
            val rootCause = Throwables.getRootCause( e );
            if( rootCause instanceof SocketException || rootCause instanceof InterruptedIOException ) {
                log.error( e.getMessage() );
            } else {
                log.error( e.getMessage(), e );
            }
        }
    }

    private void reportCounters( SortedMap<String, Counter> counters, SortedMap<String, Point.Builder> builders ) {
        if( log.isTraceEnabled() )
            log.trace( "counters {}", counters.keySet() );
        report( counters, builders, ( b, e ) -> b.addField( e.getKey(), e.getValue().getCount() ) );
    }

    private <T extends Metric> void report(
        SortedMap<String, T> counters,
        SortedMap<String, Point.Builder> builders,
        BiConsumer<Point.Builder, Map.Entry<String, T>> func ) {

        val ap = aggregate( counters );

        ap.forEach( ( pointName, metrics ) -> {
            Point.Builder builder = builders.computeIfAbsent( pointName, ( p ) -> {
                val b = toPointBuilder( pointName );
                tags.forEach( b::tag );
                return b;
            } );

            for( val entry : metrics.entrySet() ) {
                func.accept( builder, entry );
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

                val field = matcher.group( 1 ).substring( 1 );
                val tags = matcher.group( 2 );
                val point = StringUtils.removeEnd( entry.getKey(), matcher.group( 1 ) + tags ) + tags;
                val map = result.computeIfAbsent( point, ( p ) -> new TreeMap<>() );
                map.put( field, entry.getValue() );
            } else {
                val map = new TreeMap<String, T>();
                map.put( "value", entry.getValue() );
                result.put( entry.getKey(), map );
            }

        }

        return result;
    }

    private void reportMeters( SortedMap<String, Meter> meters, SortedMap<String, Point.Builder> builders ) {
        if( log.isTraceEnabled() )
            log.trace( "meters {}", meters.keySet() );

        val mc = imc.meter;

        report( meters, builders,
            ( b, e ) -> {
                val key = e.getKey();
                val m = e.getValue();

                if( skipEmpty && m.getCount() == 0 ) return;
                b.addField( key, convertRate( m.getOneMinuteRate() ) );
                if( mc.oneMinuteRate ) b.addField( key + "_oneMinuteRate", convertRate( m.getOneMinuteRate() ) );
                if( mc.fiveMinuteRate ) b.addField( key + "_fiveMinuteRate", convertRate( m.getFiveMinuteRate() ) );
                if( mc.fifteenMinuteRate ) b.addField( key + "_fifteenMinuteRate", convertRate( m.getFifteenMinuteRate() ) );
                if( mc.count ) b.addField( key + "_count", convertRate( m.getCount() ) );
                if( mc.meanRate ) b.addField( key + "_meanRate", convertRate( m.getMeanRate() ) );
            }
        );
    }

    private void reportTimers( SortedMap<String, Timer> timers, SortedMap<String, Point.Builder> builders ) {
        if( log.isTraceEnabled() )
            log.trace( "timers {}", timers.keySet() );

        val tc = imc.timer;

        report( timers, builders,
            ( b, e ) -> {
                val key = e.getKey();
                val t = e.getValue();

                if( skipEmpty && t.getCount() == 0 ) return;

                val snapshot = t.getSnapshot();
                b.addField( key, convertDuration( snapshot.getMean() ) );
                if( tc.mean ) b.addField( key + "_mean", convertDuration( snapshot.getMean() ) );
                if( tc.p75th ) b.addField( key + "_75th", convertDuration( snapshot.get75thPercentile() ) );
                if( tc.p95th ) b.addField( key + "_95th", convertDuration( snapshot.get95thPercentile() ) );
                if( tc.p98th ) b.addField( key + "_98th", convertDuration( snapshot.get98thPercentile() ) );
                if( tc.p99th ) b.addField( key + "_99th", convertDuration( snapshot.get99thPercentile() ) );
                if( tc.p999th ) b.addField( key + "_999th", convertDuration( snapshot.get999thPercentile() ) );
                if( tc.max ) b.addField( key + "_max", convertDuration( snapshot.getMax() ) );
                if( tc.min ) b.addField( key + "_min", convertDuration( snapshot.getMin() ) );
                if( tc.median ) b.addField( key + "_median", convertDuration( snapshot.getMedian() ) );
                if( tc.stddev ) b.addField( key + "_stddev", convertDuration( snapshot.getStdDev() ) );
                if( tc.count ) b.addField( key + "_count", t.getCount() );
                if( tc.oneMinuteRate ) b.addField( key + "_oneMinuteRate", convertRate( t.getOneMinuteRate() ) );
                if( tc.fiveMinuteRate ) b.addField( key + "_fiveMinuteRate", convertRate( t.getFiveMinuteRate() ) );
                if( tc.fifteenMinuteRate ) b.addField( key + "_fifteenMinuteRate", convertRate( t.getFifteenMinuteRate() ) );
                if( tc.meanRate ) b.addField( key + "_meanRate", convertRate( t.getMeanRate() ) );
            }
        );

        if( resetTimersAfterReport ) {
            for( val timerName : timers.keySet() ) {
                if( Metrics2.hdrTimers.containsKey( timerName ) ) continue;

                Metrics.registry.remove( timerName );
            }
        }
    }

    private void reportGauges( SortedMap<String, Gauge> gauges, SortedMap<String, Point.Builder> builders ) {
        if( log.isTraceEnabled() )
            log.trace( "gauges {}", gauges.keySet() );

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
        if( log.isTraceEnabled() )
            log.trace( "histograms {}", histograms.keySet() );

        val hc = imc.histogram;

        report( histograms, builders,
            ( b, e ) -> {
                val key = e.getKey();
                val h = e.getValue();

                if( skipEmpty && h.getCount() == 0 ) return;

                val snapshot = h.getSnapshot();
                b.addField( key, snapshot.getMean() );
                if( hc.mean ) b.addField( key + "_mean", snapshot.getMean() );
                if( hc.p75th ) b.addField( key + "_75th", snapshot.get75thPercentile() );
                if( hc.p95th ) b.addField( key + "_95th", snapshot.get95thPercentile() );
                if( hc.p98th ) b.addField( key + "_98th", snapshot.get98thPercentile() );
                if( hc.p99th ) b.addField( key + "_99th", snapshot.get99thPercentile() );
                if( hc.p999th ) b.addField( key + "_999th", snapshot.get999thPercentile() );
                if( hc.max ) b.addField( key + "_max", snapshot.getMax() );
                if( hc.min ) b.addField( key + "_min", snapshot.getMin() );
                if( hc.median ) b.addField( key + "_median", snapshot.getMedian() );
                if( hc.stddev ) b.addField( key + "_stddev", snapshot.getStdDev() );
                if( hc.count ) b.addField( key + "_count", h.getCount() );
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
        private boolean skipEmpty = false;
        private InfluxMetricsConfiguration imc = new InfluxMetricsConfiguration();

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

        public Builder withImc( InfluxMetricsConfiguration imc ) {
            this.imc = imc;
            return this;
        }

        public Builder convertDurationsTo( TimeUnit durationUnit ) {
            this.durationUnit = durationUnit;
            return this;
        }

        public InfluxDBReporter build() {
            val builder = new OkHttpClient.Builder()
                .connectTimeout( connectionTimeout, MILLISECONDS )
                .readTimeout( readTimeout, MILLISECONDS )
                .writeTimeout( writeTimeout, MILLISECONDS );

            val influxDB = InfluxDBFactory.connect( "http://" + host + ":" + port, login, password, builder );

            if( log.isTraceEnabled() )
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
                resetTimersAfterReport,
                skipEmpty,
                imc );
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

        public Builder withSkipEmpty( boolean skipEmpty ) {
            this.skipEmpty = skipEmpty;
            return this;
        }
    }
}
