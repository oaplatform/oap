package oap.ws.openapi;

import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.Schema;
import org.testng.annotations.Test;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.assertThat;

public class OpenApiModuleTest {
    @Test
    public void jsonAndYamlRoundTripWithReferencesAndExtensions() {
        var mapper = JsonMapper.builder().addModule( new OpenApiModule() ).build();
        String document = """
            {"openapi":"3.0.1","info":{"title":"API","version":"1"},"x-owner":"oap",
             "components":{"schemas":{"Node":{"type":"object","properties":{
               "child":{"$ref":"#/components/schemas/Node"}}}}}}
            """;
        var api = mapper.readValue( document, OpenAPI.class );
        assertThat( api.getExtensions() ).containsEntry( "x-owner", "oap" );
        Schema<?> child = ( Schema<?> ) api.getComponents().getSchemas().get( "Node" ).getProperties().get( "child" );
        assertThat( child.get$ref() )
            .isEqualTo( "#/components/schemas/Node" );
        assertThat( mapper.readTree( mapper.writeValueAsString( api ) ) ).isEqualTo( mapper.readTree( document ) );
        var fromYaml = Yaml.mapper().readValue( Yaml.mapper().writeValueAsString( api ), OpenAPI.class );
        assertThat( mapper.readTree( mapper.writeValueAsString( fromYaml ) ) ).isEqualTo( mapper.readTree( document ) );
    }
}
