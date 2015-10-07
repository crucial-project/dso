package org.infinispan.atomic.benchmarks.count;

import org.infinispan.atomic.utils.AOFUtils;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.*;


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

      AOFUtils.createAOF(server);
      ExecutorService service = Executors.newFixedThreadPool(C);

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
         List<Future<Long>> futures = service.invokeAll(clients);
         long avgTime = 0;
         for (Future<Long> future : futures) {
            avgTime += future.get();
         }
	 avgTime=avgTime/futures.size();
         for (Counter counter : hits) {
            System.out.println(counter);
         }
         System.out.println("Average time: " + avgTime);
      } catch (InterruptedException | ExecutionException e) {
         e.printStackTrace();
      }
   }

   private static class IncrementCallable implements Callable<Long> {

      private int T;
      private List<Counter> counters;
      private Random random;

      public IncrementCallable(int T, List<Counter> counters){
         this.counters = counters;
         this.T = T;
         random = new Random();
      }

      @Override
      public Long call() throws Exception {
         long start = System.currentTimeMillis();
         // increment T counters at random
         for (int t = 0; t < T; t++) {
            counters.get(random.nextInt(counters.size())).increment();
         }
         return new Long((System.currentTimeMillis()-start)/T);

      }
   }

}
