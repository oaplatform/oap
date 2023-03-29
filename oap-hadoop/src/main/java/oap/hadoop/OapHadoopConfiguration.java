package oap.hadoop;

import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.conf.Configuration;

import java.util.Map;

@Slf4j
public class OapHadoopConfiguration extends Configuration {

    public static final String PREFIX = "hadoop_";

    public OapHadoopConfiguration() {
        withEnv();
    }

    public OapHadoopConfiguration( boolean loadDefaults ) {
        super( loadDefaults );

        withEnv();
    }

    public OapHadoopConfiguration( Configuration other ) {
        super( other );

        withEnv();
    }

    @Override
    public synchronized void reloadConfiguration() {
        super.reloadConfiguration();

        withEnv();
    }

    private void withEnv() {
        Map<String, String> env = System.getenv();

        env.forEach( ( k, v ) -> {
            if( k.startsWith( PREFIX ) ) {
                String hadoopKey = unescapeEnv( k.substring( PREFIX.length() ) );

                log.trace( "hadoop env {} = {}", hadoopKey, v );
                set( hadoopKey, v );
            }
        } );
    }

    private static String unescapeEnv( String name ) {
        StringBuilder ret = new StringBuilder();

        boolean escape = false;

        for( int i = 0; i < name.length(); i++ ) {
            char ch = name.charAt( i );
            if( ch == '_' ) {
                if( !escape ) {
                    escape = true;
                } else {
                    ret.append( '_' );
                    escape = false;
                }
            } else {
                if( escape ) {
                    ret.append( '.' );
                    escape = false;
                }
                ret.append( ch );
            }
        }

        if( escape ) ret.append( '.' );

        return ret.toString();
    }
}
