package org.esa.snap.core.gpf.common.resample;

import com.bc.ceres.core.Assert;
import com.bc.ceres.core.ServiceRegistry;
import com.bc.ceres.core.ServiceRegistryListener;
import org.esa.snap.core.util.ServiceLoader;
import org.esa.snap.core.util.SystemUtils;

import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Stream;

/**
 * Created by obarrile on 11/04/2019.
 */
public class UpsamplerSpiRegistryImpl implements UpsamplerSpiRegistry{

    private final ServiceRegistry<UpsamplerSpi> serviceRegistry;
    private final Map<String, String> classNames;
    private final Map<String, UpsamplerSpi> extraUpsamplerSpis;

    /**
     * The constructor.
     *
     * @param serviceRegistry The underlying service registry used by this instance.
     */
    public UpsamplerSpiRegistryImpl(ServiceRegistry<UpsamplerSpi> serviceRegistry) {
        this.serviceRegistry = serviceRegistry;
        classNames = new ConcurrentHashMap<>(20);
        this.serviceRegistry.addListener(new ServiceRegistryListener<UpsamplerSpi>() {
            @Override
            public void serviceAdded(ServiceRegistry<UpsamplerSpi> registry, UpsamplerSpi service) {
                registerAlias(service);
            }

            @Override
            public void serviceRemoved(ServiceRegistry<UpsamplerSpi> registry, UpsamplerSpi service) {
                unregisterAlias(service);
            }
        });
        Set<UpsamplerSpi> services = this.serviceRegistry.getServices();
        for (UpsamplerSpi upsamplerSpi : services) {
            registerAlias(upsamplerSpi);
        }
        extraUpsamplerSpis = new ConcurrentHashMap<>();
    }

    /**
     * Loads the SPIs defined in {@code META-INF/services}.
     */
    @Override
    public void loadUpsamplerSpis() {
        ServiceLoader.loadServices(getServiceRegistry());
    }

    /**
     * @return The set of all registered operator SPIs.
     */
    @Override
    public Set<UpsamplerSpi> getUpsamplerSpis() {
        HashSet<UpsamplerSpi> operatorSpis = new HashSet<>(serviceRegistry.getServices());
        operatorSpis.addAll(extraUpsamplerSpis.values());
        return operatorSpis;
    }

    /**
     * Gets the {@link ServiceRegistry ServiceRegistry}
     *
     * @return the {@link ServiceRegistry service registry}
     */
    @Override
    public ServiceRegistry<UpsamplerSpi> getServiceRegistry() {
        return serviceRegistry;
    }

    /**
     * Gets a registered upsampler SPI.
     *
     * @param alias A name identifying the upsampler SPI.
     * @return the upsampler SPI, or {@code null}
     */
    @Override
    public UpsamplerSpi getUpsamplerSpi(String alias) {
        UpsamplerSpi service = serviceRegistry.getService(alias);
        if (service != null) {
            return service;
        }

        service = getName(extraUpsamplerSpis, alias);
        if (service != null) {
            return service;
        }

        String className = getName(classNames, alias);
        if (className != null) {
            service = serviceRegistry.getService(className);
            if (service != null) {
                return service;
            }
        }

        return null;
    }

    private <T> T getName(Map<String, T> tMap, String alias) {
        Optional<String> optional = tMap.keySet().stream().filter(p -> p.equalsIgnoreCase(alias)).findFirst();
        return optional.isPresent() ? tMap.get(optional.get()) : null;
    }

    /**
     * Adds the given {@link UpsamplerSpi operatorSpi} to this registry.
     *
     * @param upsamplerSpi the SPI to add
     * @return {@code true}, if the {@link UpsamplerSpi} could be successfully added, otherwise {@code false}
     */
    @Override
    public boolean addUpsamplerSpi(UpsamplerSpi upsamplerSpi) {
        String spiClassName = upsamplerSpi.getClass().getName();
        if (serviceRegistry.getService(spiClassName) == upsamplerSpi) {
            return false;
        }
        registerAlias(upsamplerSpi);
        return serviceRegistry.addService(upsamplerSpi);
    }

    /**
     * Adds the given {@link UpsamplerSpi upsamplerSpi} to this registry.
     *
     * @param (alias) name used as key for the registration.
     * @param upsamplerSpi  the SPI to add
     * @return {@code true}, if the {@link UpsamplerSpi} could be successfully added, otherwise {@code false}
     */
    @Override
    public boolean addUpsamplerSpi(String alias, UpsamplerSpi upsamplerSpi) {
        if (alias.equals(upsamplerSpi.getClass().getName())) {
            return addUpsamplerSpi(upsamplerSpi);
        }
        if (extraUpsamplerSpis.get(alias) == upsamplerSpi) {
            return false;
        }
        registerAlias(upsamplerSpi.getClass().getName(), alias);
        extraUpsamplerSpis.put(alias, upsamplerSpi);
        return true;
    }

    /**
     * Removes the given {@link UpsamplerSpi upsamplerSpi} this registry.
     *
     * @param upsamplerSpi the SPI to remove
     * @return {@code true}, if the SPI could be removed, otherwise {@code false}
     */
    @Override
    public boolean removeUpsamplerSpi(UpsamplerSpi upsamplerSpi) {
        if (!serviceRegistry.removeService(upsamplerSpi)) {
            Stream<Map.Entry<String, UpsamplerSpi>> extraSpiStream = extraUpsamplerSpis.entrySet().stream();
            Optional<Map.Entry<String, UpsamplerSpi>> spiEntry = extraSpiStream.filter(entry -> entry.getValue() == upsamplerSpi).findFirst();
            if (spiEntry.isPresent() && extraUpsamplerSpis.remove(spiEntry.get().getKey(), spiEntry.get().getValue())) {
                unregisterAlias(spiEntry.get().getValue());
            } else {
                return false;
            }
        }
        return true;
    }

    /**
     * Gets the set of all aliases.
     *
     * @return the set of alias names.
     */
    public Set<String> getAliases() {
        return classNames.keySet();
    }


    private void registerAlias(String spiClassName, String aliasName) {
        Assert.notNull(aliasName, "aliasName");
        Assert.notNull(spiClassName, "spiClassName");
        if (classNames.get(aliasName) != null) {
            SystemUtils.LOG.severe(
                    spiClassName + ':' + aliasName + " conflicts with " + classNames.get(aliasName) + ':' + aliasName);
        }
        classNames.put(aliasName, spiClassName);
    }

    private void registerAlias(UpsamplerSpi upsamplerSpi) {
        String alias = upsamplerSpi.getAlias();
        if (alias != null) {
            registerAlias(upsamplerSpi.getClass().getName(), alias);
        }
    }

    private void unregisterAlias(UpsamplerSpi upsamplerSpi) {
        String alias = upsamplerSpi.getAlias();
        if (classNames.remove(alias) == null) {
            String spiClassName = upsamplerSpi.getClass().getName();
            Stream<String> stream = new HashSet<>(classNames.keySet()).stream();
            stream.filter(key -> classNames.get(key).equals(spiClassName)).forEach(classNames::remove);
        }
    }
}
