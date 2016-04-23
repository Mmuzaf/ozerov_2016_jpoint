package ru.devozerov.jpoint2016.e01_perf_latency;

import java.util.Collection;
import java.util.Iterator;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

/**
 * Special implementation of {@link BlockingQueue} which delegates to {@link ConcurrentLinkedQueue}
 * and spins during blocking calls.
 */
@SuppressWarnings({"NullableProblems", "SuspiciousToArrayCall", "SuspiciousMethodCalls"})
public class E01SpinningBlockingQueue<T> implements BlockingQueue<T> {
    /** Underlying non-blocking queue. */
    private final ConcurrentLinkedQueue<T> queue = new ConcurrentLinkedQueue<>();

    /** {@inheritDoc} */
    @Override public boolean add(T t) {
        return queue.add(t);
    }

    /** {@inheritDoc} */
    @Override public boolean offer(T t) {
        return queue.offer(t);
    }

    /** {@inheritDoc} */
    @Override public void put(T t) throws InterruptedException {
        queue.add(t);
    }

    /** {@inheritDoc} */
    @Override public boolean offer(T t, long l, TimeUnit timeUnit) throws InterruptedException {
        return queue.offer(t);
    }

    /** {@inheritDoc} */
    @Override public int remainingCapacity() {
        return Integer.MAX_VALUE;
    }

    /** {@inheritDoc} */
    @Override public boolean remove(Object o) {
        return queue.remove(o);
    }

    /** {@inheritDoc} */
    @Override public boolean contains(Object o) {
        return queue.contains(o);
    }

    /** {@inheritDoc} */
    @Override public int drainTo(Collection<? super T> c) {
        // Dummy implementation to avoid exceptions during thread pool shutdown.
        return 0;
    }

    /** {@inheritDoc} */
    @Override public int drainTo(Collection<? super T> c, int i) {
        throw new UnsupportedOperationException();
    }

    /** {@inheritDoc} */
    @Override public T remove() {
        return queue.remove();
    }

    /** {@inheritDoc} */
    @Override public T take() throws InterruptedException {
        while (true) {
            T res = queue.poll();

            if (res != null)
                return res;

            if (Thread.interrupted())
                throw new InterruptedException();
        }
    }

    /** {@inheritDoc} */
    @Override public T poll(long l, TimeUnit timeUnit) throws InterruptedException {
        while (true) {
            T res = queue.poll();

            if (res != null)
                return res;

            if (Thread.interrupted())
                throw new InterruptedException();
        }
    }

    /** {@inheritDoc} */
    @Override public T poll() {
        return queue.poll();
    }

    /** {@inheritDoc} */
    @Override public T element() {
        return queue.element();
    }

    /** {@inheritDoc} */
    @Override public T peek() {
        return queue.peek();
    }

    /** {@inheritDoc} */
    @Override public int size() {
        return queue.size();
    }

    /** {@inheritDoc} */
    @Override public boolean isEmpty() {
        return queue.isEmpty();
    }

    /** {@inheritDoc} */
    @Override public Iterator<T> iterator() {
        return queue.iterator();
    }

    /** {@inheritDoc} */
    @Override public Object[] toArray() {
        return queue.toArray();
    }

    /** {@inheritDoc} */
    @Override public <T1> T1[] toArray(T1[] a) {
        return queue.toArray(a);
    }

    /** {@inheritDoc} */
    @Override public boolean containsAll(Collection<?> c) {
        return queue.contains(c);
    }

    /** {@inheritDoc} */
    @Override public boolean addAll(Collection<? extends T> c) {
        return queue.addAll(c);
    }

    /** {@inheritDoc} */
    @Override public boolean removeAll(Collection<?> c) {
        return queue.removeAll(c);
    }

    /** {@inheritDoc} */
    @Override public boolean retainAll(Collection<?> c) {
        return queue.retainAll(c);
    }

    /** {@inheritDoc} */
    @Override public void clear() {
        queue.clear();
    }
}
