package oap.template;

import oap.util.Lists;
import oap.util.Result;
import org.testng.annotations.Test;

import javax.tools.JavaFileObject;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class TemplateClassCompilerTest {
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
        var compiler = new TemplateClassCompiler();
        Result<Map<String, TemplateClassCompiler.CompiledJavaFile>, String> result = compiler.compile(
                Lists.of(
                        new TemplateClassCompiler.SourceJavaFile(  "A", JavaFileObject.Kind.SOURCE, sourceA )
                )
        );

        assertThat( result.isSuccess() ).isTrue();
    }

    @Test
    public void testCompileBatchFilesOk() {
        var compiler = new TemplateClassCompiler();
        Result<Map<String, TemplateClassCompiler.CompiledJavaFile>, String> result = compiler.compile(
                Lists.of(
                        new TemplateClassCompiler.SourceJavaFile(  "A", JavaFileObject.Kind.SOURCE, sourceA ),
                        new TemplateClassCompiler.SourceJavaFile(  "B", JavaFileObject.Kind.SOURCE, sourceB )
                )
        );

        assertThat( result.isSuccess() ).isTrue();
    }

    @Test
    public void testCompileSingleFileSyntaxError() {
        var compiler = new TemplateClassCompiler();
        Result<Map<String, TemplateClassCompiler.CompiledJavaFile>, String> result = compiler.compile(
                Lists.of(
                        new TemplateClassCompiler.SourceJavaFile(  "Bad", JavaFileObject.Kind.SOURCE, sourceNoImport )
                )
        );

        assertThat( result.isSuccess() ).isFalse();
        assertThat( result.getFailureValue() ).contains( "package RandomGenerator does not exist" );
    }

    @Test
    public void testCompileInvalidSourceError() {
        var compiler = new TemplateClassCompiler();
        Result<Map<String, TemplateClassCompiler.CompiledJavaFile>, String> result = compiler.compile(
                Lists.of(
                        new TemplateClassCompiler.SourceJavaFile(  "InvalidJavaFileName", JavaFileObject.Kind.SOURCE, sourceA )
                )
        );

        assertThat( result.isSuccess() ).isFalse();
        assertThat( result.getFailureValue() ).contains( "class A is public, should be declared in a file named A.java" );
    }
}
