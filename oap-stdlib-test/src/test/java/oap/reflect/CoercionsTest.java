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

package oap.reflect;

import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.ToString;
import oap.system.Env;
import oap.testng.Fixtures;
import oap.testng.TestDirectoryFixture;
import oap.util.BitSet;
import oap.util.LinkedHashMaps;
import org.testng.annotations.Test;

import java.lang.annotation.RetentionPolicy;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

public class CoercionsTest extends Fixtures {
    private final TestDirectoryFixture testDirectoryFixture;

    public CoercionsTest() {
        testDirectoryFixture = fixture( new TestDirectoryFixture().withDeployTestData( getClass() ) );
    }

    @Test
    public void cast() {
        var coercions = Coercions.basic().withIdentity();
        assertThat( coercions.cast( Reflect.reflect( int.class ), 1L ) ).isEqualTo( 1 );
        assertThat( coercions.cast( Reflect.reflect( int.class ), "1" ) ).isEqualTo( 1 );
        assertThat( coercions.cast( Reflect.reflect( int.class ), "-1" ) ).isEqualTo( -1 );
        assertThat( coercions.cast( Reflect.reflect( Integer.class ), 1L ) ).isEqualTo( 1 );
        assertThat( coercions.cast( Reflect.reflect( Integer.class ), -1L ) ).isEqualTo( -1 );
        assertThat( coercions.cast( Reflect.reflect( boolean.class ), true ) ).isEqualTo( true );
        assertThat( coercions.cast( Reflect.reflect( Boolean.class ), true ) ).isEqualTo( true );
        assertThat( coercions.cast( Reflect.reflect( char.class ), "c" ) ).isEqualTo( 'c' );
        assertThat( coercions.cast( Reflect.reflect( Character.class ), "a" ) ).isEqualTo( 'a' );
        assertThat( coercions.cast( Reflect.reflect( String.class ), "a" ) ).isEqualTo( "a" );
        assertThat( coercions.cast( Reflect.reflect( Path.class ), "/a" ) ).isEqualTo( Paths.get( "/a" ) );
        assertThat( coercions.cast( Reflect.reflect( Class.class ), "java.lang.String" ) ).isEqualTo( String.class );
        assertThat( coercions.cast( Reflect.reflect( RetentionPolicy.class ), "SOURCE" ) ).isEqualTo( RetentionPolicy.SOURCE );

        var expected = new BitSet();
        expected.set( 1, 6 );
        assertThat( coercions.cast( Reflect.reflect( BitSet.class ), "1-5" ) ).isEqualTo( expected );
    }

    @Test
    public void testJsonFunction() {
        var coercions = Coercions.basic().withIdentity();
        assertThat( coercions.cast( Reflect.reflect( new TypeRef<Map<String, TestConfiguration>>() {} ), "json({\"k\":{\"key1\":\"1\",\"key2\":\"2\"}})" ) )
            .isEqualTo( LinkedHashMaps.of( "k", new TestConfiguration( "1", "2" ) ) );
    }

    @Test
    public void testCastOptional() {
        var coercions = Coercions.basic().withIdentity();
        assertThat( coercions.cast( Reflect.reflect( new TypeRef<Optional<String>>() {} ), "va" ) ).isEqualTo( Optional.of( "va" ) );
    }

    @Test
    public void testCastFunctionString() {
        assertThat( Coercions.castFunction( Reflect.reflect( new TypeRef<String>() {} ), "classpath(/oap/reflect/CoercionsTest/test.yaml)" ) ).isEqualTo( "a: b" );
    }

    @Test
    public void testCastFunctionPathWithEnv() {
        Env.set( "TEST_ENV", testDirectoryFixture.testDirectory().toString() );

        assertThat( Coercions.castFunction( Reflect.reflect( new TypeRef<String>() {} ), "path(${ENV.TEST_ENV}/test.yaml)" ) ).isEqualTo( "a: b" );
    }

    @Test
    public void testUrl() throws MalformedURLException {
        var coercions = Coercions.basic().withIdentity();

        assertThat( coercions.cast( Reflect.reflect( URL.class ), "file:///tmp" ) ).isEqualTo( new URL( "file:/tmp" ) );
        assertThat( coercions.cast( Reflect.reflect( URL.class ), "/tmp" ) ).isEqualTo( Paths.get( "/tmp" ).toUri().toURL() );

        assertThat( coercions.cast( Reflect.reflect( URL.class ), "/oap/reflect/CoercionsTest.class" ) )
            .isEqualTo( Coercions.class.getResource( "/oap/reflect/CoercionsTest.class" ) );
        assertThat( coercions.cast( Reflect.reflect( URL.class ), "classpath(/oap/reflect/CoercionsTest.class)" ) )
            .isEqualTo( Coercions.class.getResource( "/oap/reflect/CoercionsTest.class" ) );
    }

    @Test
    public void testCastWithFunctions() {
        var coercions = Coercions.basic().withIdentity();
        assertThat( coercions.cast( Reflect.reflect( new TypeRef<Map<String, TestConfiguration2>>() {} ), Map.of( "k", Map.of( "key1", "str(1)" ) ) ) )
            .isEqualTo( LinkedHashMaps.of( "k", new TestConfiguration2( "1" ) ) );
    }

    @ToString
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class TestConfiguration {
        public final String key1;
        public final String key2;
    }

    @ToString
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class TestConfiguration2 {
        public final String key1;
    }
}
