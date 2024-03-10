package oap.tools;

import io.micrometer.core.instrument.Tags;
import oap.metrics.MetricsFixture;
import oap.testng.Fixtures;
import oap.testng.TestDirectoryFixture;
import oap.util.Lists;
import oap.util.Result;
import org.testng.annotations.Test;

import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class TemplateClassCompilerTest extends Fixtures {
    private static String sourceNoImport = """
        public class Bad {
            public int getRandomNumber() {
                return RandomGenerator.StreamableGenerator.of("L128X256MixRandom").nextInt();
            }
        }
        """;
    private static String sourceA = """
        import java.util.random.RandomGenerator;
        public class A {
            public int getRandomNumber() {
                return RandomGenerator.StreamableGenerator.of("L128X256MixRandom").nextInt();
            }
        }
        """;
    private static String sourceB = """
        import java.util.random.RandomGenerator;
        public class B {
                 public static class InnerB extends B {
                     @Override
                     public double getRandomNumber() {
                         return 0.0;
                     }
                 }
                 public double getRandomNumber() {
                     return RandomGenerator.StreamableGenerator.of("Xoroshiro128PlusPlus").nextDouble();
                 }
                 public static void main(String[] args) {
                     System.err.println( new B().getRandomNumber() );
                 }
             }
        """;
    private final MetricsFixture metricsFixture;
    private final TestDirectoryFixture testDirectoryFixture;

    public TemplateClassCompilerTest() {
        testDirectoryFixture = fixture( new TestDirectoryFixture( getClass() ) );
        metricsFixture = fixture( new MetricsFixture() );
    }

    @Test
    public void testCompileSingleFileOk() {
        var compiler = new TemplateClassCompiler( testDirectoryFixture.testDirectory() );
        Result<Map<String, TemplateClassCompiler.CompiledJavaFile>, String> result = compiler.compile(
            Lists.of(
                new TemplateClassCompiler.SourceJavaFile( "A", sourceA )
            )
        );

        assertThat( result.isSuccess() ).isTrue();

        var compiler2 = new TemplateClassCompiler( testDirectoryFixture.testDirectory() );
        Result<Map<String, TemplateClassCompiler.CompiledJavaFile>, String> result2 = compiler2.compile(
            Lists.of(
                new TemplateClassCompiler.SourceJavaFile( "A", sourceA )
            )
        );

        assertThat( result2.isSuccess() ).isTrue();

        metricsFixture.assertMetric( "oap_template", Tags.of( "type", "disk" ) )
            .isCounter()
            .isEqualTo( 1.0d );
        metricsFixture.assertMetric( "oap_template", Tags.of( "type", "compile" ) )
            .isCounter()
            .isEqualTo( 1.0d );
    }

    @Test
    public void testCompileBatchFilesOk() {
        var compiler = new TemplateClassCompiler( null );
        Result<Map<String, TemplateClassCompiler.CompiledJavaFile>, String> result = compiler.compile(
            Lists.of(
                new TemplateClassCompiler.SourceJavaFile( "A", sourceA ),
                new TemplateClassCompiler.SourceJavaFile( "B", sourceB )
            )
        );

        assertThat( result.isSuccess() ).isTrue();
    }

    @Test
    public void testCompileSingleFileSyntaxError() {
        var compiler = new TemplateClassCompiler( null );
        Result<Map<String, TemplateClassCompiler.CompiledJavaFile>, String> result = compiler.compile(
            Lists.of(
                new TemplateClassCompiler.SourceJavaFile( "Bad", sourceNoImport )
            )
        );

        assertThat( result.isSuccess() ).isFalse();
        assertThat( result.getFailureValue() ).contains( "package RandomGenerator does not exist" );
    }

    @Test
    public void testCompileInvalidSourceError() {
        var compiler = new TemplateClassCompiler( null );
        Result<Map<String, TemplateClassCompiler.CompiledJavaFile>, String> result = compiler.compile(
            Lists.of(
                new TemplateClassCompiler.SourceJavaFile( "InvalidJavaFileName", sourceA )
            )
        );

        assertThat( result.isSuccess() ).isFalse();
        assertThat( result.getFailureValue() ).contains( "class A is public, should be declared in a file named A.java" );
    }
}
