package oap.http.pniov2;

import oap.http.server.nio.HttpServerExchange;

public class DefaultPnioHttpHandler extends AbstractPnioHttpHandler<DefaultPnioExchange> {
    public DefaultPnioHttpHandler( PnioHttpSettings settings, ComputeTask<DefaultPnioExchange> task, PnioListener<DefaultPnioExchange> pnioListener, PnioController pnioController ) {
        super( settings, task, pnioListener, pnioController );
    }

    @Override
    protected DefaultPnioExchange createPnioExchange( HttpServerExchange oapExchange, long timeout, byte[] message,
                                                      PnioController pnioController, ComputeTask<DefaultPnioExchange> computeTask, PnioListener<DefaultPnioExchange> pnioListener ) {
        return new DefaultPnioExchange( message, responseSize, pnioController, computeTask, oapExchange, timeout, pnioListener );
    }
}
