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

package oap.storagev2;

import oap.id.Identifier;
import org.testng.annotations.Test;

import static oap.id.Identifier.Option.COMPACT;
import static oap.id.Identifier.Option.FILL;
import static oap.storagev2.Storage.Lock.CONCURRENT;
import static oap.storagev2.Storage.Lock.SERIALIZED;
import static org.assertj.core.api.Assertions.assertThat;

public class DefaultIdentifierTest {

    @Test
    public void forPath() {
        MemoryStorage<String, Bean> storage = new MemoryStorage<>( Identifier.<Bean>forPath( "s" ).build(), SERIALIZED );
        storage.store( new Bean( "1", "aaaa" ), Storage.MODIFIED_BY_SYSTEM );
        storage.store( new Bean( "2", "bbbb" ), Storage.MODIFIED_BY_SYSTEM );
        assertThat( storage.get( "aaaa" ) )
            .isPresent()
            .hasValue( new Bean( "1", "aaaa" ) );
        assertThat( storage.get( "bbbb" ) )
            .isPresent()
            .hasValue( new Bean( "2", "bbbb" ) );
    }

    @Test
    public void forId() {
        MemoryStorage<String, Bean> storage = new MemoryStorage<>( Identifier.<Bean>forId( b -> b.id ).build(), SERIALIZED );
        storage.store( new Bean( "1", "aaaa" ), Storage.MODIFIED_BY_SYSTEM );
        storage.store( new Bean( "2", "bbbb" ), Storage.MODIFIED_BY_SYSTEM );
        assertThat( storage.get( "1" ) )
            .isPresent()
            .hasValue( new Bean( "1", "aaaa" ) );
        assertThat( storage.get( "2" ) )
            .isPresent()
            .hasValue( new Bean( "2", "bbbb" ) );
    }

    @Test
    public void forIdWithSetter() {
        MemoryStorage<String, Bean> storage = new MemoryStorage<>( Identifier.<Bean>forId( b -> b.id, ( o, id ) -> o.id = id )
            .suggestion( o -> o.s )
            .build(), SERIALIZED );
        storage.store( new Bean( "1", "aaaa" ), Storage.MODIFIED_BY_SYSTEM );
        storage.store( new Bean( "2", "bbbb" ), Storage.MODIFIED_BY_SYSTEM );
        assertThat( storage.get( "1" ) )
            .isPresent()
            .hasValue( new Bean( "1", "aaaa" ) );
        assertThat( storage.get( "2" ) )
            .isPresent()
            .hasValue( new Bean( "2", "bbbb" ) );
    }

    @Test
    public void idAndSizeGeneration() {
        var identifier = Identifier.<Bean>forPath( "id" )
            .suggestion( bean -> bean.s )
            .length( 7 )
            .options( Identifier.Option.COMPACT, Identifier.Option.FILL )
            .build();
        var storage = new MemoryStorage<>( identifier, SERIALIZED );
        var a = new Bean( null, "some text" );
        var b = new Bean( null, "another text" );

        storage.store( a, Storage.MODIFIED_BY_SYSTEM );
        storage.store( b, Storage.MODIFIED_BY_SYSTEM );

        assertThat( a.id ).isEqualTo( "SMTXTXX" );
        assertThat( b.id ).isEqualTo( "NTHRTXT" );

    }

    @Test
    public void conflictResolution() {
        var identifier = Identifier.<Bean>forPath( "id" )
            .suggestion( bean -> bean.s )
            .length( 7 )
            .options( COMPACT, FILL )
            .build();
        var storage = new MemoryStorage<>( identifier, CONCURRENT );
        var a = new Bean( null, "some text" );
        var b = new Bean( null, "some text" );
        var c = new Bean( null, "some text" );
        var d = new Bean( null, "some text" );
        var e = new Bean( null, "some text" );
        var f = new Bean( null, "some text" );
        var g = new Bean( null, "some text" );

        storage.store( a, Storage.MODIFIED_BY_SYSTEM );
        storage.store( b, Storage.MODIFIED_BY_SYSTEM );
        storage.store( c, Storage.MODIFIED_BY_SYSTEM );
        storage.store( d, Storage.MODIFIED_BY_SYSTEM );
        storage.store( e, Storage.MODIFIED_BY_SYSTEM );
        storage.store( f, Storage.MODIFIED_BY_SYSTEM );
        storage.store( g, Storage.MODIFIED_BY_SYSTEM );

        assertThat( a.id ).isEqualTo( "SMTXTXX" );
        assertThat( b.id ).isEqualTo( "SMTXTX0" );
        assertThat( c.id ).isEqualTo( "SMTXTX1" );
        assertThat( d.id ).isEqualTo( "SMTXTX2" );
        assertThat( e.id ).isEqualTo( "SMTXTX3" );
        assertThat( f.id ).isEqualTo( "SMTXTX4" );
        assertThat( g.id ).isEqualTo( "SMTXTX5" );
    }

}
