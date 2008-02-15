package com.bc.ceres.core;

import junit.framework.TestCase;

import java.util.*;

public class ExtensionManagerTest extends TestCase {

    public void testX() {
        ExtensionManager em = ExtensionManager.getInstance();
        assertNotNull(em);

        List<ExtensionFactory<String>> efl1 = em.getExtensionFactories(String.class);
        assertNotNull(efl1);
        assertEquals(0, efl1.size());

        final ExtensionFactory<X> ef = new MyFactory();
        em.register(X.class, ef);

        final List<ExtensionFactory<X>> list = em.getExtensionFactories(X.class);
        assertNotNull(list);
        assertEquals(1, list.size());
        assertSame(ef, list.get(0));

        final X x1 = new X();
        final IA a1 = em.getExtension(x1, IA.class);
        assertNotNull(a1);
        assertSame(x1, a1.getX());

        final X x2 = new X();
        final IA a2 = x2.getExtension(IA.class);
        assertNotNull(a2);
        assertSame(x2, a2.getX());
    }

    private static class X implements Extendible {
        public <ET> ET getExtension(Class<ET> extensionType) {
            return ExtensionManager.getInstance().getExtension(this, extensionType);
        }
    }

    private static class Y {
    }

    private interface IA {
        X getX();
    }

    private static class A1 implements IA {
        private X x;

        public A1(X x) {
            this.x = x;
        }

        public X getX() {
            return x;
        }
    }

    private static class A2 extends A1 {
        private A2(X x) {
            super(x);
        }
    }


    private class MyFactory implements ExtensionFactory<X> {
        public <E> E getExtension(X x, Class<E> extensionType) {
            if (IA.class.equals(extensionType)) {
                return (E) new A1(x);
            }
            return null;
        }

        public Class<?>[] getExtensionTypes() {
            return new Class<?>[] {IA.class};
        }
    }
}