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
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static oap.io.content.ContentWriter.ofString;
import static org.assertj.core.api.Assertions.assertThat;

public class LogConfigurationTest extends Fixtures {
    private final ThreadLocalStringBuilder threadLocalStringBuilder = new ThreadLocalStringBuilder();
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
    public void testConsumerCompact() {
        var exp1 = aexp( "a.b", af( "a", aopt( af( "b", aps() ) ) ) );
        var exp2 = aexp( "a.c", af( "a", aopt( af( "c", aps() ) ) ) );

        var ar = new AstRoot( new TemplateType( getClass() ) );
        ar.children.addAll( List.of( exp1, new AstText( "123" ), exp2 ) );

        CompactAstPostProcessor.INSTANCE.accept( ar );

        System.out.println( ar.print() );

        assertThat( ar.children ).hasSize( 1 );
        assertThat( ar.children.get( 0 ).children ).hasSize( 1 );
        assertThat( ar.children.get( 0 ).children.get( 0 ).children ).hasSize( 1 );
        assertThat( ar.children.get( 0 ).children.get( 0 ).children.get( 0 ) ).isInstanceOf( AstOptional.class );
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
        logConfiguration.compact = true;
        var dictionaryTemplate = logConfiguration.forType( new TypeRef<TestTemplateClass>() {}, "TEST" );

        var c = new TestTemplateClass();
        c.list = List.of( 1, 2 );

        var res = dictionaryTemplate.templateFunction.render( c );
        assertThat( res ).isEqualTo( "[1,2]" );
    }

    @Test
    public void testCompact() {
        Files.write( TestDirectoryFixture.testPath( "conf/config.v1.conf" ), """
            {
              name = config.v1
              version = 1
              values = [
                {
                  id = TEST
                  values = [
                    {
                      id = c_n_str
                      type = STRING
                      default = ""
                      path = childOpt.fieldOpt
                      tags = [LOG]
                    }
                    {
                      id = i_int
                      type = INTEGER
                      default = 0
                      path = intField
                      tags = [LOG]
                    }
                    {
                      id = c_i_int
                      type = INTEGER
                      default = 0
                      path = childOpt.intField
                      tags = [LOG]
                    }
                    {
                      id = f_str
                      type = STRING
                      default = ""
                      path = fieldNullable
                      tags = [LOG]
                    }
                    {
                      id = c_f_str
                      type = STRING
                      default = ""
                      path = childOpt.fieldNullable
                      tags = [LOG]
                    }
                    {
                      id = c_c_i_int
                      type = INTEGER
                      default = 0
                      path = childOpt.childNullable.intField
                      tags = [LOG]
                    }
                    {
                      id = c_c_n_str
                      type = STRING
                      default = ""
                      path = childOpt.childNullable.fieldOpt
                      tags = [LOG]
                    }
                  ]
                }
              ]
            }
            """, ofString() );

        var logConfiguration = new LogConfiguration( engine, TestDirectoryFixture.testPath( "conf" ) );
        logConfiguration.compact = true;
        var dictionaryTemplate = logConfiguration.forType( new TypeRef<TestTemplateClass>() {}, "TEST" );

        var c = new TestTemplateClass();
        var cp = new TestTemplateClass();
        var cp2 = new TestTemplateClass();
        c.fieldOpt = Optional.of( "o" );
        c.intField = 10;
        c.fieldNullable = "a";

        cp2.fieldOpt = Optional.of( "o2" );
        cp2.intField = 20;
        cp2.fieldNullable = "a2";

        cp.intField = 5;
        cp.fieldNullable = "b";

        cp.childOpt = Optional.of( c );
        c.childNullable = cp2;

        var res = dictionaryTemplate.templateFunction.render( cp );

        assertThat( res ).isEqualTo( "10\to\ta\t20\to2\tb\t5" );
    }

    @Test
    public void testCompactOr() {
        Files.write( TestDirectoryFixture.testPath( "conf/config.v1.conf" ), """
            {
              name = config.v1
              version = 1
              values = [
                {
                  id = TEST
                  values = [
                    {
                      id = c_n_str
                      type = STRING
                      default = ""
                      path = childOpt.fieldOpt|childNullable.fieldOpt
                      tags = [LOG]
                    }
                    {
                      id = c_i_int
                      type = INTEGER
                      default = 0
                      path = childOpt.intField
                      tags = [LOG]
                    }
                  ]
                }
              ]
            }
            """, ofString() );

        var logConfiguration = new LogConfiguration( engine, TestDirectoryFixture.testPath( "conf" ) );
        logConfiguration.compact = true;
        var dictionaryTemplate = logConfiguration.forType( new TypeRef<TestTemplateClass>() {}, "TEST" );

        var cOpt = new TestTemplateClass();
        var cNull = new TestTemplateClass();
        var cp = new TestTemplateClass();

        cOpt.fieldOpt = Optional.empty();
        cOpt.intField = 10;

        cNull.fieldOpt = Optional.of( "1" );

        cp.childOpt = Optional.of( cOpt );
        cp.childNullable = cNull;

        StringBuilder out = threadLocalStringBuilder.get();
        dictionaryTemplate.templateFunction.render( cp, out );

        assertThat( out.toString() ).isEqualTo( "1\t10" );
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
