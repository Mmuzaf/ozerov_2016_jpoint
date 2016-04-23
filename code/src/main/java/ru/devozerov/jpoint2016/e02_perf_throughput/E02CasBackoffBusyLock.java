package ru.devozerov.jpoint2016.e02_perf_throughput;

import org.openjdk.jmh.infra.Blackhole;

import java.util.concurrent.atomic.AtomicIntegerFieldUpdater;

/**
 * Same implementation as {@link E02CasBusyLock}, but with simple tiered backoff strategy
 * to decrease contention. Adopted from .NET
 * <a href="http://referencesource.microsoft.com/#mscorlib/system/threading/SpinLock.cs">SpinLock</a>
 * class.
 * <p>
 * As opposed to other implementations, we count failed attempts. Based on this number we choose
 * different backoff policies:
 * <ul>
 *     <li>First we try to spin with dummy payload. Now it is {@link Blackhole#consumeCPU(long)}, while
 *     .NET {@code SpinLock} relies on dummy cycles with {@code PAUSE} x86 instruction which improves
 *     spinning latency. We do not have access to this instruction in Java yet, but we might expect
 *     it to be in Java 9 (see <a href="http://openjdk.java.net/jeps/285">JEP 285</a>), thanks to
 *     <b>Azul Systems</b> efforts.</li>
 *     <li>If contention is high enough, we eventually fallback to {@code Thread.sleep(0)}, which
 *     effectively gives up thread's time quantum. If there are more threads than cores, this policy
 *     could improve situation.</li>
 *     <li>If yielding policy doesn't help we finally fallback to real thread sleep calling
 *     {@code Thread.sleep(1)}.</li>
 * </ul>
 */
public class E02CasBackoffBusyLock {
    /** Mask to check whether there writer asked for lock. */
    private static int WRITER_MASK = 1 << 30;

    /** Heuristic for spin backoff. */
    private static final int LOCK_SPIN_CYCLES = 20;

    /** Heuristic for spin backoff. */
    private static final int LOCK_SPIN_CNT = 10;

    /** Heuristic for sleep backoff. */
    private static final int LOCK_SLEEP_0_CNT = 5;

    /** State which encodes amount of active readers and whether writer has asked for lock. */
    private static final AtomicIntegerFieldUpdater<E02CasBackoffBusyLock> STATE_UPDATER =
        AtomicIntegerFieldUpdater.newUpdater(E02CasBackoffBusyLock.class, "state");

    /** State which encodes amount of active readers and whether writer has asked for lock. */
    private volatile int state;

    /**
     * Acquire read lock.
     *
     * @return {@code True} if acquired.
     */
    public boolean tryAcquire() throws InterruptedException {
        for (int i = 0;; i++) {
            int state0 = STATE_UPDATER.get(this);

            if ((state0 & WRITER_MASK) == WRITER_MASK)
                return false;

            if (STATE_UPDATER.compareAndSet(this, state0, state0 + 1))
                return true;
            else {
                if (i < LOCK_SPIN_CNT) {
                    for (int j = 0; j < LOCK_SPIN_CYCLES * (i + 1); j++)
                        Blackhole.consumeCPU(j);
                }
                else if (i < (LOCK_SPIN_CNT + LOCK_SLEEP_0_CNT))
                    Thread.sleep(0); // Give up my quantum.
                else
                    Thread.sleep(1); // Real sleep.
            }
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
        while (true) {
            int state0 = STATE_UPDATER.get(this);

            if (STATE_UPDATER.compareAndSet(this, state0, state0 | WRITER_MASK))
                break;
        }

        while (STATE_UPDATER.get(this) != WRITER_MASK)
            Thread.yield();
    }
}
