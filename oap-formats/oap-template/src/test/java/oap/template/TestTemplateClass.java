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

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import oap.json.ext.Ext;
import org.joda.time.DateTime;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.List;
import java.util.Optional;
import java.util.Set;

public class TestTemplateClass {
    public String field;
    public Optional<String> fieldOpt = Optional.empty();
    @Nullable
    public String fieldNullable;
    public String field2;
    public TestTemplateClass child;
    public TestTemplateClass2 child2;
    public TestTemplateEnum enumField;
    public TestTemplateEnumWithoutDefaultValue enumFieldWithoutDefaultValue;
    @Nonnull
    public TestTemplateEnum nonNullEnumField = TestTemplateEnum.VAL1;
    public boolean booleanField;
    public Boolean booleanObjectField;
    public short byteField;
    public Byte byteObjectField;
    public short shortField;
    public Short shortObjectField;
    public int intField;
    public Integer intObjectField;
    public long longField;
    public Long longObjectField;
    public float floatField;
    public Float floatObjectField;
    public double doubleField;
    public Double doubleObjectField;
    public Optional<TestTemplateClass> childOpt = Optional.empty();
    @Nullable
    public TestTemplateClass childNullable;
    @Nullable
    public Ext ext;
    public ITestTemplateClassExt ext2;
    public TestTemplateClassExt ext3 = new TestTemplateClassExt();
    @Nullable
    public List<Integer> list;
    @Nullable
    public List<Integer> list2;
    public List<String> listString;

    public DateTime dateTime;
    public Optional<DateTime> dateTimeOptional = Optional.empty();
    public List<TestTemplateEnum> listEnum;
    public Set<String> setString;
    @JsonProperty( "jsonTestNew" )
    @JsonAlias( { "jsonTestAlias1", "jsonTestAlias2" } )
    public String jsonTest;

    public String fieldM() {
        return field;
    }

    public String fieldMInt( int value ) {
        return field + "-" + value;
    }

    public String fieldMDouble( double value ) {
        return field + "-" + value;
    }

    public String fieldMString( String value ) {
        return field + "-" + value;
    }

    public TestTemplateClass childM() {
        return child;
    }

    public static class ITestTemplateClassExt extends Ext {

    }
}
