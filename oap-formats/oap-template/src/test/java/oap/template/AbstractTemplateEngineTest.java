package oap.template;

import oap.reflect.TypeRef;
import oap.testng.Fixtures;
import oap.testng.TestDirectoryFixture;
import oap.util.Dates;
import org.testng.annotations.BeforeMethod;

import java.lang.reflect.Method;
import java.util.Map;
import java.util.function.Consumer;

import oap.template.render.AstRender;

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

    /** Override to {@code true} in subclasses that run against the runtime interpreter. */
    protected boolean useRuntime() {
        return false;
    }

    protected <TIn, TOut, TOutMutable, TA extends TemplateAccumulator<TOut, TOutMutable, TA>>
    Template<TIn, TOut, TOutMutable, TA> getTemplate( String name, TypeRef<TIn> type, String template, TA acc,
                                                       Consumer<AstRender> postProcess ) {
        return useRuntime()
            ? engine.getRuntimeTemplate( name, type, template, acc, postProcess )
            : engine.getTemplate( name, type, template, acc, postProcess );
    }

    protected <TIn, TOut, TOutMutable, TA extends TemplateAccumulator<TOut, TOutMutable, TA>>
    Template<TIn, TOut, TOutMutable, TA> getTemplate( String name, TypeRef<TIn> type, String template, TA acc,
                                                       Map<String, String> aliases, Consumer<AstRender> postProcess ) {
        return useRuntime()
            ? engine.getRuntimeTemplate( name, type, template, acc, aliases, postProcess )
            : engine.getTemplate( name, type, template, acc, aliases, postProcess );
    }

    protected <TIn, TOut, TOutMutable, TA extends TemplateAccumulator<TOut, TOutMutable, TA>>
    Template<TIn, TOut, TOutMutable, TA> getTemplate( String name, TypeRef<TIn> type, String template, TA acc,
                                                       ErrorStrategy errorStrategy, Consumer<AstRender> postProcess ) {
        return useRuntime()
            ? engine.getRuntimeTemplate( name, type, template, acc, errorStrategy, postProcess )
            : engine.getTemplate( name, type, template, acc, errorStrategy, postProcess );
    }

    protected <TIn, TOut, TOutMutable, TA extends TemplateAccumulator<TOut, TOutMutable, TA>>
    Template<TIn, TOut, TOutMutable, TA> getTemplate( String name, TypeRef<TIn> type, String template, TA acc,
                                                       Map<String, String> aliases, ErrorStrategy errorStrategy ) {
        return useRuntime()
            ? engine.getRuntimeTemplate( name, type, template, acc, aliases, errorStrategy )
            : engine.getTemplate( name, type, template, acc, aliases, errorStrategy );
    }

    protected <TIn, TOut, TOutMutable, TA extends TemplateAccumulator<TOut, TOutMutable, TA>>
    Template<TIn, TOut, TOutMutable, TA> getTemplate( String name, TypeRef<TIn> type, String template, TA acc,
                                                       Map<String, String> aliases, ErrorStrategy errorStrategy,
                                                       Consumer<AstRender> postProcess ) {
        return useRuntime()
            ? engine.getRuntimeTemplate( name, type, template, acc, aliases, errorStrategy, postProcess )
            : engine.getTemplate( name, type, template, acc, aliases, errorStrategy, postProcess );
    }
}
