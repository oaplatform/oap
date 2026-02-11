package oap.http.client;

import org.eclipse.jetty.client.Response;

import java.util.concurrent.CompletableFuture;

public class InputStreamResponseListener extends org.eclipse.jetty.client.InputStreamResponseListener {
    public final CompletableFuture<Response> responseAsync = new CompletableFuture<>();

    @Override
    public void onHeaders( Response response ) {
        super.onHeaders( response );

        responseAsync.complete( response );
    }

    @Override
    public void onFailure( Response response, Throwable failure ) {
        super.onFailure( response, failure );

        responseAsync.completeExceptionally( failure );
    }

    public CompletableFuture<Response> getResponseAsync() {
        return responseAsync;
    }
}
