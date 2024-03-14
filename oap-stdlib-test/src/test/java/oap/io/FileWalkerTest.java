package oap.io;

import oap.testng.Fixtures;
import oap.testng.TestDirectoryFixture;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.function.Consumer;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static oap.io.content.ContentWriter.ofString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertEqualsNoOrder;

public class FileWalkerTest extends Fixtures {
    private final TestDirectoryFixture testDirectoryFixture;

    public FileWalkerTest() {
        testDirectoryFixture = fixture( new TestDirectoryFixture() );
    }

    @BeforeMethod
    public void init() {
        Files.write( testDirectoryFixture.testPath( "/wildcard/1.txt" ), "1", ofString() );
        Files.write( testDirectoryFixture.testPath( "/wildcard/w2/3.txt" ), "1", ofString() );
        Files.write( testDirectoryFixture.testPath( "/wildcard/w2/33.txt" ), "1", ofString() );
        Files.write( testDirectoryFixture.testPath( "/wildcard/w2/w1/4.txt" ), "1", ofString() );
    }

    @Test
    public void walkFileTreeBasePathNotFound() {
        final CollectingVisitor visitor = new CollectingVisitor();
        new FileWalker( Paths.get( "/aaa" ), "*.txt" ).walkFileTree( visitor );

        assertThat( visitor.files ).isEmpty();
    }

    @Test
    public void walkFileTreeStaticPath() {
        final CollectingVisitor visitor = new CollectingVisitor();
        new FileWalker( testDirectoryFixture.testPath( "wildcard" ), "w2/3.txt" ).walkFileTree( visitor );

        assertEquals( visitor.files, singletonList( testDirectoryFixture.testPath( "/wildcard/w2/3.txt" ) ) );
    }

    @Test
    public void walkFileTreeStaticPathNotFound() {
        final CollectingVisitor visitor = new CollectingVisitor();
        new FileWalker( testDirectoryFixture.testPath( "wildcard" ), "unknown/3.txt" ).walkFileTree( visitor );

        assertEquals( visitor.files, emptyList() );
    }

    @Test
    public void walkFileTreeAny() {
        final CollectingVisitor visitor = new CollectingVisitor();
        new FileWalker( testDirectoryFixture.testPath( "wildcard" ), "w2\\*" ).walkFileTree( visitor );

        assertEqualsNoOrder( visitor.files.toArray(), new Path[] {
            testDirectoryFixture.testPath( "/wildcard/w2/33.txt" ), testDirectoryFixture.testPath( "/wildcard/w2/w1" ),
            testDirectoryFixture.testPath( "/wildcard/w2/3.txt" )
        } );
    }

    @Test
    public void walkFileTreeAny2() {
        final CollectingVisitor visitor = new CollectingVisitor();
        new FileWalker( testDirectoryFixture.testPath( "wildcard" ), "*/*.txt" ).walkFileTree( visitor );

        assertEqualsNoOrder( visitor.files.toArray(), new Path[] {
            testDirectoryFixture.testPath( "/wildcard/w2/33.txt" ), testDirectoryFixture.testPath( "/wildcard/w2/3.txt" )
        } );
    }

    @Test
    public void walkFileTreeFilePattern() {
        final CollectingVisitor visitor = new CollectingVisitor();
        new FileWalker( testDirectoryFixture.testPath( "wildcard" ), "w2/3*.txt" ).walkFileTree( visitor );

        assertThat( visitor.files )
            .containsOnly( testDirectoryFixture.testPath( "/wildcard/w2/3.txt" ), testDirectoryFixture.testPath( "/wildcard/w2/33.txt" ) );
    }

    @Test
    public void walkFileTreeFilePatternCache() {
        final FileWalkerCache fwc = new FileWalkerCache();

        final CollectingVisitor visitor1 = new CollectingVisitor();
        new FileWalker( testDirectoryFixture.testPath( "wildcard" ), "w2/3*.txt", fwc ).walkFileTree( visitor1 );
        assertThat( visitor1.files ).containsOnly( testDirectoryFixture.testPath( "/wildcard/w2/3.txt" ),
            testDirectoryFixture.testPath( "/wildcard/w2/33.txt" ) );

        final CollectingVisitor visitor2 = new CollectingVisitor();
        new FileWalker( testDirectoryFixture.testPath( "wildcard" ), "w2/3*.txt", fwc ).walkFileTree( visitor2 );
        assertThat( visitor2.files ).containsOnly( testDirectoryFixture.testPath( "/wildcard/w2/3.txt" ),
            testDirectoryFixture.testPath( "/wildcard/w2/33.txt" ) );
    }

    private static class CollectingVisitor implements Consumer<Path> {
        public final ArrayList<Path> files = new ArrayList<>();

        @Override
        public void accept( Path path ) {
            files.add( path );
        }
    }
}
