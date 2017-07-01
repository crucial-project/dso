package org.infinispan.creson;

import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.creson.utils.ConfigurationHelper;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.server.hotrod.HotRodServer;
import org.infinispan.server.hotrod.configuration.HotRodServerConfigurationBuilder;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import sun.misc.Signal;
import sun.misc.SignalHandler;

import static org.infinispan.creson.CresonModuleLifeCycle.CRESON_CACHE_NAME;

/**
 * @author Pierre Sutra
 */
public class Server {

   private static final String defaultServer ="localhost:11222";

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
      gbuilder.transport().clusterName("creson-cluster");
      gbuilder.transport().nodeName("creson-server-" + host);
      // gbuilder.transport().addProperty("configurationFile", "jgroups-creson.xml"); FIXME using default TCP configuration

      ConfigurationBuilder builder= ConfigurationHelper.buildConfiguration(
              CacheMode.DIST_ASYNC,
              replicationFactor,
              maxEntries,
              System.getProperty("store-creson-server" + host),
              false);

      final EmbeddedCacheManager cm
              = new DefaultCacheManager(gbuilder.build(), builder.build(), true);

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

      System.out.println("LAUNCHED");

      SignalHandler sh = s -> {
         System.out.println("CLOSING");
         try {
            Factory factory = Factory.forCache(cm.getCache(CRESON_CACHE_NAME));
            if (factory != null)
               factory.close();
            server.stop();
            cm.stop();
            System.exit(0);
         }catch(Throwable t) {
            System.exit(-1);
         }
      };
      Signal.handle(new Signal("INT"), sh);
      Signal.handle(new Signal("TERM"), sh);

      Thread.currentThread().interrupt();

   }

}
