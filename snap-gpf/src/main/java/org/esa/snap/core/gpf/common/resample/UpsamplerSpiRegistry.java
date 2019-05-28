package org.esa.snap.core.gpf.common.resample;

import com.bc.ceres.core.ServiceRegistry;

import java.util.Set;

/**
 * Created by obarrile on 11/04/2019.
 */
public interface UpsamplerSpiRegistry {

    /**
     * Loads the SPIs defined in {@code META-INF/services}.
     */
    void loadUpsamplerSpis();

    /**
     * @return The set of all registered operator SPIs.
     * @since BEAM 5
     */
    Set<UpsamplerSpi> getUpsamplerSpis();

    /**
     * Gets the {@link ServiceRegistry ServiceRegistry}
     *
     * @return the {@link ServiceRegistry service registry}
     */
    ServiceRegistry<UpsamplerSpi> getServiceRegistry();

    /**
     * Gets a registered upsampler SPI.
     *
     * @param alias a name identifying the upsampler SPI.
     *
     * @return the upsampler SPI, or <code>null</code>
     */
    UpsamplerSpi getUpsamplerSpi(String alias);

    /**
     * Adds the given {@link UpsamplerSpi upsamplerSpi} to this registry.
     *
     * @param upsamplerSpi the SPI to add
     *
     * @return {@code true}, if the {@link UpsamplerSpi} could be successfully added, otherwise {@code false}
     */
    boolean addUpsamplerSpi(UpsamplerSpi upsamplerSpi);

    /**
     * Adds the given {@link UpsamplerSpi upsamplerSpi} to this registry.
     *
     * @param (upsamplerAlias) name used as key for the registration.
     * @param upsamplerSpi the SPI to add
     *
     * @return {@code true}, if the {@link UpsamplerSpi} could be successfully added, otherwise {@code false}
     */
    boolean addUpsamplerSpi(String upsamplerAlias, UpsamplerSpi upsamplerSpi);

    /**
     * Removes the given {@link UpsamplerSpi upsamplerSpi} this registry.
     *
     * @param upsamplerSpi the SPI to remove
     *
     * @return {@code true}, if the SPI could be removed, otherwise {@code false}
     */
    boolean removeUpsamplerSpi(UpsamplerSpi upsamplerSpi);


    /**
     *  Gets a set of all aliases
     *
     * @return the Set&lt;string&gt; of alias keys
     */
    public Set getAliases();
}
