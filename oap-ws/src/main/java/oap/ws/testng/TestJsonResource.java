package oap.ws.testng;

import oap.io.Resources;
import oap.json.testng.JsonAsserts;

/**
 * Copyright (c) 2015 Igor Petrenko <igor.petrenko@madberry.net>
 */
public interface TestJsonResource {
    String getContent();

    default void assertEqualToResource( Class clazz, String resourcePath ) {
        JsonAsserts.assertEquals( getContent(),
            Resources.readString( clazz, clazz.getSimpleName() + "/" + resourcePath ).get() );
    }

    class TestHttpResponseJsonResource implements TestJsonResource {
        private final HttpAsserts.Response response;

        public TestHttpResponseJsonResource( HttpAsserts.Response response ) {
            this.response = response;
        }

        @Override
        public final String getContent() {
            return response.body;
        }
    }
}
