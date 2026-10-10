package oap.json.schema;

import org.testng.annotations.Test;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import static org.assertj.core.api.Assertions.assertThat;

public class JsonMessageTest {
    @Test
    public void testCodesAreUniqueAndStartFromThousand() {
        Set<String> codes = Arrays.stream( JsonMessage.values() )
            .map( JsonMessage::code )
            .collect( Collectors.toSet() );

        assertThat( codes ).hasSize( JsonMessage.values().length );
        assertThat( codes ).allMatch( code -> code.startsWith( "JSON-" ) && Integer.parseInt( code.substring( "JSON-".length() ) ) >= 100 );
    }

    @Test
    public void testPlaceholdersFollowTemplateOrderWithoutPrefix() {
        assertThat( JsonMessage.MIN_LENGTH.placeholders() ).containsExactly( "value", "minLength" );
        assertThat( JsonMessage.REQUIRED.placeholders() ).isEmpty();
    }

    @Test
    public void testOfFindsMessageByCode() {
        assertThat( JsonMessage.of( "JSON-110" ) ).isEqualTo( JsonMessage.REQUIRED );
    }
}
