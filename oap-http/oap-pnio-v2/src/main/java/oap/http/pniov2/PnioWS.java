package oap.http.pniov2;

import oap.util.Dates;
import oap.ws.WsMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static oap.http.server.nio.HttpServerExchange.HttpMethod.GET;

public class PnioWS {
    private final Map<String, PnioHttpHandlerReference> pnioHttpHandler;

    public PnioWS( Map<String, PnioHttpHandlerReference> pnioHttpHandler ) {
        this.pnioHttpHandler = pnioHttpHandler;
    }

    @WsMethod( method = GET, path = "/" )
    public List<PnioExchangeView> queue() {
        ArrayList<PnioExchangeView> views = new ArrayList<>();

        pnioHttpHandler.forEach( ( name, handler ) -> {
            for( AbstractPnioExchange pnioExchange : handler.getPnioHttpHandler().exchanges.values() ) {
                views.add( new PnioExchangeView( name, pnioExchange.processState.name(),
                    pnioExchange.id, pnioExchange.isRequestGzipped(), pnioExchange.oapExchange.getRequestURI(),
                    pnioExchange.getRequestStartTime(), pnioExchange.getTimeLeftNano() ) );
            }
        } );

        return views;
    }

    public static class PnioExchangeView {
        public final String pnioHandler;
        public final String processState;
        public final long id;
        public final boolean requestGzipped;
        public final String requestURI;
        public final String duration;
        public final String timeLeft;

        public PnioExchangeView( String pnioHandler, String processState, long id, boolean requestGzipped,
                                 String requestURI, long requestStartTime, long timeLeftNano ) {
            this.pnioHandler = pnioHandler;
            this.processState = processState;
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
