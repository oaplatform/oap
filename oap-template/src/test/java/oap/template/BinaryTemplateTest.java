package oap.template;

import oap.reflect.TypeRef;
import oap.util.Strings;
import org.joda.time.DateTime;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.Map;

import static oap.template.TemplateAccumulators.BINARY;
import static org.assertj.core.api.Assertions.assertThat;
import static org.joda.time.DateTimeZone.UTC;

public class BinaryTemplateTest {
    private TemplateEngine engine;
    private String testMethodName;

    @BeforeClass
    public void beforeClass() {
        engine = new TemplateEngine();
    }

    @BeforeMethod
    public void nameBefore( Method method ) {
        testMethodName = method.getName();
    }

    @Test
    public void testRenderUNKNOWNStringTextAsBinary() throws IOException {
        assertThat( BinaryUtils.read( engine.getTemplate( testMethodName, new TypeRef<Map<String, String>>() {}, Strings.UNKNOWN, BINARY, null ).render( null ).get() ) )
            .isEqualTo( List.of( List.of( "" ) ) );
    }

    @Test
    public void testDefaultDoubleBinary() throws IOException {
        assertThat( BinaryUtils.read( engine.getTemplate( testMethodName, new TypeRef<Map<String, Double>>() {}, "${bbb??0.0}", BINARY, null ).render( Map.of( "prop", 1.1 ) ).get() ) )
            .isEqualTo( List.of( List.of( 0.0d ) ) );
    }

    @Test
    public void testDefaultDateTime() throws IOException {
        assertThat( BinaryUtils.read( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${dateTimeOptional??'2023-01-04 18:09:11'}", BINARY, null ).render( new TestTemplateClass() ).get() ) )
            .isEqualTo( List.of( List.of( new DateTime( 2023, 1, 4, 18, 9, 11, UTC ) ) ) );
    }

    @Test
    public void testOrDefaultBinary() throws IOException {
        var c = new TestTemplateClass();

        assertThat( BinaryUtils.read( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${intObjectField | childNullable.intObjectField??3}", BINARY, null ).render( c ).get() ) )
            .isEqualTo( List.of( List.of( 3 ) ) );
    }

    @Test
    public void testOrEmptyStringWithBinaryAccumulator() throws IOException {
        var c = new TestTemplateClass();
        c.field2 = "f2";

        assertThat( BinaryUtils.read( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${field | field2}", BINARY, null ).render( c ).get() ) )
            .isEqualTo( List.of( List.of( "f2" ) ) );
    }

    @Test
    public void testConcatenationBinary() throws IOException {
        var c = new TestTemplateClass();
        c.field = "f1";
        c.field2 = "f2";

        assertThat( BinaryUtils.read( engine.getTemplate( testMethodName, new TypeRef<TestTemplateClass>() {}, "${{field,\"x\",field2}}", BINARY, null ).render( c ).get() ) )
            .isEqualTo( List.of( List.of( "f1xf2" ) ) );
    }
}
