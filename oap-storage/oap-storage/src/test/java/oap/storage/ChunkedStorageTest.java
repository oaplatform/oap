package oap.storage;

import oap.testng.Fixtures;
import oap.testng.TestDirectoryFixture;
import org.apache.commons.lang3.RandomStringUtils;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;

import static oap.testng.Asserts.assertEventually;
import static oap.testng.Asserts.assertFile;
import static oap.testng.TestDirectoryFixture.deployTestData;
import static oap.testng.TestDirectoryFixture.testPath;
import static org.assertj.core.api.Assertions.assertThat;

public class ChunkedStorageTest extends Fixtures {

    {
        fixture( TestDirectoryFixture.FIXTURE );
    }

    @BeforeMethod
    public void beforeMethod() {
        deployTestData( this.getClass() );
    }

    @Test
    public void putGetStream() {
        Path path = testPath( "chunks" );
        ChunkedStorage<String> storage = new ChunkedStorage<>( s -> s, path );

        List<String> all = new ArrayList<>();
        for( int i = 0; i < 5; i++ ) {
            List<String> items = new ArrayList<>();
            for( int y = 0; y < 100; y++ ) {
                String au = RandomStringUtils.random( 2 * ( i + 2 ), true, true );
                items.add( au );
                items.add( au );
                all.add( au + au );
            }
            storage.mergeAll( items, i, ( a, b ) -> a + b );
        }

        assertEventually( 100, 20, () -> {
            assertFile( path.resolve( "chunk0.gz" ) ).exists();
            assertFile( path.resolve( "chunk1.gz" ) ).exists();
            assertFile( path.resolve( "chunk2.gz" ) ).exists();
            assertFile( path.resolve( "chunk3.gz" ) ).exists();
            assertFile( path.resolve( "chunk4.gz" ) ).exists();

            assertThat( storage.stream().toList() ).containsAll( all );
        } );
    }

}
