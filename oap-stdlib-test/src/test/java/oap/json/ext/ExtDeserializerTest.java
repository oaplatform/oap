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

package oap.json.ext;

import lombok.EqualsAndHashCode;
import lombok.ToString;
import oap.json.Binder;
import org.testng.annotations.Test;

import static org.assertj.core.api.Assertions.assertThat;

@SuppressWarnings( "unused" )
public class ExtDeserializerTest {
    @Test
    public void extOk() {
        var aaa = new Bean( TestExt.newExt( "aaa" ) );
        var json = "{\"ext\":{\"value\":\"aaa\"}}";
        assertThat( Binder.json.marshal( aaa ) ).isEqualTo( json );
        assertThat( Binder.json.unmarshal( Bean.class, json ) )
                .isEqualTo( aaa );
    }

    @Test
    public void extOverwritten() {
        var aaa = new Bean( TestExtOverwritten.newExt( "aaa" ) );
        var json = "{\"ext\":{\"value\":\"aaa\"}}";
        assertThat( Binder.json.marshal( aaa ) ).isEqualTo( json );
        assertThat( Binder.json.unmarshal( Bean.class, json ) )
            .isEqualTo( aaa );
    }

    @Test
    public void ext2() {
        var aaa = new Bean();
        aaa.ext2 = Ext2.newExt( Bean.class, "ext2", new Class[] { String.class }, new Object[] { "aaa" } );
        assertThat( Binder.json.clone( aaa ).ext2 ).isEqualTo( new TestExt( "aaa" ) );
    }

    @EqualsAndHashCode
    @ToString
    public static class Bean {
        Ext ext;
        Ext2 ext2;

        Ext noext;

        public Bean() {
        }

        public Bean( Ext ext ) {
            this.ext = ext;
        }
    }

    @SuppressWarnings( "checkstyle:AbstractClassName" )
    @EqualsAndHashCode( callSuper = true )
    @ToString
    public abstract static class Ext2 extends Ext {

    }


    @EqualsAndHashCode( callSuper = true )
    @ToString
    public static class TestExtOverwritten extends TestExt {
        String valueNew;

        public TestExtOverwritten() {
        }

        public TestExtOverwritten( String value ) {
            this.valueNew = value;
        }

        public static TestExt newExt() {
            return newExt( Bean.class, "ext", new Class[0], new Object[0] );
        }

        public static TestExt newExt( String value ) {
            return newExt( Bean.class, "ext", new Class[] { String.class }, new Object[] { value } );
        }
    }

    @EqualsAndHashCode( callSuper = true )
    @ToString
    public static class TestExt extends Ext2 {
        String value;

        public TestExt() {
        }

        public TestExt( String value ) {
            this.value = value;
        }

        public static TestExt newExt() {
            return newExt( Bean.class, "ext", new Class[0], new Object[0] );
        }

        public static TestExt newExt( String value ) {
            return newExt( Bean.class, "ext", new Class[] { String.class }, new Object[] { value } );
        }
    }
}
