package oap.json.schema;

import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class JsonSchemaValidatorErrorsTest {
    @Test
    public void codesAreUniqueAndStartFromThousand() {
        Set<Integer> codes = Arrays.stream( JsonSchemaValidatorErrors.values() )
            .map( e -> e.code )
            .collect( Collectors.toSet() );

        assertThat( codes ).hasSize( JsonSchemaValidatorErrors.values().length );
        assertThat( codes ).allMatch( code -> code >= 1000 );
    }

    @Test
    public void placeholdersFollowTemplateOrder() {
        assertThat( JsonSchemaValidatorErrors.MIN_LENGTH.placeholders() ).containsExactly( "value", "minLength" );
        assertThat( JsonSchemaValidatorErrors.REQUIRED.placeholders() ).isEmpty();
    }
}
