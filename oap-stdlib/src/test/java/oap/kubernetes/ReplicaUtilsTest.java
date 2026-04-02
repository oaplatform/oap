package oap.kubernetes;

import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ReplicaUtilsTest {
    @Test
    public void testGetReplicaId() {
        assertThat( ReplicaUtils.getReplicaId( "aasdasd-6-ff" ) ).isZero();

        assertThat( ReplicaUtils.getReplicaId( "aasdasd-6-0" ) ).isZero();
        assertThat( ReplicaUtils.getReplicaId( "aasdasd-6-12" ) ).isEqualTo( 12 );
    }
}
