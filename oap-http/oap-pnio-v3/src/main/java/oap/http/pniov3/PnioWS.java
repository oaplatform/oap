package oap.http.pniov3;

import oap.util.Dates;
import oap.ws.WsMethod;
import org.apache.commons.lang3.mutable.MutableObject;

import java.util.ArrayList;
import java.util.Map;

import static oap.http.server.nio.HttpServerExchange.HttpMethod.GET;

public class PnioWS {
    private final Map<String, PnioHttpHandlerReference> pnioHttpHandler;

    public PnioWS( Map<String, PnioHttpHandlerReference> pnioHttpHandler ) {
        this.pnioHttpHandler = pnioHttpHandler;
    }

    @WsMethod( method = GET, path = "/" )
    public PnioView queue() {
        PnioView pnioView = new PnioView();

        MutableObject<PnioController> pnioController = new MutableObject<>();

        pnioHttpHandler.forEach( ( name, handler ) -> {
            for( PnioExchange<?> pnioExchange : handler.getPnioHttpHandler().exchanges.values() ) {
                if( pnioController.getValue() == null ) {
                    pnioController.setValue( pnioExchange.controller );
                }
                pnioView.exchanges.add( new PnioExchangeView( name, pnioExchange.printState(),
                    pnioExchange.id, pnioExchange.isRequestGzipped(), pnioExchange.oapExchange.getRequestURI(),
                    pnioExchange.getRequestStartTime(), pnioExchange.getTimeLeftNano() ) );
            }
        } );

        if( pnioController.getValue() != null ) {
            pnioView.tasks = pnioController.getValue().pool.getQueuedTaskCount();
            pnioView.importantTasks = pnioController.getValue().importantPool.getQueuedTaskCount();
        }

        return pnioView;
    }

    public static class PnioView {
        public final ArrayList<PnioExchangeView> exchanges = new ArrayList<>();
        public long tasks;
        public long importantTasks;
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
