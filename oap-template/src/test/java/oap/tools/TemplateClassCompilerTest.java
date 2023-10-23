package oap.tools;

import io.micrometer.core.instrument.Metrics;
import io.micrometer.prometheus.PrometheusConfig;
import io.micrometer.prometheus.PrometheusMeterRegistry;
import oap.testng.Fixtures;
import oap.testng.TestDirectoryFixture;
import oap.util.Lists;
import oap.util.Result;
import org.testng.annotations.Test;

import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class TemplateClassCompilerTest extends Fixtures {
    public TemplateClassCompilerTest() {
        fixture( TestDirectoryFixture.FIXTURE );
    }

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

    @Test
    public void testCompileSingleFileOk() {
        var prometheusRegistry = new PrometheusMeterRegistry( PrometheusConfig.DEFAULT );
        Metrics.addRegistry( prometheusRegistry );
        try {

            var compiler = new TemplateClassCompiler( TestDirectoryFixture.testDirectory() );
            Result<Map<String, TemplateClassCompiler.CompiledJavaFile>, String> result = compiler.compile(
                Lists.of(
                    new TemplateClassCompiler.SourceJavaFile( "A", sourceA )
                )
            );

            assertThat( result.isSuccess() ).isTrue();

            var compiler2 = new TemplateClassCompiler( TestDirectoryFixture.testDirectory() );
            Result<Map<String, TemplateClassCompiler.CompiledJavaFile>, String> result2 = compiler2.compile(
                Lists.of(
                    new TemplateClassCompiler.SourceJavaFile( "A", sourceA )
                )
            );

            assertThat( result2.isSuccess() ).isTrue();

            String scrape = prometheusRegistry.scrape();
            assertThat( scrape ).contains( "oap_template_total{type=\"disk\",} 1.0" );
            assertThat( scrape ).contains( "oap_template_total{type=\"compile\",} 1.0" );

        } finally {
            Metrics.removeRegistry( prometheusRegistry );
        }
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
