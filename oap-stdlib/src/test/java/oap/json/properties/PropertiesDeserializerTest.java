package oap.json.properties;

import oap.json.Binder;
import org.testng.annotations.Test;

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
              }
            }
            """;

        TestJsonProperties tp = Binder.json.unmarshal( TestJsonProperties.class, json );
        assertThat( tp.getProperties() ).contains(
            entry( "property1", new TestJsonProperties.TestProperty1( "n1", "n2" ) ),
            entry( "property2", new TestJsonProperties.TestProperty2( 1, 2 ) )
        );
    }
}
