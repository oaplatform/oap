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

        assertThat( listener.javaCode ).containsOnlyOnce( "if ( with_1 != null ) {" );
    }
}
