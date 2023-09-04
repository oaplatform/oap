package oap.hadoop;

import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.conf.Configuration;

import java.util.Map;

@Slf4j
public class OapHadoopConfiguration extends Configuration {
    public OapHadoopConfiguration( Map<String, String> configuration ) {
        super( false );

        log.info( "hadoop conf {}", configuration );
        configuration.forEach( this::set );
    }

    public OapHadoopConfiguration( Configuration other ) {
        super( other );
    }
}
