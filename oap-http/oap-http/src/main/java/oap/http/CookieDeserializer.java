package oap.http;

import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdNodeBasedDeserializer;
import io.undertow.server.handlers.CookieImpl;
import oap.json.Binder;

import java.io.IOException;

public class CookieDeserializer extends StdNodeBasedDeserializer<CookieImpl> {
    protected CookieDeserializer() {
        super( CookieImpl.class );
    }

    @Override
    public CookieImpl convert( JsonNode root, DeserializationContext ctxt ) throws IOException {
        CookieImpl cookie = new CookieImpl( root.get( "name" ).asText(), root.get( "value" ).asText() );

        Binder.json.readerForUpdating( cookie ).readValue( root );

        return cookie;
    }
}
