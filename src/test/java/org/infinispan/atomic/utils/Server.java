package org.infinispan.atomic.utils;

import org.infinispan.atomic.AtomicObjectFactory;
import org.infinispan.atomic.filter.FilterConverterFactory;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.SingleFileStoreConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.eviction.EvictionStrategy;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.server.hotrod.HotRodServer;
import org.infinispan.server.hotrod.configuration.HotRodServerConfigurationBuilder;
import org.infinispan.transaction.TransactionMode;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import sun.misc.Signal;
import sun.misc.SignalHandler;

import static org.infinispan.test.AbstractCacheTest.getDefaultClusteredCacheConfig;

/**
 * @author Pierre Sutra
 */
public class Server {

   private static final String defaultServer ="localhost:11222";

   private CacheMode CACHE_MODE = CacheMode.DIST_SYNC;
      
   @Option(name = "-server", usage = "ip:port or ip of the server")
   private String server = defaultServer;
   
   @Option(name = "-proxy", usage = "proxy server as seen by clients")
   private String proxyServer = null;

   @Option(name = "-rf", usage = "replication factor")
   private int replicationFactor = 1;

   @Option(name = "-me", usage = "max #entries in the object cache (implies -p)")
   private long maxEntries = Long.MAX_VALUE;

   public Server () {}

   public Server (String server, String proxyServer, int replicationFactor, boolean usePersistence) {
      this.server = server;
      this.proxyServer = proxyServer;
      this.replicationFactor = replicationFactor;
   }
   
   public static void main(String args[]) {
      new Server().doMain(args);
   }
   
   
   public void doMain(String[] args) {
      
      CmdLineParser parser = new CmdLineParser(this);

      parser.setUsageWidth(80);

      try {
         parser.parseArgument(args);
      } catch( CmdLineException e ) {
         System.err.println(e.getMessage());
         parser.printUsage(System.err);
         System.err.println();
         return;
      }

      String host = server.split(":")[0];
      int port = Integer.valueOf(
            server.split(":").length == 2
                  ? server.split(":")[1] : "11222");

      GlobalConfigurationBuilder gbuilder = GlobalConfigurationBuilder.defaultClusteredBuilder();
      gbuilder.transport().clusterName("aof-cluster");
      gbuilder.transport().nodeName("aof-server-" + host);
      gbuilder.transport().addProperty("configurationFile", "jgroups-aof.xml");

      ConfigurationBuilder builder= getDefaultClusteredCacheConfig(CACHE_MODE);
      builder
            .clustering().hash().numOwners(replicationFactor)
            .compatibility().enable();
      builder.clustering().stateTransfer()
            .awaitInitialTransfer(true)
            .timeout(60000)
            .fetchInMemoryState(true);
      builder.locking()
            .concurrencyLevel(10000)
            .lockAcquisitionTimeout(2000);
      builder.transaction().transactionMode(TransactionMode.NON_TRANSACTIONAL);

      if (maxEntries!=Long.MAX_VALUE) {
         System.out.println("Eviction is ON (maxEntries=" + maxEntries + ")");
         builder.eviction()
               .maxEntries(maxEntries)
               .strategy(EvictionStrategy.LRU);
         SingleFileStoreConfigurationBuilder storeConfigurationBuilder
               = builder.persistence().addSingleFileStore();
         storeConfigurationBuilder
               .location(System.getProperty("store-aof-server" + host))
               .purgeOnStartup(true);
         storeConfigurationBuilder.purgeOnStartup(false);
         storeConfigurationBuilder.fetchPersistentState(true);
         storeConfigurationBuilder.persistence().passivation(true);
      }

      final EmbeddedCacheManager cm = new DefaultCacheManager(gbuilder.build(), builder.build(), true);

      HotRodServerConfigurationBuilder hbuilder = new HotRodServerConfigurationBuilder();
      hbuilder.topologyStateTransfer(true);
      hbuilder.host(host);
      hbuilder.port(port);

      if (proxyServer != null && !proxyServer.equals(defaultServer)) {
         String proxyHost = proxyServer.split(":")[0];
         int proxyPort = Integer.valueOf(
               proxyServer.split(":").length == 2
                     ? proxyServer.split(":")[1] : "11222");
         hbuilder.proxyHost(proxyHost);
         hbuilder.proxyPort(proxyPort);
      }

      hbuilder.workerThreads(100);
      hbuilder.tcpNoDelay(true);

      final HotRodServer server = new HotRodServer();
      server.start(hbuilder.build(), cm);
      server.addCacheEventFilterConverterFactory(FilterConverterFactory.FACTORY_NAME, new FilterConverterFactory());

      System.out.println("LAUNCHED");

      SignalHandler sh = new SignalHandler() {
         @Override
         public void handle(Signal s) {
            System.out.println("CLOSING");
            try {
               AtomicObjectFactory factory = AtomicObjectFactory.forCache(cm.getCache());
               if (factory != null)
                  factory.close();
               server.stop();
               cm.stop();
               System.exit(0);
            }catch(Throwable t) {
               System.exit(-1);
            }
         }
      };
      Signal.handle(new Signal("INT"), sh);
      Signal.handle(new Signal("TERM"), sh);

      Thread.currentThread().interrupt();

   }

}
