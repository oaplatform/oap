package oap.storage.mongo;

import org.apache.commons.lang3.StringUtils;
import org.bson.Document;

public class MigrationUtils {
    public static String getString( Document document, String key ) {
        String[] keys = StringUtils.split( key, "." );

        Document current = document;
        Object lastValue = null;

        for( String field : keys ) {
            if( lastValue != null ) {
                if( lastValue instanceof Document doc ) {
                    current = doc;
                } else {
                    return null;
                }
            }

            lastValue = current.get( field );
        }

        return ( String ) lastValue;
    }
}
