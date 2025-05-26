package oap.http.pniov2;

import java.util.concurrent.CompletableFuture;

@FunctionalInterface
public interface AsyncTask<T, E extends AbstractPnioExchange<E>> {
    CompletableFuture<T> apply( E pnioExchange ) throws Exception;
}
