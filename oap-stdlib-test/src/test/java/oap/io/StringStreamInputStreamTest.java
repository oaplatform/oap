package oap.io;

import oap.util.Stream;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

public class StringStreamInputStreamTest {

    @Test
    public void read() {
        StringStreamInputStream is = new StringStreamInputStream( Stream.of( "a", "bbb", "cc", "ddd" ) );
        int c;
        int i = 0;
        char[] result = new char[9];
        while( ( c = is.read() ) != -1 ) result[i++] = ( char ) c;
        assertThat( String.valueOf( result ) ).isEqualTo( "abbbccddd" );
    }

    @Test
    public void readArray() {
        assertThat( new StringStreamInputStream( Stream.of( "a", "bbb", "cc", "ddd" ) ) )
            .hasContent( "abbbccddd" );
    }

    @Test
    public void withDelimiter() {
        assertThat( new StringStreamInputStream( Stream.of( "a", "bbb", "cc", "ddd" ), "," ) )
            .hasContent( "a,bbb,cc,ddd" );
        assertThat( new StringStreamInputStream( Stream.of( "a" ), "," ) )
            .hasContent( "a" );
    }
}
