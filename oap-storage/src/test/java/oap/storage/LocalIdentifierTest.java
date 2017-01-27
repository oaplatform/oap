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

public class LocalIdentifierTest extends AbstractTest {

    @BeforeMethod
    @Override
    public void beforeMethod() throws Exception {
        super.beforeMethod();
        TypeIdFactory.register( Bean.class, Bean.class.getName() );
    }

    @Test
    public void testShouldStoreObjectsWithIdFromPath() {
        try( FileStorage<Bean> storage = new FileStorage<>( tmpPath( "data" ),
            IdentifierBuilder.identityPath( "s" ), 50 )
        ) {
            storage.store( new Bean( "1", "aaaa" ) );
            storage.store( new Bean( "2", "bbbb") );
        }

        try( FileStorage<Bean> storage2 = new FileStorage<>( tmpPath( "data" ),
            IdentifierBuilder.identityPath( "s" ) )
        ) {
            assertThat( storage2.select() )
                .containsExactly( new Bean( "1", "aaaa" ), new Bean( "2", "bbbb" ) );
        }
    }

    @Test
    public void testShouldStoreObjectsWithIdAndSizeGeneration() {
        try( FileStorage<Bean> storage = new FileStorage<>( tmpPath( "data" ),
            IdentifierBuilder.<Bean>identityPath( "id" ).suggestion( bean -> bean.s ).size( 7 ), 50 )
        ) {
            final Bean beanA = new Bean( null, "some text" );
            final Bean beanB = new Bean( null, "another text" );

            storage.store( beanA );
            storage.store( beanB );

            assertThat( beanA ).extracting( "id" ).hasSize( 1 ).contains( "smtxtXXX" );
            assertThat( beanB ).extracting( "id" ).hasSize( 1 ).contains( "nthrtxtX" );
        }

        try( FileStorage<Bean> storage2 = new FileStorage<>( tmpPath( "data" ),
            IdentifierBuilder.<Bean>identityPath( "id" ).suggestion( bean -> bean.s ).size( 7 ) )
        ) {
            assertThat( storage2.select() )
                .containsExactly( new Bean( "nthrtxtX", "another text" ), new Bean( "smtxtXXX", "some text" ) );
        }
    }

    @Test
    public void testShouldStoreObjectsWithIdConflictResolutionWhileGenerating() {
        try( FileStorage<Bean> storage = new FileStorage<>( tmpPath( "data" ),
            IdentifierBuilder.<Bean>identityPath( "id" ).suggestion( bean -> bean.s ).size( 7 ), 50 )
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

            assertThat( beanA ).extracting( "id" ).containsExactly( "smtxtXXX" );
            assertThat( beanB ).extracting( "id" ).containsExactly( "smtxtXX0" );
            assertThat( beanC ).extracting( "id" ).containsExactly( "smtxtXX1" );
            assertThat( beanD ).extracting( "id" ).containsExactly( "smtxtXX2" );
            assertThat( beanE ).extracting( "id" ).containsExactly( "smtxtXX3" );
            assertThat( beanF ).extracting( "id" ).containsExactly( "smtxtXX4" );
            assertThat( beanG ).extracting( "id" ).containsExactly( "smtxtXX5" );
        }

        try( FileStorage<Bean> storage2 = new FileStorage<>( tmpPath( "data" ),
            IdentifierBuilder.<Bean>identityPath( "id" ).suggestion( bean -> bean.s ).size( 7 ) )
        ) {
            assertThat( storage2.select() )
                .containsExactly(
                    new Bean( "smtxtXXX", "some text" ),
                    new Bean( "smtxtXX1", "some text" ),
                    new Bean( "smtxtXX0", "some text" ),
                    new Bean( "smtxtXX3", "some text" ),
                    new Bean( "smtxtXX2", "some text" ),
                    new Bean( "smtxtXX5", "some text" ),
                    new Bean( "smtxtXX4", "some text" )
                );
        }
    }

}