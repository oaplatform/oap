package oap.util;

import org.testng.annotations.Test;

import java.util.stream.Stream;

import static java.util.stream.Collectors.toList;
import static oap.util.Lists.of;
import static org.assertj.core.api.Assertions.assertThat;

public class MatchTransformIteratorTest {
    @Test
    public void join() {
        assertThat( MatchTransformIterator.stream(
            Stream.of( "1", "2", "3", "4" ),
            Stream.of( "1", "2", "3", "4" ),
            a -> true,
            String::compareTo, ( a, b ) -> a + b )
            .collect( toList() ) ).isEqualTo( of( "11", "22", "33", "44" ) );
        assertThat( MatchTransformIterator.stream(
            Stream.of( "1", "2", "3", "4" ),
            Stream.of( "1", "2", "3" ),
            a -> true,
            String::compareTo, ( a, b ) -> a + b )
            .collect( toList() ) ).isEqualTo( of( "11", "22", "33" ) );
        assertThat( MatchTransformIterator.stream(
            Stream.of( "1", "2" ),
            Stream.of( "1", "2", "3" ),
            a -> true,
            String::compareTo, ( a, b ) -> a + b )
            .collect( toList() ) ).isEqualTo( of( "11", "22" ) );
    }

    @Test
    public void joinWithLookupSkips() {
        assertThat( MatchTransformIterator.stream(
            Stream.of( "1", "2", "3", "4", "7", "9" ),
            Stream.of( "0", "1", "2", "4", "5", "6", "7", "8" ),
            a -> true,
            String::compareTo, ( a, b ) -> a + b )
            .collect( toList() ) ).isEqualTo( of( "11", "22", "44", "77" ) );

    }

    @Test
    public void invalid() {
        assertThat( MatchTransformIterator.stream(
            Stream.of( "111", "2", "3", "4", "7", "9" ),
            Stream.of( "0", "1", "2", "4", "5", "6", "7", "8" ),
            a -> a.length() == 1,
            String::compareTo, ( a, b ) -> a + b )
            .collect( toList() ) ).isEqualTo( of( "22", "44", "77" ) );
    }
}