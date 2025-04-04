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
import oap.util.Dates;
import oap.util.Strings;
import org.apache.commons.io.FileUtils;
import org.codehaus.plexus.util.StringUtils;
import org.joda.time.DateTime;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.ISO_8859_1;
import static java.nio.charset.StandardCharsets.UTF_8;
import static oap.template.ErrorStrategy.ERROR;
import static oap.template.ErrorStrategy.IGNORE;
import static oap.template.TemplateAccumulators.BINARY;
import static oap.template.TemplateAccumulators.OBJECT;
import static oap.template.TemplateAccumulators.STRING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.joda.time.DateTimeZone.UTC;

public class TemplateEngineTest extends Fixtures {
    private final TestDirectoryFixture testDirectoryFixture;
    private TemplateEngine engine;
    private String testMethodName;

    public TemplateEngineTest() {
        testDirectoryFixture = fixture( new TestDirectoryFixture() );
    }

    @BeforeMethod
    public void beforeCMethod() {
        engine = new TemplateEngine( Dates.d( 10 ) );
    }

    @BeforeMethod
    public void nameBefore( Method method ) {
        testMethodName = method.getName();
    }

    @Test
    public void testRenderStringText() {
        assertThat( engine.getTemplate( testMethodName, new TypeRef<Map<String, String>>() {}, "sdkjf hdkfgj d$...{}", STRING, null ).render( null ).get() )
            .isEqualTo( "sdkjf hdkfgj d$...{}" );
    }

    @Test
    public void testWithoutDefaultValue() {
        TestTemplateClass c = new TestTemplateClass();
        c.childNullable = new TestTemplateClass();
        c.childNullable.longField = 0;
        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "{{childNullable.longField}}", OBJECT, null ).render( c ).get() )
            .isEqualTo( 0L );
    }

    @Test
    public void testRenderUNKNOWNStringTextAsBinary() throws IOException {
        assertThat( BinaryUtils.read( engine.getTemplate( testMethodName, new TypeRef<Map<String, String>>() {}, Strings.UNKNOWN, BINARY, null ).render( null ).get() ) )
            .isEqualTo( List.of( List.of( "" ) ) );
    }

    @Test
    public void testEscapeVariables() {
        TestTemplateClass c = new TestTemplateClass();
        c.field = "1";

        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "{{ field }}\t{{ field }}", new TestTemplateAccumulatorString(), null )
            .render( c ).get() ).isEqualTo( "12\t12" );
    }

    @Test
    public void testEscapeExpression() {
        TestTemplateClass c = new TestTemplateClass();
        c.field = "1";

        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "$${field}", new TestTemplateAccumulatorString(), null )
            .render( c ).get() ).isEqualTo( "${field}" );
    }

    @Test
    public void testMapProperty() {
        assertThat( engine.getTemplate( testMethodName, new TypeRef<Map<String, String>>() {}, "{{ prop }}", STRING, null ).render( Map.of( "prop", "val" ) ).get() )
            .isEqualTo( "val" );
    }

    @Test
    public void testEscapeClassName() {
        assertThat( engine.getTemplate( "111-" + testMethodName + "-? ()?пп1", new TypeRef<Map<String, String>>() {}, "{{ prop }}", STRING, null ).render( Map.of( "prop", "val" ) ).get() )
            .isEqualTo( "val" );
    }

    @Test
    public void testField() {
        TestTemplateClass c = new TestTemplateClass();
        c.field = "val1";
        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "{{ field }}", STRING, null ).render( c ).get() )
            .isEqualTo( "val1" );
    }

    @Test
    public void testFieldWithJsonProperty() {
        TestTemplateClass c = new TestTemplateClass();
        c.jsonTest = "val1";
        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "{{ jsonTest }}", STRING, null ).render( c ).get() )
            .isEqualTo( "val1" );
        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "{{ jsonTestNew }}", STRING, null ).render( c ).get() )
            .isEqualTo( "val1" );
    }

    @Test
    public void testFieldWithJsonAlias() {
        TestTemplateClass c = new TestTemplateClass();
        c.jsonTest = "val1";
        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "{{ jsonTest }}", STRING, null ).render( c ).get() )
            .isEqualTo( "val1" );
        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "{{ jsonTestAlias1 }}", STRING, null ).render( c ).get() )
            .isEqualTo( "val1" );
        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "{{ jsonTestAlias2 }}", STRING, null ).render( c ).get() )
            .isEqualTo( "val1" );
    }

    @Test
    public void testEnumField() {
        TestTemplateClass c = new TestTemplateClass();
        c.enumField = TestTemplateEnum.VAL1;
        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "{{ enumField }}", STRING, null ).render( c ).get() )
            .isEqualTo( "VAL1" );
    }

    @Test
    public void testEnumFieldDefaultValue() {
        TestTemplateClass c = new TestTemplateClass();
        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "{{ <java.lang.Enum>child.enumField ?? 'UNKNOWN' }}", STRING, null ).render( c ).get() )
            .isEqualTo( "UNKNOWN" );
    }

    @Test
    public void testListField() {
        TestTemplateClass c = new TestTemplateClass();
        c.list = List.of( 1, 2, 3 );
        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "{{ list }}", STRING, null ).render( c ).get() )
            .isEqualTo( "[1,2,3]" );
    }

    @Test
    public void testListFieldWithoutDefault() {
        TestTemplateClass c = new TestTemplateClass();
        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "{{ list }}", STRING, null ).render( c ).get() )
            .isEqualTo( "[]" );
    }

    @Test
    public void testListString() {
        TestTemplateClass c = new TestTemplateClass();
        c.listString = List.of( "1", "'", "\\" );
        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "{{ listString }}", STRING, null ).render( c ).get() )
            .isEqualTo( "['1','\\'','\\\\']" );
    }

    @Test
    public void testListEnum() {
        TestTemplateClass c = new TestTemplateClass();
        c.listEnum = List.of( TestTemplateEnum.VAL2, TestTemplateEnum.VAL1 );
        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "{{ listEnum }}", STRING, null ).render( c ).get() )
            .isEqualTo( "['VAL2','VAL1']" );
    }

    @Test
    public void testListFieldDefaultValue() {
        TestTemplateClass c = new TestTemplateClass();
        c.list = null;
        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "{{ list ?? [] }}", STRING, null ).render( c ).get() )
            .isEqualTo( "[]" );
    }

    @Test
    public void testChain() {
        TestTemplateClass c1 = new TestTemplateClass();
        TestTemplateClass c2 = new TestTemplateClass();
        c1.child = c2;
        c2.field = "val3";
        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "{{ child.field }}", STRING, null ).render( c1 ).get() )
            .isEqualTo( "val3" );
    }

    @Test
    public void testOrEmptyString() {
        TestTemplateClass c = new TestTemplateClass();
        c.field2 = "f2";

        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "{{ field | default field2 }}", STRING, null ).render( c ).get() )
            .isEqualTo( "f2" );
    }

    @Test
    public void testOrCollections() {
        TestTemplateClass c = new TestTemplateClass();
        c.list2 = List.of( 2, 3 );

        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "{{ list | default list2 }}", STRING, null ).render( c ).get() )
            .isEqualTo( "[2,3]" );
    }

    @Test
    public void testOptional() {
        TestTemplateClass c = new TestTemplateClass();

        c.fieldOpt = Optional.of( "o" );
        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "{{ fieldOpt }}", STRING, null ).render( c ).get() )
            .isEqualTo( "o" );

        c.fieldOpt = Optional.empty();
        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "{{ fieldOpt }}", STRING, null ).render( c ).get() )
            .isEqualTo( "" );
    }

    @Test
    public void testChildOptional() {
        TestTemplateClass c = new TestTemplateClass();
        TestTemplateClass cp = new TestTemplateClass();
        c.fieldOpt = Optional.of( "o" );
        c.intField = 10;

        cp.childOpt = Optional.of( c );
        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "{{ childOpt.fieldOpt }}-{{ childOpt.intField }}", STRING, Map.of(), IGNORE ).render( cp ).get() )
            .isEqualTo( "o-10" );

        cp.childOpt = Optional.empty();
        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "{{ childOpt.fieldOpt }}", STRING, null ).render( cp ).get() )
            .isEqualTo( "" );
    }

    @Test
    public void testNullable() {
        TestTemplateClass c = new TestTemplateClass();

        c.fieldNullable = "o";
        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "{{ fieldNullable }}", STRING, null ).render( c ).get() )
            .isEqualTo( "o" );

        c.fieldNullable = null;
        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "{{ fieldNullable }}", STRING, null ).render( c ).get() )
            .isEqualTo( "" );
    }

    @Test
    public void testChildNullable() {
        TestTemplateClass c = new TestTemplateClass();
        TestTemplateClass cp = new TestTemplateClass();
        c.fieldNullable = "o";

        cp.childNullable = c;
        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "{{ childNullable.fieldNullable ?? '' }}", STRING, null ).render( cp ).get() )
            .isEqualTo( "o" );

        cp.childNullable = null;
        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "{{ childNullable.fieldNullable ?? '' }}", STRING, null ).render( cp ).get() )
            .isEqualTo( "" );
    }

    @Test
    public void testDefaultString() {
        assertThat( engine.getTemplate( testMethodName, new TypeRef<Map<String, String>>() {}, "{{ bbb ?? 'test' }}", STRING, null ).render( Map.of( "prop", "val" ) ).get() )
            .isEqualTo( "test" );
    }

    @Test
    public void testDefaultInt() {
        assertThat( engine.getTemplate( testMethodName, new TypeRef<Map<String, Integer>>() {}, "{{ bbb ?? -1 }}", STRING, null ).render( Map.of( "prop", 1 ) ).get() )
            .isEqualTo( "-1" );
    }

    @Test
    public void testDefaultBoolean() {
        TestTemplateClass c = new TestTemplateClass();
        c.childNullable = null;
        c.childOpt = Optional.empty();

        assertThat( engine.getTemplate( testMethodName + "True", new TypeRef<TestTemplateClass>() {}, "{{ childNullable.booleanObjectField ?? true }}", STRING, null ).render( c ).get() )
            .isEqualTo( "true" );
        assertThat( engine.getTemplate( testMethodName + "True", new TypeRef<TestTemplateClass>() {}, "{{ childOpt.booleanObjectField ?? true }}", STRING, Map.of(), IGNORE ).render( c ).get() )
            .isEqualTo( "true" );
    }

    @Test
    public void testDefaultDouble() {
        assertThat( engine.getTemplate( testMethodName, new TypeRef<Map<String, Double>>() {}, "{{ bbb ?? 0.0 }}", STRING, null ).render( Map.of( "prop", 1.1 ) ).get() )
            .isEqualTo( "0.0" );
    }

    @Test
    public void testDefaultDoubleBinary() throws IOException {
        assertThat( BinaryUtils.read( engine.getTemplate( testMethodName, new TypeRef<Map<String, Double>>() {}, "{{ bbb ?? 0.0 }}", BINARY, null ).render( Map.of( "prop", 1.1 ) ).get() ) )
            .isEqualTo( List.of( List.of( 0.0d ) ) );
    }

    @Test
    public void testDefaultDateTime() throws IOException {
        assertThat( BinaryUtils.read( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "{{ dateTimeOptional ?? '2023-01-04 18:09:11' }}", BINARY, null ).render( new TestTemplateClass() ).get() ) )
            .isEqualTo( List.of( List.of( new DateTime( 2023, 1, 4, 18, 9, 11, UTC ) ) ) );
    }

    @Test
    public void testMix() {
        assertThat( engine.getTemplate( testMethodName, new TypeRef<Map<String, String>>() {}, "-{{ prop }}-{{ b }}-", STRING, null ).render( Map.of( "prop", "val", "b", "b1" ) ).get() )
            .isEqualTo( "-val-b1-" );
    }

    @Test
    public void testAliases() {
        TestTemplateClass c1 = new TestTemplateClass();
        TestTemplateClass c2 = new TestTemplateClass();
        TestTemplateClass2 c3 = new TestTemplateClass2();
        c1.child = c2;
        c1.child2 = c3;

        c2.field = "val3";
        c3.field2 = "f2";

        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "{{ child.field }}", STRING,
            Map.of( "child.field", "child2.field2" ), ERROR ).render( c1 ).get() )
            .isEqualTo( "f2" );
    }

    @Test
    public void testDiskCache() {
        TestTemplateClass c1 = new TestTemplateClass();
        c1.field = "1";
        c1.field2 = "2";

        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "{{ field }}", STRING, ERROR, null ).render( c1 ).get() )
            .isEqualTo( "1" );

        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "{{ field2 }}", STRING, ERROR, null ).render( c1 ).get() )
            .isEqualTo( "2" );

        TemplateEngine engine2 = new TemplateEngine( testDirectoryFixture.testDirectory(), Dates.d( 10 ) );
        assertThat( engine2.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "{{ field }}", STRING, ERROR, null ).render( c1 ).get() )
            .isEqualTo( "1" );
    }

    @Test
    public void testDiskCacheChangeSourceCode() throws IOException {
        engine = new TemplateEngine( testDirectoryFixture.testDirectory(), Dates.d( 10 ) );
        TestTemplateClass c1 = new TestTemplateClass();
        c1.field = "1";
        c1.field2 = "2";

        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "{{ field }}", STRING, ERROR, null ).render( c1 ).get() )
            .isEqualTo( "1" );

        replace( "oap.template.testDiskCacheChangeSourceCode_156c7bcafb683f4fea4f07418942daf1.class" );
        replace( "oap.template.testDiskCacheChangeSourceCode_156c7bcafb683f4fea4f07418942daf1.java" );

        TemplateEngine engine2 = new TemplateEngine( testDirectoryFixture.testDirectory(), Dates.d( 10 ) );
        assertThat( engine2.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "{{ field }}", STRING, ERROR, null ).render( c1 ).get() )
            .isEqualTo( "1" );
    }

    private void replace( String fileName ) throws IOException {
        File file = testDirectoryFixture.testPath( fileName ).toFile();
        String classStr = new String( FileUtils.readFileToByteArray( file ), ISO_8859_1 );
        byte[] classBytes = StringUtils.replace( classStr, "TemplateAccumulator", "Fake____Accumulator" ).getBytes( ISO_8859_1 );
        FileUtils.writeByteArrayToFile( file, classBytes );
    }

    @Test
    public void testErrorSyntax() {
        assertThatThrownBy( () -> engine.getTemplate( testMethodName, new TypeRef<Map<String, String>>() {}, "id={{ v; toUpperCase()", STRING, ERROR, null ) )
            .isInstanceOf( TemplateException.class );
    }

    @Test
    public void testExt() {
        TestTemplateClass c = new TestTemplateClass();
        c.ext2 = new TestTemplateClassExt( "ev" );
        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "{{ ext.a | default ext2.a }}", STRING, null ).render( c ).get() )
            .isEqualTo( "ev" );
    }

    @Test
    public void testDefaultExt() {
        TestTemplateClass c = new TestTemplateClass();
        c.ext3.a = "123";
        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "{{ ext2.a | default ext3.a }}", STRING, null ).render( c ).get() )
            .isEqualTo( "123" );
    }

    @Test
    public void testConcatenation() {
        TestTemplateClass c = new TestTemplateClass();
        c.field = "f1";
        c.field2 = "f2";

        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "{{ {field,\"x\",field2} }}", STRING, null ).render( c ).get() )
            .isEqualTo( "f1xf2" );
    }

    @Test
    public void testNestedConcatenation() {
        TestTemplateClass c = new TestTemplateClass();
        TestTemplateClass c1 = new TestTemplateClass();
        c.child = c1;
        c1.field = "f1";
        c1.field2 = "f2";

        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "{{ child{field,\"x\",field2} }}", STRING, null ).render( c ).get() )
            .isEqualTo( "f1xf2" );
    }

    @Test
    public void testNestedConcatenationWithDot() {
        TestTemplateClass c = new TestTemplateClass();
        TestTemplateClass2 c1 = new TestTemplateClass2();
        c.child2 = c1;
        c1.field2 = "f1";
        c1.field22 = "f2";

        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "{{ child2.{field2,\"x\",field22} }}", STRING, null ).render( c ).get() )
            .isEqualTo( "f1xf2" );
    }

    @Test
    public void testSum() {
        TestTemplateClass c = new TestTemplateClass();
        c.intField = 123;

        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "{{ intField + 12.45 }}", STRING, null ).render( c ).get() )
            .isEqualTo( "135.45" );
    }

    @Test
    public void testSumDefault() {
        TestTemplateClass c = new TestTemplateClass();

        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "{{ intObjectField + 12.45 ?? 5 }}", STRING, null ).render( c ).get() )
            .isEqualTo( "5" );
    }

    @Test
    public void testComment() {
        TestTemplateClass c = new TestTemplateClass();
        c.intField = 123;

        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "{{ /* intField */intField }}", STRING, null ).render( c ).get() )
            .isEqualTo( "123" );
    }

    @Test
    public void testErrorSyntaxMsg() {
        assertThatThrownBy( () -> engine.getTemplate( testMethodName, new TypeRef<Map<String, String>>() {}, "id={{ unknownField.unknownField }}", STRING, ERROR, null ) )
            .isInstanceOf( TemplateException.class )
            .hasMessageContaining( "unknownField.unknownField" );
    }

    @Test
    public void testPrimitiveAsObject() {
        TestPrimitiveTemplateAccumulatorString templateAccumulator = new TestPrimitiveTemplateAccumulatorString();
        TestTemplateClass templateClass = new TestTemplateClass();
        templateClass.booleanField = true;
        templateClass.booleanObjectField = true;
        templateClass.intField = 1;
        templateClass.intObjectField = 2;

        String str = engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {},
            "booleanField:{{ booleanField }},booleanObjectField:{{ booleanObjectField }},intField:{{ intField }},intObjectField:{{ intObjectField }}",
            templateAccumulator, ERROR, null ).render( templateClass ).get();


        assertThat( str ).isEqualTo( "booleanField:true_b,booleanObjectField:true_b,intField:1_i,intObjectField:2_i" );
    }

    @SuppressWarnings( "checkstyle:NoWhitespaceBefore" )
    @Test
    public void testPrimitiveAsObjectDefaultValue() {
        TestPrimitiveTemplateAccumulatorString templateAccumulator = new TestPrimitiveTemplateAccumulatorString();
        TestTemplateClass templateClass = new TestTemplateClass();

        String str = engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {},
            "booleanField:{{ booleanField ?? true }},booleanObjectField:{{ booleanObjectField ?? true }}"
                + ",byteField:{{ byteField ?? 1 }},byteObjectField:{{ byteObjectField ?? 1 }}"
                + ",shortField:{{ shortField ?? 1 }},shortObjectField:{{ shortObjectField ?? 1 }}"
                + ",intField:{{ intField ?? 1 }},intObjectField:{{ intObjectField ?? 1 }}"
                + ",longField:{{ longField ?? 1 }},longObjectField:{{ longObjectField ?? 1 }}"
                + ",floatField:{{ floatField ?? 1 }},floatObjectField:{{ floatObjectField ?? 1 }}"
                + ",doubleField:{{ doubleField ?? 1 }},doubleObjectField:{{ doubleObjectField ?? 1 }}"
            , templateAccumulator, ERROR, null ).render( templateClass ).get();


        assertThat( str ).isEqualTo( "booleanField:false_b,booleanObjectField:true_b"
            + ",byteField:0,byteObjectField:1"
            + ",shortField:0,shortObjectField:1"
            + ",intField:0_i,intObjectField:1_i"
            + ",longField:0,longObjectField:1"
            + ",floatField:0.0,floatObjectField:1.0"
            + ",doubleField:0.0,doubleObjectField:1.0" );
    }

    @Test
    public void testCacheClassFormatError() throws IOException {
        FileUtils.write( testDirectoryFixture.testPath( "oap.template.testCacheClassFormatError.class" ).toFile(), "", UTF_8 );

        TestTemplateClass c = new TestTemplateClass();
        c.field = "1";

        assertThat( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "{{ field }}\t{{ field }}", new TestTemplateAccumulatorString(), null )
            .render( c ).get() ).isEqualTo( "12\t12" );

        FileUtils.write( testDirectoryFixture.testPath( "oap.template.testCacheClassFormatError.class" ).toFile(), "", UTF_8 );

        TemplateEngine engine2 = new TemplateEngine( testDirectoryFixture.testDirectory(), Dates.d( 20 ) );

        assertThat( engine2.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "{{ field }}\t{{ field }}", new TestTemplateAccumulatorString(), null )
            .render( c ).get() ).isEqualTo( "12\t12" );
    }

    public static class TestTemplateAccumulatorString extends TemplateAccumulatorString {
        @Override
        public void accept( String text ) {
            super.accept( text + "2" );
        }

        @Override
        public TemplateAccumulatorString newInstance() {
            return new TestTemplateAccumulatorString();
        }
    }

    public static class TestPrimitiveTemplateAccumulatorString extends TemplateAccumulatorString {
        @Override
        public void accept( boolean b ) {
            super.accept( b + "_b" );
        }

        @Override
        public void accept( int i ) {
            super.accept( i + "_i" );
        }

        @Override
        public TemplateAccumulatorString newInstance() {
            return new TestPrimitiveTemplateAccumulatorString();
        }
    }
}
