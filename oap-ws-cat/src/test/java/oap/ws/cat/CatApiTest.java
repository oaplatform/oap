package oap.ws.cat;

import oap.testng.AbstractTest;
import org.testng.annotations.Test;

import static org.testng.Assert.assertEquals;

public class CatApiTest extends AbstractTest {
   @Test
   public void testInfluxToValues() throws Exception {
      assertEquals( CatApi.influxToZabbix( "test" ), "test" );
      assertEquals( CatApi.influxToZabbix( "test.test" ), "test.test" );
      assertEquals( CatApi.influxToZabbix( "test,a=test" ), "test[test]" );
      assertEquals( CatApi.influxToZabbix( "test,a=test,b=hhhj-f" ), "test[test,hhhj-f]" );
   }
}