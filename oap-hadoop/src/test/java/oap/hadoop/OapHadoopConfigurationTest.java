package oap.hadoop;

import oap.system.Env;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 *
 */
public class OapHadoopConfigurationTest {
    @Test
    public void testEnv() {
        testEquals( "hadoop_fs_sftp_user", "fs.sftp.user" );
        testEquals( "hadoop_fs_sftp_host__user", "fs.sftp.host_user" );
        testEquals( "hadoop_fs_sftp_host___user", "fs.sftp.host_.user" );
        testEquals( "hadoop_fs_sftp_host____user", "fs.sftp.host__user" );

        testEquals( "hadoop__", "." );
        testEquals( "hadoop___", "_" );
    }

    private static void testEquals( String envName, String name ) {
        Env.set( envName, "v" );

        OapHadoopConfiguration configuration = new OapHadoopConfiguration();
        assertThat( configuration.get( name ) ).isEqualTo( "v" );
    }
}
