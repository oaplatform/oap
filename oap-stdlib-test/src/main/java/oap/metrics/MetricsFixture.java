package oap.metrics;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Tags;
import io.micrometer.core.instrument.search.MeterNotFoundException;
import io.micrometer.core.instrument.search.RequiredSearch;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import oap.testng.AbstractFixture;
import org.assertj.core.api.AbstractAssert;

import static org.assertj.core.api.Assertions.assertThat;

public class MetricsFixture extends AbstractFixture<MetricsFixture> {

    private SimpleMeterRegistry registry;

    public MetricsFixture() {
        super( "METRICS" );
    }

    public MetricsAssertion assertMetric( String name, Tags tags ) {
        return new MetricsAssertion( name, tags );
    }

    @Override
    protected void before() {
        registry = new SimpleMeterRegistry();
        Metrics.globalRegistry.add( registry );
        super.before();
    }

    public RequiredSearch get( String name, Tags tags ) {
        return RequiredSearch.in( registry ).name( name ).tags( tags );
    }

    @Override
    protected void after() {
        super.after();
        if( registry != null ) {
            Metrics.globalRegistry.remove( registry );
        }
    }

    public static class CounterMetricsAssertion extends AbstractAssert<CounterMetricsAssertion, Counter> {
        public CounterMetricsAssertion( Counter actual ) {
            super( actual, CounterMetricsAssertion.class );
        }

        public CounterMetricsAssertion isEqualTo( double expected ) {
            assertThat( actual.count() ).isEqualTo( expected );

            return this;
        }
    }

    public static class GaugeMetricsAssertion extends AbstractAssert<GaugeMetricsAssertion, Gauge> {
        public GaugeMetricsAssertion( Gauge actual ) {
            super( actual, GaugeMetricsAssertion.class );
        }

        public GaugeMetricsAssertion isEqualTo( double expected ) {
            assertThat( actual.value() ).isEqualTo( expected );

            return this;
        }
    }

    public class MetricsAssertion extends AbstractAssert<MetricsAssertion, RequiredSearch> {
        protected MetricsAssertion( String name, Tags tags ) {
            super( get( name, tags ), MetricsAssertion.class );
        }

        public CounterMetricsAssertion isCounter() {
            try {
                return new CounterMetricsAssertion( actual.counter() );
            } catch( ClassCastException e ) {
                throw failure( "a metric is not a counter" );
            } catch( MeterNotFoundException e ) {
                throw failure( "metric not found" );
            }
        }

        public GaugeMetricsAssertion isGauge() {
            try {
                return new GaugeMetricsAssertion( actual.gauge() );
            } catch( ClassCastException e ) {
                throw failure( "a metric is not a gauge" );
            } catch( MeterNotFoundException e ) {
                throw failure( "metric not found" );
            }
        }
    }
}
