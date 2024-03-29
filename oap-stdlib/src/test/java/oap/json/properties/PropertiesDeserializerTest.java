package oap.json.properties;

import oap.json.Binder;
import org.testng.annotations.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;

public class PropertiesDeserializerTest {
    @Test
    public void testDeserialize() {
        var json = """
            {
              "property1": {
                "name1": "n1",
                "name2": "n2"
              },
              "property2": {
                "name1": 1,
                "name2": 2
              },
              "property3": "str",
              "property4": 5,
              "property5": [
                "list"
              ],
              "property6": {
                "k": "v"
              }
            }
            """;

        TestJsonProperties tp = Binder.json.unmarshal( TestJsonProperties.class, json );
        assertThat( tp.getProperties() ).contains(
            entry( "property1", new TestJsonProperties.TestProperty1( "n1", "n2" ) ),
            entry( "property2", new TestJsonProperties.TestProperty2( 1, 2 ) ),
            entry( "property3", "str" ),
            entry( "property4", 5L ),
            entry( "property5", List.of( "list" ) ),
            entry( "property6", Map.of( "k", "v" ) )
        );
    }
}
