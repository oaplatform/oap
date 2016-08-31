package oap.storage;

import oap.io.Files;
import oap.testng.Env;
import org.apache.commons.lang3.RandomStringUtils;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import java.util.ArrayList;
import java.util.List;

import static oap.testng.Asserts.*;
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
   public void testPutGetStream() throws Exception {
      ChunkedStorage<String> db = new ChunkedStorage<>( s -> s, Env.tmpRoot.resolve( "audience-chunks" ), 5, 1000 );
      db.start();
      List<String> audienceProfiles = new ArrayList<>();
      for( int i = 0; i < 10000; i++ ) {
         String au = RandomStringUtils.random( 20, true, true );
         audienceProfiles.add( au );
         db.put( au );
      }

      audienceProfiles.forEach( a -> assertThat( db.get( a ) ).isNotNull() );
      assertThat( db.stream().toList() ).containsAll( audienceProfiles );

      assertEventually( 100, 20, () -> {
         assertFile( Env.tmpRoot.resolve( "audience-chunks" ).resolve( "chunk0.gz" ) ).exists();
         assertFile( Env.tmpRoot.resolve( "audience-chunks" ).resolve( "chunk1.gz" ) ).exists();
         assertFile( Env.tmpRoot.resolve( "audience-chunks" ).resolve( "chunk2.gz" ) ).exists();
         assertFile( Env.tmpRoot.resolve( "audience-chunks" ).resolve( "chunk3.gz" ) ).exists();
         assertFile( Env.tmpRoot.resolve( "audience-chunks" ).resolve( "chunk4.gz" ) ).exists();
      } );
   }

}