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

package oap.testng.casesuite;

import lombok.extern.slf4j.Slf4j;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import static oap.testng.casesuite.CaseSuite.casesOf;
import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
public class CaseSuiteTest {

    @DataProvider
    public Object[][] collectCases() {
        Object[][] objects = casesOf( this, CaseSuiteTest.class );
        System.out.println( objects );
        return objects;
    }

    public static Object[] thecase( String param ) {
        return CaseSuite.thecase( param );
    }

    @Test( dataProvider = "collectCases" )
    public void check( @SuppressWarnings( "unused" ) CaseContext context, String param ) {
        context.assertion( () -> {
            assertThat( context.caseClass ).isIn( XCases.class, YCases.class );
            assertThat( param ).isIn( "X", "Y" );
        } );
    }
}
