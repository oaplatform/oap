package oap.http.pniov2;

@FunctionalInterface
public interface ComputeTask<RequestState> {
    void accept( PnioExchange<RequestState> pnioExchange ) throws Exception;
}
