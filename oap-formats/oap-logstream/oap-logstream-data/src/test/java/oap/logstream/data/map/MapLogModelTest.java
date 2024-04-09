/*
 *
 *  * Copyright (c) Xenoss
 *  * Unauthorized copying of this file, via any medium is strictly prohibited
 *  * Proprietary and confidential
 *
 *
 */

package oap.logstream.data.map;

import oap.dictionary.DictionaryRoot;
import oap.util.Dates;
import org.testng.annotations.Test;

import java.util.Map;

import static java.nio.charset.StandardCharsets.UTF_8;
import static oap.testng.Asserts.assertString;
import static oap.testng.Asserts.objectOfTestResource;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;

public class MapLogModelTest {
    @Test
    public void render() {
        Dates.setTimeFixed( 2022, 12, 16, 18, 1, 1 );

        MapLogModel dataModel = new MapLogModel( objectOfTestResource( DictionaryRoot.class, getClass(), "datamodel.conf" ) );
        MapLogRenderer renderer = dataModel.renderer( "EVENT1", "LOG" );
        assertThat( renderer.headers() ).isEqualTo( new String[] { "TIMESTAMP\tNAME\tVALUE1\tVALUE2" } );
        assertString( new String( renderer.render( Map.of( "name", "n", "value1", "v", "value2", 2 ) ), UTF_8 ) )
            .isEqualTo( "2022-12-16 18:01:01\tn\tv\t2\n" );
    }
}
