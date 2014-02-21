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

package org.esa.beam.operator.cloud.internal;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.dataio.ProductSubsetDef;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;

import java.awt.Rectangle;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * <p><i><b>IMPORTANT NOTE:</b>
 * This class not an API.
 * It is not intended to be used by clients.</i>
 * </p>
 * <p/>
 * A ProcessingNode takes a single input product and creates an output product
 * using the same pixel resolution and the same spatial reference system as the input product.
 * <p/>
 * The method {@link #readFrameData(org.esa.beam.framework.datamodel.Band, int, int, int, int, org.esa.beam.framework.datamodel.ProductData)}
 * will continue using pre-cached and pre-computed data as long as the frame rectangle will not change.
 * <p>If the frame rectangle changes, the {@link #processFrame(int, int, int, int, ProgressMonitor)} method is called in order
 * to compute the data frame for each of the bands contained in the target product.
 */
public abstract class ProcessingNode implements ProductReader {

    private Product sourceProduct;
    private Product targetProduct;
    private Rectangle frameRectangle;
    private Map<String, ProductData> frameDataMap;

    protected ProcessingNode() {
        this.frameRectangle = null;
        this.frameDataMap = new HashMap<>(31);
    }

    protected Product getSourceProduct() {
        return sourceProduct;
    }

    public Product createTargetProduct() {
        this.targetProduct = createTargetProductImpl();
        this.targetProduct.setProductReader(this);
        return targetProduct;
    }

    public ProductData getFrameData(final String targetBandName) {
        final Band targetBand = getTargetBand(targetBandName);
        final ProductData frameData;
        if (targetBand != null) {
            frameData = getFrameData(targetBand);
        } else {
            frameData = null;
        }
        return frameData;
    }

    public Band getTargetBand(final String targetBandName) {
        return targetProduct.getBand(targetBandName);
    }

    public ProductData getFrameData(final Band targetBand) {
        if (frameRectangle == null) {
            return null;
        }
        ProductData frameData = frameDataMap.get(targetBand.getName());
        final int numElems = frameRectangle.width * frameRectangle.height;
        if (frameData == null || frameData.getNumElems() != numElems) {
            frameData = targetBand.createCompatibleProductData(numElems);
            frameDataMap.put(targetBand.getName(), frameData);
        }
        return frameData;
    }

    public void startProcessing() throws Exception {
    }

    protected abstract Product createTargetProductImpl();

    protected abstract void processFrame(int frameX, int frameY, int frameW, int frameH, ProgressMonitor pm) throws
                                                                                                             IOException;

    protected void clearFrameDataMap() {
        frameDataMap.clear();
    }

    public void setUp(final Map config) throws IOException {
    }

    private void setFrameRectangle(final int frameX, final int frameY, final int frameW, final int frameH) {
        frameRectangle = new Rectangle(frameX, frameY, frameW, frameH);
    }

    private synchronized void readFrameData(final Band targetBand, final int frameX, final int frameY, final int frameW,
                                            final int frameH, final ProductData targetData) throws IOException {
        if (isNewFrame(frameX, frameY, frameW, frameH)) {
            setFrameRectangle(frameX, frameY, frameW, frameH);
            processFrame(frameX, frameY, frameW, frameH, ProgressMonitor.NULL);
        }
        final ProductData frameData = getFrameData(targetBand);
        copyFrameData(frameData, targetData, frameX, frameY, frameW, frameH);
    }

    private boolean isNewFrame(final int frameX, final int frameY, final int frameW, final int frameH) {
        return frameRectangle == null || !frameRectangle.contains(frameX, frameY, frameW, frameH);
    }

    private void copyFrameData(final ProductData sourceData,
                               final ProductData targetData,
                               final int targetX,
                               final int targetY,
                               final int targetW,
                               final int targetH) throws IOException {
        final Object sourceElems = sourceData.getElems();
        final Object targetElems = targetData.getElems();
        final int targetNumElems = targetData.getNumElems();
        if (sourceElems.getClass().equals(targetElems.getClass())) {
            if (frameRectangle.x == targetX &&
                frameRectangle.y == targetY &&
                frameRectangle.width == targetW &&
                frameRectangle.height == targetH) {
                System.arraycopy(sourceElems, 0, targetElems, 0, targetNumElems);
            } else {
                final int offsetY = targetY - frameRectangle.y;
                int sourceIndex = frameRectangle.width * offsetY + targetX;
                int targetIndex = 0;
                for (int y = offsetY; y < offsetY + targetH; y++) {
                    System.arraycopy(sourceElems, sourceIndex, targetElems, targetIndex, targetW);
                    sourceIndex += frameRectangle.width;
                    targetIndex += targetW;
                }
            }
        } else {
            throw new IOException("unsupported type conversion");
        }
    }

    @Override
    public Product readProductNodes(final Object input, final ProductSubsetDef subsetDef) throws IOException {
        if (subsetDef != null) {
            throw new IllegalArgumentException("subsetDef != null (subsets are not supported)");
        }
        if (!(input instanceof Product)) {
            throw new IllegalArgumentException("illegal input type");
        }

        sourceProduct = (Product) input;
        return createTargetProduct();
    }


    @Override
    public void readBandRasterData(final Band targetBand,
                                   final int frameX,
                                   final int frameY,
                                   final int frameW,
                                   final int frameH,
                                   final ProductData targetData,
                                   ProgressMonitor pm) throws IOException {
        readFrameData(targetBand, frameX, frameY, frameW, frameH, targetData);
    }

    @Override
    public void close() throws IOException {
        clearFrameDataMap();
    }


    @Override
    public ProductReaderPlugIn getReaderPlugIn() {
        throw new IllegalStateException("Should never be asked for reader plugin");
    }

    @Override
    public Object getInput() {
        throw new IllegalStateException("Should never be asked for input");
    }

    @Override
    public final ProductSubsetDef getSubsetDef() {
        throw new IllegalStateException("Should never be asked for subset definition");
    }

}
