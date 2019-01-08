package org.infinispan.creson;

import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.global.GlobalConfigurationBuilder;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.server.hotrod.HotRodServer;
import org.infinispan.server.hotrod.configuration.HotRodServerConfigurationBuilder;
import org.infinispan.test.AbstractCacheTest;
import org.infinispan.util.logging.Log;
import org.infinispan.util.logging.LogFactory;
import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;
import sun.misc.Signal;
import sun.misc.SignalHandler;

import java.io.File;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import static org.infinispan.creson.Factory.CRESON_CACHE_NAME;
import static org.infinispan.creson.utils.ConfigurationHelper.installCreson;

/**
 * @author Pierre Sutra
 */
public class Server{

    private static final Log log = LogFactory.getLog(Server.class);
    private static final String defaultServer = "localhost:11222";
    private static final String userLibraries = "/tmp";

    @Option(name = "-server", usage = "ip:port or ip of the server")
    private String server = defaultServer;

    @Option(name = "-proxy", usage = "proxy server as seen by clients")
    private String proxyServer = null;

    @Option(name = "-passivation", usage = "store data on disk")
    private boolean passivation = false;

    @Option(name = "-rf", usage = "replication factor")
    private int replicationFactor = 1;

    @Option(name = "-me", usage = "max #entries in the object cache (implies -p)")
    private long maxEntries = - 1;

    @Option(name = "-ec2", usage = "use AWS EC2 jgroups configuration")
    private boolean useEC2 = false;

    @Option(name = "-userLibs", usage = "directory containing the user libraries")
    private String userLib = userLibraries;

    @Option(name = "-wt", usage = "number of HotRod worker threads")
    private int workerThreads = 100;

    private volatile boolean running = false;

    public Server(){
    }

    public Server(String server, String proxyServer, int replicationFactor, boolean usePersistence){
        this.server = server;
        this.proxyServer = proxyServer;
        this.replicationFactor = replicationFactor;
    }

    public static void main(String args[]){
        new Server().doMain(args);
    }

    public static synchronized void loadLibrary(java.io.File jar){
        try {
            java.net.URLClassLoader loader = (java.net.URLClassLoader) ClassLoader.getSystemClassLoader();
            java.net.URL url = jar.toURI().toURL();
            for (java.net.URL it : java.util.Arrays.asList(loader.getURLs())) {
                if (it.equals(url)) {
                    return;
                }
            }
            log.info("Loading " + jar.getName());
            java.lang.reflect.Method method = java.net.URLClassLoader.class.
                    getDeclaredMethod("addURL", new Class[]{java.net.URL.class});
            method.setAccessible(true); /*promote the method to public access*/
            method.invoke(loader, new Object[]{url});
        } catch (final java.lang.NoSuchMethodException |
                java.lang.IllegalAccessException |
                java.net.MalformedURLException |
                java.lang.reflect.InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    public void doMain(String[] args){

        CmdLineParser parser = new CmdLineParser(this);

        parser.setUsageWidth(80);

        try {
            parser.parseArgument(args);
        } catch (CmdLineException e) {
            System.err.println(e.getMessage());
            parser.printUsage(System.err);
            System.err.println();
            return;
        }

        Runnable runnable = () -> {
            File folder = new File(userLib);
            File[] listOfFiles = folder.listFiles();
            for (File file : listOfFiles) {
                if (file.isFile() && file.getName().matches(".*\\.jar")) {
                    loadLibrary(file);
                }
            }
        };

        try {
            runnable.run();
        } catch (Exception e) {
            // ignore
        }

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(runnable, 3, 3, TimeUnit.SECONDS);

        String host = server.split(":")[0];
        int port = Integer.valueOf(
                server.split(":").length == 2
                        ? server.split(":")[1] : "11222");

        GlobalConfigurationBuilder gbuilder = GlobalConfigurationBuilder.defaultClusteredBuilder();
        gbuilder.transport().clusterName("creson-cluster");
        gbuilder.transport().nodeName("creson-server-" + host);
//        gbuilder.defaultCacheName("default");

        if (useEC2)
            gbuilder.transport().addProperty("configurationFile", "jgroups-ec2.xml");

        ConfigurationBuilder cBuilder
                = AbstractCacheTest.getDefaultClusteredCacheConfig(CacheMode.DIST_ASYNC, false);

        final EmbeddedCacheManager cm
                = new DefaultCacheManager(gbuilder.build(), cBuilder.build(), true);

        installCreson(cm,
                CacheMode.DIST_ASYNC,
                replicationFactor,
                maxEntries,
                passivation,
                System.getProperty("store-creson-server" + host),
                true,
                false);

        HotRodServerConfigurationBuilder hbuilder = new HotRodServerConfigurationBuilder();
        hbuilder.topologyStateTransfer(true);
        hbuilder.host(host);
        hbuilder.port(port);

        if (proxyServer != null && ! proxyServer.equals(defaultServer)) {
            String proxyHost = proxyServer.split(":")[0];
            int proxyPort = Integer.valueOf(
                    proxyServer.split(":").length == 2
                            ? proxyServer.split(":")[1] : "11222");
            hbuilder.proxyHost(proxyHost);
            hbuilder.proxyPort(proxyPort);
        }

        hbuilder.workerThreads(workerThreads);
        hbuilder.tcpNoDelay(true);

        final HotRodServer server = new HotRodServer();
        server.start(hbuilder.build(), cm);

        System.out.println("LAUNCHED");

        SignalHandler sh = s -> {
            System.out.println("CLOSING");
            try {
                scheduler.shutdown();
                Factory factory = Factory.forCache(cm.getCache(CRESON_CACHE_NAME));
                if (factory != null)
                    factory.close();
                server.stop();
                cm.stop();
                System.exit(0);
            } catch (Throwable t) {
                System.exit(- 1);
            }
        };
        Signal.handle(new Signal("INT"), sh);
        Signal.handle(new Signal("TERM"), sh);

        Thread.currentThread().interrupt();

    }
}
