package oap.http.pniov2;

import java.util.concurrent.CompletableFuture;

@FunctionalInterface
public interface AsyncTask<T, State> {
    CompletableFuture<T> apply( PnioExchange<State> pnioExchange, State state ) throws Exception;
}
