package oap.http;

import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class HttpTest {
    @Test
    public void testReplaceHostInUrl() {
        assertThat( Https.replaceHostInUrl( null, null ) ).isNull();

        assertThat( Https.replaceHostInUrl( "http://localhost/me/out?it=5", "myserver:20000" ) ).isEqualTo( "http://myserver:20000/me/out?it=5" );
        assertThat( Https.replaceHostInUrl( "http://localhost:19000/me/out?it=5", "myserver:20000" ) ).isEqualTo( "http://myserver:20000/me/out?it=5" );
        assertThat( Https.replaceHostInUrl( "http://www.test.com/me/out?it=5", "super" ) ).isEqualTo( "http://super/me/out?it=5" );
        assertThat( Https.replaceHostInUrl( "https://localhost/me/out?it=5", "myserver:20000" ) ).isEqualTo( "https://myserver:20000/me/out?it=5" );
        assertThat( Https.replaceHostInUrl( "https://localhost:19000/me/out?it=5", "myserver:20000" ) ).isEqualTo( "https://myserver:20000/me/out?it=5" );
        assertThat( Https.replaceHostInUrl( "https://www.test.com/me/out?it=5", "super" ) ).isEqualTo( "https://super/me/out?it=5" );
        assertThat( Https.replaceHostInUrl( "https://www.test.com:4300/me/out?it=5", "super" ) ).isEqualTo( "https://super/me/out?it=5" );
        assertThat( Https.replaceHostInUrl( "https://www.test.com:4300/me/out?it=5", "super:443" ) ).isEqualTo( "https://super/me/out?it=5" );
        assertThat( Https.replaceHostInUrl( "http://www.test.com:4300/me/out?it=5", "super:80" ) ).isEqualTo( "http://super/me/out?it=5" );
        assertThat( Https.replaceHostInUrl( "http://www.test.com:80/me/out?it=5", "super:8080" ) ).isEqualTo( "http://super:8080/me/out?it=5" );
        assertThat( Https.replaceHostInUrl( "http://www.test.com:80/me/out?it=5&test=5", "super:80" ) ).isEqualTo( "http://super/me/out?it=5&test=5" );
        assertThat( Https.replaceHostInUrl( "https://www.test.com:80/me/out?it=5&test=5", "super:80" ) ).isEqualTo( "https://super:80/me/out?it=5&test=5" );
        assertThat( Https.replaceHostInUrl( "https://www.test.com:80/me/out?it=5&test=5", "super:443" ) ).isEqualTo( "https://super/me/out?it=5&test=5" );
        assertThat( Https.replaceHostInUrl( "http://www.test.com:443/me/out?it=5&test=5", "super:443" ) ).isEqualTo( "http://super:443/me/out?it=5&test=5" );
        assertThat( Https.replaceHostInUrl( "HTTP://www.test.com:443/me/out?it=5&test=5", "super:443" ) ).isEqualTo( "http://super:443/me/out?it=5&test=5" );
        assertThat( Https.replaceHostInUrl( "HTTP://WWW.TEST.COM:443/ME/OUT?IT=5&TEST=5", "SUPERDUPER:443" ) ).isEqualTo( "http://SUPERDUPER:443/ME/OUT?IT=5&TEST=5" );
        assertThat( Https.replaceHostInUrl( "HTTPS://WWW.TEST.COM:22/ME/OUT?IT=5&TEST=5", "SUPERDUPER:23" ) ).isEqualTo( "https://SUPERDUPER:23/ME/OUT?IT=5&TEST=5" );
    }
}
