package oap.http.client;

import org.eclipse.jetty.client.Request;

import java.util.Map;

public class JettyRequestExtensions {
    public static Request addParams( Request request, Map<String, Object> params ) {
        params.forEach( ( k, v ) -> request.param( k, v.toString() ) );

        return request;
    }

    public static Request addHeaders( Request request, Map<String, Object> headers ) {
        return request.headers( h -> {
            headers.forEach( ( k, v ) -> h.add( k, v == null ? "" : v.toString() ) );
        } );
    }
}
