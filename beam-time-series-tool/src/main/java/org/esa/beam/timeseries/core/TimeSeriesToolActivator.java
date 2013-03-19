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
import org.esa.beam.dataio.dimap.DimapProductReaderPlugIn;
import org.esa.beam.dataio.dimap.DimapProductWriterPlugIn;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.dataio.ProductIOPlugInManager;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.dataio.ProductWriter;
import org.esa.beam.framework.dataio.ProductWriterPlugIn;

import java.util.Iterator;

/**
 * Activator class for patching the default reader and writer.
 *
 *<p><i>Note that this class is not yet public API. Interface may change in future releases.</i></p>
 *
 * @author Marco Peters
 * @author Thomas Storm
 */
public class TimeSeriesToolActivator implements Activator {

    public TimeSeriesToolActivator() {
    }


    @Override
    public void start(ModuleContext moduleContext) throws CoreException {
        patchDefaultReader();
        patchDefaultWriter();
    }

    private void patchDefaultReader() {
        ProductIOPlugInManager productIOPIM = ProductIOPlugInManager.getInstance();
        Iterator<ProductReaderPlugIn> readerIterator = productIOPIM.getReaderPlugIns(ProductIO.DEFAULT_FORMAT_NAME);
        //todo call of next() not sufficient
        final ProductReaderPlugIn readerPlugIn = readerIterator.next();
        productIOPIM.removeReaderPlugIn(readerPlugIn);
        productIOPIM.addReaderPlugIn(new PatchedDimapProductReaderPlugIn());
    }

    private void patchDefaultWriter() {
        ProductIOPlugInManager productIOPIM = ProductIOPlugInManager.getInstance();
        Iterator<ProductWriterPlugIn> dimapWriterIterator = productIOPIM.getWriterPlugIns(
                ProductIO.DEFAULT_FORMAT_NAME);
        //todo call of next() not sufficient
        final ProductWriterPlugIn dimapWriterPlugIn = dimapWriterIterator.next();
        productIOPIM.removeWriterPlugIn(dimapWriterPlugIn);
        productIOPIM.addWriterPlugIn(new PatchedDimapProductWriterPlugIn());
    }

    @Override
    public void stop(ModuleContext moduleContext) throws CoreException {
    }

    private static class PatchedDimapProductReaderPlugIn extends DimapProductReaderPlugIn {

        @Override
        public ProductReader createReaderInstance() {
            return new TimeSeriesProductReader(this);
        }
    }

    private static class PatchedDimapProductWriterPlugIn extends DimapProductWriterPlugIn {

        @Override
        public ProductWriter createWriterInstance() {
            return new TimeSeriesProductWriter(this);
        }

    }

}
