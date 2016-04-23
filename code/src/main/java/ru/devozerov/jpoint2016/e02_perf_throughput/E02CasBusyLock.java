package ru.devozerov.jpoint2016.e02_perf_throughput;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/**
 * Busy lock implementation based on CAS loop.
 * <p>
 * Read lock acquisition is <b>lock-free</b> if there are no writers.
 */
public class E02CasBusyLock {
    /** Mask to check whether there writer asked for lock. */
    private static int WRITER_MASK = 1 << 30;

    /** State which encodes amount of active readers and whether writer has asked for lock. */
    private static final AtomicIntegerFieldUpdater<E02CasBusyLock> STATE_UPDATER =
        AtomicIntegerFieldUpdater.newUpdater(E02CasBusyLock.class, "state");

    /** State which encodes amount of active readers and whether writer has asked for lock. */
    private volatile int state;

    /**
     * Acquire read lock.
     *
     * @return {@code True} if acquired.
     */
    public boolean tryAcquire() {
        while (true) {
            int state0 = STATE_UPDATER.get(this);

            if ((state0 & WRITER_MASK) == WRITER_MASK)
                return false;

            if (STATE_UPDATER.compareAndSet(this, state0, state0 + 1))
                return true;
        }
    }

    /**
     * Release read lock.
     */
    public void release() {
        STATE_UPDATER.decrementAndGet(this);
    }

    /**
     * Acquire write lock.
     */
    public void block() {
        // First phase:
        while (true) {
            int state0 = STATE_UPDATER.get(this);

            if (STATE_UPDATER.compareAndSet(this, state0, state0 | WRITER_MASK))
                break;
        }

        while (STATE_UPDATER.get(this) != WRITER_MASK)
            Thread.yield();
    }
}
