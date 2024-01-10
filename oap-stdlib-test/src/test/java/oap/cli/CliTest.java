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
package oap.cli;


import oap.util.Result;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.entry;
import static org.testng.Assert.assertEquals;

public class CliTest {

    private Cli cli;
    private Map<String, Object> result;

    @BeforeMethod
    public void beforeMethod() {
        result = new HashMap<>();
        cli = Cli.<String>create()
            .group( "magic generation", result::putAll,
                Option.simple( "generate" ).required().description( "generate magic" ),
                Option.string( "macaddress" ).required().description( "macaddress" ),
                Option.string( "name" ).required().description( "magic name" ),
                Option.<Map<String, String>>option( "attributes" ).argument( ValueParser.forMap ).description( "magic attributes( comma separated key=value pairs )" ) )
            .group( "magic inspection", result::putAll,
                Option.simple( "inspect" ).required().description( "inspect magic" ),
                Option.string( "magic" ).required().description( "magic file" ),
                Option.string( "macaddress" ).required().description( "macaddress" ) );
    }

    @Test
    public void complex() {
        cli.act( "--generate --macaddress=1:1:1:1:1:1 --name=aaa --attributes=1=2,3=4" );
        assertThat( result ).containsOnly(
            entry( "generate", null ),
            entry( "macaddress", "1:1:1:1:1:1" ),
            entry( "name", "aaa" ),
            entry( "attributes", Map.of( "1", "2", "3", "4" )
            ) );

        result.clear();
        cli.act( "--inspect --macaddress=1:1:1:1:1:1 --magic=aaa" );
        assertThat( result ).containsOnly(
            entry( "inspect", null ),
            entry( "macaddress", "1:1:1:1:1:1" ),
            entry( "magic", "aaa" )
        );

        result.clear();
        cli.act( "--generate --macaddress=1:1:1:1:1:1 --attributes=1=2,3=4" );
        assertThat( result ).isEmpty();
        cli.act( new String[] {} );
        assertThat( result ).isEmpty();
    }

    @Test
    public void optionParse() {
        assertEquals( Option.option( "g" ).required().parse( null ), Result.<Object, String>success( null ) );
    }
}
