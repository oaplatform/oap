/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015 Volodymyr Kyrychenko <vladimir.kirichenko@gmail.com>
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

package oap.application;

import oap.io.Resources;
import oap.testng.AbstractTest;
import oap.util.Lists;
import oap.util.Maps;
import oap.util.Try;
import org.testng.annotations.Test;

import java.util.ArrayList;

import static oap.util.Pair.__;
import static org.testng.Assert.assertEquals;

public class ModuleTest extends AbstractTest {
    @Test
    public void parse() {
        System.setProperty( "number", "222" );
        assertEquals( Resources.
                filePath( ModuleTest.class, "oap-module.json" ).
                map( Try.map( Module::parse ) )
                .orElse( null ),
            new Module( "oap-test",
                Lists.of( "dep1", "dep2" ),
                Lists.of( new Module.Service(
                    "test-service",
                    "oap.application.TestService",
                    Maps.of(
                        __( "port", 8080l ),
                        __( "home", System.getenv( "HOME" ) ),
                        __( "os", System.getProperty( "os.name" ) ),
                        __( "number", 222l )
                    ),
                    new Module.Supervision( true ),
                    new ArrayList<>()
                ) ) ) );
    }
}
