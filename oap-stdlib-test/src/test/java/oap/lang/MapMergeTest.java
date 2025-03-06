package oap.lang;

import org.testng.annotations.Test;

import static oap.json.testng.JsonAsserts.assertJson;


public class MapMergeTest {
    @Test
    public void testMergeObject() {
        assertJson( MapMerge.mergeHocon( """
            a {
              b = 10
              c = 20
              m = {}
              l = []
            }
            """, """
            a {
              b = str
              m = []
              l = 1
            }
            """ ) ).isEqualTo(
            """
                {
                  "a" : {
                    "c" : 20,
                    "m" : [ ],
                    "b" : "str",
                    "l" : 1
                  }
                }""" );
    }

    @Test
    public void testMergeArray() {
        assertJson( MapMerge.mergeHocon( """
            a {
              b = [
                {c: 1}
              ]
            }
            """, """
            a {
              b = [
                {d: 1}
                {e: str}
              ]
            }
            """ ) ).isEqualTo(
            """
                {
                   "a" : {
                     "b" : [ {
                       "c" : 1,
                       "d" : 1
                     }, {
                       "e" : "str"
                     } ]
                   }
                 }""" );
    }
}
