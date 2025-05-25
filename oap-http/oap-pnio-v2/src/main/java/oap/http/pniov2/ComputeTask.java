package oap.http.pniov2;

@FunctionalInterface
public interface ComputeTask<State> {
    void accept( PnioExchange<State> pnioExchange, State state ) throws Exception;
}
