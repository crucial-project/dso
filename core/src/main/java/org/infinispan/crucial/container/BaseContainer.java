package org.infinispan.crucial.container;

import javassist.util.proxy.MethodHandler;
import javassist.util.proxy.ProxyFactory;
import javassist.util.proxy.ProxyObject;
import org.infinispan.commons.api.BasicCache;
import org.infinispan.crucial.ReadOnly;
import org.infinispan.crucial.object.BoxedReference;
import org.infinispan.crucial.object.Call;
import org.infinispan.crucial.object.CallConstruct;
import org.infinispan.crucial.object.CallResponse;
import org.infinispan.crucial.object.CallInvoke;
import org.infinispan.crucial.object.Reference;
import org.infinispan.crucial.utils.Context;
import org.infinispan.crucial.utils.ContextManager;
import org.infinispan.crucial.utils.ID;
import org.infinispan.crucial.utils.Reflection;

import javax.persistence.Entity;
import java.lang.reflect.InvocationTargetException;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

/**
 * @author Pierre Sutra
 */
public class BaseContainer extends AbstractContainer {

    private boolean isOpen;
    private Reference reference;
    private BasicCache<Reference, Call> cache;

    public BaseContainer(BasicCache cache, Class clazz, java.lang.Object key, boolean readOptimization,
                         boolean forceNew, java.lang.Object... initArgs)
            throws IllegalAccessException, InstantiationException,
            NoSuchMethodException, InvocationTargetException {

        super(clazz, readOptimization, forceNew, initArgs);
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

            if (this.readOptimization
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


            Context context = ContextManager.get();

            open();

            UUID uuid = context.getGenerator().generate();

            if (log.isTraceEnabled()) {
                log.trace("generated " + uuid + " m=" + m.getName() + "[" + context + "]");
            }

            Object ret = execute(
                    new CallInvoke(
                            reference,
                            uuid,
                            m.getName(),
                            args)
            );

            assert (m.getReturnType().equals(Void.TYPE) && ret == null) || Reflection.isCompatible(ret, m.getReturnType())
                    : m.getReturnType() + " => " + ret.getClass() + " [" + reference.getClazz() + "." + m.getName() + "()]";

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
                log.trace(" Opening - "+this.toString());

            CallConstruct construct = new CallConstruct(reference,
                    ID.generator().generate(), forceNew, initArgs, readOptimization);

            execute(construct);

            isOpen = true;

            if (log.isTraceEnabled())
                log.trace(" Opened - "+this.toString());

        }

    }

    public interface WriteReplace {
        java.lang.Object writeReplace() throws java.io.ObjectStreamException;
    }

}
