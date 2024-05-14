package oap.http;


import lombok.extern.slf4j.Slf4j;
import oap.json.Binder;
import org.joda.time.DateTime;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
public class CookieTest {
    @Test
    public void testJson() {
        Cookie cookie = Cookie.builder( "name", "val" ).withExpires( new DateTime() ).build();

        log.trace( Binder.json.marshalWithDefaultPrettyPrinter( cookie ) );

        assertThat( Binder.json.marshalWithDefaultPrettyPrinter( Binder.json.clone( cookie ) ) ).isEqualTo( Binder.json.marshalWithDefaultPrettyPrinter( cookie ) );

        log.trace( Binder.json.marshalWithDefaultPrettyPrinter( Binder.json.clone( cookie ) ) );
    }
}
