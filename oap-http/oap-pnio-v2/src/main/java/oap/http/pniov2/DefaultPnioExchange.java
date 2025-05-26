package oap.http.pniov2;

import oap.http.server.nio.HttpServerExchange;

public class DefaultPnioExchange extends AbstractPnioExchange<DefaultPnioExchange> {
    public DefaultPnioExchange( byte[] requestBuffer, int responseSize, PnioController controller,
                                ComputeTask task, HttpServerExchange oapExchange, long timeout,
                                PnioListener pnioListener ) {
        super( requestBuffer, responseSize, controller, task, oapExchange, timeout, pnioListener );
    }
}
