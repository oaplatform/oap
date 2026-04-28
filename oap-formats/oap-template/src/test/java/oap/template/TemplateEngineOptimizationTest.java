package oap.template;

import oap.reflect.TypeRef;
import org.testng.annotations.Test;

import static oap.template.TemplateAccumulators.STRING;
import static org.assertj.core.api.Assertions.assertThat;

public class TemplateEngineOptimizationTest extends AbstractTemplateEngineTest {
    @Test
    public void testBlockWithMultipleFields() {
        TestTemplateClass c = new TestTemplateClass();
        c.child = new TestTemplateClass();
        c.child.field = "f1";
        c.child.field2 = "f2";
        assertThat( getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {},
            "{{% with child }}{{ field }}-{{ field2 }}{{% end }}", STRING, null ).render( c ).get() )
            .isEqualTo( "f1-f2" );

        assertThat( listener.javaCode )
            .containsOnlyOnce( """
                    oap.template.TestTemplateClass s_child = s.child;
                    // --- with ( child ) START BODY
                    if ( s_child != null ) {
                """ )
            .containsOnlyOnce( """
                    } else {
                      acc.acceptNull( java.lang.String.class );
                      acc.acceptText( "-" );
                      acc.acceptNull( java.lang.String.class );
                    }
                """ );
    }

    @Test
    public void testBlockWithMultipleFieldsAndComments() {
        TestTemplateClass c = new TestTemplateClass();
        c.child = new TestTemplateClass();
        c.child.field = "f1";
        c.child.field2 = "f2";
        assertThat( getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {},
            "{{% with child }}{{ /** c field */ field }}-{{ /** c field2 */field2 }}{{% end }}", STRING, null ).render( c ).get() )
            .isEqualTo( "f1-f2" );

        assertThat( listener.javaCode ).containsOnlyOnce( "if ( s_child != null ) {" );
    }
}
