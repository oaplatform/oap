package oap.http.server.nio.handlers;

import io.undertow.conduits.GzipStreamSourceConduit;
import io.undertow.conduits.InflatingStreamSourceConduit;
import io.undertow.server.HttpHandler;
import io.undertow.server.handlers.encoding.ContentEncodingRepository;
import io.undertow.server.handlers.encoding.DeflateEncodingProvider;
import io.undertow.server.handlers.encoding.EncodingHandler;
import io.undertow.server.handlers.encoding.GzipEncodingProvider;
import io.undertow.server.handlers.encoding.RequestEncodingHandler;
import oap.http.server.nio.NioHandlerBuilder;

public class CompressionNioHandler implements NioHandlerBuilder {
    public static final String DEFLATE = "deflate";
    public static final String GZIP = "gzip";

    private static final ContentEncodingRepository contentEncodingRepository;

    static {
        contentEncodingRepository = new ContentEncodingRepository();
        contentEncodingRepository.addEncodingHandler( GZIP, new GzipEncodingProvider(), 100 );
        contentEncodingRepository.addEncodingHandler( DEFLATE, new DeflateEncodingProvider(), 50 );
    }

    @Override
    public HttpHandler build( HttpHandler next ) {
        HttpHandler httpHandler = next;
        httpHandler = new EncodingHandler( httpHandler, contentEncodingRepository );
        httpHandler = new RequestEncodingHandler( httpHandler )
            .addEncoding( GZIP, GzipStreamSourceConduit.WRAPPER )
            .addEncoding( DEFLATE, InflatingStreamSourceConduit.WRAPPER );

        return httpHandler;
    }
}
