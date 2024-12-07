package oap.http.pnio;

import io.undertow.server.HttpServerExchange;
import oap.http.Http;

public class PnioTimeoutTask extends AbstractPnioTask {
    public PnioTimeoutTask( HttpServerExchange exchange ) {
        super( null, exchange, -1 );
    }

    @Override
    public void run() {
        exchange.setStatusCode( Http.StatusCode.NO_CONTENT );
        exchange.endExchange();
    }
}
