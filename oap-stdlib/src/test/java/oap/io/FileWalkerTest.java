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
import static oap.testng.TestDirectoryFixture.testPath;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertEqualsNoOrder;

public class FileWalkerTest extends Fixtures {
    {
        fixture( TestDirectoryFixture.FIXTURE );
    }

    @BeforeMethod
    public void init() {
        Files.writeString( testPath( "/wildcard/1.txt" ), "1" );
        Files.writeString( testPath( "/wildcard/w2/3.txt" ), "1" );
        Files.writeString( testPath( "/wildcard/w2/33.txt" ), "1" );
        Files.writeString( testPath( "/wildcard/w2/w1/4.txt" ), "1" );
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
        new FileWalker( testPath( "wildcard" ), "w2/3.txt" ).walkFileTree( visitor );

        assertEquals( visitor.files, singletonList( testPath( "/wildcard/w2/3.txt" ) ) );
    }

    @Test
    public void walkFileTreeStaticPathNotFound() {
        final CollectingVisitor visitor = new CollectingVisitor();
        new FileWalker( testPath( "wildcard" ), "unknown/3.txt" ).walkFileTree( visitor );

        assertEquals( visitor.files, emptyList() );
    }

    @Test
    public void walkFileTreeAny() {
        final CollectingVisitor visitor = new CollectingVisitor();
        new FileWalker( testPath( "wildcard" ), "w2\\*" ).walkFileTree( visitor );

        assertEqualsNoOrder( visitor.files.toArray(), new Path[] {
            testPath( "/wildcard/w2/33.txt" ), testPath( "/wildcard/w2/w1" ), testPath( "/wildcard/w2/3.txt" )
        } );
    }

    @Test
    public void walkFileTreeAny2() {
        final CollectingVisitor visitor = new CollectingVisitor();
        new FileWalker( testPath( "wildcard" ), "*/*.txt" ).walkFileTree( visitor );

        assertEqualsNoOrder( visitor.files.toArray(), new Path[] {
            testPath( "/wildcard/w2/33.txt" ), testPath( "/wildcard/w2/3.txt" )
        } );
    }

    @Test
    public void walkFileTreeFilePattern() {
        final CollectingVisitor visitor = new CollectingVisitor();
        new FileWalker( testPath( "wildcard" ), "w2/3*.txt" ).walkFileTree( visitor );

        assertThat( visitor.files )
            .containsOnly( testPath( "/wildcard/w2/3.txt" ), testPath( "/wildcard/w2/33.txt" ) );
    }

    @Test
    public void walkFileTreeFilePatternCache() {
        final FileWalkerCache fwc = new FileWalkerCache();

        final CollectingVisitor visitor1 = new CollectingVisitor();
        new FileWalker( testPath( "wildcard" ), "w2/3*.txt", fwc ).walkFileTree( visitor1 );
        assertThat( visitor1.files ).containsOnly( testPath( "/wildcard/w2/3.txt" ), testPath( "/wildcard/w2/33.txt" ) );

        final CollectingVisitor visitor2 = new CollectingVisitor();
        new FileWalker( testPath( "wildcard" ), "w2/3*.txt", fwc ).walkFileTree( visitor2 );
        assertThat( visitor2.files ).containsOnly( testPath( "/wildcard/w2/3.txt" ), testPath( "/wildcard/w2/33.txt" ) );
    }

    private static class CollectingVisitor implements Consumer<Path> {
        public final ArrayList<Path> files = new ArrayList<>();

        @Override
        public void accept( Path path ) {
            files.add( path );
        }
    }
}
