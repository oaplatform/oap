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
 * Created by macchiatow on 9/22/16.
 */
public class ChunkedFileStorageTest {


   @BeforeMethod
   public void beforeMethod() {
      Files.delete( Env.tmpRoot );
      Env.deployTestData( this.getClass() );
   }

   @Test
   public void testStore() throws Exception {
      ChunkedFileStorage<String> storage =
         new ChunkedFileStorage<>( Env.tmpRoot.resolve( "publishers" ), s -> s, 5, 1000 );

      for( int i = 0; i < 5; i++ ) {
         for( int y = 0; y < 100; y++ ) {
            String au = RandomStringUtils.random( 2 * ( i + 2 ), true, true );
            storage.store( au );
         }
      }

      assertEventually( 100, 20, () -> {
         assertFile( Env.tmpRoot.resolve( "publishers" ).resolve( "0.json.gz" ) ).exists();
         assertFile( Env.tmpRoot.resolve( "publishers" ).resolve( "1.json.gz" ) ).exists();
         assertFile( Env.tmpRoot.resolve( "publishers" ).resolve( "2.json.gz" ) ).exists();
         assertFile( Env.tmpRoot.resolve( "publishers" ).resolve( "3.json.gz" ) ).exists();
         assertFile( Env.tmpRoot.resolve( "publishers" ).resolve( "4.json.gz" ) ).exists();
      } );

      assertThat( storage.load( Env.tmpRoot.resolve( "publishers" ) ).size() ).isEqualTo( 500 );
   }

}