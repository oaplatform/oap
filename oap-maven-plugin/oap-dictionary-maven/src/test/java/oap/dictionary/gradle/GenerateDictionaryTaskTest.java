package oap.dictionary.gradle;

import oap.testng.Fixtures;
import oap.testng.TestDirectoryFixture;
import org.gradle.api.Project;
import org.gradle.testfixtures.ProjectBuilder;
import org.testng.annotations.Test;

import java.util.List;

import static oap.testng.Asserts.assertFile;
import static oap.testng.Asserts.pathOfTestResource;

public class GenerateDictionaryTaskTest extends Fixtures {
    private final TestDirectoryFixture testDirectoryFixture;

    public GenerateDictionaryTaskTest() {
        testDirectoryFixture = fixture( new TestDirectoryFixture() );
    }

    @Test
    public void execute() {
        Project project = ProjectBuilder.builder().build();
        GenerateDictionaryTask task = project.getTasks()
            .register( "generateDictionary", GenerateDictionaryTask.class )
            .get();

        task.getSourceDirectory().set( project.file( "src/test/resources/dictionary" ) );
        task.getSourceDirectoryExts().set( List.of( "src/test/resources/dictionary" ) );
        task.getDictionaryPackage().set( "test" );
        task.getOutputDirectory().set( testDirectoryFixture.testPath( "dictionary" ).toFile() );
        task.getExcludes().set( List.of( "**/test-dictionary.*" ) );

        task.generate();

        assertFile( testDirectoryFixture.testPath( "dictionary/test/TestDictionaryExternalIdAsCharacter.java" ) )
            .hasSameContentAs( pathOfTestResource( getClass(), "TestDictionaryExternalIdAsCharacter.java" ) );
        assertFile( testDirectoryFixture.testPath( "dictionary/test/Child1.java" ) )
            .hasSameContentAs( pathOfTestResource( getClass(), "Child1.java" ) );
        assertFile( testDirectoryFixture.testPath( "dictionary/test/Child2.java" ) )
            .hasSameContentAs( pathOfTestResource( getClass(), "Child2.java" ) );
        assertFile( testDirectoryFixture.testPath( "dictionary/test/TestDictionaryNoEid.java" ) )
            .hasSameContentAs( pathOfTestResource( getClass(), "TestDictionaryNoEid.java" ) );
    }
}
