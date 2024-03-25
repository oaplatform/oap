package oap.metrics;

import io.prometheus.metrics.core.metrics.Counter;
import io.prometheus.metrics.core.metrics.GaugeWithCallback;
import io.prometheus.metrics.core.metrics.Histogram;
import io.prometheus.metrics.core.metrics.Info;
import io.prometheus.metrics.core.metrics.StateSet;
import io.prometheus.metrics.core.metrics.Summary;
import io.prometheus.metrics.model.snapshots.Unit;

import java.util.List;
import java.util.function.Consumer;

public class Metrics {
    public static Counter counter( String name, List<String> labelNames ) {
        return Counter.builder().name( name ).labelNames( labels( labelNames ) ).register();
    }

    public static Counter counter( String name ) {
        return Counter.builder().name( name ).register();
    }

    @SafeVarargs
    public static void gaugeWithCallback( String name, List<String> labelNames, Consumer<GaugeWithCallback.Callback>... callbacks ) {
        GaugeWithCallback
            .builder()
            .name( name )
            .labelNames( labels( labelNames ) )
            .callback( callback -> {
                for( var c : callbacks ) {
                    c.accept( callback );
                }
            } )
            .register();
    }

    @SafeVarargs
    public static void gaugeWithCallback( String name, Consumer<GaugeWithCallback.Callback>... callbacks ) {
        gaugeWithCallback( name, List.of(), callbacks );
    }

    public static Info info( String name, List<String> labelNames ) {
        return Info.builder().name( name ).labelNames( labels( labelNames ) ).build();
    }

    public static StateSet stateSet( String name, List<String> labelNames, String... states ) {
        return StateSet.builder().name( name ).labelNames( labels( labelNames ) ).states( states ).register();
    }

    public static StateSet stateSet( String name, List<String> labelNames, Class<? extends Enum<?>> states ) {
        return StateSet.builder().name( name ).labelNames( labels( labelNames ) ).states( states ).register();
    }

    private static String[] labels( List<String> labelNames ) {
        return labelNames.toArray( new String[0] );
    }

    public static Histogram histogram( String name, Unit unit ) {
        return histogram( name, List.of(), unit );
    }

    public static Histogram histogram( String name, List<String> labelNames, Unit unit ) {
        return Histogram.builder().name( name ).labelNames( labels( labelNames ) ).unit( unit ).register();
    }

    public static Summary summary( String name, Unit unit ) {
        return summary( name, List.of(), unit );
    }

    public static Summary summary( String name, List<String> labelNames, Unit unit ) {
        return Summary.builder().name( name ).labelNames( labels( labelNames ) ).unit( unit ).register();
    }
}
