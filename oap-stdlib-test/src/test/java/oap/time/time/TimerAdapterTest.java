package oap.time.time;

import io.micrometer.core.instrument.Timer;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import oap.time.TimerAdapter;
import org.testng.annotations.Test;

import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;


public class TimerAdapterTest {

    @Test
    public void recordMetricCallable() {
        Timer timer = Timer.builder( "test" ).register( new SimpleMeterRegistry() );
        boolean result = TimerAdapter.<Boolean>recordForCallable( timer, () -> {
            TimeUnit.MILLISECONDS.sleep( 500 );
            return true;
        } );
        assertThat( result ).isTrue();
        assertThat( timer.totalTime( TimeUnit.MILLISECONDS ) ).isGreaterThan( 499 );
    }

    @Test
    public void recordMetricSupplier() {
        Timer timer = Timer.builder( "test" ).register( new SimpleMeterRegistry() );
        String result = TimerAdapter.recordForCallable( timer, () -> {
            TimeUnit.MILLISECONDS.sleep( 500 );
            return "abc";
        } );
        assertThat( result ).isEqualTo( "abc" );
        assertThat( timer.totalTime( TimeUnit.MILLISECONDS ) ).isGreaterThan( 499 );
    }
}
