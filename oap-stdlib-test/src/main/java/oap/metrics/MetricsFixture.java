package oap.metrics;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.search.RequiredSearch;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import oap.testng.AbstractScopeFixture;

import static org.assertj.core.api.Assertions.assertThat;

public class MetricsFixture extends AbstractScopeFixture<MetricsFixture> {

    private SimpleMeterRegistry registry;

    @Override
    protected void before() {
        registry = new SimpleMeterRegistry();
        Metrics.globalRegistry.add( registry );
        super.before();
    }

    public RequiredSearch get( String name, String... tags ) {

        RequiredSearch rs = RequiredSearch.in( Metrics.globalRegistry ).name( name );

        assertThat( ( tags.length / 2 ) * 2 ).isEqualTo( tags.length );

        for( int i = 0; i < tags.length / 2; i += 2 ) {
            rs = rs.tag( tags[i], tags[i + 1] );
        }

        return rs;
    }

    @Override
    protected void after() {
        super.after();
        if( registry != null ) {
            Metrics.globalRegistry.remove( registry );
        }
    }
}
