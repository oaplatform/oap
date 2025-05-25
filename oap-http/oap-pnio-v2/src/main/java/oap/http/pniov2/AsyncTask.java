package oap.http.pniov2;

import java.util.concurrent.CompletableFuture;

@FunctionalInterface
public interface AsyncTask<State> {
    CompletableFuture<Void> apply( PnioExchange<State> pnioExchange, State state ) throws Exception;
}
