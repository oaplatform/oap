package oap.kubernetes;

import oap.system.Env;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReplicaUtils {
    public static final Pattern POD_NAME_PATTERN = Pattern.compile( "^.+-(\\d+)$" );

    public static int getReplicaId( String hostname ) {
        Matcher matcher = POD_NAME_PATTERN.matcher( hostname );
        if( matcher.find() ) {
            return Integer.parseInt( matcher.group( 1 ) );
        }

        return 0;
    }
}
