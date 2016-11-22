package oap.io;

import oap.testng.AbstractTest;
import oap.testng.Env;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.function.Consumer;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static oap.testng.Env.tmpPath;
import static org.assertj.core.api.Assertions.assertThat;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertEqualsNoOrder;

/**
 * Created by Igor Petrenko on 14.01.2016.
 */
public class FileWalkerTest extends AbstractTest {

    @BeforeMethod
    @Override
    public void beforeMethod() {
        super.beforeMethod();

        Files.writeString( Env.tmp( "/wildcard/1.txt" ), "1" );
        Files.writeString( Env.tmp( "/wildcard/w2/3.txt" ), "1" );
        Files.writeString( Env.tmp( "/wildcard/w2/33.txt" ), "1" );
        Files.writeString( Env.tmp( "/wildcard/w2/w1/4.txt" ), "1" );
    }

    @Test
    public void walkFileTreeBasePathNotFound() throws Exception {
        final MockVisitor visitor = new MockVisitor();
        new FileWalker( Paths.get( "/aaa" ), "*.txt" ).walkFileTree( visitor );

        assertThat( visitor.files ).isEmpty();
    }

    @Test
    public void walkFileTreeStaticPath() throws Exception {
        final MockVisitor visitor = new MockVisitor();
        new FileWalker( tmpPath( "wildcard" ), "w2/3.txt" ).walkFileTree( visitor );

        assertEquals( visitor.files, singletonList( tmpPath( "/wildcard/w2/3.txt" ) ) );
    }

    @Test
    public void walkFileTreeStaticPathNotFound() throws Exception {
        final MockVisitor visitor = new MockVisitor();
        new FileWalker( tmpPath( "wildcard" ), "unknown/3.txt" ).walkFileTree( visitor );

        assertEquals( visitor.files, emptyList() );
    }

    @Test
    public void walkFileTreeAny() throws Exception {
        final MockVisitor visitor = new MockVisitor();
        new FileWalker( tmpPath( "wildcard" ), "w2\\*" ).walkFileTree( visitor );

        assertEqualsNoOrder( visitor.files.toArray(), new Path[] {
            tmpPath( "/wildcard/w2/33.txt" ), tmpPath( "/wildcard/w2/w1" ), tmpPath( "/wildcard/w2/3.txt" )
        } );
    }

    @Test
    public void walkFileTreeAny2() throws Exception {
        final MockVisitor visitor = new MockVisitor();
        new FileWalker( tmpPath( "wildcard" ), "*/*.txt" ).walkFileTree( visitor );

        assertEqualsNoOrder( visitor.files.toArray(), new Path[] {
            tmpPath( "/wildcard/w2/33.txt" ), tmpPath( "/wildcard/w2/3.txt" )
        } );
    }

    @Test
    public void walkFileTreeFilePattern() throws Exception {
        final MockVisitor visitor = new MockVisitor();
        new FileWalker( tmpPath( "wildcard" ), "w2/3*.txt" ).walkFileTree( visitor );

        assertEquals( visitor.files, asList(
            tmpPath( "/wildcard/w2/3.txt" ), tmpPath( "/wildcard/w2/33.txt" )
        ) );
    }

    @Test
    public void walkFileTreeFilePatternCache() throws Exception {
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
