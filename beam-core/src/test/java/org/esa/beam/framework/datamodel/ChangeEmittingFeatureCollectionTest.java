package org.esa.beam.framework.datamodel;

import junit.framework.TestCase;
import org.geotools.feature.CollectionEvent;
import org.geotools.feature.CollectionListener;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;

import java.io.IOException;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Collection;
import java.util.Iterator;

public class ChangeEmittingFeatureCollectionTest extends TestCase {
    public void testDelegateCalled() throws IOException {
        final FCMock fcMock = new FCMock();
        final FeatureCollection delegate = createDelegate(fcMock);
        final ChangeEmittingFeatureCollection fc = new ChangeEmittingFeatureCollection(delegate);

        fc.accepts(null, null);
        fc.add(null);
        fc.addAll((Collection) null);
        fc.addAll((FeatureCollection) null);
        fc.addListener(null);
        fc.clear();
        fc.close((FeatureIterator) null);
        fc.close((Iterator) null);
        fc.contains(null);
        fc.containsAll(null);
        fc.features();
        fc.getBounds();
        fc.getID();
        fc.getSchema();
        fc.iterator();
        fc.isEmpty();
        fc.purge();
        fc.remove(null);
        fc.removeAll(null);
        fc.removeListener(null);
        fc.retainAll(null);
        fc.size();
        fc.sort(null);
        fc.subCollection(null);
        fc.toArray();
        fc.toArray(null);
        fc.toString();

        assertEquals(
                "add(a0:Feature):boolean\n" +
                        "addAll(a0:Collection):boolean\n" +
                        "addAll(a0:FeatureCollection):boolean\n" +
                        "addListener(a0:CollectionListener):void\n" +
                        "clear():void\n" +
                        "close(a0:FeatureIterator):void\n" +
                        "close(a0:Iterator):void\n" +
                        "contains(a0:Object):boolean\n" +
                        "containsAll(a0:Collection):boolean\n" +
                        "features():FeatureIterator\n" +
                        "getBounds():ReferencedEnvelope\n" +
                        "getID():String\n" +
                        "getSchema():FeatureType\n" +
                        "iterator():Iterator\n" +
                        "isEmpty():boolean\n" +
                        "purge():void\n" +
                        "remove(a0:Object):boolean\n" +
                        "removeAll(a0:Collection):boolean\n" +
                        "removeListener(a0:CollectionListener):void\n" +
                        "retainAll(a0:Collection):boolean\n" +
                        "size():int\n" +
                        "sort(a0:SortBy):FeatureCollection\n" +
                        "subCollection(a0:Filter):FeatureCollection\n" +
                        "toArray():Object[]\n" +
                        "toArray(a0:Object[]):Object[]\n",
                fcMock.sb.toString());
    }

    public void testDelegateNotCalled() throws IOException {
        final FCMock fcMock = new FCMock();
        final FeatureCollection delegate = createDelegate(fcMock);
        final ChangeEmittingFeatureCollection fc = new ChangeEmittingFeatureCollection(delegate);

        fc.fireFeaturesChanged();
        fc.getListeners();

        assertEquals("", fcMock.sb.toString());
    }

    public void testListeners() throws IOException {
        final FCMock fcMock = new FCMock();
        final FeatureCollection delegate = createDelegate(fcMock);
        final ChangeEmittingFeatureCollection fc = new ChangeEmittingFeatureCollection(delegate);

        assertNotNull(fc.getListeners());
        assertEquals(0, fc.getListeners().length);

        final MyCollectionListener l1 = new MyCollectionListener();
        final MyCollectionListener l2 = new MyCollectionListener();
        fc.addListener(l1);
        fc.addListener(l2);

        assertNotNull(fc.getListeners());
        assertEquals(2, fc.getListeners().length);
        assertSame(l1, fc.getListeners()[0]);
        assertSame(l2, fc.getListeners()[1]);

        fc.fireFeaturesChanged();

        fc.removeListener(l2);
        fc.removeListener(l1);

        assertNotNull(fc.getListeners());
        assertEquals(0, fc.getListeners().length);
    }

    private FeatureCollection createDelegate(FCMock fcMock) {
        return (FeatureCollection) Proxy.newProxyInstance(FeatureCollection.class.getClassLoader(),
                                                          new Class<?>[]{FeatureCollection.class},
                                                          fcMock);
    }

    private static class FCMock implements InvocationHandler {
        StringBuilder sb = new StringBuilder();

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            sb.append(method.getName()).append("(");
            final Class<?>[] parameterTypes = method.getParameterTypes();
            if (args != null) {
                for (int i = 0; i < args.length; i++) {
                    if (i > 0) {
                        sb.append(", ");
                    }
                    sb.append("a").append(i).append(":").append(parameterTypes[i].getSimpleName());
                }
            }
            final Class<?> returnType = method.getReturnType();
            sb.append("):").append(returnType.getSimpleName()).append("\n");
            if (returnType.isPrimitive()) {
                if (Boolean.TYPE.equals(returnType)) {
                    return false;
                } else if (Integer.TYPE.equals(returnType)) {
                    return 0;
                }
            }
            return null;
        }
    }

    private static class MyCollectionListener implements CollectionListener {
        String trace = "";

        @Override
        public void collectionChanged(CollectionEvent collectionEvent) {
            trace += collectionEvent.getEventType() + ";";
        }
    }
}
