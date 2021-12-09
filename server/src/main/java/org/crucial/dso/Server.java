package org.crucial.dso;

import static org.crucial.dso.Factory.DSO_CACHE_NAME;

import java.io.File;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.crucial.dso.utils.ConfigurationHelper;
import org.infinispan.commons.marshall.JavaSerializationMarshaller;
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

/**
 * @author Pierre Sutra
 */
public class Server {

    private static final Log log = LogFactory.getLog(Server.class);
    private static final String DEFAULT_SERVER = "localhost:11222";
    private static final String USER_LIBRARIES = "/tmp";
    private static final String STORAGE_PATH_PREFIX = "/tmp";

    @Option(name = "-server", usage = "ip:port or ip of the server")
    private String server = DEFAULT_SERVER;

    @Option(name = "-proxy", usage = "proxy server as seen by clients")
    private String proxyServer = null;

    @Option(name = "-passivation", usage = "persist data on disk")
    private boolean passivation = false;

    @Option(name = "-rf", usage = "replication factor")
    private int replicationFactor = 1;

    @Option(name = "-me", usage = "max #entries in the object cache (implies -p)")
    private long maxEntries = -1;

    @Option(name = "-userLibs", usage = "directory containing the user libraries")
    private String userLib = USER_LIBRARIES;

    @Option(name = "-wt", usage = "number of HotRod worker threads")
    private int workerThreads = 100;

    @Option(name = "-idempotence", usage = "idempotence of method calls")
    private boolean withIdempotence = false;

    public Server() {
    }

    public Server(String server, String proxyServer, int replicationFactor) {
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
        } catch (CmdLineException e) {
            System.err.println(e.getMessage());
            parser.printUsage(System.err);
            System.err.println();
            return;
        }

        List<String> jars = new ArrayList<>();
        Runnable runnable = () -> {
            try {
                File folder = new File(userLib);
                log.info("Looking for user jars in "+userLib);
                File[] listOfFiles = folder.listFiles();
                for (File file : listOfFiles) {
                    if (file.isFile() && file.getName().matches(".*\\.jar") && !jars.contains(file.getName())) {
                        loadLibrary(file);
                        jars.add(file.getName()); // FIXME checksum for re-loading?
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
                return;
            }

        };

        try {
            runnable.run();
        } catch (Exception e) {
            // ignore
        }

        ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
        scheduler.scheduleAtFixedRate(runnable, 10, 10, TimeUnit.SECONDS);

        String host = server.split(":")[0];
        int port = Integer.valueOf(
                server.split(":").length == 2
                        ? server.split(":")[1] : "11222");

        GlobalConfigurationBuilder gbuilder = GlobalConfigurationBuilder.defaultClusteredBuilder();

        //FIXME the DSO cache should be the default one.
        gbuilder.defaultCacheName("__DEFAULT_CACHE");

        // transport
        gbuilder.transport().clusterName("dso-cluster");
        gbuilder.transport().nodeName("dso-server-" + host);
        gbuilder.transport().addProperty("configurationFile", "jgroups.xml");

        // marshalling
        gbuilder.serialization()
                .marshaller(new JavaSerializationMarshaller())
                .allowList()
                .addRegexps(".*");

        ConfigurationBuilder cBuilder
                = AbstractCacheTest.getDefaultClusteredCacheConfig(CacheMode.DIST_ASYNC, false);

        final EmbeddedCacheManager cm
                = new DefaultCacheManager(gbuilder.build(), cBuilder.build(), true);

        ConfigurationHelper.installCache(cm,
                CacheMode.DIST_ASYNC,
                replicationFactor,
                maxEntries,
                false,
                STORAGE_PATH_PREFIX + "/" + host,
                true,
                false,
                false);

        HotRodServerConfigurationBuilder hbuilder = new HotRodServerConfigurationBuilder();
        hbuilder.host(host);
        hbuilder.port(port);
        hbuilder.recvBufSize(1000000);
        hbuilder.sendBufSize(1000000);
        hbuilder.tcpNoDelay(true);

        if (proxyServer != null && !proxyServer.equals(DEFAULT_SERVER)) {
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
                Factory factory = Factory.forCache(cm.getCache(DSO_CACHE_NAME));
                if (factory != null)
                    factory.close();
                server.stop();
                cm.stop();
                System.exit(0);
            } catch (Throwable t) {
                System.exit(-1);
            }
        };
        Signal.handle(new Signal("INT"), sh);
        Signal.handle(new Signal("TERM"), sh);

        Thread.currentThread().interrupt();

    }

    public static synchronized void loadLibrary(java.io.File jar) {
        try {
            System.out.println("Loading " + jar.getName());
            ClassLoader classLoader = ClassLoader.getSystemClassLoader();
            try {
                Method method = classLoader.getClass().getDeclaredMethod("addURL", URL.class);
                method.setAccessible(true);
                method.invoke(classLoader, jar.toURI().toURL());
            } catch (NoSuchMethodException e) {
                Method method = classLoader.getClass()
                        .getDeclaredMethod("appendToClassPathForInstrumentation", String.class);
                method.setAccessible(true);
                method.invoke(classLoader, jar.getPath());
            }
        } catch (final java.lang.NoSuchMethodException |
                java.lang.IllegalAccessException |
                java.net.MalformedURLException |
                java.lang.reflect.InvocationTargetException e) {
            e.printStackTrace();
        }
    }
}
