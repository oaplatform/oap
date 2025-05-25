package oap.http.pniov2;

public interface AsyncRunnable {
    void fork();

    void join();
}
