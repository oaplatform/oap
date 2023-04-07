package oap.template;

import oap.util.Lists;
import oap.util.Maps;
import oap.util.Result;
import org.testng.annotations.Test;

import javax.tools.JavaFileObject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class TemplateClassSupplierTest {
    private static String sourceA = """
            import java.util.random.RandomGenerator;
            public class A {
                public int getRandomNumber() {
                    RandomGenerator.StreamableGenerator.of("L128X256MixRandom").nextInt();
                    return 5;
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
    private static String sourceC = """
            public class C {
                public String get() {
                    return "OK";
                }
            }
            """;
    @Test
    public void compileAndRunClassA() throws Exception {
        var compiler = new TemplateClassCompiler();
        compiler.compile(
                Lists.of(
                        new TemplateClassCompiler.SourceJavaFile(  "A", JavaFileObject.Kind.SOURCE, sourceA )
                )
        );

        var supplier = new TemplateClassSupplier( compiler.compiledJavaFiles );

        Class classA = supplier.loadClasses( List.of ( "A", "B" ) ).get( "A" ).getSuccessValue();
        var exemplarOfClassA = classA.getDeclaredConstructor().newInstance();
        var methodOfClassA = classA.getDeclaredMethod( "getRandomNumber" );

        assertThat( methodOfClassA.invoke( exemplarOfClassA ) ).isEqualTo( 5 );
    }

    @Test
    public void compileAndRunClassInnerB() throws Exception {
        var compiler = new TemplateClassCompiler();
        compiler.compile(
                Lists.of(
                        new TemplateClassCompiler.SourceJavaFile(  "B", JavaFileObject.Kind.SOURCE, sourceB )
                )
        );

        var supplier = new TemplateClassSupplier( compiler.compiledJavaFiles );

        Class classInnerB = supplier.loadClasses( List.of( "B$InnerB" ) ).get( "B$InnerB" ).getSuccessValue();
        var exemplarOfInnerClassB = classInnerB.getDeclaredConstructor().newInstance();
        var methodOfClassInnerB = classInnerB.getDeclaredMethod( "getRandomNumber" );

        assertThat( methodOfClassInnerB.invoke( exemplarOfInnerClassB ) ).isEqualTo( 0.0 );
    }

    @Test
    public void compileAndRunClassBAndInnerBWithinSingleClassLoader() throws Exception {
        var compiler = new TemplateClassCompiler();
        compiler.compile(
                Lists.of(
                        new TemplateClassCompiler.SourceJavaFile(  "A", JavaFileObject.Kind.SOURCE, sourceA ),
                        new TemplateClassCompiler.SourceJavaFile(  "B", JavaFileObject.Kind.SOURCE, sourceB ),
                        new TemplateClassCompiler.SourceJavaFile(  "C", JavaFileObject.Kind.SOURCE, sourceC )
                )
        );

        var classLoader = new TemplateClassSupplier.TemplateClassLoader( compiler.compiledJavaFiles );
        var supplier = new TemplateClassSupplier( classLoader );

        Map<String, Result<Class, Throwable>> loadedClasses = supplier.loadClasses(List.of("A", "B$InnerB"));
        Class classInnerB = loadedClasses.get( "B$InnerB" ).getSuccessValue();
        var exemplarOfInnerClassB = classInnerB.getDeclaredConstructor().newInstance();
        var methodOfClassInnerB = classInnerB.getDeclaredMethod( "getRandomNumber" );

        assertThat( methodOfClassInnerB.invoke( exemplarOfInnerClassB ) ).isEqualTo( 0.0 );

        assertThat( classLoader.getLoadedClasses().size() ).isEqualTo( 3 );

        Class classA = loadedClasses.get( "A" ).getSuccessValue();
        var exemplarOfClassA = classA.getDeclaredConstructor().newInstance();
        var methodOfClassA = classA.getDeclaredMethod( "getRandomNumber" );

        assertThat( methodOfClassA.invoke( exemplarOfClassA ) ).isEqualTo( 5 );

        assertThat( classLoader.getLoadedClasses().size() ).isEqualTo( 3 );
    }

    @Test
    public void compile2TimesAndLoadIntoSingleClassLoader() {
        var compiler = new TemplateClassCompiler();
        var result1 = compiler.compile(
                Lists.of(
                        new TemplateClassCompiler.SourceJavaFile(  "A", JavaFileObject.Kind.SOURCE, sourceA )
                )
        ).getSuccessValue();
        var result2 = compiler.compile(
                Lists.of(
                        new TemplateClassCompiler.SourceJavaFile(  "B", JavaFileObject.Kind.SOURCE, sourceB ),
                        new TemplateClassCompiler.SourceJavaFile(  "C", JavaFileObject.Kind.SOURCE, sourceC )
                )
        ).getSuccessValue();

        Map<String, TemplateClassCompiler.CompiledJavaFile> allCompiledClasses = new HashMap<>( result1 );
        allCompiledClasses.putAll( result2 );

        var classLoader = new TemplateClassSupplier.TemplateClassLoader( allCompiledClasses );
        var supplier = new TemplateClassSupplier( classLoader );

        Map<String, Result<Class, Throwable>> loadedClasses = supplier.loadClasses( List.of( "A", "B", "C", "B$InnerB" ) );
        assertThat( loadedClasses.get( "A" ).isSuccess() ).isTrue();
        assertThat( loadedClasses.get( "B" ).isSuccess() ).isTrue();
        assertThat( loadedClasses.get( "B$InnerB" ).isSuccess() ).isTrue();
        assertThat( loadedClasses.get( "C" ).isSuccess() ).isTrue();

        Class classA = loadedClasses.get( "A" ).getSuccessValue();
        Class classB = loadedClasses.get( "B" ).getSuccessValue();
        Class classInnerB = loadedClasses.get( "B$InnerB" ).getSuccessValue();
        Class classC = loadedClasses.get( "C" ).getSuccessValue();

        assertThat( classA ).isNotNull();
        assertThat( classB ).isNotNull();
        assertThat( classInnerB ).isNotNull();
        assertThat( classC ).isNotNull();
    }
}
