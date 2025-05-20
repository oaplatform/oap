package oap.http.pniov2;

import oap.util.Dates;
import oap.ws.WsMethod;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static oap.http.server.nio.HttpServerExchange.HttpMethod.GET;

public class PnioWS {
    private final Map<String, PnioHttpHandler<?>> pnioHttpHandler;

    public PnioWS( Map<String, PnioHttpHandler<?>> pnioHttpHandler ) {
        this.pnioHttpHandler = pnioHttpHandler;
    }

    @WsMethod( method = GET, path = "/" )
    public List<PnoExchangeView> queue() {
        ArrayList<PnoExchangeView> views = new ArrayList<>();

        pnioHttpHandler.forEach( ( name, handler ) -> {
            for( PnioExchange<?> pnioExchange : handler.exchanges.values() ) {
                views.add( new PnoExchangeView( name, pnioExchange.processState, pnioExchange.getCurrentTaskName(),
                    pnioExchange.id, pnioExchange.isRequestGzipped(), pnioExchange.oapExchange.getRequestURI(),
                    pnioExchange.getRequestStartTime(), pnioExchange.getTimeLeftNano() ) );
            }
        } );

        return views;
    }

    public static class PnoExchangeView {
        public final String pnioHandler;
        public final PnioExchange.ProcessState processState;
        public final String currentTaskName;
        public final long id;
        public final boolean requestGzipped;
        public final String requestURI;
        public final String duration;
        public final String timeLeft;

        public PnoExchangeView( String pnioHandler, PnioExchange.ProcessState processState, String currentTaskName, long id, boolean requestGzipped,
                                String requestURI, long requestStartTime, long timeLeftNano ) {
            this.pnioHandler = pnioHandler;
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
