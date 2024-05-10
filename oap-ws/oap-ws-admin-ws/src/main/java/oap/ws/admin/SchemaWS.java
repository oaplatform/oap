package oap.ws.admin;

import oap.http.Http;
import oap.json.Binder;
import oap.json.schema.ResourceSchemaStorage;
import oap.ws.Response;
import oap.ws.WsMethod;
import oap.ws.WsParam;

import java.util.Map;

import static oap.ws.WsParam.From.QUERY;

public class SchemaWS {
    @WsMethod( path = "/" )
    public Response getSchema( @WsParam( from = QUERY ) String path ) {
        String json = ResourceSchemaStorage.INSTANCE.get( path );

        return new Response( Http.StatusCode.OK )
            .withBody( Binder.json.marshalWithDefaultPrettyPrinter( Binder.json.unmarshal( Map.class, json ) ), true )
            .withContentType( Http.ContentType.APPLICATION_JSON );
    }
}
