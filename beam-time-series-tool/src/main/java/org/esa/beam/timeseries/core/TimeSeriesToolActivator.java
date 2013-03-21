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

package org.esa.beam.timeseries.core;

import com.bc.ceres.core.CoreException;
import com.bc.ceres.core.runtime.Activator;
import com.bc.ceres.core.runtime.ModuleContext;
import org.esa.beam.dataio.dimap.DimapProductWriter;
import org.esa.beam.dataio.dimap.DimapProductWriterPlugIn;
import org.esa.beam.framework.dataio.ProductIOPlugInManager;
import org.esa.beam.framework.dataio.ProductWriterPlugIn;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.timeseries.core.timeseries.datamodel.AbstractTimeSeries;

import java.util.Iterator;

/**
 * Activator class for extend the default writer behavior and listening to visat´s ProductManager.
 * <p/>
 * <p><i>Note that this class is not yet public API. Interface may change in future releases.</i></p>
 *
 * @author Sabine Embacher
 */
public class TimeSeriesToolActivator implements Activator {

    private final DimapProductWriter.VetoableShouldWriteListener vetoableShouldWriteListener;

    public TimeSeriesToolActivator() {
        vetoableShouldWriteListener = createVetoableShouldWriteListener();
    }

    @Override
    public void start(ModuleContext moduleContext) throws CoreException {
        final ProductIOPlugInManager ioPlugInManager = ProductIOPlugInManager.getInstance();
        final Iterator<ProductWriterPlugIn> allWriterPlugIns = ioPlugInManager.getAllWriterPlugIns();
        while (allWriterPlugIns.hasNext()) {
            ProductWriterPlugIn writerPlugIn = allWriterPlugIns.next();
            if (writerPlugIn instanceof DimapProductWriterPlugIn) {
                ((DimapProductWriterPlugIn) writerPlugIn).addVetoableShouldWriteListener(vetoableShouldWriteListener);
            }
        }
    }

    @Override
    public void stop(ModuleContext moduleContext) throws CoreException {
        final ProductIOPlugInManager ioPlugInManager = ProductIOPlugInManager.getInstance();
        final Iterator<ProductWriterPlugIn> allWriterPlugIns = ioPlugInManager.getAllWriterPlugIns();
        while (allWriterPlugIns.hasNext()) {
            ProductWriterPlugIn writerPlugIn = allWriterPlugIns.next();
            if (writerPlugIn instanceof DimapProductWriterPlugIn) {
                ((DimapProductWriterPlugIn) writerPlugIn).removeVetoableShouldWriteListener(vetoableShouldWriteListener);
            }
        }
    }

    private DimapProductWriter.VetoableShouldWriteListener createVetoableShouldWriteListener() {
        return new DimapProductWriter.VetoableShouldWriteListener() {
            @Override
            public boolean shouldWrite(ProductNode node) {
                if (!node.getProduct().getProductType().equals(AbstractTimeSeries.TIME_SERIES_PRODUCT_TYPE)) {
                    return true;
                } else if (node instanceof RasterDataNode) {
                    return false;
                }
                return true;
            }
        };
    }
}
