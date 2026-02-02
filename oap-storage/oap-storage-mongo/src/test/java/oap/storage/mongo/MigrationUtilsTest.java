package oap.storage.mongo;

import org.bson.Document;
import org.testng.annotations.Test;

import static oap.testng.Asserts.assertString;

public class MigrationUtilsTest {
    @Test
    public void testGetString() {
        Document document = Document.parse( """
            {
            a: "123",
            b: {
              ba: "12",
              bb: {
                bbv: "123+"
              }
            }
            }
            """ );


        assertString( MigrationUtils.getString( document, "a" ) ).isEqualTo( "123" );
        assertString( MigrationUtils.getString( document, "unknown" ) ).isNull();
        assertString( MigrationUtils.getString( document, "a.b.c" ) ).isNull();
        assertString( MigrationUtils.getString( document, "b.bb.bbv" ) ).isEqualTo( "123+" );
        assertString( MigrationUtils.getString( document, "b.bb.unk" ) ).isNull();
        assertString( MigrationUtils.getString( document, "b.unk.unk" ) ).isNull();
    }
}
