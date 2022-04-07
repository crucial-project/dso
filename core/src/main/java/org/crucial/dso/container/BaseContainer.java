package org.crucial.dso.container;

import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;
import javassist.util.proxy.ProxyObject;
import org.crucial.dso.object.*;
import org.crucial.dso.utils.Context;
import org.crucial.dso.utils.ContextManager;
import org.crucial.dso.utils.Reflection;
import org.infinispan.commons.api.BasicCache;

import javax.persistence.Entity;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.UUID;

/**
 * @author Pierre Sutra
 */
public class BaseContainer extends AbstractContainer {

    private boolean isOpen;
    private Reference reference;
    private BasicCache<Reference, Call> cache;

    public BaseContainer(BasicCache cache, Class clazz, java.lang.Object key, @Deprecated boolean readOptimization, boolean isIdempotent,
                         boolean forceNew, java.lang.Object... initArgs)
            throws IllegalAccessException, InstantiationException,
            NoSuchMethodException, InvocationTargetException {

        super(clazz, readOptimization, isIdempotent, forceNew, initArgs);
        this.isOpen = false;

        // build the proxy
        MethodHandler handler = (self, m, proceed, args) -> {

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

            if (m.getName().equals("toString")){ // for debugging purposes
                return reference.toString();
            }

            if (m.getName().equals("hashCode")) {
                return reference.hashCode();
            }

            if (m.getName().equals("writeReplace")) {
                open();
                return new BoxedReference(reference);
            }

            if (!Reflection.isMethodSupported(reference.getClazz(), m)) {
                throw new IllegalArgumentException("Unsupported method " + m.getName() + " in " + reference.getClazz());
            }

            open();

            Object ret = execute(
                    new CallInvoke(
                            reference,
                            generateCallID(),
                            m.getName(),
                            args)
            );

            assert (m.getReturnType().equals(Void.TYPE) && ret == null) || Reflection.isCompatible(ret, m.getReturnType())
                    : m.getReturnType() + " => " + ret + " [" + reference.getClazz() + "." + m.getName() + "()]";

            return ret;

        };

        ProxyFactory fact = new ProxyFactory();
        fact.setSuperclass(clazz);
        fact.setFilter(methodFilter);
        fact.setInterfaces(new Class[]{WriteReplace.class});
        fact.setUseWriteReplace(false);
        this.proxy = Reflection.instantiate(fact.createClass(), initArgs);
        ((ProxyObject) proxy).setHandler(handler);

        // build reference and set key

        // FIXME deprecated (linked to @Entity)
        if (clazz.getAnnotation(Entity.class) != null) {
            java.lang.reflect.Field field = Reference.getIDField(clazz);
            if (clazz.getPackage().getName().startsWith("java.util.concurrent"))
                throw new ClassFormatError("Not supported");
            if (field == null)
                throw new ClassFormatError("Missing key field");
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
        this.cache = cache;

        if (forceNew) {
            try {
                open();
            } catch (Throwable throwable) {
                throwable.printStackTrace();
            }
        }

    }


    @Override
    public void doExecute(Call call) {
        handleFuture((CallResponse) cache.put(reference, call));
    }

    @Override
    public Reference getReference() {
        return this.reference;
    }

    @Override
    public String toString() {
        return "Container[" + getReference() + "]";
    }

    // internals

    private synchronized void open()
            throws Throwable {

        if (!isOpen) {

            if (log.isTraceEnabled())
                log.trace(" Opening - " + this);

            CallConstruct construct = new CallConstruct(reference,
                    generateCallID(), forceNew, initArgs, readOptimization, isIdempotent);

            execute(construct);

            isOpen = true;

            if (log.isTraceEnabled())
                log.trace(" Opened - " + this);

        }

    }

    public interface WriteReplace {
        java.lang.Object writeReplace() throws java.io.ObjectStreamException;
    }

    private UUID generateCallID(){
        Context context = ContextManager.get();
        UUID uuid = context.getGenerator().generate();
        if (log.isTraceEnabled()) {
            log.trace("generated " + uuid + " [" + context + "]");
        }
        return uuid;
    }

}
