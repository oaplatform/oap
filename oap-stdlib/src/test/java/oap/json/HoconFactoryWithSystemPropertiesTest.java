package oap.json;


import org.testng.annotations.Test;

import java.net.URI;
import java.net.URISyntaxException;

import static org.assertj.core.api.Assertions.assertThat;

@Test
public class HoconFactoryWithSystemPropertiesTest {
    @Test
    public void testGetParent() throws URISyntaxException {
        assertThat( HoconFactoryWithSystemProperties.getParent( new URI( "jar:file:///test-1.0.0..jar!/META-INF/a.prop" ) ) )
            .isEqualTo( new URI( "jar:file:///test-1.0.0..jar!/META-INF/" ) );
        assertThat( HoconFactoryWithSystemProperties.getParent( new URI( "file:///test-1.0.0..jar!/META-INF/a.prop" ) ) )
            .isEqualTo( new URI( "file:///test-1.0.0..jar!/META-INF/" ) );
    }

}
