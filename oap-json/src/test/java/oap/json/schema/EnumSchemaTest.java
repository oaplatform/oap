package oap.json.schema;

import org.testng.annotations.Test;

/**
 * Copyright (c) 2015 Igor Petrenko <igor.petrenko@madberry.net>
 */
public class EnumSchemaTest extends AbstractSchemaTest {
    @Test
    public void testStaticEnum() {
        String schema = "{'type': 'string', 'enum': ['test', 'test1']}";

        vOk( schema, "null" );
        vOk( schema, "'test'" );
        vOk( schema, "'test1'" );

        vFail( schema, "'test2'", "instance does not match any member of the enumeration [test,test1]" );
    }

    @Test
    public void testDynamicEnumPathSingleton() {
        String schema = "{" +
            "'type':'object'," +
            "'properties':{" +
            "  'a':{" +
            "    'type':'string'," +
            "  }," +
            "  'b':{" +
            "    'type': 'string', " +
            "    'enum': {'json-path':'a'}" +
            "  }" +
            "}" +
            "}";

        vOk( schema, "{'b':null}" );
        vOk( schema, "{'a':'test', 'b':'test'}" );

        vFail( schema, "{'a':'test', 'b':'test2'}", "/b: instance does not match any member of the enumeration [test]" );
    }

    @Test
    public void testDynamicEnumPathListObjects() {
        String schema = "{" +
            "'type':'object'," +
            "'properties':{" +
            "  'a':{" +
            "    'type':'array'," +
            "    'items': {" +
            "      'type':'object'," +
            "      'properties':{" +
            "        'c': {'type':'string'}" +
            "      }" +
            "    }" +
            "  }," +
            "  'b':{" +
            "    'type': 'string', " +
            "    'enum': {'json-path':'a.c'}" +
            "  }" +
            "}" +
            "}";

        vOk( schema, "{'b':null}" );
        vOk( schema, "{'a':[{'c':'test'}], 'b':'test'}" );

        vFail( schema, "{'a':[{'c':'test'}], 'b':'test2'}", "/b: instance does not match any member of the enumeration [test]" );
    }

    @Test
    public void testDynamicEnumPathList() {
        String schema = "{" +
            "'type':'object'," +
            "'properties':{" +
            "  'a':{" +
            "    'type':'array'," +
            "    'items': {" +
            "      'type':'string'" +
            "    }" +
            "  }," +
            "  'b':{" +
            "    'type': 'string', " +
            "    'enum': {'json-path':'a'}" +
            "  }" +
            "}" +
            "}";

        vOk( schema, "{'b':null}" );
        vOk( schema, "{'a':['test'], 'b':'test'}" );

        vFail( schema, "{'a':['test'], 'b':'test2'}", "/b: instance does not match any member of the enumeration [test]" );
    }
}
