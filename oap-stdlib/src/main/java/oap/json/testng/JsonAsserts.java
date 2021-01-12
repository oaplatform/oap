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
import oap.util.Pair;
import oap.util.Strings;
import org.assertj.core.api.AbstractAssert;
import org.testng.Assert;

import java.util.List;
import java.util.Map;
import java.util.function.Function;

import static oap.io.content.ContentReader.ofString;
import static oap.testng.Asserts.assertString;
import static oap.testng.Asserts.contentOfTestResource;
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
        assertEquals( Binder.json.canonicalize( clazz, actual ),
            readCanonical( context, clazz, expectedResourcePath, substitutions ) );
    }

    @Deprecated
    public static void assertEqualsCanonical( Class<?> clazz, String actual, String expected ) {
        assertEquals( Binder.json.canonicalize( clazz, actual ), Binder.json.canonicalize( clazz, expected ) );
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
        return Binder.json.unmarshalResource( context, clazz, resourcePath );

    }

    public static <T> T objectOfTestJsonResource( Class<?> context, Class<T> clazz, String resourcePath ) {
        return Binder.json.unmarshal( clazz, contentOfTestResource( context, resourcePath, ofString() ) );
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

        public JsonAssertion isEqualTo( String expected ) {
            isNotNull();
            assertString( Formatter.format( actual ) )
                .isEqualTo( Formatter.format( expected ) );
            return this;

        }

        @Override
        public JsonAssertion isEqualTo( Object expected ) {
            return isEqualTo( String.valueOf( expected ) );
        }

        public JsonAssertion isStructurallyEqualTo( String expected ) {
            isNotNull();
            assertThat( unmarshal( actual ) )
                .isEqualTo( unmarshal( expected ) );
            return this;
        }

        public JsonAssertion isStructurallyEqualToResource( Class<?> contextClass, String resource ) {
            isNotNull();
            return isStructurallyEqualTo( contentOfTestResource( contextClass, resource, ofString() ) );

        }

        private static Object unmarshal( String content ) {
            if( content.trim().startsWith( "[" ) ) return Binder.json.unmarshal( List.class, content );
            else return Binder.json.unmarshal( Map.class, content );
        }
        private JsonAssertion isEqualCanonically( Class<?> clazz, String actual, String expected ) {
            assertString( Binder.json.canonicalizeWithDefaultPrettyPrinter( clazz, actual ) )
                .isEqualTo( Binder.json.canonicalizeWithDefaultPrettyPrinter( clazz, expected ) );
            return this;
        }

        public JsonAssertion isEqualCanonically( Class<?> clazz, String expected ) {
            return isEqualCanonically( clazz, this.actual, expected );
        }

        @SafeVarargs
        public final JsonAssertion isEqualCanonically( Class<?> clazz, String expected, Pair<String, Object>... substitutions ) {
            return isEqualCanonically( clazz, Strings.substitute( expected, substitutions ) );
        }

        public final JsonAssertion isEqualCanonically( Class<?> clazz, String expected, Function<String, String> substitutions ) {
            return isEqualCanonically( clazz, substitutions.apply( actual ), expected );
        }
    }
}
