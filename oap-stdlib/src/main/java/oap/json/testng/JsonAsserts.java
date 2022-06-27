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
package oap.json.testng;

import oap.io.Resources;
import oap.json.Binder;
import oap.json.Formatter;
import oap.reflect.Reflect;
import oap.reflect.TypeRef;
import oap.util.BiStream;
import oap.util.Either;
import oap.util.Maps;
import oap.util.Pair;
import oap.util.Strings;
import org.assertj.core.api.AbstractAssert;
import org.testng.Assert;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static oap.io.content.ContentReader.ofJson;
import static oap.io.content.ContentReader.ofString;
import static oap.json.Binder.json;
import static oap.testng.Asserts.assertString;
import static oap.testng.Asserts.contentOfTestResource;
import static oap.util.Pair.__;
import static org.assertj.core.api.Assertions.assertThat;

public class JsonAsserts {
    @Deprecated
    public static void assertEquals( String actual, String expected ) {
        Assert.assertEquals( Formatter.format( actual ), Formatter.format( expected ) );
    }

    @Deprecated
    @SafeVarargs
    public static void assertEqualsCanonical( Class<?> context, Class<?> clazz,
                                              String actual, String expectedResourcePath,
                                              Pair<String, Object>... substitutions ) {
        assertEquals( json.canonicalize( clazz, actual ),
            readCanonical( context, clazz, expectedResourcePath, substitutions ) );
    }

    @Deprecated
    public static void assertEqualsCanonical( Class<?> clazz, String actual, String expected ) {
        assertEquals( json.canonicalize( clazz, actual ), json.canonicalize( clazz, expected ) );
    }

    @SafeVarargs
    public static String readCanonical( Class<?> context, Class<?> clazz, String resourcePath,
                                        Pair<String, Object>... substitutions ) {
        return Resources.read( context, resourcePath, ofString() )
            .map( json -> Binder.json.canonicalize( clazz, Strings.substitute( json, substitutions ) ) )
            .orElseThrow( () -> new AssertionError( "not found " + resourcePath ) );
    }

    /**
     * @see #objectOfTestJsonResource(Class, Class, String)
     */
    @Deprecated
    public static <T> T readObject( Class<?> context, Class<T> clazz, String resourcePath ) {
        return json.unmarshalResource( context, clazz, resourcePath );

    }

    /**
     * #see {@link oap.testng.Asserts#contentOfTestResource(Class, String, oap.io.content.ContentReader)}
     */
    @Deprecated
    public static <T> T objectOfTestJsonResource( Class<?> context, Class<T> clazz, String resourcePath ) {
        return contentOfTestResource( context, resourcePath, ofJson( clazz ) );
    }

    public static JsonAssertion assertJson( String json ) {
        return new JsonAssertion( json );
    }

    public static JsonAssertion assertJson( Map<String, Object> json ) {
        return new JsonAssertion( Binder.json.marshal( json ) );
    }

    @SuppressWarnings( "UnusedReturnValue" )
    public static class JsonAssertion extends AbstractAssert<JsonAssertion, String> {

        public JsonAssertion( String json ) {
            super( json, JsonAssertion.class );
        }

        private static Either<List<?>, Map<String, ?>> unmarshal( String content ) {
            return content.trim().startsWith( "[" )
                ? Either.left( json.unmarshal( new TypeRef<>() {}, content ) )
                : Either.right( json.unmarshal( new TypeRef<>() {}, content ) );
        }

        public JsonAssertion isEqualTo( String expected ) {
            return isEqualTo( expected, Map.of() );
        }

        public JsonAssertion isEqualTo( String expected, Map<String, Object> substitutions ) {
            isNotNull();
            String actualJson = unmarshal( actual )
                .map( JsonAssertion::deepSort, JsonAssertion::deepSort )
                .map( l -> substitute( l, substitutions ), r -> substitute( r, substitutions ) )
                .map( e -> json.marshal( e.isLeft() ? e.leftValue : e.rightValue ) );
            String expectedJson = unmarshal( expected )
                .map( JsonAssertion::deepSort, JsonAssertion::deepSort )
                .map( e -> json.marshal( e.isLeft() ? e.leftValue : e.rightValue ) );
            assertString( actualJson ).isEqualTo( expectedJson );
            return this;
        }

        public JsonAssertion isEqualTo( Class<?> contextClass, String resource ) {
            return isEqualTo( contentOfTestResource( contextClass, resource, ofString() ) );
        }

        @Override
        public JsonAssertion isEqualTo( Object expected ) {
            return isEqualTo( String.valueOf( expected ) );
        }

        @SuppressWarnings( "unchecked" )
        private static <R> R substitute( Object o, Map<String, Object> substitutions ) {
            substitutions.forEach( ( k, v ) -> Reflect.set( o, k, v, true ) );
            return ( R ) o;
        }

        @SuppressWarnings( "unchecked" )
        private static <R> R deepSort( Object o ) {
            if( o == null ) return null;
            if( o instanceof List<?> list ) return ( R ) list.stream().map( JsonAssertion::deepSort ).sorted().toList();
            if( o instanceof Map<?, ?> map ) return ( R ) BiStream.of( map ).map( ( k, v ) -> __( k, deepSort( v ) ) ).collect( Maps.Collectors.toTreeMap() );
            return ( R ) o;
        }


        /**
         * @see #isEqualTo(Object)
         */
        @Deprecated
        public JsonAssertion isStructurallyEqualTo( String expected ) {
            return isEqualTo( expected );
        }

        /**
         * @see #isEqualTo(Object)
         */
        @Deprecated
        public JsonAssertion isStructurallyEqualToResource( Class<?> contextClass, String resource ) {
            return isEqualTo( contextClass, resource );
        }

        private JsonAssertion isEqualCanonically( Class<?> clazz, String actual, String expected ) {
            assertThat( json.canonicalizeWithDefaultPrettyPrinter( clazz, actual ) )
                .isEqualTo( json.canonicalizeWithDefaultPrettyPrinter( clazz, expected ) );
            return this;
        }

        private JsonAssertion isEqualCanonically( TypeRef<?> typeRef, String actual, String expected ) {
            assertThat( json.canonicalizeWithDefaultPrettyPrinter( typeRef, actual ) )
                .isEqualTo( json.canonicalizeWithDefaultPrettyPrinter( typeRef, expected ) );
            return this;
        }

        public JsonAssertion isEqualCanonically( Class<?> clazz, String expected ) {
            return isEqualCanonically( clazz, this.actual, expected );
        }

        @SafeVarargs
        public final JsonAssertion isEqualCanonically( Class<?> clazz, String expected, Pair<String, Object>... substitutions ) {
            return isEqualCanonically( clazz, Strings.substitute( expected, substitutions ) );
        }

        public JsonAssertion isEqualCanonically( TypeRef<?> typeRef, String expected ) {
            return isEqualCanonically( typeRef, this.actual, expected );
        }

        @SafeVarargs
        public final JsonAssertion isEqualCanonically( TypeRef<?> typeRef, String expected, Pair<String, Object>... substitutions ) {
            return isEqualCanonically( typeRef, Strings.substitute( expected, substitutions ) );
        }

        public final JsonAssertion isEqualCanonically( Class<?> clazz, String expected, Function<String, String> substitutions ) {
            return isEqualCanonically( clazz, substitutions.apply( actual ), expected );
        }

        public final JsonAssertion isEqualCanonically( TypeRef<?> typeRef, String expected, Function<String, String> substitutions ) {
            return isEqualCanonically( typeRef, substitutions.apply( actual ), expected );
        }
    }
}
