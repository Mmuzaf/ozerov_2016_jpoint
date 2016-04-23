package ru.devozerov.jpoint2016.e02_perf_throughput;

import org.openjdk.jol.info.ClassLayout;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/**
 * Same implementation as {@link E02CasBusyLock}, but with stripes to minimize sharing for
 * readers at the cost of increased memory overhead and decreased writer performance.
 * <p>
 * Every thread is assigned with an index on first access. We use this index to find correct
 * stripe for the thread.
 * <p>
 * Each stripe is essentially a separate busy lock. As different threads work with different locks,
 * true sharing is minimized. And false sharing is avoided using padding, which ensures that stripes
 * are at least 128 bytes away from each other.
 */
public class E02CasStripedBusyLock {
    /** Hard-coded number of stripes (for simplicity). */
    private static final int STRIPE_CNT = 16;

    /** Counter to generate thread indexes. */
    private static final AtomicInteger THREAD_IDX_CTR = new AtomicInteger();

    /** Thread index. */
    private static ThreadLocal<Integer> THREAD_IDX = new ThreadLocal<Integer>() {
        @Override protected Integer initialValue() {
            return THREAD_IDX_CTR.getAndIncrement() % STRIPE_CNT;
        }
    };

    /** Stripes. */
    private final InnerLock[] locks;

    /**
     * Constructor.
     */
    public E02CasStripedBusyLock() {
        locks = new InnerLock[STRIPE_CNT];

        for (int i = 0; i < STRIPE_CNT; i++)
            locks[i] = new InnerLock();
    }

    /**
     * Acquire read lock.
     *
     * @return {@code True} if acquired.
     */
    public boolean tryAcquire() {
        return lockForThread().tryReadLock();
    }

    /**
     * Release read lock.
     */
    public void release() {
        lockForThread().readUnlock();
    }

    /**
     * Acquire write lock.
     */
    public void block() {
        for (int i = 0; i < STRIPE_CNT; i++)
            locks[i].writeLock();
    }

    /**
     * Get lock for current thread.
     *
     * @return Lock for current thread.
     */
    private InnerLock lockForThread() {
        return locks[THREAD_IDX.get()];
    }

    /**
     * Inner lock implementation.
     */
    private static class InnerLock {
        /** Mask to check whether there writer asked for lock. */
        private static int WRITER_MASK = 1 << 30;

        /** State which encodes amount of active readers and whether writer has asked for lock. */
        private static final AtomicIntegerFieldUpdater<InnerLock> STATE_UPDATER =
            AtomicIntegerFieldUpdater.newUpdater(InnerLock.class, "state");

        /** State which encodes amount of active readers and whether writer has asked for lock. */
        private volatile int state;

        /** Padding to ensure that locks are at least 128 bytes far from each other. */
        private long               pad02, pad03, pad04, pad05, pad06, pad07,
                     pad10, pad11, pad12, pad13, pad14, pad15, pad16, pad17;

        /**
         * Acquire read lock.
         *
         * @return {@code True} if acquired.
         */
        public boolean tryReadLock() {
            while (true) {
                int state0 = STATE_UPDATER.get(this);

                if ((state0 & WRITER_MASK) == WRITER_MASK)
                    return false;
                else {
                    if (STATE_UPDATER.compareAndSet(this, state0, state0 + 1))
                        return true;
                }
            }
        }

        /**
         * Release read lock.
         */
        public void readUnlock() {
            STATE_UPDATER.decrementAndGet(this);
        }

        /**
         * Acquire write lock.
         */
        public void writeLock() {
            while (true) {
                int state0 = STATE_UPDATER.get(this);

                if (STATE_UPDATER.compareAndSet(this, state0, state0 | WRITER_MASK))
                    break;
            }

            while (STATE_UPDATER.get(this) != WRITER_MASK)
                Thread.yield();
        }
    }

    /**
     * Entry point.
     * <p>
     * Prints {@link InnerLock} layout to the console.
     */
    public static void main(String[] args) {
        System.out.println(ClassLayout.parseInstance(new InnerLock()).toPrintable());
    }
}
