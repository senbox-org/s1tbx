package org.esa.snap.python.gpf;

import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.VirtualBand;
import org.esa.snap.core.gpf.internal.TileImpl;
import org.junit.Test;

import java.awt.image.BufferedImage;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static org.junit.Assert.*;


/**
 * @author Marco Peters
 * @author Norman Fomferra
 */
public class ProxyTest {

    @Test
    public void testProxy() {
        final MyInvocationHandler handler = new MyInvocationHandler();
        final PyOperatorDelegate instance = (PyOperatorDelegate) Proxy.newProxyInstance(Thread.currentThread().getContextClassLoader(),
                                                                                        new Class[]{PyOperatorDelegate.class}, handler);
        final VirtualBand band = new VirtualBand("mock", ProductData.TYPE_UINT8, 2, 2, "1");
        final BufferedImage bufferedImage = new BufferedImage(2, 2, BufferedImage.TYPE_BYTE_GRAY);

        instance.initialize(null);
        instance.computeTile(null, band, new TileImpl(band, bufferedImage.getRaster()));
        instance.dispose(null);
        assertEquals("initialize;computeTile;compute;dispose;", handler.trace);
    }

    private static class MyInvocationHandler implements InvocationHandler {

        String trace = "";

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            trace += method.getName() + ";";
            // see https://opencredo.com/dynamic-proxies-java-part-2/
            if (method.isDefault()) {
                final MethodCallHandler methodCallHandler = DefaultMethodCallHandler.forMethod(method);
                return methodCallHandler.invoke(proxy, args);
            }
            return null;
        }
    }


    @FunctionalInterface
    public interface MethodCallHandler {

        Object invoke(Object proxy, Object[] args) throws Throwable;
    }

    final static class DefaultMethodCallHandler {

        private DefaultMethodCallHandler() {
        }

        private static final ConcurrentMap<Method, MethodCallHandler> cache = new ConcurrentHashMap<>();

        public static MethodCallHandler forMethod(Method method) {
            return cache.computeIfAbsent(method, m -> {
                MethodHandle handle = getMethodHandle(m);
                return (proxy, args) -> handle.bindTo(proxy).invokeWithArguments(args);
            });
        }

        private static MethodHandle getMethodHandle(Method method) {
            Class<?> declaringClass = method.getDeclaringClass();

            try {
                Constructor<MethodHandles.Lookup> constructor = MethodHandles.Lookup.class
                        .getDeclaredConstructor(Class.class, int.class);
                constructor.setAccessible(true);

                return constructor.newInstance(declaringClass, MethodHandles.Lookup.PRIVATE)
                        .unreflectSpecial(method, declaringClass);
            } catch (IllegalAccessException | NoSuchMethodException |
                    InstantiationException | InvocationTargetException e) {
                throw new RuntimeException(e);
            }
        }
    }
}