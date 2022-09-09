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
package oap.testng;

import com.google.common.base.Preconditions;
import lombok.SneakyThrows;
import oap.concurrent.Threads;
import oap.io.Files;
import oap.io.IoStreams;
import oap.io.Resources;
import oap.io.content.ContentReader;
import oap.json.Binder;
import oap.util.Result;
import oap.util.Stream;
import oap.util.Strings;
import oap.util.function.Try;
import org.apache.commons.lang3.StringUtils;
import org.assertj.core.api.AbstractCharSequenceAssert;
import org.assertj.core.api.AbstractFileAssert;
import org.testng.Assert;

import javax.annotation.Nonnull;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.nio.file.Path;
import java.util.Map;

import static java.util.stream.Collectors.toList;
import static oap.io.content.ContentReader.ofBytes;
import static oap.io.content.ContentReader.ofString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.Assert.fail;

public final class Asserts {

    @SneakyThrows
    public static void eventually( long retryTimeout, int retries, Try.ThrowingRunnable asserts ) {
        boolean passed = false;
        Throwable exception = null;
        int count = retries;

        while( !passed && count >= 0 ) {
            try {
                asserts.run();
                passed = true;
            } catch( Throwable e ) {
                exception = e;
                Threads.sleepSafely( retryTimeout );
                count--;
            }
        }
        if( !passed )
            if( exception != null ) throw exception;
            else throw new AssertionError( "timeout" );
    }

    @SneakyThrows
    public static void assertEventually( long retryTimeout, int retries, oap.util.function.Try.ThrowingRunnable asserts ) {
        eventually( retryTimeout, retries, asserts );
    }

    @Deprecated
    public static <A> void assertEquals( java.util.stream.Stream<? extends A> actual, java.util.stream.Stream<? extends A> expected ) {
        if( actual == null && expected != null ) fail( "actual stream is null" );
        else if( actual != null && expected != null )
            Assert.assertEquals( actual.collect( toList() ), expected.collect( toList() ) );
    }

    @Deprecated
    public static void assertEquals( int[] actual, int[] expected ) {
        Assert.assertNotNull( actual );
        Assert.assertNotNull( expected );
        Assert.assertEquals( actual.length, expected.length, "array length" );
        for( int i = 0; i < actual.length; i++ ) {
            Assert.assertEquals( actual[i], expected[i], " at index " + i );
        }

    }

    public static StringAssertion assertString( CharSequence actual ) {
        return new StringAssertion( actual );
    }

    public static <S, F> ResultAssertion<S, F> assertResult( Result<S, F> result ) {
        return new ResultAssertion<>( result );
    }

    public static FileAssertion assertFile( Path actual ) {
        return new FileAssertion( actual );
    }

    public static void failNotEquals( Object actual, Object expected, String message ) {
        try {
            Method failNotEquals = Assert.class.getDeclaredMethod( "failNotEquals", Object.class, Object.class, String.class );
            failNotEquals.setAccessible( true );
            failNotEquals.invoke( null, actual, expected, message );
        } catch( NoSuchMethodException | IllegalAccessException e ) {
            throw new Error( e );
        } catch( InvocationTargetException e ) {
            if( e.getTargetException() instanceof AssertionError ) throw ( AssertionError ) e.getTargetException();
            else throw new Error( e );
        }
    }

    public static void failNotEquals( Object actual, Object expected ) {
        failNotEquals( actual, expected, null );
    }

    public static <R> R contentOfTestResource( Class<?> contextClass, String resource, ContentReader<R> reader ) {
        return Resources.read( contextClass, contextClass.getSimpleName() + "/" + resource, reader )
            .orElseThrow( () -> new AssertionError( "resource " + resource + " not found" ) );
    }

    /**
     * @see #contentOfTestResource(Class, String, ContentReader)
     */
    @Deprecated
    public static String contentOfTestResource( Class<?> contextClass, String resource ) {
        return contentOfTestResource( contextClass, resource, Map.of() );
    }

    public static String contentOfTestResource( Class<?> contextClass, String resource, Map<String, Object> substitutions ) {
        return Resources.read( contextClass, contextClass.getSimpleName() + "/" + resource, ofString() )
            .map( content -> Strings.substitute( content, substitutions ) )
            .orElseThrow( () -> new AssertionError( "resource " + resource + " not found" ) );
    }

    public static byte[] bytesOfTestResource( Class<?> contextClass, String resource ) {
        return Resources.read( contextClass, contextClass.getSimpleName() + "/" + resource, ofBytes() )
            .orElseThrow( () -> new AssertionError( "resource " + resource + " not found" ) );
    }

    public static String sortedLinesOfTestResource( Class<?> contextClass, String resource ) {
        String content = Resources.read( contextClass, contextClass.getSimpleName() + "/" + resource, ofString() )
            .orElseThrow( () -> new AssertionError( "resource " + resource + " not found" ) );
        return Strings.sortLines( content );
    }

    /**
     * @see oap.io.Resources#filePath(Class, String)
     */
    @Deprecated
    public static Path pathOfResource( Class<?> contextClass, String resource ) {
        return Resources.filePath( contextClass, resource == null ? "" : resource )
            .orElseThrow( () -> new AssertionError( "resource " + resource + " not found" ) );
    }

    public static Path pathOfTestResource( Class<?> contextClass, String resource ) {
        return Resources.filePath( contextClass, contextClass.getSimpleName() + ( resource == null ? ""
                : "/" + resource ) )
            .orElseThrow( () -> new AssertionError( contextClass + ": resource " + resource + " not found" ) );
    }

    public static Path pathOfTestResource( Class<?> contextClass ) {
        return pathOfTestResource( contextClass, null );
    }

    public static <T> T objectOfTestResource( Class<T> objectClass, Class<?> contextClass, String resource ) {
        return Binder.hoconWithoutSystemProperties.unmarshal( objectClass, pathOfTestResource( contextClass, resource ) );
    }

    public static Stream<String> linesOfTestResource( Class<?> contextClass, String resource ) {
        return Resources.read( contextClass, contextClass.getSimpleName() + ( resource == null ? ""
                : "/" + resource ), ContentReader.ofLinesStream() )
            .orElseThrow( () -> new AssertionError( "resource " + resource + " not found" ) );
    }

    public static URL urlOfTestResource( Class<?> contextClass, String resource ) {
        return Resources.url( contextClass, contextClass.getSimpleName()
                + ( resource == null ? "" : "/" + resource ) )
            .orElseThrow( () -> new AssertionError( "resource " + resource + " not found" ) );
    }


    public static String locationOfTestResource( Class<?> contetClass, @Nonnull String resource ) {
        Preconditions.checkNotNull( resource );

        return "/" + contetClass.getName().replace( ".", "/" ) + ( resource.startsWith( "/" ) ? "" : "/" ) + resource;
    }

    public static class StringAssertion extends AbstractCharSequenceAssert<StringAssertion, CharSequence> {
        protected StringAssertion( CharSequence value ) {
            super( value, StringAssertion.class );
        }

        /**
         * This assertion is implemented to get IntelliJ Idea to bring up string comparison dialog on error.
         * AssertJ's error ain't gona cut it.
         */
        @Override
        public StringAssertion isEqualTo( Object expected ) {
            Assert.assertEquals( this.actual, expected );
            return this;
        }

        @Override
        public StringAssertion isEqualToIgnoringCase( CharSequence expected ) {
            if( !StringUtils.equalsIgnoreCase( this.actual, expected ) ) failNotEquals( this.actual, expected );
            return this;
        }

        public StringAssertion isEqualToLineSorting( String expected ) {
            Assert.assertEquals( Strings.sortLines( this.actual ), expected );
            return this;
        }
    }

    public static class FileAssertion extends AbstractFileAssert<FileAssertion> {
        protected FileAssertion( Path actual ) {
            super( actual.toFile(), FileAssertion.class );
        }

        public FileAssertion hasSameContentAs( Path expected ) {
            String actual = Files.read( this.actual.toPath(), ofString() );
            assertThat( actual ).isEqualTo( Files.read( expected, ofString() ) );
            return this;
        }

        public FileAssertion hasSize( long size ) {
            assertThat( actual.length() ).isEqualTo( size );
            return this;
        }

        @Override
        public FileAssertion hasContent( String expected ) {
            return hasContent( expected, IoStreams.Encoding.PLAIN );
        }

        public FileAssertion hasContent( String expected, IoStreams.Encoding encoding ) {
            exists();
            String actual = Files.read( this.actual.toPath(), encoding, ofString() );
            assertThat( actual ).isEqualTo( expected );
            return this;
        }

        @Deprecated
        public FileAssertion hasSortedLinesContent( String expected, IoStreams.Encoding encoding ) {
            return hasContentLineSorting( expected, encoding );
        }

        public FileAssertion hasContentLineSorting( String expected, IoStreams.Encoding encoding ) {
            exists();
            assertThat( Strings.sortLines( Files.read( this.actual.toPath(), encoding, ofString() ) ) ).isEqualTo( expected );
            return this;
        }
    }
}
