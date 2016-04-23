package ru.devozerov.jpoint2016.e02_perf_throughput;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/**
 * Busy lock implementation based on CAS loop.
 * <p>
 * Read lock acquisition is <b>wait-free</b> if there are no writers.
 * <p>
 * Note that although pretty simple, this implementation could lead to writer starvation.
 * Consider the following sequence of actions:
 * <ul>
 *     <li>Thread 1 demands write lock setting the writer bit;</li>
 *     <li>Thread 2 tries to acquire a read lock and performs increment (0 -> 1);</li>
 *     <li>Thread 1 sees a pseudo-active reader (Thread2) and retries the loop;</li>
 *     <li>Thread 3 tries to acquire a read lock and performs increment (1 -> 2);</li>
 *     <li>Thread 2 sees the writer, performs decrement and exits with {@code false} (2 -> 1);</li>
 *     <li>Thread 1 checks reader count and still see one reader, so it retries the loop again;</li>
 *     <li>Thread X tries to acquire a read lock ...</li>
 * </ul>
 * This way it is possible that new readers will not acquire a read lock and return {@code false} to
 * the callee, but writer will never be able to finally acquire a write lock.
 * <p>
 * To mitigate the problem it makes sense to perform additional check for writer bit before increment.
 * However, under high contention this solution will be less than optimal because additional volatile
 * read will be performed on a cache line which is likely to be invalidated, thus effectively doubling
 * bus traffic for the whole read lock routine.
 * <p>
 * We could optimize this if we place writer bit far enough from the "state" variable to avoid false-sharing.
 * This way writer check will be performed on a separate cache line which is only rarely updated.
 * <p>
 * NB: As it was correctly mentioned by Tagir Valeev during the talk, increment/decrement is not necessarily an
 * operation which completes in a bounded time. In fact, this is true for x86 and Java 8 and later. In this
 * increment will compile to LOCK XADD. Otherwise it could still be an infinite loop based on CAS. As such, this
 * algorithm could be considered either wait-free or lock-free depending on the environment and JDK implementation
 * details.
 */
public class E02IncrementBusyLock {
    /** Updater for "state" field. */
    private static final AtomicIntegerFieldUpdater<E02IncrementBusyLock> READERS_UPDATER =
        AtomicIntegerFieldUpdater.newUpdater(E02IncrementBusyLock.class, "readers");

    /** State which encodes amount of active readers and whether writer has asked for lock. */
    private volatile int readers;

    /** Writer flag. */
    private volatile boolean writer;

    /**
     * Acquire read lock.
     *
     * @return {@code True} if acquired.
     */
    public boolean tryAcquire() {
        READERS_UPDATER.incrementAndGet(this);

        if (writer) {
            READERS_UPDATER.decrementAndGet(this);

            return false;
        }

        return true;
    }

    /**
     * Release read lock.
     */
    public void release() {
        READERS_UPDATER.decrementAndGet(this);
    }

    /**
     * Acquire write lock.
     */
    public void block() {
        writer = true;

        while (READERS_UPDATER.get(this) != 0)
            Thread.yield();
    }
}
