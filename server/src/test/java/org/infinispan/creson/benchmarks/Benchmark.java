package org.infinispan.creson.benchmarks;

import org.infinispan.creson.Factory;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

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


    @Option(name = "-class", required = true, usage = "object class")
    String className;

    @Option(name = "-parameters", usage = "parameters of the call ")
    String[] parameters;

    @Option(name = "-instances", required = true, usage = "#instances")
    int instances;

    @Option(name = "-clients", required = true, usage = "#clients")
    int clients;

    @Option(name = "-calls", required = true, usage = "#calls per client")
    int calls;

    @Option(name = "-server", usage = "connection string to server")
    String server = "127.0.0.1:11222";

    @Option(name = "-verbose", usage = "real-time report of total throughput (in ops/sec)")
    boolean verbosity = false;


    public static void main(String args[]) throws ClassNotFoundException, InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
        new Benchmark().doMain(args);
        System.exit(0);
    }

    public void doMain(String[] args) throws ClassNotFoundException, NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {

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
        ExecutorService service = Executors.newFixedThreadPool(clients + 1);

        // create instances
        List<Object> objects = new ArrayList<>();
        for (int i = 0; i < instances; i++) {
            Class<Object> clazz = (Class<Object>) ClassLoader.getSystemClassLoader().loadClass(className);
            Object object = factory.getInstanceOf(clazz,"object"+i);
            objects.add(object);
        }

        // create clients clients
        List<Task> clients = new ArrayList<>();
        for (int i = 0; i < this.clients; i++) {
            Class<Task> clazz = (Class<Task>) ClassLoader.getSystemClassLoader().loadClass(className+"Task");
            Task task = clazz.getConstructor(List.class, String[].class, int.class).newInstance(objects , parameters, calls);
            clients.add(task);
        }

        // run clients then print results
        try {

            if (verbosity)
                service.submit(new TroughtputReporter(clients));

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

    private static class TroughtputReporter implements Callable<Void> {

        private List<Task> tasks;

        public TroughtputReporter(List<Task> tasks) {
            this.tasks = tasks;
        }

        @Override
        public Void call() throws Exception {
            boolean isOver = false;
            int throughput = 0;
            while (!isOver) {
                Thread.sleep(10000);
                throughput = 0;
                for (Task callable : tasks) {
                    isOver |= callable.getIsOver();
                    throughput += callable.getLastThroughput();
                }
                System.out.println(+ System.currentTimeMillis() + ":" + throughput);
            }

            throughput = 0;
            for (Task callable : tasks) {
                throughput += callable.getThroughput();
            }

            System.out.println("Throughput:" + System.currentTimeMillis() + ":" + throughput);

            return null;
        }
    }

}

