package oap.http.pniov2;

import java.util.concurrent.CompletableFuture;

@FunctionalInterface
public interface AsyncTask<T, RequestState> {
    CompletableFuture<T> apply( PnioExchange<RequestState> pnioExchange ) throws Exception;
}
