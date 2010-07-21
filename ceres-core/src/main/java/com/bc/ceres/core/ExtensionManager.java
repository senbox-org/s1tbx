/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package com.bc.ceres.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * The {@code ExtensionManager} is a service used to register and unregister {@link ExtensionFactory}s with a given
 * type of an extensible object.
 *
 * @see Extensible
 * @see ExtensibleObject
 */
public abstract class ExtensionManager {
    private static ExtensionManager instance = new Impl();

    private static final TypeCondition IS_EQUAL_TO = new TypeCondition() {
        @Override
        public boolean fulfilled(Class<?> a, Class<?> b) {
            return a.equals(b);
        }
    };

    private static final TypeCondition IS_A = new TypeCondition() {
        @Override
        public boolean fulfilled(Class<?> a, Class<?> b) {
            return b.isAssignableFrom(a);
        }
    };

    /**
     * @return The service instance.
     */
    public static ExtensionManager getInstance() {
        return instance;
    }

    /**
     * @param instance The service instance.
     */
    public static void setInstance(ExtensionManager instance) {
        Assert.notNull(instance, "instance");
        ExtensionManager.instance = instance;
    }

    /**
     * Registers an extension factory for the given extensible type.
     *
     * @param extensibleType The extensible type.
     * @param factory        The factory.
     */
    public abstract void register(Class<?> extensibleType, ExtensionFactory factory);

    /**
     * Unregisters an extension factory for the given extensible type.
     *
     * @param extensibleType The extensible type.
     * @param factory        The factory.
     */
    public abstract void unregister(Class<?> extensibleType, ExtensionFactory factory);

    /**
     * Gets all extension factories registered for the given type.
     *
     * @param extensibleType The extensible type.
     * @return The list of extension factories. May be empty.
     */
    public abstract ExtensionFactory[] getExtensionFactories(Class<?> extensibleType);

    /**
     * Gets a dynamic extension for the given (extensible) object.
     *
     * @param extensibleObject The (extensible) object.
     * @param extensionType    The type of the requested extension.
     * @return The extension instance, or {@code null} if the given object is not extensible by this factory.
     * @see ExtensionFactory#getExtension(Object, Class)
     */
    public <E> E getExtension(Object extensibleObject, Class<E> extensionType) {
        Assert.notNull(extensibleObject, "extensibleObject");
        Assert.notNull(extensionType, "extensionType");
        final Class<?> extensibleType = extensibleObject.getClass();
        final ExtensionFactory factory = findFactory(extensibleType, extensionType);
        if (factory != null) {
            return (E) factory.getExtension(extensibleObject, extensionType);
        }
        return null;
    }

    /**
     * Finds the extension factory for the given types of the extensible object and the extension.
     *
     * @param extensibleType The type of the extensible object.
     * @param extensionType  The type of the extension.
     * @return The factory, or {@code null} if no such can be found.
     */
    public <E> ExtensionFactory findFactory(Class<?> extensibleType, Class<E> extensionType) {
        Assert.notNull(extensibleType, "extensibleType");
        Assert.notNull(extensionType, "extensionType");

        ExtensionFactory factory = findFactoryFlat(extensibleType, extensionType, IS_EQUAL_TO);

        if (factory == null) {
            factory = findFactoryFlat(extensibleType, extensionType, IS_A);
        }

        if (factory == null) {
            final Class<?> cls = extensibleType.getSuperclass();
            if (cls != null) {
                factory = findFactory(cls, extensionType);
            }
        }

        if (factory == null) {
            final Class<?>[] interfaces = extensibleType.getInterfaces();
            for (Class<?> ifc : interfaces) {
                factory = findFactory(ifc, extensionType);
                if (factory != null) {
                    break;
                }
            }
        }

        return factory;
    }

    private interface TypeCondition {
        boolean fulfilled(Class<?> a, Class<?> b);
    }

    private <E> ExtensionFactory findFactoryFlat(Class<?> extensibleType, Class<E> extensionType, TypeCondition condition) {
        for (ExtensionFactory factory : getExtensionFactories(extensibleType)) {
            for (Class<?> cls : factory.getExtensionTypes()) {
                if (condition.fulfilled(cls, extensionType)) {
                    return factory;
                }
            }
        }
        return null;
    }

    private static class Impl extends ExtensionManager {
        private HashMap<Class, List<ExtensionFactory>> extensionFactoryListMap = new HashMap<Class, List<ExtensionFactory>>();
        private static final ExtensionFactory[] NO_EXTENSION_FACTORIES = new ExtensionFactory[0];

        @Override
        public void register(Class<?> extensibleType, ExtensionFactory factory) {
            Assert.notNull(extensibleType, "extensibleType");
            Assert.notNull(factory, "factory");
            List<ExtensionFactory> extensionFactoryList = extensionFactoryListMap.get(extensibleType);
            if (extensionFactoryList == null) {
                extensionFactoryList = new ArrayList<ExtensionFactory>();
                extensionFactoryListMap.put(extensibleType, extensionFactoryList);
            }
            if (!extensionFactoryList.contains(factory)) {
                extensionFactoryList.add(factory);
            }
        }

        @Override
        public void unregister(Class<?> extensibleType, ExtensionFactory factory) {
            Assert.notNull(extensibleType, "extensibleType");
            Assert.notNull(factory, "factory");
            List<ExtensionFactory> extensionFactoryList = extensionFactoryListMap.get(extensibleType);
            if (extensionFactoryList != null) {
                extensionFactoryList.remove(factory);
            }
        }

        @Override
        public ExtensionFactory[] getExtensionFactories(Class<?> extensibleType) {
            Assert.notNull(extensibleType, "extensibleType");
            List<ExtensionFactory> extensionFactoryList = extensionFactoryListMap.get(extensibleType);
            if (extensionFactoryList != null) {
                return extensionFactoryList.toArray(new ExtensionFactory[extensionFactoryList.size()]);
            } else {
                return NO_EXTENSION_FACTORIES;
            }
        }
    }
}
