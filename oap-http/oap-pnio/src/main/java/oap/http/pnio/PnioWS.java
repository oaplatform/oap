package oap.http.pnio;

import oap.util.Dates;
import oap.ws.WsMethod;

import java.util.ArrayList;
import java.util.List;

import static oap.http.server.nio.HttpServerExchange.HttpMethod.GET;

public class PnioWS<WorkflowState> {
    private final PnioHttpHandler<WorkflowState> pnioHttpHandler;

    public PnioWS( PnioHttpHandler<WorkflowState> pnioHttpHandler ) {
        this.pnioHttpHandler = pnioHttpHandler;
    }

    @WsMethod( method = GET, path = "/" )
    public List<PnoExchangeView> queue() {
        ArrayList<PnoExchangeView> views = new ArrayList<>();

        for( PnioExchange<WorkflowState> pnioExchange : pnioHttpHandler.exchanges ) {
            views.add( new PnoExchangeView( pnioExchange.processState, pnioExchange.getCurrentTaskName(),
                pnioExchange.id, pnioExchange.isRequestGzipped(), pnioExchange.oapExchange.getRequestURI(),
                pnioExchange.getRequestStartTime(), pnioExchange.getTimeLeftNano() ) );
        }

        return views;
    }

    public static class PnoExchangeView {
        public final PnioExchange.ProcessState processState;
        public final String currentTaskName;
        public final long id;
        public final boolean requestGzipped;
        public final String requestURI;
        public final String duration;
        public final String timeLeft;

        public PnoExchangeView( PnioExchange.ProcessState processState, String currentTaskName, long id, boolean requestGzipped,
                                String requestURI, long requestStartTime, long timeLeftNano ) {
            this.processState = processState;
            this.currentTaskName = currentTaskName;
            this.id = id;
            this.requestGzipped = requestGzipped;
            this.requestURI = requestURI;
            this.duration = nonoDurationToString( System.nanoTime() - requestStartTime );
            this.timeLeft = nonoDurationToString( timeLeftNano );
        }

        private String nonoDurationToString( long nanos ) {
            long ms = nanos / 1_000_000;
            long diff = nanos - ms * 1_000_000;
            return Dates.durationToString( ms ) + ( diff > 0 ? diff + "ns" : "" );
        }
    }
}
