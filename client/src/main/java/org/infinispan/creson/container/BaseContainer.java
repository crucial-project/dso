package org.infinispan.creson.container;

import com.fasterxml.uuid.impl.RandomBasedGenerator;
import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;
import javassist.util.proxy.ProxyObject;
import org.infinispan.client.hotrod.Flag;
import org.infinispan.client.hotrod.RemoteCache;
import org.infinispan.commons.api.BasicCache;
import org.infinispan.creson.ReadOnly;
import org.infinispan.creson.object.Call;
import org.infinispan.creson.object.CallConstruct;
import org.infinispan.creson.object.CallFuture;
import org.infinispan.creson.object.CallInvoke;
import org.infinispan.creson.object.Reference;
import org.infinispan.creson.utils.Reflection;
import org.infinispan.creson.utils.ThreadLocalUUIDGenerator;

import javax.persistence.Entity;
import javax.persistence.Id;
import java.io.IOException;
import java.io.Serializable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author Pierre Sutra
 */
public class BaseContainer extends AbstractContainer {

    // object's fields
    private AtomicInteger pendingCalls;
    private boolean isOpen;
    private Reference reference;
    private RandomBasedGenerator generator;
    private BasicCache<Reference, Call> cache;

    public BaseContainer(BasicCache c, Class clazz, java.lang.Object key, final boolean readOptimization,
                         final boolean forceNew, final java.lang.Object... initArgs)
            throws IOException, ClassNotFoundException, IllegalAccessException, InstantiationException,
            InterruptedException, ExecutionException, NoSuchMethodException, InvocationTargetException,
            java.util.concurrent.TimeoutException, NoSuchFieldException {

        super(clazz, readOptimization, forceNew, initArgs);
        this.pendingCalls = new AtomicInteger();
        this.isOpen = false;

        // build the proxy
        MethodHandler handler = new BaseContainerMethodHandler(this);
        ProxyFactory fact = new ProxyFactory();
        fact.setSuperclass(clazz);
        fact.setFilter(methodFilter);
        fact.setInterfaces(new Class[]{WriteReplace.class});
        fact.setUseWriteReplace(false);
        this.proxy = Reflection.instantiate(fact.createClass(), initArgs);
        ((ProxyObject) proxy).setHandler(handler);

        // build reference and set key
        if (clazz.getAnnotation(Entity.class) != null) {
            java.lang.reflect.Field field = null;
            for (java.lang.reflect.Field f : clazz.getDeclaredFields()) {
                f.setAccessible(true);
                if (f.getAnnotation(Id.class) != null) {
                    field = f;
                    break;
                }
            }
            if (field == null) throw new ClassFormatError("Missing id field");
            if (key == null) {
                field.setAccessible(true);
                key = field.get(proxy);
                assert key != null : " field " + field.getName() + " is null for " + clazz;
            } else {
                field.set(proxy, key);
            }

        }
        assert key != null;
        this.reference = new Reference(clazz, key);
        this.cache = c;

        // build generator
        generator = new RandomBasedGenerator(new Random(System.nanoTime()));
    }


    @Override
    public void execute(Reference reference, Call call) {
        if (cache instanceof RemoteCache) {
            handleFuture((CallFuture) ((RemoteCache) cache).withFlags(Flag.FORCE_RETURN_VALUE).put(reference, call));
        } else {
            handleFuture((CallFuture) cache.put(reference, call));
        }
    }


    @Override
    public synchronized void open()
            throws InterruptedException, ExecutionException, java.util.concurrent.TimeoutException {

        if (!isOpen) {

            if (log.isTraceEnabled())
                log.trace(" Opening.");

            execute(new CallConstruct(generator.generate(), forceNew, initArgs, readOptimization));
            isOpen = true;

            if (log.isTraceEnabled())
                log.trace(" Opened.");

        }

    }

    @Override
    public synchronized void close()
            throws InterruptedException, ExecutionException, java.util.concurrent.TimeoutException {

        if (log.isTraceEnabled())
            log.trace(" Closing.");

        while (pendingCalls.get() != 0) {
            this.wait();
        }

        if (isOpen) {

            isOpen = false;
            forceNew = false;

        }

        if (log.isTraceEnabled())
            log.trace(" Closed.");

    }

    @Override
    public Reference getReference() {
        return this.reference;
    }

    ;

    @Override
    public String toString() {
        return "Container[" + getReference() + "]";
    }

    private class BaseContainerMethodHandler implements MethodHandler, Serializable {

        BaseContainer container;

        public BaseContainerMethodHandler(BaseContainer container) {
            this.container = container;
        }

        public java.lang.Object invoke(java.lang.Object self, Method m, Method proceed, java.lang.Object[] args) throws Throwable {

            if (log.isTraceEnabled())
                log.trace("Calling " + reference.getClazz() + "." + m.getName() + "(" + Arrays.toString(args) + ")");

            if (m.getName().equals("equals")) {
                if (args[0] == null) {
                    return false;
                } else if (args[0] == proxy) {
                    return true;
                } else if (args[0] instanceof Reference) {
                    return reference.equals(args[0]);
                } else if (ProxyFactory.isProxyClass(args[0].getClass())) {
                    return args[0].equals(reference); // FIXME might not be the most satisfying
                }
                return args[0].equals(proxy);
            }

            if (m.getName().equals("toString")) {
                return reference.toString();
            }

            if (m.getName().equals("hashCode")) {
                return reference.hashCode();
            }

            if (m.getName().equals("writeReplace")) {
                return reference;
            }

            if (!Reflection.isMethodSupported(reference.getClazz(), m)) {
                throw new IllegalArgumentException("Unsupported method " + m.getName() + " in " + reference.getClazz());
            }

            if (readOptimization
                    && state != null
                    && (m.isAnnotationPresent(ReadOnly.class))) {
                if (log.isTraceEnabled()) log.trace("local call: " + m.getName());
                return Reflection.callObject(state, m.getName(), args);
            } else {
                if (log.isTraceEnabled())
                    log.trace("remote call: " + m.getName() + ";reason: +"
                            + "null state=" + new Boolean(state == null) + ", "
                            + "isAnnotationPresent=" + new Boolean(m.isAnnotationPresent(ReadOnly.class)));
            }

            pendingCalls.incrementAndGet();

            open();

            // handle UUID generator
            RandomBasedGenerator lgenerator = ThreadLocalUUIDGenerator.getThreadLocal();
            UUID uuid = lgenerator == null ? generator.generate() : lgenerator.generate();
            if (log.isTraceEnabled()) {
                log.trace("generated " + uuid + " m=" + m.getName()
                        + ", reference=" + reference + "[" + ((lgenerator == null) ? "null" : lgenerator.toString()) + "]");
            }

            java.lang.Object ret = execute(
                    new CallInvoke(
                            uuid,
                            m.getName(),
                            args)
            );

            if (pendingCalls.decrementAndGet() == 0) {
                synchronized (container) {
                    container.notifyAll();
                }
            }

            ret = Reference.unreference(ret, cache);

            assert (m.getReturnType().equals(Void.TYPE) && ret == null) || Reflection.isCompatible(ret, m.getReturnType())
                    : m.getReturnType() + " => " + ret.getClass() + " [" + reference.getClazz() + "." + m.getName() + "()]";

            return ret;

        }

        @Override
        public String toString() {
            return "MethodHandler [" + getReference() + "]";
        }

    }

    public interface WriteReplace {
        java.lang.Object writeReplace() throws java.io.ObjectStreamException;
    }

}
