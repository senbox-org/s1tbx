/*
 * $Id: ProductIOPlugInManager.java,v 1.14 2007/03/22 14:11:08 norman Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.framework.dataio;

import com.bc.ceres.core.ServiceRegistry;
import com.bc.ceres.core.ServiceRegistryFactory;
import org.esa.beam.util.Debug;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.logging.BeamLogManager;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * The <code>ProductIOPlugInManager</code> class is used to manage all registered reader and writer plug-ins.
 * <p/>
 * <p> This class implements the singleton design pattern, since only one manager instance is required in the system.
 *
 * @author Norman Fomferra
 * @version $Revision: 1.14 $ $Date: 2007/03/22 14:11:08 $
 */
public class ProductIOPlugInManager {

    private static ProductIOPlugInManager instance;
    private final List<ProductReaderPlugIn> readerPlugIns;
    private final List<ProductWriterPlugIn> writerPlugIns;
    private static Logger logger = BeamLogManager.getSystemLogger();

    /**
     * Gets this's managers singleton instance.
     */
    public synchronized static ProductIOPlugInManager getInstance() {
        if (instance == null) {
            instance = new ProductIOPlugInManager();
            ServiceRegistryFactory factory = ServiceRegistryFactory.getInstance();
            ServiceRegistry<ProductReaderPlugIn> readerRegistry = factory.getServiceRegistry(ProductReaderPlugIn.class);
            Set<ProductReaderPlugIn> readerServices = readerRegistry.getServices();
            Debug.trace("registering product reader plugins...");
            for (ProductReaderPlugIn plugIn : readerServices) {
                instance.addReaderPlugIn(plugIn);
                Debug.trace("product reader plugin registered: " + plugIn.getClass().getName());
            }
            ServiceRegistry<ProductWriterPlugIn> writerRegistry = factory.getServiceRegistry(ProductWriterPlugIn.class);
            Set<ProductWriterPlugIn> writerServices = writerRegistry.getServices();
            Debug.trace("registering product writer plugins...");
            for (ProductWriterPlugIn plugIn : writerServices) {
                instance.addWriterPlugIn(plugIn);
                Debug.trace("product writer plugin registered: " + plugIn.getClass().getName());
            }
        }
        return instance;
    }

    /**
     * Gets all registered reader plug-ins. In the case that no reader plug-ins are registered, an empty iterator is
     * returned.
     *
     * @return an iterator containing all registered reader plug-ins
     */
    public Iterator getAllReaderPlugIns() {
        return readerPlugIns.iterator();
    }

    /**
     * Gets all reader plug-ins which support the given format name. In the case that no reader plug-in is found, an
     * empty iterator is returned.
     *
     * @param formatName the name of the format, must not be <code>null</code>
     *
     * @return an iterator containing all reader plug-ins supporting the given format
     */
    public Iterator getReaderPlugIns(String formatName) {
        Guardian.assertNotNull("formatName", formatName);
        return getProductIOPlugIns(readerPlugIns, formatName);
    }

    /**
     * Registers the specified reader plug-in by adding it to this manager. If the given reader plug-in is
     * <code>null</code>, nothing happens.
     *
     * @param readerPlugIn the reader plug-in to be added to this manager
     */
    public void addReaderPlugIn(ProductReaderPlugIn readerPlugIn) {
        if (readerPlugIn != null) {
            for (Object _readerPlugIn : readerPlugIns) {
                ProductIOPlugIn productIOPlugIn = (ProductIOPlugIn) _readerPlugIn;
                if (productIOPlugIn.equals(readerPlugIn) ||
                    productIOPlugIn.getClass().equals(readerPlugIn.getClass())) {
                    return;
                }
            }
            readerPlugIns.add(readerPlugIn);
        }
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
        return readerPlugIn != null && readerPlugIns.remove(readerPlugIn);
    }

    /**
     * Gets all registered writer plug-ins. In the case that no writer plug-ins are registered, an empty iterator is
     * returned.
     *
     * @return an iterator containing all registered writer plug-ins
     */
    public Iterator getAllWriterPlugIns() {
        return writerPlugIns.iterator();
    }

    /**
     * Gets all writer plug-ins which support the given format name. In the case that no writer plug-in is found, an
     * empty iterator is returned.
     *
     * @param formatName the name of the format, must not be <code>null</code>
     *
     * @return an iterator containing all writer plug-ins supporting the given format
     */
    public Iterator getWriterPlugIns(String formatName) {
        Guardian.assertNotNull("formatName", formatName);
        return getProductIOPlugIns(writerPlugIns, formatName);
    }


    /**
     * Registers the specified writer plug-in by adding it to this manager. If the given writer plug-in is
     * <code>null</code>, nothing happens.
     *
     * @param writerPlugIn the writer plug-in to be added to this manager
     */
    public void addWriterPlugIn(ProductWriterPlugIn writerPlugIn) {

        if (writerPlugIn != null) {
            for (ProductWriterPlugIn productIOPlugIn : writerPlugIns) {
                if (productIOPlugIn.equals(writerPlugIn) ||
                    productIOPlugIn.getClass().equals(writerPlugIn.getClass())) {
                    return;
                }
            }
            writerPlugIns.add(writerPlugIn);
        }
    }

    /**
     * Removes the first occurrence of the specified writer plug-in. If this manager does not contain the writer, it is
     * unchanged.
     *
     * @param writerPlugIn the writer plug-in to be removed from this manager, if present
     *
     * @return <code>true</code> if this manager contained the specified writer plug-in
     */
    public boolean removeWriterPlugIn(ProductWriterPlugIn writerPlugIn) {
        return writerPlugIn != null && writerPlugIns.remove(writerPlugIn);
    }

    /**
     * Returns a <code>String[]</code> which contains all the product writer format strings of registered product
     * writers, never Null. Returns never Null.
     *
     * @return a <code>String[]</code> which contains all the product writer format strings of registered product
     *         writers.
     */
    public String[] getAllProductWriterFormatStrings() {
        Iterator iterator = getAllWriterPlugIns();
        ProductIOPlugIn writer;
        ArrayList<String> formats = new ArrayList<String>();
        String[] formatNames;

        while (iterator.hasNext()) {
            writer = (ProductIOPlugIn) iterator.next();
            formatNames = writer.getFormatNames();
            for (String formatName : formatNames) {
                if (!formats.contains(formatName)) {
                    formats.add(formatName);
                }
            }
        }

        return formats.toArray(new String[formats.size()]);
    }

    ///////////////////////////////////////////////////////////////////////////
    /////// END OF PUBLIC
    ///////////////////////////////////////////////////////////////////////////

    private static Iterator getProductIOPlugIns(List ioPlugIns, String formatName) {
        Debug.assertNotNull(ioPlugIns);
        Debug.assertNotNull(formatName);

        List<ProductIOPlugIn> validPlugins = new ArrayList<ProductIOPlugIn>();
        // Reverse iterate through list (LIFO order)
        for (int i = ioPlugIns.size() - 1; i >= 0; --i) {
            ProductIOPlugIn plugin = (ProductIOPlugIn) ioPlugIns.get(i);
            String[] formatNames = plugin.getFormatNames();
            for (String otherFormatName : formatNames) {
                if (otherFormatName.equalsIgnoreCase(formatName)) {
                    validPlugins.add(plugin);
                }
            }
        }
        return validPlugins.iterator();
    }

    /**
     * Protected constructor - singleton
     */
    protected ProductIOPlugInManager() {
        readerPlugIns = new ArrayList<ProductReaderPlugIn>();
        writerPlugIns = new ArrayList<ProductWriterPlugIn>();
    }
}
