package oap.lang;

import oap.json.Binder;

import java.util.List;
import java.util.Map;

public class MapMerge {
    public static String mergeHocon( String sourceHocon, String setHocon ) {
        Map sourceMap = Binder.hoconWithoutSystemProperties.unmarshal( Map.class, sourceHocon );
        mergeMap( sourceMap, Binder.hoconWithoutSystemProperties.unmarshal( Map.class, setHocon ) );

        return Binder.json.marshalWithDefaultPrettyPrinter( sourceMap );
    }

    public static void merge( Map sourceMap, Map setMap ) {
        mergeMap( sourceMap, setMap );
    }

    @SuppressWarnings( "unchecked" )
    private static void mergeMap( Map sourceMap, Map setMap ) {
        setMap.forEach( ( setKey, setValue ) -> {
            Object sourceValue = sourceMap.get( setKey );
            if( sourceValue == null ) {
                sourceMap.put( setKey, setValue );
            } else if( sourceValue instanceof Map mapSourceValue && setValue instanceof Map mapSetValue ) {
                mergeMap( mapSourceValue, mapSetValue );
            } else if( sourceValue instanceof List listSourceValue && setValue instanceof List listSetValue ) {
                mergeList( listSourceValue, listSetValue );
            } else {
                sourceMap.put( setKey, setValue );
            }
        } );
    }

    @SuppressWarnings( "unchecked" )
    private static void mergeList( List sourceList, List setList ) {
        for( int i = 0; i < setList.size(); i++ ) {
            Object setValue = setList.get( i );

            if( sourceList.size() < i + 1 ) {
                sourceList.add( setValue );
            } else {
                Object sourceValue = sourceList.get( i );

                if( sourceValue instanceof Map mapSourceValue && setValue instanceof Map mapSetValue ) {
                    mergeMap( mapSourceValue, mapSetValue );
                } else if( sourceValue instanceof List listSourceValue && setValue instanceof List listSetValue ) {
                    mergeList( listSourceValue, listSetValue );
                } else {
                    sourceList.set( i, setValue );
                }
            }
        }
    }
}
