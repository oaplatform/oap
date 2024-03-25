package oap.metrics;

import io.prometheus.metrics.model.registry.Collector;
import io.prometheus.metrics.model.registry.MultiCollector;
import io.prometheus.metrics.model.registry.PrometheusRegistry;
import oap.testng.AbstractFixture;
import org.apache.commons.lang3.reflect.FieldUtils;

import java.util.List;
import java.util.Set;

public class MetricsFixture extends AbstractFixture<MetricsFixture> {

    private static final List<Collector> collectors;
    private static final List<MultiCollector> multiCollectors;
    private static final Set<String> prometheusNames;

    static {
        try {
            collectors = ( List<Collector> ) FieldUtils.readDeclaredField( PrometheusRegistry.defaultRegistry, "collectors", true );
            multiCollectors = ( List<MultiCollector> ) FieldUtils.readDeclaredField( PrometheusRegistry.defaultRegistry, "multiCollectors", true );
            prometheusNames = ( Set<String> ) FieldUtils.readDeclaredField( PrometheusRegistry.defaultRegistry, "prometheusNames", true );
        } catch( IllegalAccessException e ) {
            throw new RuntimeException( e );
        }
    }

    @Override
    protected void before() {
        collectors.clear();
        multiCollectors.clear();
        prometheusNames.clear();

        super.before();
    }

    @Override
    protected void after() {
        super.after();

        prometheusNames.clear();
        multiCollectors.clear();
        collectors.clear();
    }
}
