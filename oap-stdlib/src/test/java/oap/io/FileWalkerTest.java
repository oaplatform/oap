package oap.io;

import oap.testng.AbstractTest;
import oap.testng.Env;
import org.testng.Assert;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.function.Consumer;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static oap.testng.Env.tmpPath;

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
    public void testWalkFileTree_static_path() throws Exception {
        final MockVisitor visitor = new MockVisitor();
        new FileWalker( tmpPath( "wildcard" ), "w2/3.txt" ).walkFileTree( visitor );

        Assert.assertEquals( visitor.files, singletonList( tmpPath( "/wildcard/w2/3.txt" ) ) );
    }

    @Test
    public void testWalkFileTree_static_path_not_found() throws Exception {
        final MockVisitor visitor = new MockVisitor();
        new FileWalker( tmpPath( "wildcard" ), "unknown/3.txt" ).walkFileTree( visitor );

        Assert.assertEquals( visitor.files, emptyList() );
    }

    @Test
    public void testWalkFileTree_any() throws Exception {
        final MockVisitor visitor = new MockVisitor();
        new FileWalker( tmpPath( "wildcard" ), "w2/*" ).walkFileTree( visitor );

        Assert.assertEquals( visitor.files, asList(
            tmpPath( "/wildcard/w2/3.txt" ), tmpPath( "/wildcard/w2/33.txt" ), tmpPath( "/wildcard/w2/w1" )
        ) );
    }

    @Test
    public void testWalkFileTree_file_pattern() throws Exception {
        final MockVisitor visitor = new MockVisitor();
        new FileWalker( tmpPath( "wildcard" ), "w2/3*.txt" ).walkFileTree( visitor );

        Assert.assertEquals( visitor.files, asList(
            tmpPath( "/wildcard/w2/3.txt" ), tmpPath( "/wildcard/w2/33.txt" )
        ) );
    }

    private class MockVisitor implements Consumer<Path> {
        public final ArrayList<Path> files = new ArrayList<>();

        @Override
        public void accept( Path path ) {
            files.add( path );
        }
    }
}