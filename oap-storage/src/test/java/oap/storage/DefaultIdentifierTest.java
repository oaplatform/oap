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

package oap.storage;

import org.testng.annotations.Test;

import static oap.storage.Storage.LockStrategy.Lock;
import static oap.storage.Storage.LockStrategy.NoLock;
import static oap.testng.Asserts.assertString;
import static org.assertj.core.api.Assertions.assertThat;

public class DefaultIdentifierTest {

    @Test
    public void idFromPath() {
        MemoryStorage<Bean> storage = new MemoryStorage<>( IdentifierBuilder.identityPath( "s" ).build(), Lock );
        storage.store( new Bean( "1", "aaaa" ) );
        storage.store( new Bean( "2", "bbbb" ) );
        assertThat( storage.get( "aaaa" ) )
            .isPresent()
            .hasValue( new Bean( "1", "aaaa" ) );
        assertThat( storage.get( "bbbb" ) )
            .isPresent()
            .hasValue( new Bean( "2", "bbbb" ) );
    }

    @Test
    public void idAndSizeGeneration() {
        Identifier<Bean> identifier = IdentifierBuilder.<Bean>identityPath( "id" )
            .suggestion( bean -> bean.s )
            .size( 7 )
            .build();
        MemoryStorage<Bean> storage = new MemoryStorage<>( identifier, Lock );
        Bean a = new Bean( null, "some text" );
        Bean b = new Bean( null, "another text" );

        storage.store( a );
        storage.store( b );

        assertString( a.id ).isEqualTo( "SMTXTXX" );
        assertString( b.id ).isEqualTo( "NTHRTXT" );

    }

    @Test
    public void conflictResolution() {
        Identifier<Bean> identifier = IdentifierBuilder.<Bean>identityPath( "id" )
            .suggestion( bean -> bean.s )
            .size( 7 )
            .build();
        MemoryStorage<Bean> storage = new MemoryStorage<>( identifier, NoLock );
        Bean a = new Bean( null, "some text" );
        Bean b = new Bean( null, "some text" );
        Bean c = new Bean( null, "some text" );
        Bean d = new Bean( null, "some text" );
        Bean e = new Bean( null, "some text" );
        Bean f = new Bean( null, "some text" );
        Bean g = new Bean( null, "some text" );

        storage.store( a );
        storage.store( b );
        storage.store( c );
        storage.store( d );
        storage.store( e );
        storage.store( f );
        storage.store( g );

        assertString( a.id ).isEqualTo( "SMTXTXX" );
        assertString( b.id ).isEqualTo( "SMTXTX0" );
        assertString( c.id ).isEqualTo( "SMTXTX1" );
        assertString( d.id ).isEqualTo( "SMTXTX2" );
        assertString( e.id ).isEqualTo( "SMTXTX3" );
        assertString( f.id ).isEqualTo( "SMTXTX4" );
        assertString( g.id ).isEqualTo( "SMTXTX5" );
    }

}