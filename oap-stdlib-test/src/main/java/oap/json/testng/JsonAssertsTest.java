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

package oap.json.testng;

import org.testng.annotations.Test;

import java.util.Map;

import static oap.json.testng.JsonAsserts.assertJson;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class JsonAssertsTest {

    @Test
    public void isEqualTo() {
        assertJson( "{\"a\":\"b\", \"c\":{\"a\":\"b\"}}" ).isEqualTo( "{\"c\":{\"a\":\"b\"}, \"a\":\"b\"}" );
        assertJson( "{\"a\":\"b\", \"c\":{\"a\":\"bb\"}}" ).isEqualTo( "{\"c\":{\"a\":\"b\"}, \"a\":\"b\"}", str -> str.replace( "bb", "b" ) );
        assertJson( "[{\"a\":\"b\", \"c\":{\"a\":\"b\"}}]" ).isEqualTo( "[{\"c\":{\"a\":\"b\"}, \"a\":\"b\"}]" );
        try {
            assertJson( "{\"a\":\"b\", \"c\":null}" ).isEqualTo( "{\"c\":{\"a\":\"b\"}, \"a\":\"b\"}" );
        } catch( AssertionError e ) {
            assertThat( e.getMessage() ).isEqualTo( """
                expected:<"{
                  "a" : "b"[,
                  "c" : {
                    "a" : "b"
                  }]
                }"> but was:<"{
                  "a" : "b"[]
                }">""" );
        }
    }

    @Test
    public void isEqaulToWithSubstitutes() {
        assertJson( "{\"a\":\"b\", \"c\":{\"a\":\"b\"}}" )
            .isEqualTo( "{\"c\":{\"a\":\"d\"}, \"a\":\"b\"}", Map.of( "[c].[a]", "d" ) );
    }
}
