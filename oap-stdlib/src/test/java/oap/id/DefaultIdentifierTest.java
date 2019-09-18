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

package oap.id;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import oap.util.Functions;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static oap.testng.Asserts.assertString;
import static oap.util.Strings.FriendlyIdOption.FILL;
import static oap.util.Strings.FriendlyIdOption.NO_VOWELS;


public class DefaultIdentifierTest {

    @Test
    public void forPath() {
        var identifier = Identifier.<Bean>forPath( "s" ).build();
        assertString( identifier.get( new Bean( "1", "aaaa" ) ) ).isEqualTo( "aaaa" );
        assertString( identifier.get( new Bean( "2", "bbbb" ) ) ).isEqualTo( "bbbb" );
    }

    @Test
    public void forId() {
        var identifier = Identifier.<Bean>forId( b -> b.id ).build();
        assertString( identifier.get( new Bean( "1", "aaaa" ) ) ).isEqualTo( "1" );
        assertString( identifier.get( new Bean( "2", "bbbb" ) ) ).isEqualTo( "2" );
    }

    @Test
    public void forIdWithSetter() {
        var identifier = Identifier.<Bean>forId( b -> b.id, ( o, id ) -> o.id = id )
            .suggestion( o -> o.s )
            .options()
            .build();
        var a = new Bean( "aaaa" );
        assertString( identifier.getOrInit( a, Functions.empty.reject() ) ).isEqualTo( "AAAA" );
        assertString( a.id ).isEqualTo( "AAAA" );
    }

    @Test
    public void idAndSizeGeneration() {
        var identifier = Identifier.<Bean>forPath( "id" )
            .suggestion( bean -> bean.s )
            .length( 7 )
            .options( NO_VOWELS, FILL )
            .build();
        var a = new Bean( null, "some text" );
        var b = new Bean( null, "another text" );

        assertString( identifier.getOrInit( a, Functions.empty.reject() ) ).isEqualTo( "SMTXTXX" );
        assertString( a.id ).isEqualTo( "SMTXTXX" );

        assertString( identifier.getOrInit( b, Functions.empty.reject() ) ).isEqualTo( "NTHRTXT" );

    }

    @Test
    public void conflictResolution() {
        Identifier<Bean> identifier = Identifier.<Bean>forPath( "id" )
            .suggestion( bean -> bean.s )
            .length( 7 )
            .options( NO_VOWELS, FILL )
            .build();

        String[] results = { "SMTXTXX", "SMTXTX0", "SMTXTX1", "SMTXTX2", "SMTXTX3", "SMTXTX4", "SMTXTX5" };
        List<String> list = new ArrayList<>();
        for( int i = 0; i < 7; i++ ) {
            var value = identifier.getOrInit( new Bean( "some text" ), list::contains );
            list.add( value );
            assertString( value ).isEqualTo( results[i] );
        }

    }

    @ToString
    @EqualsAndHashCode
    static class Bean {

        public String id;
        public String s;

        public Bean( String id, String s ) {
            this.id = id;
            this.s = s;
        }

        public Bean( String s ) {
            this( null, s );
        }

    }

}
