package org.infinispan.crucial.benchmarks.queue;

import org.infinispan.crucial.Factory;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

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

   @Option(name = "-clients", required = true, usage = "# clients")
   int C;

   @Option(name = "-operations", required = true, usage = "# operations")
   int T;

   @Option(name = "-server", usage = "connection string to server")
   String server = "127.0.0.1:11222";

   public static void main(String args[]) {
      new Benchmark().doMain(args);
      System.exit(0);
   }

   public void doMain(String[] args) {

      CmdLineParser parser = new CmdLineParser(this);
      parser.setUsageWidth(80);
      try {
         if (args.length < 4)
            throw new CmdLineException(parser, "Not enough arguments are given.");
         parser.parseArgument(args);
      } catch (CmdLineException e) {
         System.err.println(e.getMessage());
         parser.printUsage(System.err);
         System.err.println();
         return;
      }

      Factory.get(server);
      ExecutorService service = Executors.newFixedThreadPool(C);

      // create one shared queue
      Queue<Integer> queue = new Queue<>("test");

      // create C clients
      List<ExerciceQueueCallable> clients = new ArrayList<>();
      for (int i = 0; i < C; i++) {
         clients.add(new ExerciceQueueCallable(T, queue));
      }

      // run clients then print results
      try {
         List<Future<Long>> futures = service.invokeAll(clients);
         long avgTime = 0;
         for (Future<Long> future : futures) {
            avgTime += future.get();
         }
         avgTime=avgTime/futures.size();
         System.out.println("Average time: " + avgTime);
         assert queue.isEmpty();
      } catch (InterruptedException | ExecutionException e) {
         e.printStackTrace();
      }
   }

   private static class ExerciceQueueCallable implements Callable<Long> {

      private int T;
      private Queue<Integer> queue;

      public ExerciceQueueCallable(int T, Queue<Integer> queue){
         this.queue = queue;
         this.T = T;
      }

      @Override
      public Long call() throws Exception {
         long start = System.currentTimeMillis();
         // put then remove an element from the queue
         for (int t = 0; t < T; t++) {
            boolean success = queue.add(t);
            success &= (queue.remove() !=null);
            assert success;
         }
         return new Long((System.currentTimeMillis()-start)/T);

      }
   }

}
