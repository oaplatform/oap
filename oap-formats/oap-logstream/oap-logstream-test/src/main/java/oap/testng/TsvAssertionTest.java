package oap.testng;

import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.entry;


@Deprecated
public class TsvAssertionTest {

    @Test
    public void testContainsHeader() {
        TsvAssertion.assertTsv( "a\tb\tc\n1\t2\t3" ).containsHeader( "a" );

        assertThatThrownBy( () ->
            TsvAssertion.assertTsv( "a\tb\tc\n1\t2\t3" ).containsHeader( "unknown" ) )
            .isInstanceOf( AssertionError.class );
    }

    @Test
    public void testContainsRowCols() {
        TsvAssertion.assertTsv( """
            a\tb\tc
            11\t12\t13
            21\t22\t23""" ).containsRowCols( entry( "a", "11" ), entry( "b", "12" ) );

        assertThatThrownBy( () ->
            TsvAssertion.assertTsv( """
                a\tb\tc
                11\t12\t13
                21\t22\t23""" ).containsRowCols( entry( "a", "11" ), entry( "b", "22" ) )
                .isInstanceOf( AssertionError.class ) );
    }
}
