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
 * Created by obarrile on 12/04/2019.
 */
public class DownsamplerSpiRegistryImpl implements DownsamplerSpiRegistry{

    private final ServiceRegistry<DownsamplerSpi> serviceRegistry;
    private final Map<String, String> classNames;
    private final Map<String, DownsamplerSpi> extraDownsamplerSpis;

    /**
     * The constructor.
     *
     * @param serviceRegistry The underlying service registry used by this instance.
     */
    public DownsamplerSpiRegistryImpl(ServiceRegistry<DownsamplerSpi> serviceRegistry) {
        this.serviceRegistry = serviceRegistry;
        classNames = new ConcurrentHashMap<>(20);
        this.serviceRegistry.addListener(new ServiceRegistryListener<DownsamplerSpi>() {
            @Override
            public void serviceAdded(ServiceRegistry<DownsamplerSpi> registry, DownsamplerSpi service) {
                registerAlias(service);
            }

            @Override
            public void serviceRemoved(ServiceRegistry<DownsamplerSpi> registry, DownsamplerSpi service) {
                unregisterAlias(service);
            }
        });
        Set<DownsamplerSpi> services = this.serviceRegistry.getServices();
        for (DownsamplerSpi downsamplerSpi : services) {
            registerAlias(downsamplerSpi);
        }
        extraDownsamplerSpis = new ConcurrentHashMap<>();
    }

    /**
     * Loads the SPIs defined in {@code META-INF/services}.
     */
    @Override
    public void loadDownsamplerSpis() {
        ServiceLoader.loadServices(getServiceRegistry());
    }

    /**
     * @return The set of all registered operator SPIs.
     */
    @Override
    public Set<DownsamplerSpi> getDownsamplerSpis() {
        HashSet<DownsamplerSpi> downsamplerSpis = new HashSet<>(serviceRegistry.getServices());
        downsamplerSpis.addAll(extraDownsamplerSpis.values());
        return downsamplerSpis;
    }

    /**
     * Gets the {@link ServiceRegistry ServiceRegistry}
     *
     * @return the {@link ServiceRegistry service registry}
     */
    @Override
    public ServiceRegistry<DownsamplerSpi> getServiceRegistry() {
        return serviceRegistry;
    }

    /**
     * Gets a registered Downsampler SPI.
     *
     * @param alias A name identifying the Downsampler SPI.
     * @return the Downsampler SPI, or {@code null}
     */
    @Override
    public DownsamplerSpi getDownsamplerSpi(String alias) {
        DownsamplerSpi service = serviceRegistry.getService(alias);
        if (service != null) {
            return service;
        }

        service = getName(extraDownsamplerSpis, alias);
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
     * Adds the given {@link DownsamplerSpi operatorSpi} to this registry.
     *
     * @param downsamplerSpi the SPI to add
     * @return {@code true}, if the {@link DownsamplerSpi} could be successfully added, otherwise {@code false}
     */
    @Override
    public boolean addDownsamplerSpi(DownsamplerSpi downsamplerSpi) {
        String spiClassName = downsamplerSpi.getClass().getName();
        if (serviceRegistry.getService(spiClassName) == downsamplerSpi) {
            return false;
        }
        registerAlias(downsamplerSpi);
        return serviceRegistry.addService(downsamplerSpi);
    }

    /**
     * Adds the given {@link DownsamplerSpi downsamplerSpi} to this registry.
     *
     * @param (alias) name used as key for the registration.
     * @param downsamplerSpi  the SPI to add
     * @return {@code true}, if the {@link DownsamplerSpi} could be successfully added, otherwise {@code false}
     */
    @Override
    public boolean addDownsamplerSpi(String alias, DownsamplerSpi downsamplerSpi) {
        if (alias.equals(downsamplerSpi.getClass().getName())) {
            return addDownsamplerSpi(downsamplerSpi);
        }
        if (extraDownsamplerSpis.get(alias) == downsamplerSpi) {
            return false;
        }
        registerAlias(downsamplerSpi.getClass().getName(), alias);
        extraDownsamplerSpis.put(alias, downsamplerSpi);
        return true;
    }

    /**
     * Removes the given {@link DownsamplerSpi downsamplerSpi} this registry.
     *
     * @param downsamplerSpi the SPI to remove
     * @return {@code true}, if the SPI could be removed, otherwise {@code false}
     */
    @Override
    public boolean removeDownsamplerSpi(DownsamplerSpi downsamplerSpi) {
        if (!serviceRegistry.removeService(downsamplerSpi)) {
            Stream<Map.Entry<String, DownsamplerSpi>> extraSpiStream = extraDownsamplerSpis.entrySet().stream();
            Optional<Map.Entry<String, DownsamplerSpi>> spiEntry = extraSpiStream.filter(entry -> entry.getValue() == downsamplerSpi).findFirst();
            if (spiEntry.isPresent() && extraDownsamplerSpis.remove(spiEntry.get().getKey(), spiEntry.get().getValue())) {
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

    private void registerAlias(DownsamplerSpi downsamplerSpi) {
        String alias = downsamplerSpi.getAlias();
        if (alias != null) {
            registerAlias(downsamplerSpi.getClass().getName(), alias);
        }
    }

    private void unregisterAlias(DownsamplerSpi downsamplerSpi) {
        String alias = downsamplerSpi.getAlias();
        if (classNames.remove(alias) == null) {
            String spiClassName = downsamplerSpi.getClass().getName();
            Stream<String> stream = new HashSet<>(classNames.keySet()).stream();
            stream.filter(key -> classNames.get(key).equals(spiClassName)).forEach(classNames::remove);
        }
    }
}
