package org.infinispan.crucial.benchmarks.count;

import com.google.common.util.concurrent.AtomicDouble;
import org.infinispan.crucial.Factory;
import org.infinispan.crucial.Counter;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


/**
 * @author Pierre Sutra
 */
public class Benchmark {

    @Option(name = "-clients", required = true, usage = "# clients")
    int C;

    @Option(name = "-counters", required = true, usage = "# counters")
    int N;

    @Option(name = "-increments", required = true, usage = "# increments")
    int T;

    @Option(name = "-server", required = false, usage = "connection string to server")
    String server = "127.0.0.1:11222";

    @Option(name = "-verbose", required = false, usage = "real-time report of ops/sec")
    boolean verbosity = false;

    public static void main(String args[]) {
        new Benchmark().doMain(args);
        System.exit(0);
    }

    public void doMain(String[] args) {

        CmdLineParser parser = new CmdLineParser(this);
        parser.setUsageWidth(80);
        try {
            if (args.length < 6)
                throw new CmdLineException(parser, "Not enough arguments are given.");
            parser.parseArgument(args);
        } catch (CmdLineException e) {
            System.err.println(e.getMessage());
            parser.printUsage(System.err);
            System.err.println();
            return;
        }

        Factory factory = Factory.get(server);
        ExecutorService service = Executors.newFixedThreadPool(C + 1);

        // create N counters
        List<Counter> hits = new ArrayList<>();
        for (int i = 0; i < N; i++) {
            hits.add(new Counter("counter" + i));
        }

        // create C clients
        List<IncrementCallable> clients = new ArrayList<>();
        for (int i = 0; i < C; i++) {
            clients.add(new IncrementCallable(T, hits));
        }

        // run clients then print results
        try {

            if (verbosity)
                service.submit(new VerbosityCallable(clients));

            List<Future<Double>> futures = service.invokeAll(clients);

            double avgTime = 0;
            for (Future<Double> future : futures) {
                try {
                    avgTime += future.get();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            avgTime = avgTime / futures.size();
            System.out.println("Average time: " + avgTime + " [Throughput=" + (1 / avgTime) * clients.size() + "]");

        } catch (InterruptedException | ExecutionException e) {
            e.printStackTrace();
        }

        factory.close();
    }

    private static class IncrementCallable implements Callable<Double> {

        private int T;
        private List<Counter> counters;
        private List<Long> latencies;
        private Random random;

        private AtomicDouble throughput;
        private boolean isOver;

        public IncrementCallable(int T, List<Counter> counters) {
            this.counters = counters;
            this.T = T;
            random = new Random();

            latencies = new ArrayList<>();
            throughput = new AtomicDouble(0);
            isOver = false;
        }

        public double getThroughput() {
            return throughput.get();
        }

        public boolean getIsOver() {
            return isOver;
        }

        @Override
        public Double call() throws Exception {
            long beg = System.currentTimeMillis();
            // increment T counters at random
            for (int t = 0; t < T; t++) {
                long start = System.nanoTime();
                counters.get(random.nextInt(counters.size())).increment();
                latencies.add(System.nanoTime() - start);
                double avrgLatency = 0; // average latency over the last 100 operations
                int count = Math.min(1000, latencies.size());
                for (int i = 1; i <= count; i++) {
                    avrgLatency += latencies.get(latencies.size() - i);
                }
                avrgLatency = avrgLatency / ((double) count);
                avrgLatency = avrgLatency * Math.pow(10, -9);
                throughput.set(((double) 1) / avrgLatency);
            }
            isOver = true;
            return (Math.pow(10, -3) * ((double) (System.currentTimeMillis() - beg))) / (double) T;
        }
    }

    private static class VerbosityCallable implements Callable<Void> {

        private List<IncrementCallable> callables;

        public VerbosityCallable(List<IncrementCallable> callables) {
            this.callables = callables;
        }

        @Override
        public Void call() throws Exception {
            boolean isOver = false;
            int throughput = 0;
            while (!isOver) {
                Thread.sleep(100);
                throughput = 0;
                for (IncrementCallable callable : callables) {
                    isOver |= callable.getIsOver();
                    throughput += callable.getThroughput();
                }
                System.out.println("Throughput:" + System.currentTimeMillis() + ":" + throughput);
            }
            System.out.println("Throughput:" + System.currentTimeMillis() + ":" + throughput);
            return null;
        }
    }

}
