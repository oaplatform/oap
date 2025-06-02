package oap.http.pniov2;

import javax.annotation.Nullable;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class PnioWorkQueue {
    protected final ReentrantLock lock = new ReentrantLock();
    protected final Condition notEmpty = lock.newCondition();
    private final int queueSize;
    protected Node first;
    protected Node last;
    protected int count;

    public PnioWorkQueue( int queueSize ) {
        this.queueSize = queueSize;
    }

    public boolean tryPushTask( PnioWorkerTask<?, ?> task ) {
        lock.lock();
        try {
            if( count >= queueSize ) {
                return false;
            }

            Node node = new Node( task );
            if( last == null ) {
                node.next = first;
                last = first = node;
            } else {
                last.next = node;
                last = node;
            }

            count++;

            notEmpty.signal();

            return true;
        } finally {
            lock.unlock();
        }
    }

    public void forcePushTask( PnioWorkerTask<?, ?> task ) {
        lock.lock();
        try {
            Node node = new Node( task );
            node.next = first;
            first = node;
            if( last == null ) {
                last = node;
            }

            count++;

            notEmpty.signal();
        } finally {
            lock.unlock();
        }
    }

    @Nullable
    public PnioWorkerTask<?, ?> takeTask() throws InterruptedException {
        lock.lock();
        try {
            if( first == null ) {
                notEmpty.await();
            }

            if( first == null ) {
                return null;
            }

            Node f = first;
            first = f.next;
            if( first == null ) {
                last = null;
            }

            count--;

            return f.item;
        } finally {
            lock.unlock();
        }
    }

    public int size() {
        return count;
    }

    public boolean isEmpty() {
        return count == 0;
    }

    public void signal() {
        lock.lock();
        try {
            notEmpty.signalAll();
        } finally {
            lock.unlock();
        }
    }

    protected static class Node {
        PnioWorkerTask<?, ?> item;

        Node next;

        Node( PnioWorkerTask<?, ?> x ) {
            item = x;
        }
    }
}
