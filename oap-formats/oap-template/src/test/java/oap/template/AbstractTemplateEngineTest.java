package oap.template;

import oap.reflect.TypeRef;
import oap.template.render.AstRender;
import oap.testng.Fixtures;
import oap.testng.TestDirectoryFixture;
import oap.util.Dates;
import org.testng.annotations.BeforeMethod;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.function.Consumer;

public abstract class AbstractTemplateEngineTest extends Fixtures {
    protected final TestDirectoryFixture testDirectoryFixture;
    protected TemplateEngine engine;
    protected String testMethodName;

    protected MockTemplateEngineListener listener;

    protected AbstractTemplateEngineTest() {
        testDirectoryFixture = fixture( new TestDirectoryFixture() );
    }

    @BeforeMethod
    public void beforeCMethod() {
        engine = new TemplateEngine( Dates.d( 10 ) );

        listener = new MockTemplateEngineListener();
    }

    @BeforeMethod
    public void nameBefore( Method method ) {
        testMethodName = method.getName();
    }

    /**
     * Override to {@code true} in subclasses that run against the runtime interpreter.
     */
    protected boolean useRuntime() {
        return false;
    }

    protected <TIn, TOut, TOutMutable, TA extends TemplateAccumulator<TOut, TOutMutable, TA>>
    Template<TIn, TOut, TOutMutable, TA, ?> getTemplate( String name, TypeRef<TIn> type, String template, TA acc,
                                                         Consumer<AstRender> postProcess ) {
        return useRuntime()
            ? engine.getRuntimeTemplate( name, type, template, acc, postProcess, listener )
            : engine.getTemplate( name, type, template, acc, postProcess, listener );
    }

    protected <TIn, TOut, TOutMutable, TA extends TemplateAccumulator<TOut, TOutMutable, TA>>
    Template<TIn, TOut, TOutMutable, TA, ?> getTemplate( String name, TypeRef<TIn> type, String template, TA acc,
                                                         Map<String, String> aliases, Consumer<AstRender> postProcess ) {
        return useRuntime()
            ? engine.getRuntimeTemplate( name, type, template, acc, aliases, postProcess, listener )
            : engine.getTemplate( name, type, template, acc, aliases, postProcess, listener );
    }

    protected <TIn, TOut, TOutMutable, TA extends TemplateAccumulator<TOut, TOutMutable, TA>>
    Template<TIn, TOut, TOutMutable, TA, ?> getTemplate( String name, TypeRef<TIn> type, String template, TA acc,
                                                         ErrorStrategy errorStrategy, Consumer<AstRender> postProcess ) {
        return useRuntime()
            ? engine.getRuntimeTemplate( name, type, template, acc, errorStrategy, postProcess, listener )
            : engine.getTemplate( name, type, template, acc, errorStrategy, postProcess, listener );
    }

    protected <TIn, TOut, TOutMutable, TA extends TemplateAccumulator<TOut, TOutMutable, TA>>
    Template<TIn, TOut, TOutMutable, TA, ?> getTemplate( String name, TypeRef<TIn> type, String template, TA acc,
                                                         Map<String, String> aliases, ErrorStrategy errorStrategy ) {
        return useRuntime()
            ? engine.getRuntimeTemplate( name, type, template, acc, aliases, errorStrategy, listener )
            : engine.getTemplate( name, type, template, acc, aliases, errorStrategy, listener );
    }

    protected <TIn, TOut, TOutMutable, TA extends TemplateAccumulator<TOut, TOutMutable, TA>>
    Template<TIn, TOut, TOutMutable, TA, ?> getTemplate( String name, TypeRef<TIn> type, String template, TA acc,
                                                         Map<String, String> aliases, ErrorStrategy errorStrategy,
                                                         Consumer<AstRender> postProcess ) {
        return useRuntime()
            ? engine.getRuntimeTemplate( name, type, template, acc, aliases, errorStrategy, postProcess, listener )
            : engine.getTemplate( name, type, template, acc, aliases, errorStrategy, postProcess, listener );
    }

    public static class MockTemplateEngineListener implements TemplateEngineListener {
        public String javaCode;

        @Override
        public void javaCode( String javaCode ) {
            this.javaCode = javaCode;
        }
    }
}
