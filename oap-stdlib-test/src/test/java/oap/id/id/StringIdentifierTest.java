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

package oap.id.id;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import oap.id.Identifier;
import oap.util.function.Functions;
import org.testng.annotations.DataProvider;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static oap.id.Identifier.Option.COMPACT;
import static oap.id.Identifier.Option.FILL;
import static org.assertj.core.api.Assertions.assertThat;


public class StringIdentifierTest {

    @Test
    public void forPath() {
        var identifier = Identifier.<Bean>forPath( "s" ).build();
        assertThat( identifier.get( new Bean( "1", "aaaa" ) ) ).isEqualTo( "aaaa" );
        assertThat( identifier.get( new Bean( "2", "bbbb" ) ) ).isEqualTo( "bbbb" );
    }

    @Test
    public void forId() {
        var identifier = Identifier.<Bean>forId( b -> b.id ).build();
        assertThat( identifier.get( new Bean( "1", "aaaa" ) ) ).isEqualTo( "1" );
        assertThat( identifier.get( new Bean( "2", "bbbb" ) ) ).isEqualTo( "2" );
    }

    @Test
    public void emptySuggestions() {
        var identifier = Identifier.<Bean>forId( b -> b.id, ( o, id ) -> o.id = id )
            .suggestion( o -> o.s )
            .options( COMPACT )
            .build();
        var a = new Bean( "aaaa" );
        assertThat( identifier.getOrInit( a, "U"::equals ) ).isEqualTo( "U0" );
        assertThat( a.id ).isEqualTo( "U0" );
    }

    @Test
    public void tooShort() {
        var identifier = Identifier.<Bean>forId( b -> b.id, ( o, id ) -> o.id = id )
            .suggestion( o -> o.s )
            .options( COMPACT )
            .build();
        var a = new Bean( "X" );
        assertThat( identifier.getOrInit( a, id -> Integer.parseInt( id, 36 ) < 36 ) ).isEqualTo( "X0" );
        assertThat( a.id ).isEqualTo( "X0" );
    }

    @Test
    public void forIdWithSetter() {
        var identifier = Identifier.<Bean>forId( b -> b.id, ( o, id ) -> o.id = id )
            .suggestion( o -> o.s )
            .options()
            .build();
        var a = new Bean( "aaaa" );
        assertThat( identifier.getOrInit( a, Functions.empty.reject() ) ).isEqualTo( "AAAA" );
        assertThat( a.id ).isEqualTo( "AAAA" );
    }

    @Test
    public void idAndSizeGeneration() {
        var identifier = Identifier.<Bean>forPath( "id" )
            .suggestion( bean -> bean.s )
            .length( 7 )
            .options( COMPACT, FILL )
            .build();
        var a = new Bean( null, "some text" );
        var b = new Bean( null, "another text" );

        assertThat( identifier.getOrInit( a, Functions.empty.reject() ) ).isEqualTo( "SMTXTXX" );
        assertThat( a.id ).isEqualTo( "SMTXTXX" );

        assertThat( identifier.getOrInit( b, Functions.empty.reject() ) ).isEqualTo( "NTHRTXT" );
    }

    @DataProvider
    public Object[][] conflicts() {
        return new Object[][] {
            { new Identifier.Option[] { COMPACT, FILL }, new String[] { "SMTXTXX", "SMTXTX0", "SMTXTX1", "SMTXTX2", "SMTXTX3", "SMTXTX4", "SMTXTX5" } },
            { new Identifier.Option[] { COMPACT }, new String[] { "SMTXT", "SMTXT0", "SMTXT1", "SMTXT2", "SMTXT3", "SMTXT4", "SMTXT5" } }
        };
    }

    @Test( dataProvider = "conflicts" )
    public void conflictResolution( Identifier.Option[] options, String[] results ) {
        var identifier = Identifier.<Bean>forPath( "id" )
            .suggestion( bean -> bean.s )
            .length( 7 )
            .options( options )
            .build();

        List<String> list = new ArrayList<>();
        for( int i = 0; i < 7; i++ ) {
            var value = identifier.getOrInit( new Bean( "some text" ), list::contains );
            list.add( value );
            assertThat( value ).isEqualTo( results[i] );
        }
        assertThat( list ).containsOnly( results );
    }

    @ToString
    @EqualsAndHashCode
    public static class Bean {

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
