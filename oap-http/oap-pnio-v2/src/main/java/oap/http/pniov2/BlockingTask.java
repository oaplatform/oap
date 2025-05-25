package oap.http.pniov2;

@FunctionalInterface
public interface BlockingTask<State> {
    void accept( PnioExchange<State> pnioExchange, State state ) throws Exception;
}
