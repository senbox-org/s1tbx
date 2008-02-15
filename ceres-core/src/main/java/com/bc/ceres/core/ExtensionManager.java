package com.bc.ceres.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public abstract class ExtensionManager {
    private static ExtensionManager instance = new Impl();

    public static ExtensionManager getInstance() {
        return instance;
    }

    public static void setInstance(ExtensionManager instance) {
        Assert.notNull(instance, "instance");
        ExtensionManager.instance = instance;
    }

    public abstract <T> void register(Class<T> extendibleType, ExtensionFactory<T> factory);

    public abstract <T> void unregister(Class<T> extendibleType, ExtensionFactory<T> factory);

    public abstract <T> List<ExtensionFactory<T>> getExtensionFactories(Class<T> extendibleType);

    public <E, T> E getExtension(T extendibleObject, Class<E> extensionType) {
        Assert.notNull(extendibleObject, "extendibleObject");
        Assert.notNull(extensionType, "extensionType");
        final Class<T> extendibleType = (Class<T>) extendibleObject.getClass();
        final ExtensionFactory<T> factory = findFactory(extendibleType, extensionType);
        if (factory != null) {
            return factory.getExtension(extendibleObject, extensionType);
        }
        return null;
    }

    public <T, E> ExtensionFactory<T> findFactory(Class<T> extendibleType, Class<E> extensionType) {
        Assert.notNull(extendibleType, "extendibleType");
        Assert.notNull(extensionType, "extensionType");
        final List<ExtensionFactory<T>> factoryList = getExtensionFactories(extendibleType);
        for (ExtensionFactory<T> factory : factoryList) {
            final Class[] classes = factory.getExtensionTypes();
            for (Class aClass : classes) {
                if (aClass.equals(extensionType)) {
                    return factory;
                }
            }
        }
        return null;
    }

    private static class Impl extends ExtensionManager {
        private HashMap<Class, List<ExtensionFactory>> extensionFactoryListMap = new HashMap<Class, List<ExtensionFactory>>();

        @Override
        public <T> void register(Class<T> extendibleType, ExtensionFactory<T> factory) {
            Assert.notNull(extendibleType, "extendibleType");
            Assert.notNull(factory, "factory");
            List<ExtensionFactory> extensionFactoryList = extensionFactoryListMap.get(extendibleType);
            if (extensionFactoryList == null) {
                extensionFactoryList = new ArrayList<ExtensionFactory>();
                extensionFactoryListMap.put(extendibleType, extensionFactoryList);
            }
            extensionFactoryList.add(factory);
        }

        @Override
        public <T> void unregister(Class<T> extendibleType, ExtensionFactory<T> factory) {
            Assert.notNull(extendibleType, "extendibleType");
            Assert.notNull(factory, "factory");
            List<ExtensionFactory> extensionFactoryList = extensionFactoryListMap.get(extendibleType);
            if (extensionFactoryList != null) {
                extensionFactoryList.remove(factory);
            }
        }

        @Override
        public <T> List<ExtensionFactory<T>> getExtensionFactories(Class<T> extendibleType) {
            Assert.notNull(extendibleType, "extendibleType");
            List<ExtensionFactory> extensionFactoryList = extensionFactoryListMap.get(extendibleType);
            if (extensionFactoryList != null) {
                final ArrayList<ExtensionFactory<T>> factoryArrayList = new ArrayList<ExtensionFactory<T>>(extensionFactoryList.size());
                for (ExtensionFactory factory : extensionFactoryList) {
                    factoryArrayList.add((ExtensionFactory<T>) factory);
                }
                return factoryArrayList;
            } else {
                return new ArrayList<ExtensionFactory<T>>(0);
            }
        }
    }
}