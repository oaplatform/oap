package oap.openapi.gradle;

import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPluginExtension;
import org.gradle.api.tasks.SourceSet;

public class OpenApiPlugin implements Plugin<Project> {
    @Override
    public void apply( Project project ) {
        project.getTasks().register( "generateOpenApi", GenerateOpenApiTask.class, task -> {
            JavaPluginExtension java = project.getExtensions().getByType( JavaPluginExtension.class );
            SourceSet main = java.getSourceSets().getByName( "main" );
            task.getClasspath().from( main.getRuntimeClasspath() );
            task.getOutputPath().convention( "swagger" );
            task.getOutputType().convention( "JSON" );
            task.getSkipDeprecated().convention( true );
            task.dependsOn( project.getTasks().named( "compileJava" ) );
        } );
    }
}
