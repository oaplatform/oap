package oap.io;

import oap.util.Strings;
import org.testng.annotations.Test;

import java.util.stream.Stream;

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
        assertThat( Strings.readString( new StringStreamInputStream( Stream.of( "a", "bbb", "cc", "ddd" ) ) ) )
            .isEqualTo( "abbbccddd" );
    }

    @Test
    public void withDelimiter() {
        assertThat( Strings.readString( new StringStreamInputStream( Stream.of( "a", "bbb", "cc", "ddd" ), "," ) ) )
            .isEqualTo( "a,bbb,cc,ddd" );
        assertThat( Strings.readString( new StringStreamInputStream( Stream.of( "a" ), "," ) ) )
            .isEqualTo( "a" );
    }
}