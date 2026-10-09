package oap.json.schema;

import oap.json.Binder;
import org.testng.annotations.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

public class JsonSchemaErrorPathTest extends AbstractSchemaTest {
    @Test
    public void testPathIsOnTheErrorNotInTheMessage() {
        String schema = "{ type = object, properties { a { type = string } }, required = [a] }";

        List<JsonSchemaError> errors = JsonSchema.schemaFromString( schema )
            .validate( Binder.json.unmarshal( Object.class, "{}" ), false );

        assertThat( errors ).hasSize( 1 );
        assertThat( errors.get( 0 ).path ).isEqualTo( "a" );
        assertThat( errors.get( 0 ).message ).isEqualTo( "required property is missing" );
    }

    @Test
    public void testNestedPathIsJoinedBySlash() {
        String schema = "{ type = object, properties { a { type = array, items { type = object, "
            + "properties { b { type = string } }, required = [b] } } } }";

        List<JsonSchemaError> errors = JsonSchema.schemaFromString( schema )
            .validate( Binder.json.unmarshal( Object.class, "{'a':[{}]}" ), false );

        assertThat( errors ).hasSize( 1 );
        assertThat( errors.get( 0 ).path ).isEqualTo( "a/0/b" );
    }

    @Test
    public void testRootErrorHasNoPath() {
        String schema = "{ type = string, required = true }";

        List<JsonSchemaError> errors = JsonSchema.schemaFromString( schema )
            .validate( null, false );

        assertThat( errors ).hasSize( 1 );
        assertThat( errors.get( 0 ).path ).isNull();
        assertThat( errors.get( 0 ).message ).isEqualTo( "required property is missing" );
    }

    @Test
    public void testArgsStillCarryThePlaceholderValues() {
        String schema = "{ type = object, properties { a { type = string, minLength = 3 } } }";

        List<JsonSchemaError> errors = JsonSchema.schemaFromString( schema )
            .validate( Binder.json.unmarshal( Object.class, "{'a':'x'}" ), false );

        assertThat( errors ).hasSize( 1 );
        JsonSchemaError error = errors.get( 0 );
        assertThat( error.path ).isEqualTo( "a" );
        assertThat( error.args ).containsEntry( "minLength", 3 );
        assertThat( messages( List.of( error ) ) ).containsExactly( "string x is shorter than minLength 3" );
    }
}
