package oap.http.pniov3;

@FunctionalInterface
public interface ComputeTask<RequestState> {
    void run( PnioExchange<RequestState> pnioExchange ) throws Exception;
}
