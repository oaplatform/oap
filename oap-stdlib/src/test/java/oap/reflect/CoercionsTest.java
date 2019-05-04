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

import oap.testng.AbstractTest;
import org.testng.annotations.Test;

import java.lang.annotation.RetentionPolicy;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

public class CoercionsTest extends AbstractTest {
    @Test
    public void cast() {
        Coercions coercions = Coercions.basic().withIdentity();
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
    }

}
