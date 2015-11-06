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
package oap.json;

import oap.io.Resources;
import oap.testng.AbstractTest;
import oap.util.Lists;
import oap.util.Maps;
import org.testng.annotations.Test;

import static oap.util.Pair.__;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;

public class ParserTest extends AbstractTest {
    public static String yearJson = Resources.readString( ParserTest.class, "year.json" ).get();

    @Test
    public void parse() {
        assertEquals( Parser.parse( "true" ), Boolean.TRUE );
        assertEquals( Parser.parse( "false" ), Boolean.FALSE );
        assertNull( Parser.parse( "null" ) );
        assertEquals( Parser.<Long>parse( "123" ), new Long( 123 ) );
        assertEquals( Parser.<Double>parse( "123.5" ), 123.5 );
        assertEquals( Parser.parse( "\"str\"" ), "str" );
        assertEquals( Lists.of(), Parser.parse( "[]" ) );
        assertEquals( Lists.of( 1l, 2l, 3l ), Parser.parse( "[1,2,3]" ) );
        assertEquals( Lists.of( 1l, Lists.of( 1l, 2l ), 3l ), Parser.parse( "[1,[1,2],3]" ) );
        assertEquals( Maps.of(), Parser.parse( "{}" ) );
        assertEquals( Lists.of( Maps.of( __( "a", Lists.of( Maps.of() ) ) ) ), Parser.parse( "[{\"a\":[{}]}]" ) );
        System.out.println( Parser.<Object>parse( yearJson ) );
    }

}
