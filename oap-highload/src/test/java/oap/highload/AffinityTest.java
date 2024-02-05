package oap.highload;

import org.testng.annotations.Test;

import java.util.stream.IntStream;

import static org.assertj.core.api.Assertions.assertThat;

public class AffinityTest {
    @Test
    public void testCpuParse() {
        assertThat( new Affinity( "1" ).getCpus() ).isEqualTo( new int[] { 1 } );
        assertThat( new Affinity( "3+" ).getCpus() ).isEqualTo( IntStream.range( 3, Runtime.getRuntime().availableProcessors() ).toArray() );
        assertThat( new Affinity( "1, 3 ,7 " ).getCpus() ).isEqualTo( new int[] { 1, 3, 7 } );
        assertThat( new Affinity( "1-3, 8" ).getCpus() ).isEqualTo( new int[] { 1, 2, 3, 8 } );
        assertThat( new Affinity( "1-3, 8" ).isEnabled() ).isTrue();
        assertThat( new Affinity( "*" ).getCpus() ).isEqualTo( new int[0] );
        assertThat( new Affinity( "*" ).isEnabled() ).isFalse();
    }
}
