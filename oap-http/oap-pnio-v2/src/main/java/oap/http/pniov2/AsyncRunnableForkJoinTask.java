package oap.http.pniov2;

import java.util.concurrent.ForkJoinTask;

public class AsyncRunnableForkJoinTask implements AsyncRunnable {
    private final ForkJoinTask<Void> task;

    public AsyncRunnableForkJoinTask( ForkJoinTask<Void> task ) {
        this.task = task;
    }

    @Override
    public void fork() {
        task.fork();
    }

    @Override
    public void join() {
        task.join();
    }
}
