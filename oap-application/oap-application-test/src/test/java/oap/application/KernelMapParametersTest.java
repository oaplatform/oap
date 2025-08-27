package oap.application;

import oap.application.module.Module;
import oap.testng.Fixtures;
import org.testng.annotations.Test;

import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static oap.testng.Asserts.urlOfTestResource;
import static org.assertj.core.api.Assertions.assertThat;

public class KernelMapParametersTest extends Fixtures {
    @Test
    public void testOrderMapParameter() {
        List<URL> modules = Module.CONFIGURATION.urlsFromClassPath();
        modules.add( urlOfTestResource( getClass(), "order-map.conf" ) );

        try( Kernel kernel = new Kernel( modules ) ) {
            kernel.start( Map.of( "boot.main", "order-map" ) );

            assertThat( new ArrayList<>( kernel.serviceOfClass( TestClassWithMapParameter.class ).orElseThrow().map.keySet() ) ).isEqualTo( List.of( "a", "b", "c", "xx", "ff" ) );
            assertThat( new ArrayList<>( kernel.serviceOfClass( TestClassWithMapParameterMap.class ).orElseThrow().map.get( "required_tp_creationDate" ).keySet() ) ).isEqualTo( List.of( "requiredFor", "tp", "creationDate" ) );
        }
    }

    public static class TestClassWithMapParameter {
        public final Map<String, Integer> map;

        public TestClassWithMapParameter( Map<String, Integer> map ) {
            this.map = map;
        }
    }

    public static class TestClassWithMapParameterMap {
        public final Map<String, Map<String, Integer>> map;

        public TestClassWithMapParameterMap( Map<String, Map<String, Integer>> map ) {
            this.map = map;
        }
    }
}
