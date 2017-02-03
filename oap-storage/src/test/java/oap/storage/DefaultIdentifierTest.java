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

import oap.json.TypeIdFactory;
import oap.testng.AbstractTest;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static oap.testng.Env.tmpPath;
import static org.assertj.core.api.Assertions.assertThat;

public class DefaultIdentifierTest extends AbstractTest {

    @BeforeMethod
    @Override
    public void beforeMethod() throws Exception {
        super.beforeMethod();
        TypeIdFactory.register( Bean.class, Bean.class.getName() );
    }

    @Test
    public void testShouldStoreObjectsWithIdFromPath() {
        try( FileStorage<Bean> storage = new FileStorage<>( tmpPath( "data" ),
            IdentifierBuilder.identityPath( "s" ).build(), 50 )
        ) {
            storage.store( new Bean( "1", "aaaa" ) );
            storage.store( new Bean( "2", "bbbb") );
        }

        try( FileStorage<Bean> storage2 = new FileStorage<>( tmpPath( "data" ),
            IdentifierBuilder.identityPath( "s" ).build() )
        ) {
            assertThat( storage2.select() )
                .containsExactly( new Bean( "1", "aaaa" ), new Bean( "2", "bbbb" ) );
        }
    }

    @Test
    public void testShouldStoreObjectsWithIdAndSizeGeneration() {
        try( FileStorage<Bean> storage = new FileStorage<>( tmpPath( "data" ),
            IdentifierBuilder.<Bean>identityPath( "id" ).suggestion( bean -> bean.s ).size( 7 ).build(), 50 )
        ) {
            final Bean beanA = new Bean( null, "some text" );
            final Bean beanB = new Bean( null, "another text" );

            storage.store( beanA );
            storage.store( beanB );

            assertThat( beanA ).extracting( "id" ).hasSize( 1 ).contains( "SMTXTXXX" );
            assertThat( beanB ).extracting( "id" ).hasSize( 1 ).contains( "NTHRTXTX" );
        }

        try( FileStorage<Bean> storage2 = new FileStorage<>( tmpPath( "data" ),
            IdentifierBuilder.<Bean>identityPath( "id" ).suggestion( bean -> bean.s ).size( 7 ).build() )
        ) {
            assertThat( storage2.select() )
                .containsExactly( new Bean( "NTHRTXTX", "another text" ), new Bean( "SMTXTXXX", "some text" ) );
        }
    }

    @Test
    public void testShouldStoreObjectsWithIdConflictResolutionWhileGenerating() {
        try( FileStorage<Bean> storage = new FileStorage<>( tmpPath( "data" ),
            IdentifierBuilder.<Bean>identityPath( "id" ).suggestion( bean -> bean.s ).size( 7 ).build(), 50 )
        ) {
            final Bean beanA = new Bean( null, "some text" );
            final Bean beanB = new Bean( null, "some text" );
            final Bean beanC = new Bean( null, "some text" );
            final Bean beanD = new Bean( null, "some text" );
            final Bean beanE = new Bean( null, "some text" );
            final Bean beanF = new Bean( null, "some text" );
            final Bean beanG = new Bean( null, "some text" );

            storage.store( beanA );
            storage.store( beanB );
            storage.store( beanC );
            storage.store( beanD );
            storage.store( beanE );
            storage.store( beanF );
            storage.store( beanG );

            assertThat( beanA ).extracting( "id" ).containsExactly( "SMTXTXXX" );
            assertThat( beanB ).extracting( "id" ).containsExactly( "SMTXTXX0" );
            assertThat( beanC ).extracting( "id" ).containsExactly( "SMTXTXX1" );
            assertThat( beanD ).extracting( "id" ).containsExactly( "SMTXTXX2" );
            assertThat( beanE ).extracting( "id" ).containsExactly( "SMTXTXX3" );
            assertThat( beanF ).extracting( "id" ).containsExactly( "SMTXTXX4" );
            assertThat( beanG ).extracting( "id" ).containsExactly( "SMTXTXX5" );
        }

        try( FileStorage<Bean> storage2 = new FileStorage<>( tmpPath( "data" ),
            IdentifierBuilder.<Bean>identityPath( "id" ).suggestion( bean -> bean.s ).size( 7 ).build() )
        ) {
            storage2.select().forEach( System.out::println );
            assertThat( storage2.select() )
                .containsExactly(
                    new Bean( "SMTXTXX0", "some text" ),
                    new Bean( "SMTXTXX1", "some text" ),
                    new Bean( "SMTXTXX2", "some text" ),
                    new Bean( "SMTXTXX3", "some text" ),
                    new Bean( "SMTXTXX4", "some text" ),
                    new Bean( "SMTXTXX5", "some text" ),
                    new Bean( "SMTXTXXX", "some text" )
                );
        }
    }

}