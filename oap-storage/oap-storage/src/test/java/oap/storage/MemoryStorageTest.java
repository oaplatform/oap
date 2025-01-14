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

import lombok.EqualsAndHashCode;
import lombok.ToString;
import oap.id.Identifier;
import oap.id.IntIdentifier;
import oap.testng.Fixtures;
import oap.testng.SystemTimerFixture;
import oap.util.Dates;
import org.joda.time.DateTime;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static oap.storage.Storage.Lock.SERIALIZED;
import static org.assertj.core.api.Assertions.assertThat;
import static org.joda.time.DateTimeZone.UTC;

@Test
public class MemoryStorageTest extends Fixtures {
    public MemoryStorageTest() {
        fixtures( new SystemTimerFixture() );
    }

    @Test
    public void update() {
        MemoryStorage<String, Bean> storage = new MemoryStorage<>(
            Identifier.<Bean>forId( b -> b.id, ( b, id ) -> b.id = id )
                .suggestion( b -> b.s )
                .build(),
            SERIALIZED );
        List<String> ids = new ArrayList<>();
        storage.addDataListener( new Storage.DataListener<>() {
            @Override
            public void added( List<IdObject<String, Bean>> objects ) {
                objects.forEach( io -> ids.add( io.id ) );
            }
        } );
        Bean noId = new Bean();
        storage.update( noId.id, b -> noId, () -> noId );
        assertThat( storage.list() ).containsOnly( noId );
        assertThat( noId.id ).isNotNull();
        assertThat( ids ).containsOnly( noId.id );
    }

    @Test
    public void testCreatedModofied() {
        DateTime created = new DateTime( 2024, 1, 14, 15, 13, 14, UTC );

        Dates.setTimeFixed( created.getMillis() );

        MemoryStorage<String, Bean> storage = new MemoryStorage<>(
            Identifier.<Bean>forId( b -> b.id, ( b, id ) -> b.id = id )
                .suggestion( b -> b.s )
                .build(),
            SERIALIZED );

        storage.store( new Bean( "id1" ) );

        Metadata<Bean> metadata = storage.getMetadata( "id1" ).orElseThrow();
        assertThat( new DateTime( metadata.created, UTC ) ).isEqualTo( created );
        assertThat( new DateTime( metadata.modified, UTC ) ).isEqualTo( created );

        Dates.incFixed( Dates.m( 3 ) );
        storage.store( new Bean( "id1", "v" ) );

        metadata = storage.getMetadata( "id1" ).orElseThrow();
        assertThat( new DateTime( metadata.created, UTC ) ).isEqualTo( created );
        assertThat( new DateTime( metadata.modified, UTC ) ).isEqualTo( created.plusMinutes( 3 ) );
    }

    @Test
    public void updateWithId() {
        MemoryStorage<String, Bean> storage = new MemoryStorage<>(
            Identifier.<Bean>forId( b -> b.id, ( b, id ) -> b.id = id )
                .suggestion( b -> b.s )
                .build(),
            SERIALIZED );
        List<String> ids = new ArrayList<>();
        storage.addDataListener( new Storage.DataListener<>() {
            @Override
            public void added( List<IdObject<String, Bean>> objects ) {
                objects.forEach( io -> ids.add( io.id ) );
            }
        } );
        Bean id = new Bean( "id" );
        storage.update( id.id, b -> id, () -> id );
        assertThat( storage.list() ).containsOnly( id );
        assertThat( id.id ).isNotNull();
        assertThat( ids ).containsOnly( id.id );
    }

    @Test
    public void get() {
        MemoryStorage<String, Bean> storage = new MemoryStorage<>(
            Identifier.<Bean>forId( b -> b.id, ( b, id ) -> b.id = id )
                .suggestion( b -> b.s )
                .build(),
            SERIALIZED );
        Bean bean = new Bean( "id" );
        assertThat( storage.get( bean.id, () -> bean ) ).isEqualTo( bean );
        assertThat( storage.list() ).containsOnly( bean );
        Bean beanNoId = new Bean();
        assertThat( storage.get( beanNoId.id, () -> beanNoId ) ).isEqualTo( beanNoId );
        assertThat( storage.list() ).containsOnly( bean, beanNoId );
    }

    @Test
    public void intId() {
        MemoryStorage<Integer, IntBean> storage = new MemoryStorage<>( IntIdentifier.<IntBean>forId( b -> b.id, ( b, id ) -> b.id = id ).build(), SERIALIZED );
        var a = new IntBean( null, "a" );
        var b = new IntBean( 2, "b" );
        var c = new IntBean( null, "c" );
        storage.store( a );
        storage.store( b );
        storage.store( c );
        assertThat( storage.list() ).containsOnly(
            new IntBean( 1, "a" ),
            new IntBean( 2, "b" ),
            new IntBean( 3, "c" )
        );
    }

    @EqualsAndHashCode
    @ToString
    static class IntBean {
        Integer id;
        String name;

        IntBean( Integer id, String name ) {
            this.id = id;
            this.name = name;
        }
    }
}
