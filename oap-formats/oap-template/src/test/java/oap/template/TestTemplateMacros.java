package oap.template;

import java.util.LinkedHashMap;

public class TestTemplateMacros {
    public static Double printDouble( LinkedHashMap<String, Object> map, String key ) {
        return ( Double ) map.get( key );
    }
}
