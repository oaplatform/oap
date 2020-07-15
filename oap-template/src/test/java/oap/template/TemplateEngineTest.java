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

package oap.template;

import oap.reflect.TypeRef;
import oap.testng.Fixtures;
import oap.testng.TestDirectoryFixture;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.Optional;

import static oap.testng.Asserts.assertString;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Created by igor.petrenko on 2020-07-13.
 */
public class TemplateEngineTest extends Fixtures {

    private TemplateEngine engine;
    private String testMethodName;

    {
        fixture( TestDirectoryFixture.FIXTURE );
    }

    @BeforeClass
    public void beforeClass() {
        engine = new TemplateEngine( TestDirectoryFixture.testDirectory() );
    }

    @BeforeMethod
    public void nameBefore( Method method ) {
        testMethodName = method.getName();
    }

    @Test
    public void testRenderStringText() {
        assertString( engine.getTemplate( testMethodName, new TypeRef<Map<String, String>>() {}, "sdkjf hdkfgj d$...{}" ).renderString( null ) )
            .isEqualTo( "sdkjf hdkfgj d$...{}" );
    }

    @Test
    public void testMapProperty() {
        assertString( engine.getTemplate( testMethodName, new TypeRef<Map<String, String>>() {}, "${prop}" ).renderString( Map.of( "prop", "val" ) ) )
            .isEqualTo( "val" );
    }

    @Test
    public void testField() {
        var c = new TestTemplateClass();
        c.field = "val1";
        assertString( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${field}" ).renderString( c ) )
            .isEqualTo( "val1" );
    }

    @Test
    public void testMethod() {
        var c = new TestTemplateClass();
        c.field = "val2";
        assertString( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${fieldM()}" ).renderString( c ) )
            .isEqualTo( "val2" );
    }

    @Test
    public void testChain() {
        var c1 = new TestTemplateClass();
        var c2 = new TestTemplateClass();
        c1.child = c2;
        c2.field = "val3";
        assertString( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${child.field}" ).renderString( c1 ) )
            .isEqualTo( "val3" );
    }

    @Test
    public void testOrEmptyString() {
        var c = new TestTemplateClass();
        c.field2 = "f2";

        assertString( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${field | field2}" ).renderString( c ) )
            .isEqualTo( "f2" );
    }

    @Test
    public void testOptional() {
        var c = new TestTemplateClass();

        c.fieldOpt = Optional.of( "o" );
        assertString( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${fieldOpt}" ).renderString( c ) )
            .isEqualTo( "o" );

        c.fieldOpt = Optional.empty();
        assertString( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${fieldOpt}" ).renderString( c ) )
            .isEqualTo( "" );
    }

    @Test
    public void testChildOptional() {
        var c = new TestTemplateClass();
        var cp = new TestTemplateClass();
        c.fieldOpt = Optional.of( "o" );

        cp.childOpt = Optional.of( c );
        assertString( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${childOpt.fieldOpt}" ).renderString( cp ) )
            .isEqualTo( "o" );

        cp.childOpt = Optional.empty();
        assertString( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${childOpt.fieldOpt}" ).renderString( cp ) )
            .isEqualTo( "" );
    }

    @Test
    public void testNullable() {
        var c = new TestTemplateClass();

        c.fieldNullable = "o";
        assertString( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${fieldNullable}" ).renderString( c ) )
            .isEqualTo( "o" );

        c.fieldNullable = null;
        assertString( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${fieldNullable}" ).renderString( c ) )
            .isEqualTo( "" );
    }

    @Test
    public void testChildNullable() {
        var c = new TestTemplateClass();
        var cp = new TestTemplateClass();
        c.fieldNullable = "o";

        cp.childNullable = c;
        assertString( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${childNullable.fieldNullable}" ).renderString( cp ) )
            .isEqualTo( "o" );

        cp.childNullable = null;
        assertString( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${childNullable.fieldNullable}" ).renderString( cp ) )
            .isEqualTo( "" );
    }

    @Test
    public void testDefaultString() {
        assertString( engine.getTemplate( testMethodName, new TypeRef<Map<String, String>>() {}, "${bbb??'test'}" ).renderString( Map.of( "prop", "val" ) ) )
            .isEqualTo( "test" );
    }

    @Test
    public void testMix() {
        assertString( engine.getTemplate( testMethodName, new TypeRef<Map<String, String>>() {}, "-${prop}-${b}-" ).renderString( Map.of( "prop", "val", "b", "b1" ) ) )
            .isEqualTo( "-val-b1-" );
    }

    @Test
    public void testFunctionUrlencode() {
        assertString( engine.getTemplate( "testFunctionUrlencode0", new TypeRef<Map<String, String>>() {}, "id=${v; urlencode(0)}" ).renderString( Map.of( "v", "a i/d" ) ) )
            .isEqualTo( "id=a i/d" );
        assertString( engine.getTemplate( "testFunctionUrlencode1", new TypeRef<Map<String, String>>() {}, "id=${v; urlencode( 1)}" ).renderString( Map.of( "v", "a i/d" ) ) )
            .isEqualTo( "id=a+i%2Fd" );
        assertString( engine.getTemplate( "testFunctionUrlencode", new TypeRef<Map<String, String>>() {}, "id=${ v ; urlencode() }" ).renderString( Map.of( "v", "a i/d" ) ) )
            .isEqualTo( "id=a+i%2Fd" );
        assertString( engine.getTemplate( "testFunctionUrlencode2", new TypeRef<Map<String, String>>() {}, "id=${v; urlencode ( 2 )}" ).renderString( Map.of( "v", "a i/d" ) ) )
            .isEqualTo( "id=a%2Bi%252Fd" );
    }

    @Test
    public void testFunctionToUpperCase() {
        assertString( engine.getTemplate( testMethodName, new TypeRef<Map<String, String>>() {}, "id=${v; toUpperCase()}" ).renderString( Map.of( "v", "a i/d" ) ) )
            .isEqualTo( "id=A I/D" );
    }

    @Test
    public void testErrorSyntax() {
        assertThatThrownBy( () -> engine.getTemplate( testMethodName, new TypeRef<Map<String, String>>() {}, "id=${v; toUpperCase()", ErrorStrategy.ERROR ) )
            .isInstanceOf( TemplateException.class );
    }

    @Test
    public void testExt() {
        var c = new TestTemplateClass();
        c.ext2 = new TestTemplateClassExt( "ev" );
        assertString( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${ext.a|ext2.a}" ).renderString( c ) )
            .isEqualTo( "ev" );
    }
}
