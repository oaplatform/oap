package oap.http.pniov2;

@FunctionalInterface
public interface ComputeTask<E extends AbstractPnioExchange<E>> {
    void accept( E pnioExchange ) throws Exception;
}
