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

import oap.io.Files;
import oap.lang.ThreadLocalStringBuilder;
import oap.reflect.Reflect;
import oap.reflect.TypeRef;
import oap.testng.Fixtures;
import oap.testng.TestDirectoryFixture;
import org.joda.time.DateTime;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import static oap.io.content.ContentWriter.ofString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.joda.time.DateTimeZone.UTC;

public class LogConfigurationTest extends Fixtures {
    private final ThreadLocalStringBuilder threadLocalStringBuilder = new ThreadLocalStringBuilder();
    private TemplateEngine engine;
    private String testMethodName;

    public LogConfigurationTest() {
        fixture( TestDirectoryFixture.FIXTURE );
    }

    private static AstExpression aexp( String content, Ast ast ) {
        return new AstExpression( ast, content, null );
    }

    private static AstPrint aps() {
        return new AstPrint( new TemplateType( String.class ), null );
    }

    private static AstField af( String fieldName, Ast ast ) {
        var astField = new AstField( fieldName, new TemplateType( String.class, false ), false );
        astField.children.add( ast );
        return astField;
    }

    public static AstOptional aopt( Ast ast ) {
        var astOptional = new AstOptional( new TemplateType( String.class, false ) );
        astOptional.children.add( ast );
        return astOptional;
    }

    public static AstText at( String value ) {
        return new AstText( value );
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
    public void testTypeListInteger() {
        java.util.Collection<java.lang.Integer> a = new ArrayList<>();
        Files.write( TestDirectoryFixture.testPath( "conf/config.v1.conf" ), """
            {
              name = config.v1
              version = 1
              values = [
                {
                  id = TEST
                  values = [
                    {
                      id = LIST_FIELD
                      type = INTEGER_ARRAY
                      default = []
                      path = list
                      tags = [LOG]
                    }
                  ]
                }
              ]
            }
            """, ofString() );

        var logConfiguration = new LogConfiguration( engine, TestDirectoryFixture.testPath( "conf" ) );
        var dictionaryTemplate = logConfiguration.forType( new TypeRef<TestTemplateClass>() {}, "TEST" );

        var c = new TestTemplateClass();
        c.list = List.of( 1, 2 );

        var res = dictionaryTemplate.templateFunction.render( c );
        assertThat( res ).isEqualTo( "[1,2]" );
    }

    @Test
    public void testTypeSetInteger() {
        java.util.Collection<java.lang.Integer> a = new ArrayList<>();
        Files.write( TestDirectoryFixture.testPath( "conf/config.v1.conf" ), """
            {
              name = config.v1
              version = 1
              values = [
                {
                  id = TEST
                  values = [
                    {
                      id = SET_FIELD
                      type = INTEGER_ARRAY
                      default = []
                      path = set
                      tags = [LOG]
                    }
                  ]
                }
              ]
            }
            """, ofString() );

        var logConfiguration = new LogConfiguration( engine, TestDirectoryFixture.testPath( "conf" ) );
        var dictionaryTemplate = logConfiguration.forType( new TypeRef<TestTemplateClass>() {}, "TEST" );

        var c = new TestTemplateClass();

        var res = dictionaryTemplate.templateFunction.render( c );
        assertThat( res ).isEqualTo( "[]" );
    }

    @Test
    public void testConcatenation() {
        java.util.Collection<java.lang.Integer> a = new ArrayList<>();
        Files.write( TestDirectoryFixture.testPath( "conf/config.v1.conf" ), """
            {
              name = config.v1
              version = 1
              values = [
                {
                  id = TEST
                  values = [
                    {
                      id = CFIELD
                      type = STRING
                      default = ""
                      path = "{booleanField, \\"x\\", fieldNullable}"
                      tags = [LOG]
                    }
                  ]
                }
              ]
            }
            """, ofString() );

        var logConfiguration = new LogConfiguration( engine, TestDirectoryFixture.testPath( "conf" ) );
        var dictionaryTemplate = logConfiguration.forType( new TypeRef<TestTemplateClass>() {}, "TEST" );

        var c = new TestTemplateClass();
        c.booleanField = true;
        c.fieldNullable = "test";

        var res = dictionaryTemplate.templateFunction.render( c );
        assertThat( res ).isEqualTo( "truextest" );
    }

    @Test
    public void testDefaultValueTypeCastException() {
        java.util.Collection<java.lang.Integer> a = new ArrayList<>();
        Files.write( TestDirectoryFixture.testPath( "conf/config.v1.conf" ), """
            {
              name = config.v1
              version = 1
              values = [
                {
                  id = TEST
                  values = [
                    {
                      id = CFIELD
                      type = BOOLEAN
                      default = 0
                      path = booleanObjectField
                      tags = [LOG]
                    }
                  ]
                }
              ]
            }
            """, ofString() );

        var logConfiguration = new LogConfiguration( engine, TestDirectoryFixture.testPath( "conf" ) );
        assertThatThrownBy( () -> logConfiguration.forType( new TypeRef<TestTemplateClass>() {}, "TEST" ) )
            .isInstanceOf( TemplateException.class );
    }

    @Test
    public void testDefaultValueTypeInteger() {
        java.util.Collection<java.lang.Integer> a = new ArrayList<>();
        Files.write( TestDirectoryFixture.testPath( "conf/config.v1.conf" ), """
            {
              name = config.v1
              version = 1
              values = [
                {
                  id = TEST
                  values = [
                    {
                      id = CFIELD
                      type = INTEGER
                      default = 2
                      path = intObjectField
                      tags = [LOG]
                    }
                  ]
                }
              ]
            }
            """, ofString() );

        var logConfiguration = new LogConfiguration( engine, TestDirectoryFixture.testPath( "conf" ) );
        var dictionaryTemplate = logConfiguration.forType( new TypeRef<TestTemplateClass>() {}, "TEST" );

        var c = new TestTemplateClass();

        var res = dictionaryTemplate.templateFunction.render( c );
        assertThat( res ).isEqualTo( "2" );
    }

    @Test
    public void testFieldTypeCastException() {
        java.util.Collection<java.lang.Integer> a = new ArrayList<>();
        Files.write( TestDirectoryFixture.testPath( "conf/config.v1.conf" ), """
            {
              name = config.v1
              version = 1
              values = [
                {
                  id = TEST
                  values = [
                    {
                      id = CFIELD
                      type = STRING
                      default = false
                      path = childOpt.booleanObjectField
                      tags = [LOG]
                    }
                  ]
                }
              ]
            }
            """, ofString() );

        var logConfiguration = new LogConfiguration( engine, TestDirectoryFixture.testPath( "conf" ) );
        assertThatThrownBy( () -> logConfiguration.forType( new TypeRef<TestTemplateClass>() {}, "TEST" ) )
            .isInstanceOf( TemplateException.class );
    }

    @Test
    public void testFieldTypeDateTimeDefault() {
        java.util.Collection<java.lang.Integer> a = new ArrayList<>();
        Files.write( TestDirectoryFixture.testPath( "conf/config.v1.conf" ), """
            {
              name = config.v1
              version = 1
              values = [
                {
                  id = TEST
                  values = [
                    {
                      id = DATETIME
                      type = DATETIME
                      default = "2022-09-07 14:32:12"
                      format = "YYYY-MM-dd HH:mm:ss"
                      path = dateTime
                      tags = [LOG]
                    }
                  ]
                }
              ]
            }
            """, ofString() );

        var logConfiguration = new LogConfiguration( engine, TestDirectoryFixture.testPath( "conf" ) );
        var dictionaryTemplate = logConfiguration.forType( new TypeRef<TestTemplateClass>() {}, "TEST" );

        var c = new TestTemplateClass();

        var res = dictionaryTemplate.templateFunction.render( c );
        assertThat( res ).isEqualTo( "2022-09-07 14:32:12" );
    }

    @Test
    public void testFieldTypeDateTimeMethod() {
        java.util.Collection<java.lang.Integer> a = new ArrayList<>();
        Files.write( TestDirectoryFixture.testPath( "conf/config.v1.conf" ), """
            {
              name = config.v1
              version = 1
              values = [
                {
                  id = TEST
                  values = [
                    {
                      id = DATETIME
                      type = DATETIME
                      default = "2022-09-07 14:32"
                      format = "YYYY-MM-dd HH:mm"
                      path = dateTime()
                      tags = [LOG]
                    }
                  ]
                }
              ]
            }
            """, ofString() );

        var logConfiguration = new LogConfiguration( engine, TestDirectoryFixture.testPath( "conf" ) );
        var dictionaryTemplate = logConfiguration.forType( new TypeRef<TestTemplateClass>() {}, "TEST" );

        var c = new TestTemplateClass();
        c.dateTime = new DateTime( 2022, 9, 7, 18, 59, 1, UTC );

        var res = dictionaryTemplate.templateFunction.render( c );
        assertThat( res ).isEqualTo( "2022-09-07 18:59" );
    }

    @Test
    public void testFieldTypeEnumDefault() {
        java.util.Collection<java.lang.Integer> a = new ArrayList<>();
        Files.write( TestDirectoryFixture.testPath( "conf/config.v1.conf" ), """
            {
              name = config.v1
              version = 1
              values = [
                {
                  id = TEST
                  values = [
                    {
                      id = TEST_ENUM
                      type = ENUM
                      default = ""
                      path = testEnum
                      tags = [LOG]
                    }
                  ]
                }
              ]
            }
            """, ofString() );

        var logConfiguration = new LogConfiguration( engine, TestDirectoryFixture.testPath( "conf" ) );
        var dictionaryTemplate = logConfiguration.forType( new TypeRef<TestTemplateClass>() {}, "TEST" );

        var c = new TestTemplateClass();
        var res = dictionaryTemplate.templateFunction.render( c );
        assertThat( res ).isEqualTo( "" );
    }

    public void testFieldTypeDateTimeInvalid() {
        java.util.Collection<java.lang.Integer> a = new ArrayList<>();
        Files.write( TestDirectoryFixture.testPath( "conf/config.v1.conf" ), """
            {
              name = config.v1
              version = 1
              values = [
                {
                  id = TEST
                  values = [
                    {
                      id = DATETIME
                      type = DATETIME
                      default = "2022-09-07T14:32:12"
                      path = dateTime
                      tags = [LOG]
                    }
                  ]
                }
              ]
            }
            """, ofString() );

        var logConfiguration = new LogConfiguration( engine, TestDirectoryFixture.testPath( "conf" ) );
        assertThatThrownBy( () -> logConfiguration.forType( new TypeRef<TestTemplateClass>() {}, "TEST" ) )
            .isInstanceOf( TemplateException.class );
    }

    @Test
    public void testFieldTypeParse() throws ClassNotFoundException {
        assertThat( FieldType.parse( "java.lang.Integer" ) ).isEqualTo( new FieldType( Integer.class ) );
        assertThat( FieldType.parse( "java.util.Collection<java.lang.Integer>" ) )
            .isEqualTo( new FieldType( Collection.class, List.of( new FieldType( Integer.class ) ) ) );
        assertThat( FieldType.parse( "java.util.Collection<java.util.List<java.lang.Integer>>" ) )
            .isEqualTo( new FieldType( Collection.class, List.of( new FieldType( List.class, List.of( new FieldType( Integer.class ) ) ) ) ) );
        assertThat( FieldType.parse( "java.util.Map<java.lang.String,java.lang.Integer>" ) )
            .isEqualTo( new FieldType( Map.class, List.of( new FieldType( String.class ), new FieldType( Integer.class ) ) ) );
    }

    @Test
    public void testFieldTypeIsAssignableFrom() {
        assertThat( new FieldType( Integer.class ).isAssignableFrom( new TemplateType( Reflect.reflect( Integer.class ).getType() ) ) ).isTrue();
        assertThat( new FieldType( Integer.class ).isAssignableFrom( new TemplateType( Reflect.reflect( int.class ).getType() ) ) ).isTrue();

        assertThat( new FieldType( Integer.class ).isAssignableFrom( new TemplateType( Reflect.reflect( Long.class ).getType() ) ) ).isFalse();
        assertThat( new FieldType( Integer.class ).isAssignableFrom( new TemplateType( Reflect.reflect( long.class ).getType() ) ) ).isFalse();

        assertThat( new FieldType( Collection.class ).isAssignableFrom( new TemplateType( Reflect.reflect( List.class ).getType() ) ) ).isTrue();

        assertThat( new FieldType( Collection.class, List.of( new FieldType( String.class ) ) )
            .isAssignableFrom( new TemplateType( Reflect.reflect( new TypeRef<HashSet<String>>() {} ).getType() ) ) ).isTrue();
        assertThat( new FieldType( Collection.class, List.of( new FieldType( String.class ) ) )
            .isAssignableFrom( new TemplateType( Reflect.reflect( new TypeRef<HashSet<Integer>>() {} ).getType() ) ) ).isFalse();
    }
}
