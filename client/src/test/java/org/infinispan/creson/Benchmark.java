package org.infinispan.creson;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import org.omg.SendingContext.RunTimeOperations;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;


/**
 * @author Pierre Sutra
 */
public class Benchmark {

    @Option(name="-id", usage = "identifier of this task")
    private long id = System.nanoTime();

    @Option(name = "-class", required = true, usage = "object class")
    private String className;

    @Option(name = "-parallelism", usage = "#concurrent benchmarks")
    private int parallelism = 1;

    @Option(name = "-instances", usage = "#object instances")
    private int instances = 1;

    @Option(name = "-threads", usage = "#threads")
    private int threads = 1;

    @Option(name = "-calls", usage = "#calls per thread")
    private int calls = 1;

    @Option(name = "-server", usage = "connection string to server")
    private String server = "127.0.0.1:11222";

    @Option(name = "-persist", usage = "persist storage after the run")
    private boolean persist = false;

    @Option(name = "-verbose", usage = "real-time report of total throughput (in ops/sec)")
    private boolean verbosity = false;

    @Option(name = "-parameters", usage = "parameters of the call ")
    private String[] parameters;

    private CyclicBarrier barrier;

    public static void main(String args[]) throws ClassNotFoundException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        new Benchmark().doMain(args);
        System.exit(0);
    }

    public void doMain(String[] args) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {

        CmdLineParser parser = new CmdLineParser(this);
        parser.setUsageWidth(80);
        try {
            if (args.length < 1)
                throw new CmdLineException(parser, "Not enough arguments are given.");
            parser.parseArgument(args);
        } catch (CmdLineException e) {
            System.err.println(e.getMessage());
            parser.printUsage(System.err);
            System.err.println();
            return;
        }

        Factory factory = Factory.get(server, id);
        barrier = new CyclicBarrier("benchmark", parallelism);
        ExecutorService service = Executors.newFixedThreadPool(threads + 1);
        List<Task> clientTasks = new ArrayList<>();

        // create threads
        Class<Task> taskClazz = (Class<Task>) ClassLoader.getSystemClassLoader().loadClass(className+"Task");
        for (int i = 0; i < this.threads; i++) {
            Task task = taskClazz.getConstructor(long.class, String[].class, int.class, int.class, int.class).newInstance(id, parameters, calls, threads, parallelism);
            clientTasks.add(task);
        }

        // create instances
        List<Object> instances = new ArrayList<>();
        for (int i = 0; i < this.instances; i++) {
            instances.add(clientTasks.get(0).newObject(i));
        }

        for (Task task : clientTasks){
            task.setInstances(instances);
        }

        // sync start
        barrier.await();
        long start = System.currentTimeMillis();

        // run threads then print results
        try {

            if (verbosity) {
                System.out.println("Reporting every 1s.");
                service.submit(new TroughtputReporter(clientTasks));
            }

            List<Future<Double>> futures = service.invokeAll(clientTasks);

            double avgTime = 0;
            for (Future<Double> future : futures) {
                try {
                    avgTime += future.get();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }

            service.shutdown();
            avgTime = avgTime / futures.size();
            System.out.println("Total time: " + (System.currentTimeMillis() - start));
            System.out.println("Average time: " + avgTime + " [Throughput=" + (1 / avgTime) * clientTasks.size() + "]");

        } catch (InterruptedException | ExecutionException e) {
            throw new RuntimeException(e);
        }


        // sync terminate
        barrier.await();
        System.out.println("Checksum: "+clientTasks.get(0).checksum());
        try {
            Thread.currentThread().sleep(3000);
        } catch (InterruptedException e) {
            // ignore
        }
        if (!persist) factory.clear();
        factory.close();
    }

    private static class TroughtputReporter implements Callable<Void> {

        private List<Task> tasks;

        public TroughtputReporter(List<Task> tasks) {
            this.tasks = tasks;
        }

        @Override
        public Void call() throws Exception {
            boolean isOver = false;
            int throughput;
            while (!isOver) {
                Thread.sleep(1000);
                throughput = 0;
                isOver = true;
                for (Task callable : tasks) {
                    isOver &= callable.getIsOver();
                    throughput += callable.getThroughput();
                }
                System.out.println(+ System.currentTimeMillis() + ":" + throughput);
            }
            return null;
        }
    }

}
