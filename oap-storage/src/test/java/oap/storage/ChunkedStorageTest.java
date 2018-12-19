package oap.storage;

import oap.io.Files;
import oap.testng.Env;
import org.apache.commons.lang3.RandomStringUtils;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static oap.testng.Asserts.assertEventually;
import static oap.testng.Asserts.assertFile;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Created by macchiatow on 8/31/16.
 */
public class ChunkedStorageTest {


    @BeforeMethod
    public void beforeMethod() {
        Files.delete( Env.tmpRoot );
        Env.deployTestData( this.getClass() );
    }

    @Test
    public void putGetStream() throws Exception {
        ChunkedStorage<String> storage = new ChunkedStorage<>( s -> s, Env.tmpRoot.resolve( "audience-chunks" ) );

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
            assertFile( Env.tmpRoot.resolve( "audience-chunks" ).resolve( "chunk0.gz" ) ).exists();
            assertFile( Env.tmpRoot.resolve( "audience-chunks" ).resolve( "chunk1.gz" ) ).exists();
            assertFile( Env.tmpRoot.resolve( "audience-chunks" ).resolve( "chunk2.gz" ) ).exists();
            assertFile( Env.tmpRoot.resolve( "audience-chunks" ).resolve( "chunk3.gz" ) ).exists();
            assertFile( Env.tmpRoot.resolve( "audience-chunks" ).resolve( "chunk4.gz" ) ).exists();

            assertThat( storage.stream().toList() ).containsAll( ( Iterable<String> ) all );
        } );
    }

}
