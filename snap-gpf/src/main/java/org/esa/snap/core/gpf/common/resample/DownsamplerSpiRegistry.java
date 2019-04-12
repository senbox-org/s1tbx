package org.esa.snap.core.gpf.common.resample;

import com.bc.ceres.core.ServiceRegistry;

import java.util.Set;

/**
 * Created by obarrile on 12/04/2019.
 */
public interface DownsamplerSpiRegistry {

    /**
     * Loads the SPIs defined in {@code META-INF/services}.
     */
    void loadDownsamplerSpis();

    /**
     * @return The set of all registered downsampler SPIs.
     */
    Set<DownsamplerSpi> getDownsamplerSpis();

    /**
     * Gets the {@link ServiceRegistry ServiceRegistry}
     *
     * @return the {@link ServiceRegistry service registry}
     */
    ServiceRegistry<DownsamplerSpi> getServiceRegistry();

    /**
     * Gets a registered upsampler SPI.
     *
     * @param alias a name identifying the upsampler SPI.
     *
     * @return the upsampler SPI, or <code>null</code>
     */
    DownsamplerSpi getDownsamplerSpi(String alias);

    /**
     * Adds the given {@link DownsamplerSpi downsamplerSpi} to this registry.
     *
     * @param downsamplerSpi the SPI to add
     *
     * @return {@code true}, if the {@link DownsamplerSpi} could be successfully added, otherwise {@code false}
     */
    boolean addDownsamplerSpi(DownsamplerSpi downsamplerSpi);

    /**
     * Adds the given {@link DownsamplerSpi downsamplerSpi} to this registry.
     *
     * @param (downsamplerAlias) name used as key for the registration.
     * @param downsamplerSpi the SPI to add
     *
     * @return {@code true}, if the {@link DownsamplerSpi} could be successfully added, otherwise {@code false}
     */
    boolean addDownsamplerSpi(String downsamplerAlias, DownsamplerSpi downsamplerSpi);

    /**
     * Removes the given {@link DownsamplerSpi downsamplerSpi} this registry.
     *
     * @param downsamplerSpi the SPI to remove
     *
     * @return {@code true}, if the SPI could be removed, otherwise {@code false}
     */
    boolean removeDownsamplerSpi(DownsamplerSpi downsamplerSpi);


    /**
     *  Gets a set of all aliases
     *
     * @return the Set&lt;string&gt; of alias keys
     */
    public Set getAliases();
}
