package ru.devozerov.jpoint2016.e02_perf_throughput;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Threads;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.locks.ReentrantLock;

/**
 * Benchmark to test throughput of various busy lock implementations. We use relatively small
 * payload {@code Blackhole.consumeCPU(10)} to keep the benchmark focused on hardware
 * infrastructure and better demonstrate contention effects.
 * <p>
 * {@link E02CasBusyLock} has the worst scalability due to very high contention on
 * a shared memory chunk.
 * <p>
 * {@link E02IncrementBusyLock} shows slightly better numbers, thanks to it's wait-free
 * nature. However, scalability is still very poor.
 * <p>
 * {@link E02CasBackoffBusyLock} demonstrates neutral scalability and outperforms both
 * CAS- and increment-based locks because with small payload contention is so high, that it
 * is better to make some threads "colder" to achieve better throughput.
 * <p>
 * {@link E02CasStripedBusyLock} has linear scalability when each thread is assigned with an
 * unique stripe index, because there is no contention at all.
 */
@Warmup(iterations = 15)
@Measurement(iterations = 15)
@Fork(1)
public class E02Benchmark {
    /** CAS lock. */
    private static final E02CasBusyLock CAS_LOCK = new E02CasBusyLock();

    /** Increment lock. */
    private static final E02IncrementBusyLock INCREMENT_LOCK = new E02IncrementBusyLock();

    /** Striped CAS lock. */
    private static final E02CasStripedBusyLock CAS_STRIPED_LOCK = new E02CasStripedBusyLock();

    /** CAS lock with backoff. */
    private static final E02CasBackoffBusyLock CAS_BACKOFF_LOCK = new E02CasBackoffBusyLock();

    /** Standard reentrant lock. */
    private static final ReentrantLock LOCK = new ReentrantLock();

    /** Standard reentrant lock with fairness. */
    private static final ReentrantLock LOCK_FAIR = new ReentrantLock(true);

    @Benchmark
    @Threads(1)
    public void cas_01() {
        cas();
    }

    @Benchmark
    @Threads(2)
    public void cas_02() {
        cas();
    }

    @Benchmark
    @Threads(4)
    public void cas_04() {
        cas();
    }

    @Benchmark
    @Threads(8)
    public void cas_08() {
        cas();
    }

    private void cas() {
        if (CAS_LOCK.tryAcquire()) {

            try {
                payload();
            } finally {
                CAS_LOCK.release();
            }
        }
        else
            throw new RuntimeException("Should never happen!");
    }

    @Benchmark
    @Threads(1)
    public void increment_01() {
        increment();
    }

    @Benchmark
    @Threads(2)
    public void increment_02() {
        increment();
    }

    @Benchmark
    @Threads(4)
    public void increment_04() {
        increment();
    }

    @Benchmark
    @Threads(8)
    public void increment_08() {
        increment();
    }

    private void increment() {
        if (INCREMENT_LOCK.tryAcquire()) {

            try {
                payload();
            } finally {
                INCREMENT_LOCK.release();
            }
        }
        else
            throw new RuntimeException("Should never happen!");
    }

    @Benchmark
    @Threads(1)
    public void casStriped_01() {
        casStriped();
    }

    @Benchmark
    @Threads(2)
    public void casStriped_02() {
        casStriped();
    }

    @Benchmark
    @Threads(4)
    public void casStriped_04() {
        casStriped();
    }

    @Benchmark
    @Threads(8)
    public void casStriped_08() {
        casStriped();
    }

    private void casStriped() {
        if (CAS_STRIPED_LOCK.tryAcquire()) {

            try {
                payload();
            } finally {
                CAS_STRIPED_LOCK.release();
            }
        }
        else
            throw new RuntimeException("Should never happen!");
    }

    @Benchmark
    @Threads(1)
    public void casBackoff_01() throws InterruptedException {
        casBackoff();
    }

    @Benchmark
    @Threads(2)
    public void casBackoff_02() throws InterruptedException {
        casBackoff();
    }

    @Benchmark
    @Threads(4)
    public void casBackoff_04() throws InterruptedException {
        casBackoff();
    }

    @Benchmark
    @Threads(8)
    public void casBackoff_08() throws InterruptedException {
        casBackoff();
    }

    private void casBackoff() throws InterruptedException {
        if (CAS_BACKOFF_LOCK.tryAcquire()) {

            try {
                payload();
            } finally {
                CAS_BACKOFF_LOCK.release();
            }
        }
        else
            throw new RuntimeException("Should never happen!");
    }

    @Benchmark
    @Threads(1)
    public void lock_01() {
        lock();
    }

    @Benchmark
    @Threads(2)
    public void lock_02() {
        lock();
    }

    @Benchmark
    @Threads(4)
    public void lock_04() {
        lock();
    }

    @Benchmark
    @Threads(8)
    public void lock_08() {
        lock();
    }

    private void lock() {
        LOCK.lock();

        try {
            payload();
        }
        finally {
            LOCK.unlock();
        }
    }

    @Benchmark
    @Threads(1)
    public void lock_fair_01() {
        lock_fair();
    }

    @Benchmark
    @Threads(2)
    public void lock_fair_02() {
        lock_fair();
    }

    @Benchmark
    @Threads(4)
    public void lock_fair_04() {
        lock_fair();
    }

    @Benchmark
    @Threads(8)
    public void lock_fair_08() {
        lock_fair();
    }

    private void lock_fair() {
        LOCK_FAIR.lock();

        try {
            payload();
        }
        finally {
            LOCK_FAIR.unlock();
        }
    }

    /**
     * Actual payload.
     */
    private void payload() {
        Blackhole.consumeCPU(10);
    }

    /**
     * Runner.
     */
    public static void main(String[] args) throws Exception {
        new Runner(new OptionsBuilder().include(E02Benchmark.class.getSimpleName()).build()).run();
    }
}
