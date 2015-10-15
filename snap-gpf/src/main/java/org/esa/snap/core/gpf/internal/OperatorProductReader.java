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

package org.esa.snap.core.gpf.internal;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.dataio.DecodeQualification;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.dataio.ProductSubsetDef;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.util.io.SnapFileFilter;

import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.IOException;
import java.util.Locale;

/**
 * The <code>OperatorProductReader</code> is an adapter class that wraps
 * <code>Operator</code>s to BEAM <code>ProductReader</code>s. It enables
 * the usage of BEAM <code>Product</code>s inside of this framework without
 * the necessity to make changes on the <code>Product</code>'s signature.
 *
 * @author Marco Peters
 * @author Norman Fomferra
 */
public class OperatorProductReader implements ProductReader {

    private static PlugIn plugIn = new PlugIn();
    private OperatorContext operatorContext;

    /**
     * Creates a <code>OperatorProductReader</code> instance.
     *
     * @param operatorContext the operator context
     */
    public OperatorProductReader(OperatorContext operatorContext) {
        this.operatorContext = operatorContext;
    }

    public OperatorContext getOperatorContext() {
        return operatorContext;
    }

    public Object getInput() {
        return operatorContext.getSourceProducts();
    }

    public ProductReaderPlugIn getReaderPlugIn() {
        return plugIn;
    }

    public ProductSubsetDef getSubsetDef() {
        return null;
    }

    public Product readProductNodes(Object input, ProductSubsetDef subsetDef) throws IOException {
        try {
            return operatorContext.getTargetProduct();
        } catch (OperatorException e) {
            throw new IOException(e);
        }
    }

    public void readBandRasterData(Band destBand, int destOffsetX,
                                   int destOffsetY, int destWidth,
                                   int destHeight,
                                   ProductData destBuffer,
                                   ProgressMonitor pm) throws IOException {
        operatorContext.getLogger().warning("OperatorProductReader.readBandRasterData: Should not come here anymore since raster data shall only be computed by OperatorImage");
        final RenderedImage image = operatorContext.getTargetImage(destBand);
        /////////////////////////////////////////////////////////////////////
        //
        // Note: GPF pull-processing is triggered here!!!
        //
        Raster data = image.getData(new Rectangle(destOffsetX,
                                                  destOffsetY,
                                                  destWidth,
                                                  destHeight));
        //
        /////////////////////////////////////////////////////////////////////
        data.getDataElements(destOffsetX, destOffsetY, destWidth, destHeight, destBuffer.getElems());
    }

    public synchronized void close() throws IOException {
        if (operatorContext != null && !operatorContext.isDisposed()) {
            operatorContext.dispose(); // disposes operator as well
            operatorContext = null;
        }
    }

    @Override
    public String toString() {
        return "OperatorProductReader[op=" + operatorContext.getOperator().getClass().getSimpleName() + "]";
    }

    private static class PlugIn implements ProductReaderPlugIn {

        public DecodeQualification getDecodeQualification(Object input) {
            return input instanceof OperatorContext ? DecodeQualification.INTENDED : DecodeQualification.UNABLE;
        }

        public ProductReader createReaderInstance() {
            throw new RuntimeException("not implemented");
        }

        public Class[] getInputTypes() {
            return new Class[]{OperatorContext.class};
        }

        public String[] getDefaultFileExtensions() {
            return new String[0];
        }

        public String getDescription(Locale locale) {
            return "Adapts an Operator to a ProductReader";
        }

        public String[] getFormatNames() {
            return new String[]{"GPF_IN_MEMORY"};
        }

        public SnapFileFilter getProductFileFilter() {
            return null;
        }
    }
}
