package oap.template;

import oap.testng.Fixtures;
import oap.testng.TestDirectoryFixture;
import oap.util.Dates;
import org.testng.annotations.BeforeMethod;

import java.lang.reflect.Method;

public abstract class AbstractTemplateEngineTest extends Fixtures {
    protected final TestDirectoryFixture testDirectoryFixture;
    protected TemplateEngine engine;
    protected String testMethodName;

    protected AbstractTemplateEngineTest() {
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
}
