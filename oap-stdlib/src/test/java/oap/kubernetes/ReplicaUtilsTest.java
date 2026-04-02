package oap.kubernetes;

import oap.system.Env;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class ReplicaUtilsTest {
    @Test
    public void testGetReplicaId() {
        try {
            Env.set( "POD_NAME", null );
            assertThat( ReplicaUtils.getReplicaId() ).isZero();

            Env.set( "POD_NAME", "aasdasd-6-ff" );
            assertThat( ReplicaUtils.getReplicaId() ).isZero();

            Env.set( "POD_NAME", "aasdasd-6-0" );
            assertThat( ReplicaUtils.getReplicaId() ).isZero();
            Env.set( "POD_NAME", "aasdasd-6-12" );
            assertThat( ReplicaUtils.getReplicaId() ).isEqualTo( 12 );
        } finally {
            Env.set( "POD_NAME", null );
        }
    }
}
