package oap.kubernetes;

import oap.system.Env;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReplicaUtils {
    public static final Pattern POD_NAME_PATTERN = Pattern.compile( "^.+-(\\d+)$" );

    public static int getReplicaId() {
        String podName = Env.get( "POD_NAME" ).orElse( null );
        if( podName == null ) {
            return 0;
        }

        Matcher matcher = POD_NAME_PATTERN.matcher( podName );
        if( matcher.find() ) {
            return Integer.parseInt( matcher.group( 1 ) );
        }

        return 0;
    }
}
