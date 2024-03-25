package oap.metrics;

import io.prometheus.metrics.model.registry.PrometheusRegistry;
import io.prometheus.metrics.model.snapshots.CounterSnapshot;
import io.prometheus.metrics.model.snapshots.DataPointSnapshot;
import io.prometheus.metrics.model.snapshots.GaugeSnapshot;
import io.prometheus.metrics.model.snapshots.HistogramSnapshot;
import io.prometheus.metrics.model.snapshots.Label;
import io.prometheus.metrics.model.snapshots.Labels;
import io.prometheus.metrics.model.snapshots.MetricSnapshot;
import io.prometheus.metrics.model.snapshots.MetricSnapshots;
import io.prometheus.metrics.model.snapshots.SummarySnapshot;
import oap.util.Result;
import org.assertj.core.api.AbstractAssert;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class MetricsAssertion extends AbstractAssert<MetricsAssertion, PrometheusRegistry> {
    protected MetricsAssertion( PrometheusRegistry prometheusRegistry ) {
        super( prometheusRegistry, MetricsAssertion.class );
    }

    private Result<DataPointSnapshot, List<Map<String, String>>> get( String name, Map<String, String> tags ) {
        MetricSnapshots metricSnapshots = PrometheusRegistry.defaultRegistry.scrape();
        for( MetricSnapshot metricSnapshot : metricSnapshots ) {
            String metricName = metricSnapshot.getMetadata().getName();

            if( !metricName.equals( name ) ) continue;

            var available = new ArrayList<Map<String, String>>();

            for( DataPointSnapshot dataPointSnapshot : metricSnapshot.getDataPoints() ) {
                Labels labels = dataPointSnapshot.getLabels();
                var map = new HashMap<String, String>();
                for( Label label : labels ) {
                    String labelName = label.getName();
                    String labelValue = label.getValue();
                    map.put( labelName, labelValue );
                }

                if( !map.equals( tags ) ) {
                    available.add( map );
                    continue;
                }

                return Result.success( dataPointSnapshot );
            }

            return Result.failure( available );
        }
        return Result.failure( List.of( Map.of() ) );
    }

    @SuppressWarnings( "unchecked" )
    private <T extends DataPointSnapshot> T getOrThrow( String name, Map<String, String> tags, Class<T> type ) {
        Result<DataPointSnapshot, List<Map<String, String>>> result = get( name, tags );
        if( !result.isSuccess() ) {
            List<Map<String, String>> failureValue = result.failureValue;
            if( failureValue.isEmpty() ) {
                throw failure( "metric not found" );
            } else {
                throw failure( "metric %s with tags %s, found %s", name, tags, failureValue );
            }
        }

        assertThat( result.successValue ).isInstanceOf( type );

        return ( T ) result.successValue;
    }

    public CounterMetricsAssertion isCounter( String name, Map<String, String> tags ) {
        return new CounterMetricsAssertion( getOrThrow( name, tags, CounterSnapshot.CounterDataPointSnapshot.class ) );
    }

    public GaugeMetricsAssertion isGauge( String name, Map<String, String> tags ) {
        return new GaugeMetricsAssertion( getOrThrow( name, tags, GaugeSnapshot.GaugeDataPointSnapshot.class ) );
    }

    public SummaryMetricsAssertion isSummary( String name, Map<String, String> tags ) {
        return new SummaryMetricsAssertion( getOrThrow( name, tags, SummarySnapshot.SummaryDataPointSnapshot.class ) );
    }

    public HistogramMetricsAssertion isHistogram( String name, Map<String, String> tags ) {
        return new HistogramMetricsAssertion( getOrThrow( name, tags, HistogramSnapshot.HistogramDataPointSnapshot.class ) );
    }

    public static class CounterMetricsAssertion extends AbstractAssert<CounterMetricsAssertion, CounterSnapshot.CounterDataPointSnapshot> {
        public CounterMetricsAssertion( CounterSnapshot.CounterDataPointSnapshot actual ) {
            super( actual, GaugeMetricsAssertion.class );
        }

        public CounterMetricsAssertion isEqualTo( double expected ) {
            assertThat( actual.getValue() ).isEqualTo( expected );

            return this;
        }
    }

    public static class GaugeMetricsAssertion extends AbstractAssert<GaugeMetricsAssertion, GaugeSnapshot.GaugeDataPointSnapshot> {
        public GaugeMetricsAssertion( GaugeSnapshot.GaugeDataPointSnapshot actual ) {
            super( actual, GaugeMetricsAssertion.class );
        }

        public GaugeMetricsAssertion isEqualTo( double expected ) {
            assertThat( actual.getValue() ).isEqualTo( expected );

            return this;
        }
    }

    public static class SummaryMetricsAssertion extends AbstractAssert<SummaryMetricsAssertion, SummarySnapshot.SummaryDataPointSnapshot> {
        public SummaryMetricsAssertion( SummarySnapshot.SummaryDataPointSnapshot actual ) {
            super( actual, SummaryMetricsAssertion.class );
        }

        public SummaryMetricsAssertion isSumEqualTo( double expected ) {
            assertThat( actual.getSum() ).isEqualTo( expected );

            return this;
        }

        public SummaryMetricsAssertion isCountEqualTo( double expected ) {
            assertThat( actual.getCount() ).isEqualTo( expected );

            return this;
        }
    }

    public static class HistogramMetricsAssertion extends AbstractAssert<HistogramMetricsAssertion, HistogramSnapshot.HistogramDataPointSnapshot> {
        public HistogramMetricsAssertion( HistogramSnapshot.HistogramDataPointSnapshot actual ) {
            super( actual, HistogramMetricsAssertion.class );
        }

        public HistogramMetricsAssertion isSumEqualTo( double expected ) {
            assertThat( actual.getSum() ).isEqualTo( expected );

            return this;
        }

        public HistogramMetricsAssertion isCountEqualTo( double expected ) {
            assertThat( actual.getCount() ).isEqualTo( expected );

            return this;
        }
    }
}
