package oap.http.pniov2;

import oap.http.server.nio.HttpServerExchange;

public class TestPnioHandler extends AbstractPnioHttpHandler<TestPnioExchange> {
    public TestPnioHandler( PnioHttpSettings settings, ComputeTask<TestPnioExchange> task, PnioListener<TestPnioExchange> pnioListener, PnioController pnioController ) {
        super( settings, task, pnioListener, pnioController );
    }

    @Override
    protected TestPnioExchange createPnioExchange( HttpServerExchange oapExchange, long timeout, byte[] message, PnioController pnioController,
                                                   ComputeTask<TestPnioExchange> computeTask, PnioListener<TestPnioExchange> pnioListener ) {
        return new TestPnioExchange( message, responseSize, pnioController, computeTask, oapExchange, timeout, pnioListener );
    }
}
