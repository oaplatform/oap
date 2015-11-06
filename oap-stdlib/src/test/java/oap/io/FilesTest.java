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
package oap.io;

import oap.testng.AbstractTest;
import oap.testng.Env;
import oap.util.Lists;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;


public class FilesTest extends AbstractTest {
    @Test
    public void wildcard() {
        Files.writeString( Env.tmp( "/wildcard/1.txt" ), "1" );
        assertEquals( Files.wildcard( Env.tmp( "/wildcard" ), "*.txt" ),
            Lists.of( Env.tmpPath( "/wildcard/1.txt" ) ) );
        assertEquals( Files.wildcard( "/aaa", "*.txt" ), Lists.empty() );

        Files.writeString( Env.tmp( "/wildcard/a/1.txt" ), "1" );
        Files.writeString( Env.tmp( "/wildcard/b/1.txt" ), "1" );
        Files.wildcard( Env.tmp( "/wildcard" ), "*/*.txt" ).forEach( System.out::println );
        assertEquals( Files.wildcard( Env.tmp( "/wildcard" ), "*/*.txt" ),
            Lists.of(
                Env.tmpPath( "/wildcard/a/1.txt" ),
                Env.tmpPath( "/wildcard/b/1.txt" )
            ) );
    }

    @Test
    public void path() {
        assertEquals( Files.path( "a", "b/c", "d" ), Files.path( "a", "b", "c", "d" ) );
    }

}
