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
package org.esa.snap.core.dataio;

import com.bc.ceres.core.ServiceRegistry;
import com.bc.ceres.core.ServiceRegistryManager;
import org.esa.snap.core.util.Debug;
import org.esa.snap.core.util.Guardian;
import org.esa.snap.core.util.ServiceLoader;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

/**
 * The <code>ProductIOPlugInManager</code> class is used to manage all registered reader and writer plug-ins.
 * <p> This class implements the singleton design pattern, since only one manager instance is required in the system.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 */
public class ProductIOPlugInManager {

    private final ServiceRegistry<ProductReaderPlugIn> readerPlugIns;
    private final ServiceRegistry<ProductWriterPlugIn> writerPlugIns;

    /**
     * Gets this's managers singleton instance.
     */
    public static ProductIOPlugInManager getInstance() {
        return Holder.instance;
    }

    /**
     * Gets all registered reader plug-ins. In the case that no reader plug-ins are registered, an empty iterator is
     * returned.
     *
     * @return an iterator containing all registered reader plug-ins
     */
    public Iterator<ProductReaderPlugIn> getAllReaderPlugIns() {
        return readerPlugIns.getServices().iterator();
    }

    /**
     * Gets all reader plug-ins which support the given format name. In the case that no reader plug-in is found, an
     * empty iterator is returned.
     *
     * @param formatName the name of the format, must not be <code>null</code>
     *
     * @return an iterator containing all reader plug-ins supporting the given format
     */
    public Iterator<ProductReaderPlugIn> getReaderPlugIns(String formatName) {
        Guardian.assertNotNull("formatName", formatName);
        return getProductIOPlugIns(readerPlugIns.getServices(), formatName);
    }

    /**
     * Registers the specified reader plug-in by adding it to this manager. If the given reader plug-in is
     * <code>null</code>, nothing happens.
     *
     * @param readerPlugIn the reader plug-in to be added to this manager
     */
    public void addReaderPlugIn(ProductReaderPlugIn readerPlugIn) {
        readerPlugIns.addService(readerPlugIn);
    }

    /**
     * Removes the first occurrence of the specified reader plug-in. If this manager does not contain the reader, it is
     * unchanged.
     *
     * @param readerPlugIn the reader plug-in to be removed from this manager, if present
     *
     * @return <code>true</code> if this manager contained the specified reader plug-in
     */
    public boolean removeReaderPlugIn(ProductReaderPlugIn readerPlugIn) {
        return readerPlugIns.removeService(readerPlugIn);
    }

    /**
     * Gets all registered writer plug-ins. In the case that no writer plug-ins are registered, an empty iterator is
     * returned.
     *
     * @return an iterator containing all registered writer plug-ins
     */
    public Iterator<ProductWriterPlugIn> getAllWriterPlugIns() {
        return writerPlugIns.getServices().iterator();
    }

    /**
     * Gets all writer plug-ins which support the given format name. In the case that no writer plug-in is found, an
     * empty iterator is returned.
     *
     * @param formatName the name of the format, must not be <code>null</code>
     *
     * @return an iterator containing all writer plug-ins supporting the given format
     */
    public Iterator<ProductWriterPlugIn> getWriterPlugIns(String formatName) {
        Guardian.assertNotNull("formatName", formatName);
        return getProductIOPlugIns(writerPlugIns.getServices(), formatName);
    }


    /**
     * Registers the specified writer plug-in by adding it to this manager. If the given writer plug-in is
     * {@code null}, nothing happens.
     *
     * @param writerPlugIn the writer plug-in to be added to this manager
     */
    public void addWriterPlugIn(ProductWriterPlugIn writerPlugIn) {
        writerPlugIns.addService(writerPlugIn);
    }

    /**
     * Removes the first occurrence of the specified writer plug-in. If this manager does not contain the writer, it is
     * unchanged.
     *
     * @param writerPlugIn the writer plug-in to be removed from this manager, if present
     *
     * @return {@code true} if this manager contained the specified writer plug-in
     */
    public boolean removeWriterPlugIn(ProductWriterPlugIn writerPlugIn) {
        return writerPlugIns.removeService(writerPlugIn);
    }

    /**
     * Returns a {@code String[]} which contains all the product writer format strings of registered product
     * writers, never Null.
     *
     * @return a {@code String[]} which contains all the product writer format strings of registered product
     *         writers.
     */
    public String[] getAllProductWriterFormatStrings() {
        Iterator<? extends ProductIOPlugIn> iterator = getAllWriterPlugIns();
        return getAvailableFormatNames(iterator);
    }

    /**
     * Returns a {@code String[]} which contains all the product reader format strings of registered product
     * readers, never Null.
     *
     * @return a {@code String[]} which contains all the product reader format strings of registered product
     *         reader.
     */
    public String[] getAllProductReaderFormatStrings() {
        Iterator<? extends ProductIOPlugIn> iterator = getAllReaderPlugIns();
        return getAvailableFormatNames(iterator);
    }

    ///////////////////////////////////////////////////////////////////////////
    /////// END OF PUBLIC
    ///////////////////////////////////////////////////////////////////////////

    private String[] getAvailableFormatNames(Iterator<? extends ProductIOPlugIn> iterator) {
        ProductIOPlugIn ioPlugIn;
        ArrayList<String> formats = new ArrayList<>();

        while (iterator.hasNext()) {
            ioPlugIn = iterator.next();
            String[] formatNames = ioPlugIn.getFormatNames();
            for (String formatName : formatNames) {
                if (!formats.contains(formatName)) {
                    formats.add(formatName);
                }
            }
        }

        return formats.toArray(new String[formats.size()]);
    }

    private static <T extends ProductIOPlugIn> Iterator<T> getProductIOPlugIns(Set<T> ioPlugIns, String formatName) {
        Debug.assertNotNull(ioPlugIns);
        Debug.assertNotNull(formatName);

        List<T> validPlugins = new ArrayList<T>();
        for (T plugIn : ioPlugIns) {
            String[] formatNames = plugIn.getFormatNames();
            for (String otherFormatName : formatNames) {
                if (otherFormatName.equalsIgnoreCase(formatName)) {
                    validPlugins.add(plugIn);
                }
            }
        }
        return validPlugins.iterator();
    }

    /**
     * Protected constructor - singleton
     */
    protected ProductIOPlugInManager() {
        ServiceRegistryManager serviceRegistryManager = ServiceRegistryManager.getInstance();
        readerPlugIns = serviceRegistryManager.getServiceRegistry(ProductReaderPlugIn.class);
        writerPlugIns = serviceRegistryManager.getServiceRegistry(ProductWriterPlugIn.class);

        ServiceLoader.loadServices(readerPlugIns);
        ServiceLoader.loadServices(writerPlugIns);
    }
    
    // Initialization on demand holder idiom
    private static class Holder {
        private static final ProductIOPlugInManager instance = new ProductIOPlugInManager();
    }
}
