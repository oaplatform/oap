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
import oap.reflect.Reflect;
import oap.reflect.TypeRef;
import oap.template.LogConfiguration.FieldType;
import oap.testng.Fixtures;
import oap.testng.TestDirectoryFixture;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import static oap.io.content.ContentWriter.ofString;
import static org.assertj.core.api.Assertions.assertThat;

public class LogConfigurationTest extends Fixtures {
    private TemplateEngine engine;
    private String testMethodName;

    public LogConfigurationTest() {
        fixture( TestDirectoryFixture.FIXTURE );
    }

    private static AstExpression aexp( String content, Ast ast ) {
        return new AstExpression( ast, content );
    }

    private static AstPrint aps() {
        return new AstPrint( new TemplateType( String.class ), "" );
    }

    private static AstField af( String fieldName, Ast ast ) {
        var astField = new AstField( fieldName, new TemplateType( String.class, false ), false, null );
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
        var dictionaryTemplate = logConfiguration.forType( new TypeRef<TestTemplateClass>() {}, "TEST", TemplateAccumulators.STRING );

        var c = new TestTemplateClass();
        c.list = List.of( 1, 2 );

        var res = dictionaryTemplate.templateFunction.render( c ).get();
        assertThat( res ).isEqualTo( "[1,2]" );
    }

    @Test
    public void testTypeSetString() {
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
                      type = STRING_ARRAY
                      default = []
                      path = child.setString
                      tags = [LOG]
                    }
                  ]
                }
              ]
            }
            """, ofString() );

        var logConfiguration = new LogConfiguration( engine, TestDirectoryFixture.testPath( "conf" ) );
        var dictionaryTemplate = logConfiguration.forType( new TypeRef<TestTemplateClass>() {}, "TEST", TemplateAccumulators.STRING );

        var c = new TestTemplateClass();
        c.child = new TestTemplateClass();
        c.child.setString = new LinkedHashSet<>( List.of( "s'1", "s2" ) );

        var res = dictionaryTemplate.templateFunction.render( c ).get();
        assertThat( res ).isEqualTo( "['s\\'1','s2']" );
    }

    @Test
    public void testConcatenation() {
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
        var dictionaryTemplate = logConfiguration.forType( new TypeRef<TestTemplateClass>() {}, "TEST", TemplateAccumulators.STRING );

        var c = new TestTemplateClass();
        c.booleanField = true;
        c.fieldNullable = "test";

        var res = dictionaryTemplate.templateFunction.render( c ).get();
        assertThat( res ).isEqualTo( "truextest" );
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
