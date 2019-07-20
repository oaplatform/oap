package oap.io;

import oap.testng.Env;
import oap.testng.Fixtures;
import oap.testng.TestDirectory;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.function.Consumer;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static oap.testng.Env.tmpPath;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertEqualsNoOrder;

public class FileWalkerTest extends Fixtures {
    {
        fixture( TestDirectory.FIXTURE );
    }

    @BeforeMethod
    public void init() {
        Files.writeString( Env.tmp( "/wildcard/1.txt" ), "1" );
        Files.writeString( Env.tmp( "/wildcard/w2/3.txt" ), "1" );
        Files.writeString( Env.tmp( "/wildcard/w2/33.txt" ), "1" );
        Files.writeString( Env.tmp( "/wildcard/w2/w1/4.txt" ), "1" );
    }

    @Test
    public void walkFileTreeBasePathNotFound() {
        final MockVisitor visitor = new MockVisitor();
        new FileWalker( Paths.get( "/aaa" ), "*.txt" ).walkFileTree( visitor );

        assertThat( visitor.files ).isEmpty();
    }

    @Test
    public void walkFileTreeStaticPath() {
        final MockVisitor visitor = new MockVisitor();
        new FileWalker( tmpPath( "wildcard" ), "w2/3.txt" ).walkFileTree( visitor );

        assertEquals( visitor.files, singletonList( tmpPath( "/wildcard/w2/3.txt" ) ) );
    }

    @Test
    public void walkFileTreeStaticPathNotFound() {
        final MockVisitor visitor = new MockVisitor();
        new FileWalker( tmpPath( "wildcard" ), "unknown/3.txt" ).walkFileTree( visitor );

        assertEquals( visitor.files, emptyList() );
    }

    @Test
    public void walkFileTreeAny() {
        final MockVisitor visitor = new MockVisitor();
        new FileWalker( tmpPath( "wildcard" ), "w2\\*" ).walkFileTree( visitor );

        assertEqualsNoOrder( visitor.files.toArray(), new Path[] {
            tmpPath( "/wildcard/w2/33.txt" ), tmpPath( "/wildcard/w2/w1" ), tmpPath( "/wildcard/w2/3.txt" )
        } );
    }

    @Test
    public void walkFileTreeAny2() {
        final MockVisitor visitor = new MockVisitor();
        new FileWalker( tmpPath( "wildcard" ), "*/*.txt" ).walkFileTree( visitor );

        assertEqualsNoOrder( visitor.files.toArray(), new Path[] {
            tmpPath( "/wildcard/w2/33.txt" ), tmpPath( "/wildcard/w2/3.txt" )
        } );
    }

    @Test
    public void walkFileTreeFilePattern() {
        final MockVisitor visitor = new MockVisitor();
        new FileWalker( tmpPath( "wildcard" ), "w2/3*.txt" ).walkFileTree( visitor );

        assertThat( visitor.files )
            .containsOnly( tmpPath( "/wildcard/w2/3.txt" ), tmpPath( "/wildcard/w2/33.txt" ) );
    }

    @Test
    public void walkFileTreeFilePatternCache() {
        final FileWalkerCache fwc = new FileWalkerCache();

        final MockVisitor visitor1 = new MockVisitor();
        new FileWalker( tmpPath( "wildcard" ), "w2/3*.txt", fwc ).walkFileTree( visitor1 );
        assertThat( visitor1.files ).containsOnly( tmpPath( "/wildcard/w2/3.txt" ), tmpPath( "/wildcard/w2/33.txt" ) );

        final MockVisitor visitor2 = new MockVisitor();
        new FileWalker( tmpPath( "wildcard" ), "w2/3*.txt", fwc ).walkFileTree( visitor2 );
        assertThat( visitor2.files ).containsOnly( tmpPath( "/wildcard/w2/3.txt" ), tmpPath( "/wildcard/w2/33.txt" ) );
    }

    private class MockVisitor implements Consumer<Path> {
        public final ArrayList<Path> files = new ArrayList<>();

        @Override
        public void accept( Path path ) {
            files.add( path );
        }
    }
}
