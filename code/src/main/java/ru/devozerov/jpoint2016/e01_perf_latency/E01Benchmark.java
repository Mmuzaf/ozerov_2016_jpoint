package ru.devozerov.jpoint2016.e01_perf_latency;

import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * Demonstrates how thread de/rescheduling could affect performance in terms of latency.
 * <p>
 * Benchmark submits a simple task to {@link ThreadPoolExecutor} and waits for its completion
 * spinning on a volatile variable. Benchmark implementation idea is borrowed from Martin Thompson's
 * talk <a href="http://www.infoq.com/presentations/low-latency-concurrrent-java-8">
 * The Quest for Low-latency with Concurrent Java</a> and is simplified to keep focus on a single
 * message pass between two threads.
 * <p>
 * Note that <b>this benchmark can only be run with a single thread</b>.
 * <p>
 * We test two thread pools: one with standard {@link ArrayBlockingQueue{, and another with
 * custom {@link E01SpinningBlockingQueue}.
 * <p>
 * Thread pool with conventional {@code ArrayBlockingQueue} has much higher latency. This is caused
 * by expensive thread park/unpark operations, which require at least several microseconds to
 * propagate a signal from active to inactive thread. In the worst case latency hit might be several
 * orders of magnitude higher, e.g. due to cache misses induced by migration to other core, or
 * evictions caused by previously executed thread on the same core.
 * <p>
 * To the contrast, {@code E01SpinningBlockingQueue} has about an order of magnitude lower latency
 * because control is passed between two "hot" threads. On the other hand, worker thread consumes
 * the whole core as it constantly spins in a busy loop waiting for new tasks.
 * <p>
 * You may see some run-to-run variance caused by threads being assigned to different cores. That
 * is, results could be different if both benchmark thread and worker thread are scheduled either
 * on the same physical core, or on different cores. To have better control over it, you may want
 * to employ libraries for affinity management, such as
 * <a href="http://mvnrepository.com/artifact/net.openhft/affinity">net.openhft.affinity</a>.
 */
@Warmup(iterations = 15)
@Measurement(iterations = 15)
@Fork(1)
@BenchmarkMode(Mode.SampleTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
@State(Scope.Benchmark)
public class E01Benchmark {
    /** Executor with ArrayBlockingQueue. */
    private static final ThreadPoolExecutor BLOCKING_TPE =
        new ThreadPoolExecutor(1, 1, Long.MAX_VALUE, TimeUnit.MILLISECONDS, new ArrayBlockingQueue<>(2));

    /** Executor with spinning queue. */
    private static final ThreadPoolExecutor SPINNING_TPE =
        new ThreadPoolExecutor(1, 1, Long.MAX_VALUE, TimeUnit.MILLISECONDS, new E01SpinningBlockingQueue<>());

    /** Flag. */
    private static volatile int flag;

    /**
     * Tear down routine.
     */
    @TearDown
    public void tearDown() {
        BLOCKING_TPE.shutdownNow();
        SPINNING_TPE.shutdownNow();
    }

    /**
     * Test executor with ArrayBlockingQueue.
     */
    @Benchmark
    public void arrayBlockingQueue() {
        run(BLOCKING_TPE);
    }

    /**
     * Test executor with spinning queue.
     */
    @Benchmark
    public void spinningBlockingQueue() {
        run(SPINNING_TPE);
    }

    /**
     * Run's the benchmark.
     *
     * @param exec Executor to be tested.
     */
    @SuppressWarnings("StatementWithEmptyBody")
    private void run(ThreadPoolExecutor exec) {
        flag = 1;

        exec.submit(() -> flag = 0);

        while (flag == 1) {}
    }

    /**
     * Runner.
     */
    public static void main(String[] args) throws Exception {
        new Runner(new OptionsBuilder().include(E01Benchmark.class.getSimpleName()).build()).run();
    }
}
