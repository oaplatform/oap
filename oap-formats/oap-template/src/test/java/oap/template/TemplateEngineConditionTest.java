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
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static oap.template.ErrorStrategy.ERROR;
import static oap.template.TemplateAccumulators.STRING;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class TemplateEngineConditionTest extends AbstractTemplateEngineTest {
    @Test
    public void testIfConditionTrue() {
        TestTemplateClass c = new TestTemplateClass();
        c.field = "val";
        c.booleanField = true;
        assertThat( getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "{{ if booleanField then field end }}", STRING, null ).render( c ).get() )
            .isEqualTo( "val" );
    }

    @Test
    public void testIfConditionFalse() {
        TestTemplateClass c = new TestTemplateClass();
        c.field = "val";
        c.booleanField = false;
        assertThat( getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "{{ if booleanField then field end }}", STRING, null ).render( c ).get() )
            .isEqualTo( "" );
    }

    @Test
    public void testIfElseConditionFalse() {
        TestTemplateClass c = new TestTemplateClass();
        c.field = "val";
        c.field2 = "val2";
        c.booleanField = false;
        assertThat( getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "{{ if booleanField then field else field2 end }}", STRING, null ).render( c ).get() )
            .isEqualTo( "val2" );
    }

    @Test
    public void testIfConditionNullableObject() {
        TestTemplateClass c = new TestTemplateClass();
        c.field = "val";

        c.booleanObjectField = true;
        assertThat( getTemplate( testMethodName + "True", new TypeRef<TestTemplateClass>() {}, "{{ if booleanObjectField then field end }}", STRING, null ).render( c ).get() )
            .isEqualTo( "val" );

        c.booleanObjectField = null;
        assertThat( getTemplate( testMethodName + "Null", new TypeRef<TestTemplateClass>() {}, "{{ if booleanObjectField then field end }}", STRING, null ).render( c ).get() )
            .isEqualTo( "" );
    }

    @Test
    public void testIfConditionWithText() {
        TestTemplateClass c = new TestTemplateClass();
        c.field = "val";
        c.booleanField = true;
        assertThat( getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "prefix-{{ if booleanField then field end }}-suffix", STRING, null ).render( c ).get() )
            .isEqualTo( "prefix-val-suffix" );
    }

    @Test
    public void testIfConditionWithDefaultValue() {
        TestTemplateClass c = new TestTemplateClass();
        c.booleanField = true;
        assertThat( getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "{{ if booleanField then field end ?? 'default' }}", STRING, null ).render( c ).get() )
            .isEqualTo( "default" );
    }

    @Test
    public void testIfConditionAnd() {
        TestTemplateClass c = new TestTemplateClass();
        c.field = "val";
        c.booleanField = true;
        c.booleanObjectField = true;
        assertThat( getTemplate( testMethodName + "BothTrue", new TypeRef<TestTemplateClass>() {}, "{{ if booleanField and booleanObjectField then field end }}", STRING, null ).render( c ).get() )
            .isEqualTo( "val" );

        c.booleanObjectField = false;
        assertThat( getTemplate( testMethodName + "SecondFalse", new TypeRef<TestTemplateClass>() {}, "{{ if booleanField and booleanObjectField then field end }}", STRING, null ).render( c ).get() )
            .isEqualTo( "" );
    }

    @Test
    public void testIfConditionOr() {
        TestTemplateClass c = new TestTemplateClass();
        c.field = "val";
        c.booleanField = false;
        c.booleanObjectField = true;
        assertThat( getTemplate( testMethodName + "SecondTrue", new TypeRef<TestTemplateClass>() {}, "{{ if booleanField or booleanObjectField then field end }}", STRING, null ).render( c ).get() )
            .isEqualTo( "val" );

        c.booleanObjectField = false;
        assertThat( getTemplate( testMethodName + "BothFalse", new TypeRef<TestTemplateClass>() {}, "{{ if booleanField or booleanObjectField then field end }}", STRING, null ).render( c ).get() )
            .isEqualTo( "" );
    }

    @Test
    public void testIfConditionNot() {
        TestTemplateClass c = new TestTemplateClass();
        c.field = "val";
        c.booleanField = false;
        assertThat( getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "{{ if not booleanField then field end }}", STRING, null ).render( c ).get() )
            .isEqualTo( "val" );
    }

    @Test
    public void testIfConditionBangNot() {
        TestTemplateClass c = new TestTemplateClass();
        c.field = "val";
        c.booleanField = true;
        assertThat( getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "{{ if !booleanField then field end }}", STRING, null ).render( c ).get() )
            .isEqualTo( "" );
    }

    @Test
    public void testIfConditionAndString() {
        TestTemplateClass c = new TestTemplateClass();
        c.field = "hello";
        c.field2 = "world";
        assertThat( getTemplate( testMethodName + "BothNonEmpty", new TypeRef<TestTemplateClass>() {}, "{{ if field and field2 then field end }}", STRING, null ).render( c ).get() )
            .isEqualTo( "hello" );

        c.field2 = "";
        assertThat( getTemplate( testMethodName + "SecondEmpty", new TypeRef<TestTemplateClass>() {}, "{{ if field and field2 then field end }}", STRING, null ).render( c ).get() )
            .isEqualTo( "" );

        c.field = null;
        c.field2 = "world";
        assertThat( getTemplate( testMethodName + "FirstNullOrSecond", new TypeRef<TestTemplateClass>() {}, "{{ if field or field2 then field2 end }}", STRING, null ).render( c ).get() )
            .isEqualTo( "world" );
    }

    @Test
    public void testIfConditionComplex() {
        TestTemplateClass c = new TestTemplateClass();
        c.field = "val";
        c.booleanField = true;
        c.booleanObjectField = true;
        assertThat( getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {},
            "{{ if (booleanField and booleanObjectField) and not booleanField then field end }}", STRING, null ).render( c ).get() )
            .isEqualTo( "" );
    }

    @Test
    public void testBlockIfTrue() {
        TestTemplateClass c = new TestTemplateClass();
        c.booleanField = true;
        c.field = "hello";
        assertThat( getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {},
            "{{% if booleanField }}{{ field }}{{% end }}", STRING, null ).render( c ).get() )
            .isEqualTo( "hello" );
    }

    @Test
    public void testBlockIfFalse() {
        TestTemplateClass c = new TestTemplateClass();
        c.booleanField = false;
        c.field = "hello";
        assertThat( getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {},
            "{{% if booleanField }}{{ field }}{{% end }}", STRING, null ).render( c ).get() )
            .isEqualTo( "" );
    }

    @Test
    public void testBlockIfElseTrue() {
        TestTemplateClass c = new TestTemplateClass();
        c.booleanField = true;
        c.field = "then-val";
        c.field2 = "else-val";
        assertThat( getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {},
            "{{% if booleanField }}{{ field }}{{% else }}{{ field2 }}{{% end }}", STRING, null ).render( c ).get() )
            .isEqualTo( "then-val" );
    }

    @Test
    public void testBlockIfElseFalse() {
        TestTemplateClass c = new TestTemplateClass();
        c.booleanField = false;
        c.field = "then-val";
        c.field2 = "else-val";
        assertThat( getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {},
            "{{% if booleanField }}{{ field }}{{% else }}{{ field2 }}{{% end }}", STRING, null ).render( c ).get() )
            .isEqualTo( "else-val" );
    }

    @Test
    public void testBlockIfElseFalseMap() {
        HashMap<String, Object> c = new HashMap<>();
        c.put( "field", "then-val" );
        c.put( "field2", "else-val" );
        assertThat( getTemplate( testMethodName, new TypeRef<HashMap<String, Object>>() {},
            "{{% if booleanField }}{{ field }}{{% else }}{{ field2 }}{{% end }}", STRING, null ).render( c ).get() )
            .isEqualTo( "else-val" );
    }

    @Test
    public void testBlockIfNullableBoolTrue() {
        TestTemplateClass c = new TestTemplateClass();
        c.booleanObjectField = true;
        c.field = "yes";
        assertThat( getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {},
            "{{% if booleanObjectField }}{{ field }}{{% end }}", STRING, null ).render( c ).get() )
            .isEqualTo( "yes" );
    }

    @Test
    public void testBlockIfNullableBoolNull() {
        TestTemplateClass c = new TestTemplateClass();
        c.booleanObjectField = null;
        c.field = "yes";
        assertThat( getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {},
            "{{% if booleanObjectField }}{{ field }}{{% end }}", STRING, null ).render( c ).get() )
            .isEqualTo( "" );
    }

    @Test
    public void testBlockIfWithSurroundingText() {
        TestTemplateClass c = new TestTemplateClass();
        c.booleanField = true;
        c.field = "world";
        assertThat( getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {},
            "Hello {{% if booleanField }}{{ field }}{{% end }} done", STRING, null ).render( c ).get() )
            .isEqualTo( "Hello world done" );
    }

    @Test
    public void testBlockIfNestedPath() {
        TestTemplateClass c = new TestTemplateClass();
        c.child = new TestTemplateClass();
        c.child.booleanField = true;
        c.child.field = "nested";
        assertThat( getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {},
            "{{% if child.booleanField }}{{ child.field }}{{% end }}", STRING, null ).render( c ).get() )
            .isEqualTo( "nested" );
    }

    @Test
    public void testBlockIfMultiLineBody() {
        TestTemplateClass c = new TestTemplateClass();
        c.booleanField = true;
        c.field = "myval";
        assertThat( getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {},
            "{{% if booleanField }}\n  value={{ field }}\n{{% end }}", STRING, null ).render( c ).get() )
            .isEqualTo( "\n  value=myval\n" );
    }

    @Test
    public void testBlockIfNested() {
        TestTemplateClass c = new TestTemplateClass();
        c.booleanField = true;
        c.child = new TestTemplateClass();
        c.child.booleanField = true;
        c.child.field = "deep";
        assertThat( getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {},
            "{{% if booleanField %}}{{% if child.booleanField }}{{ child.field }}{{% end }}{{% end }}", STRING, null ).render( c ).get() )
            .isEqualTo( "deep" );
    }

    @Test
    public void testBlockIfUnknownFieldThrows() {
        assertThatThrownBy( () ->
            getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {},
                "{{% if unknownField }}x{{% end }}", STRING, ERROR, null ) )
            .isInstanceOf( TemplateException.class );
    }

    @Test
    public void testBlockIfAnd() {
        TestTemplateClass c = new TestTemplateClass();
        c.booleanField = true;
        c.booleanObjectField = true;
        assertThat( getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {},
            "{{% if booleanField and booleanObjectField }}yes{{% else }}no{{% end }}", STRING, null ).render( c ).get() )
            .isEqualTo( "yes" );
    }

    @Test
    public void testBlockIfAndString() {
        TestTemplateClass c = new TestTemplateClass();
        c.field = "1";
        c.field2 = "2";
        assertThat( getTemplate( testMethodName + "Both", new TypeRef<TestTemplateClass>() {},
            "{{% if field and field2 }}yes{{% else }}no{{% end }}", STRING, null ).render( c ).get() )
            .isEqualTo( "yes" );

        c.field2 = "";
        assertThat( getTemplate( testMethodName + "SecondEmpty", new TypeRef<TestTemplateClass>() {},
            "{{% if field and field2 }}yes{{% else }}no{{% end }}", STRING, null ).render( c ).get() )
            .isEqualTo( "no" );

        c.field = null;
        c.field2 = "2";
        assertThat( getTemplate( testMethodName + "FirstNull", new TypeRef<TestTemplateClass>() {},
            "{{% if field or field2 }}yes{{% else }}no{{% end }}", STRING, null ).render( c ).get() )
            .isEqualTo( "yes" );

        c.field = "1";
        assertThat( getTemplate( testMethodName + "Single", new TypeRef<TestTemplateClass>() {},
            "{{% if field }}yes{{% else }}no{{% end }}", STRING, null ).render( c ).get() )
            .isEqualTo( "yes" );
    }

    @Test
    public void testBlockIfAndFalse() {
        TestTemplateClass c = new TestTemplateClass();
        c.booleanField = true;
        c.booleanObjectField = false;
        assertThat( getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {},
            "{{% if booleanField and booleanObjectField }}yes{{% else }}no{{% end }}", STRING, null ).render( c ).get() )
            .isEqualTo( "no" );
    }

    @Test
    public void testBlockIfOr() {
        TestTemplateClass c = new TestTemplateClass();
        c.booleanField = false;
        c.booleanObjectField = true;
        assertThat( getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {},
            "{{% if booleanField or booleanObjectField }}yes{{% else }}no{{% end }}", STRING, null ).render( c ).get() )
            .isEqualTo( "yes" );
    }

    @Test
    public void testBlockIfOrFalse() {
        TestTemplateClass c = new TestTemplateClass();
        c.booleanField = false;
        c.booleanObjectField = false;
        assertThat( getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {},
            "{{% if booleanField or booleanObjectField }}yes{{% else }}no{{% end }}", STRING, null ).render( c ).get() )
            .isEqualTo( "no" );
    }

    @Test
    public void testBlockIfNot() {
        TestTemplateClass c = new TestTemplateClass();
        c.booleanField = false;
        assertThat( getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {},
            "{{% if not booleanField }}yes{{% else }}no{{% end }}", STRING, null ).render( c ).get() )
            .isEqualTo( "yes" );
    }

    @Test
    public void testBlockIfBangNot() {
        TestTemplateClass c = new TestTemplateClass();
        c.booleanField = true;
        assertThat( getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {},
            "{{% if !booleanField }}yes{{% else }}no{{% end }}", STRING, null ).render( c ).get() )
            .isEqualTo( "no" );
    }

    @Test
    public void testBlockIfComplex() {
        TestTemplateClass c = new TestTemplateClass();
        c.booleanField = true;
        c.booleanObjectField = true;
        c.field = "check";
        assertThat( getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {},
            "{{% if (booleanField and booleanObjectField) and not booleanField }}yes{{% else }}no{{% end }}", STRING, null ).render( c ).get() )
            .isEqualTo( "no" );
    }

    @Test
    public void testBlockIfComplexTrue() {
        TestTemplateClass c = new TestTemplateClass();
        c.booleanField = true;
        c.booleanObjectField = true;
        assertThat( getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {},
            "{{% if (booleanField and booleanObjectField) and not booleanObjectField }}yes{{% else }}no{{% end }}", STRING, null ).render( c ).get() )
            .isEqualTo( "no" );
    }

    // --- comparison operators ---

    @Test
    public void testBlockIfEqString() {
        TestTemplateClass c = new TestTemplateClass();
        c.field = "test";
        assertThat( getTemplate( testMethodName + "Match", new TypeRef<TestTemplateClass>() {},
            "{{% if field == 'test' %}}yes{{% else }}no{{% end }}", STRING, null ).render( c ).get() )
            .isEqualTo( "yes" );
        c.field = "other";
        assertThat( getTemplate( testMethodName + "NoMatch", new TypeRef<TestTemplateClass>() {},
            "{{% if field == 'test' %}}yes{{% else }}no{{% end }}", STRING, null ).render( c ).get() )
            .isEqualTo( "no" );
    }

    @Test
    public void testBlockIfEqStringKeyword() {
        TestTemplateClass c = new TestTemplateClass();
        c.field = "test";
        assertThat( getTemplate( testMethodName + "Match", new TypeRef<TestTemplateClass>() {},
            "{{% if field eq 'test' %}}yes{{% else }}no{{% end }}", STRING, null ).render( c ).get() )
            .isEqualTo( "yes" );
    }

    @Test
    public void testBlockIfEqInt() {
        TestTemplateClass c = new TestTemplateClass();
        c.intField = 5;
        assertThat( getTemplate( testMethodName + "Match", new TypeRef<TestTemplateClass>() {},
            "{{% if intField == 5 %}}yes{{% else }}no{{% end }}", STRING, null ).render( c ).get() )
            .isEqualTo( "yes" );
        c.intField = 6;
        assertThat( getTemplate( testMethodName + "NoMatch", new TypeRef<TestTemplateClass>() {},
            "{{% if intField == 5 %}}yes{{% else }}no{{% end }}", STRING, null ).render( c ).get() )
            .isEqualTo( "no" );
    }

    @Test
    public void testBlockIfNeString() {
        TestTemplateClass c = new TestTemplateClass();
        c.field = "other";
        assertThat( getTemplate( testMethodName + "Match", new TypeRef<TestTemplateClass>() {},
            "{{% if field != 'test' %}}yes{{% else }}no{{% end }}", STRING, null ).render( c ).get() )
            .isEqualTo( "yes" );
        c.field = "test";
        assertThat( getTemplate( testMethodName + "NoMatch", new TypeRef<TestTemplateClass>() {},
            "{{% if field != 'test' %}}yes{{% else }}no{{% end }}", STRING, null ).render( c ).get() )
            .isEqualTo( "no" );
    }

    @Test
    public void testBlockIfNeKeyword() {
        TestTemplateClass c = new TestTemplateClass();
        c.intField = 6;
        assertThat( getTemplate( testMethodName + "Match", new TypeRef<TestTemplateClass>() {},
            "{{% if intField ne 5 %}}yes{{% else }}no{{% end }}", STRING, null ).render( c ).get() )
            .isEqualTo( "yes" );
    }

    @Test
    public void testBlockIfGt() {
        TestTemplateClass c = new TestTemplateClass();
        c.intField = 6;
        assertThat( getTemplate( testMethodName + "Match", new TypeRef<TestTemplateClass>() {},
            "{{% if intField > 5 %}}yes{{% else }}no{{% end }}", STRING, null ).render( c ).get() )
            .isEqualTo( "yes" );
        c.intField = 5;
        assertThat( getTemplate( testMethodName + "Equal", new TypeRef<TestTemplateClass>() {},
            "{{% if intField > 5 %}}yes{{% else }}no{{% end }}", STRING, null ).render( c ).get() )
            .isEqualTo( "no" );
    }

    @Test
    public void testBlockIfLt() {
        TestTemplateClass c = new TestTemplateClass();
        c.intField = 4;
        assertThat( getTemplate( testMethodName + "Match", new TypeRef<TestTemplateClass>() {},
            "{{% if intField < 5 %}}yes{{% else }}no{{% end }}", STRING, null ).render( c ).get() )
            .isEqualTo( "yes" );
        c.intField = 5;
        assertThat( getTemplate( testMethodName + "Equal", new TypeRef<TestTemplateClass>() {},
            "{{% if intField < 5 %}}yes{{% else }}no{{% end }}", STRING, null ).render( c ).get() )
            .isEqualTo( "no" );
    }

    @Test
    public void testBlockIfGe() {
        TestTemplateClass c = new TestTemplateClass();
        c.intField = 5;
        assertThat( getTemplate( testMethodName + "Equal", new TypeRef<TestTemplateClass>() {},
            "{{% if intField >= 5 %}}yes{{% else }}no{{% end }}", STRING, null ).render( c ).get() )
            .isEqualTo( "yes" );
        c.intField = 4;
        assertThat( getTemplate( testMethodName + "Less", new TypeRef<TestTemplateClass>() {},
            "{{% if intField >= 5 %}}yes{{% else }}no{{% end }}", STRING, null ).render( c ).get() )
            .isEqualTo( "no" );
    }

    @Test
    public void testBlockIfLe() {
        TestTemplateClass c = new TestTemplateClass();
        c.intField = 5;
        assertThat( getTemplate( testMethodName + "Equal", new TypeRef<TestTemplateClass>() {},
            "{{% if intField <= 5 %}}yes{{% else }}no{{% end }}", STRING, null ).render( c ).get() )
            .isEqualTo( "yes" );
        c.intField = 6;
        assertThat( getTemplate( testMethodName + "Greater", new TypeRef<TestTemplateClass>() {},
            "{{% if intField <= 5 %}}yes{{% else }}no{{% end }}", STRING, null ).render( c ).get() )
            .isEqualTo( "no" );
    }

    @Test
    public void testBlockIfEqi() {
        TestTemplateClass c = new TestTemplateClass();
        c.field = "Test";
        assertThat( getTemplate( testMethodName + "Match", new TypeRef<TestTemplateClass>() {},
            "{{% if field eqi 'test' %}}yes{{% else }}no{{% end }}", STRING, null ).render( c ).get() )
            .isEqualTo( "yes" );
        c.field = "other";
        assertThat( getTemplate( testMethodName + "NoMatch", new TypeRef<TestTemplateClass>() {},
            "{{% if field eqi 'test' %}}yes{{% else }}no{{% end }}", STRING, null ).render( c ).get() )
            .isEqualTo( "no" );
    }

    @Test
    public void testBlockIfContainsListString() {
        TestTemplateClass c = new TestTemplateClass();
        c.listString = new ArrayList<>( List.of( "a", "b", "c" ) );
        assertThat( getTemplate( testMethodName + "Present", new TypeRef<TestTemplateClass>() {},
            "{{% if listString contains 'b' %}}yes{{% else }}no{{% end }}", STRING, null ).render( c ).get() )
            .isEqualTo( "yes" );
        assertThat( getTemplate( testMethodName + "Absent", new TypeRef<TestTemplateClass>() {},
            "{{% if listString contains 'z' %}}yes{{% else }}no{{% end }}", STRING, null ).render( c ).get() )
            .isEqualTo( "no" );
    }

    @Test
    public void testBlockIfContainsListInteger() {
        TestTemplateClass c = new TestTemplateClass();
        c.list = new ArrayList<>( List.of( 1, 2, 3 ) );
        assertThat( getTemplate( testMethodName + "Present", new TypeRef<TestTemplateClass>() {},
            "{{% if list contains 2 %}}yes{{% else }}no{{% end }}", STRING, null ).render( c ).get() )
            .isEqualTo( "yes" );
        assertThat( getTemplate( testMethodName + "Absent", new TypeRef<TestTemplateClass>() {},
            "{{% if list contains 9 %}}yes{{% else }}no{{% end }}", STRING, null ).render( c ).get() )
            .isEqualTo( "no" );
    }

    @Test
    public void testBlockIfContainsListDouble() {
        TestTemplateClass c = new TestTemplateClass();
        c.listDouble = new ArrayList<>( List.of( 1.1, 2.2, 3.3 ) );
        assertThat( getTemplate( testMethodName + "Present", new TypeRef<TestTemplateClass>() {},
            "{{% if listDouble contains 2.2 %}}yes{{% else }}no{{% end }}", STRING, null ).render( c ).get() )
            .isEqualTo( "yes" );
        assertThat( getTemplate( testMethodName + "Absent", new TypeRef<TestTemplateClass>() {},
            "{{% if listDouble contains 9.9 %}}yes{{% else }}no{{% end }}", STRING, null ).render( c ).get() )
            .isEqualTo( "no" );
    }

    @Test
    public void testBlockIfContainsMapString() {
        TestTemplateClass c = new TestTemplateClass();
        c.mapItems.put( "k1", new TestTemplateClass() );
        c.mapItems.put( "k2", new TestTemplateClass() );
        assertThat( getTemplate( testMethodName + "Present", new TypeRef<TestTemplateClass>() {},
            "{{% if mapItems contains 'k1' %}}yes{{% else }}no{{% end }}", STRING, null ).render( c ).get() )
            .isEqualTo( "yes" );
        assertThat( getTemplate( testMethodName + "Absent", new TypeRef<TestTemplateClass>() {},
            "{{% if mapItems contains 'missing' %}}yes{{% else }}no{{% end }}", STRING, null ).render( c ).get() )
            .isEqualTo( "no" );
    }

    @Test
    public void testBlockIfContainsMapInteger() {
        TestTemplateClass c = new TestTemplateClass();
        c.mapIntItems.put( 1, "one" );
        c.mapIntItems.put( 2, "two" );
        assertThat( getTemplate( testMethodName + "Present", new TypeRef<TestTemplateClass>() {},
            "{{% if mapIntItems contains 1 %}}yes{{% else }}no{{% end }}", STRING, null ).render( c ).get() )
            .isEqualTo( "yes" );
        assertThat( getTemplate( testMethodName + "Absent", new TypeRef<TestTemplateClass>() {},
            "{{% if mapIntItems contains 9 %}}yes{{% else }}no{{% end }}", STRING, null ).render( c ).get() )
            .isEqualTo( "no" );
    }

    @Test
    public void testBlockIfCombinedEqOrNe() {
        TestTemplateClass c = new TestTemplateClass();
        c.field = "test";
        c.intField = 6;
        assertThat( getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {},
            "{{% if field == 'test' or intField != 5 %}}yes{{% else }}no{{% end }}", STRING, null ).render( c ).get() )
            .isEqualTo( "yes" );
    }

    @Test
    public void testInlineIfCompare() {
        TestTemplateClass c = new TestTemplateClass();
        c.intField = 6;
        c.field = "hello";
        c.field2 = "world";
        assertThat( getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {},
            "{{ if intField == 6 then field else field2 end }}", STRING, null ).render( c ).get() )
            .isEqualTo( "hello" );
    }

}
