package oap.http.pniov2;

@FunctionalInterface
public interface ComputeTask<RequestState> {
    void run( PnioExchange<RequestState> pnioExchange ) throws Exception;
}
