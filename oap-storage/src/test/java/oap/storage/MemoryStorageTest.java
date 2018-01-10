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

import com.fasterxml.jackson.annotation.JsonCreator;
import lombok.ToString;
import lombok.val;
import oap.util.Id;
import org.assertj.core.api.Assertions;
import org.testng.annotations.Test;

import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static oap.storage.Storage.LockStrategy.NoLock;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by igor.petrenko on 05.01.2018.
 */
public class MemoryStorageTest {
    @Test
    public void testConstraintStoreAdd() {
        try( val storage = new MemoryStorage<TestClass>( IdentifierBuilder.annotationBuild(), NoLock ) ) {
            storage.addConstraint( new UniqueField<>( "objtype", tc -> tc.name ) );

            storage.store( new TestClass( "id1", "name1" ) );
            storage.store( new TestClass( "id2", "name2" ) );

            Assertions.assertThatThrownBy( () -> storage.store( new TestClass( "id3", "name1" ) ) )
                .hasMessage( "Objtype 'name1' already exists." );
        }
    }

    @Test
    public void testConstraintStoreAddList() {
        try( val storage = new MemoryStorage<TestClass>( IdentifierBuilder.annotationBuild(), NoLock ) ) {
            storage.addConstraint( new UniqueField<>( "objtype", tc -> tc.names ) );

            storage.store( new TestClass( "id1", "name1", asList("1", "2") ) );
            storage.store( new TestClass( "id2", "name2", asList("3", "4") ) );

            Assertions.assertThatThrownBy( () -> storage.store( new TestClass( "id3", "name1", asList( "2", "5" ) ) ) )
                .hasMessage( "Objtype '[2, 5]' already exists." );
        }
    }

    @Test
    public void testConstraintStoreAddFilter() {
        try( val storage = new MemoryStorage<TestClass>( IdentifierBuilder.annotationBuild(), NoLock ) ) {
            storage.addConstraint( new UniqueField<>( "objtype", tc -> tc.name, ( n, o ) -> false ) );

            storage.store( new TestClass( "id1", "name1" ) );
            storage.store( new TestClass( "id1", "name1" ) );
            storage.store( new TestClass( "id2", "name2" ) );
            storage.store( new TestClass( "id2", "name2" ) );
        }
    }

    @Test
    public void testConstraintStoreUpdate() {
        try( val storage = new MemoryStorage<TestClass>( IdentifierBuilder.annotationBuild(), NoLock ) ) {
            storage.addConstraint( new UniqueField<>( "objtype", tc -> tc.name ) );

            storage.store( new TestClass( "id1", "name1" ) );
            storage.store( new TestClass( "id2", "name2" ) );

            storage.store( new TestClass( "id2", "name2" ) );

            Assertions.assertThatThrownBy( () -> storage.store( new TestClass( "id2", "name1" ) ) )
                .hasMessage( "Objtype 'name1' already exists." );
        }
    }

    @Test
    public void testConstraintUpdate() {
        try( val storage = new MemoryStorage<TestClass>( IdentifierBuilder.annotationBuild(), NoLock ) ) {
            storage.addConstraint( new UniqueField<>( "objtype", tc -> tc.name ) );

            storage.store( new TestClass( "id1", "name1" ) );
            storage.store( new TestClass( "id2", "name2" ) );

            storage.update( "id2", new TestClass( "id2", "name2" ) );

            Assertions.assertThatThrownBy( () -> storage.update( "id2", new TestClass( "id2", "name1" ) ) )
                .hasMessage( "Objtype 'name1' already exists." );
        }
    }

    @Test
    public void testConstraintUpdateObject() {
        try( val storage = new MemoryStorage<TestClass>( IdentifierBuilder.annotationBuild(), NoLock ) ) {
            storage.addConstraint( new UniqueField<>( "objtype", tc -> tc.name ) );

            storage.updateObject( "id1", ( a ) -> true, ( tc ) -> {
                tc.name = "name1";
                return tc;
            }, () -> new TestClass( "id1", "name1" ) );
            storage.updateObject( "id2", ( a ) -> true, ( tc ) -> {
                tc.name = "name2";
                return tc;
            }, () -> new TestClass( "id2", "name2" ) );

            Assertions.assertThatThrownBy( () -> storage.updateObject( "id3", ( a ) -> true, ( tc ) -> {
                tc.name = "name2";
                return tc;
            }, () -> new TestClass( "id3", "name2" ) ) )
                .hasMessage( "Objtype 'name2' already exists." );


            storage.updateObject( "id2", ( a ) -> true, ( tc ) -> {
                tc.name = "name2";
                return tc;
            }, () -> new TestClass( "id2", "name2" ) );

            Assertions.assertThatThrownBy( () -> storage.updateObject( "id2", ( a ) -> true, ( tc ) -> {
                tc.name = "name1";
                return tc;
            }, () -> new TestClass( "id2", "name1" ) ) )
                .hasMessage( "Objtype 'name1' already exists." );

            assertThat( storage.get( "id2" ).get().name ).isEqualTo( "name2" );
        }
    }

    @ToString
    public static class TestClass {
        @Id
        public String id;
        public String name;
        public List<String> names;

        @JsonCreator
        public TestClass( String id, String name, List<String> names ) {
            this.id = id;
            this.name = name;
            this.names = names;
        }

        public TestClass( String id, String name ) {
            this( id, name, emptyList() );
        }
    }
}