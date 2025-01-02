package oap.http.pnio;

import java.io.IOException;

@SuppressWarnings( "checkstyle:AbstractClassName" )
public abstract class BlockingPnioRequestHandler<State> extends AbstractPnioRequestHandler<State> {
    public abstract void handle( PnioExchange<State> pnioExchange, State state ) throws InterruptedException, IOException;
}
