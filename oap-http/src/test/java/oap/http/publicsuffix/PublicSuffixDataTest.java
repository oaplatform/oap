/*
 * The MIT License (MIT)
 *
 * Copyright (c) Open Application Platform Authors
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package oap.http.publicsuffix;

import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.net.IDN;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static oap.testng.Asserts.assertString;
import static oap.testng.Asserts.linesOfTestResource;
import static org.assertj.core.api.Assertions.assertThat;

public class PublicSuffixDataTest {
    @DataProvider
    public Object[][] cases() {
        Pattern pattern = Pattern.compile( "checkPublicSuffix\\(('[^']+'|null), ('[^']+'|null)\\);" );
        return linesOfTestResource( getClass(), "test_psl.txt" )
            .map( String::trim )
            .filter( l -> !l.startsWith( "//" ) && !l.isEmpty() )
            .map( pattern::matcher )
            .filter( Matcher::find )
            .map( m -> new Object[] {
                "null".equals( m.group( 1 ) ) ? null : IDN.toUnicode( m.group( 1 ).replace( "'", "" ) ),
                "null".equals( m.group( 2 ) ) ? null : IDN.toUnicode( m.group( 2 ).replace( "'", "" ) )
            } )
            .toArray( length -> new Object[length][2] );
    }

    @Test( dataProvider = "cases" )
    public void baseDomainOf( String domain, String expeted ) {
        assertString( PublicSuffixData.baseDomainOf( domain ) ).isEqualTo( expeted );
    }

    @Test
    public void testEmpty() {
        assertThat( PublicSuffixData.baseDomainOf( null ) ).isNull();
        assertThat( PublicSuffixData.baseDomainOf( "" ) ).isEmpty();
    }
}
