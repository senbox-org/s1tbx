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
package org.esa.snap.core.datamodel;

import com.bc.ceres.core.Assert;
import com.bc.ceres.jai.GeneralFilterFunction;
import com.bc.ceres.jai.operator.GeneralFilterDescriptor;

import java.awt.RenderingHints;
import java.awt.image.RenderedImage;

/**
 * A band that obtains its input data from an underlying source raster and filters
 * its data using a predefined {@link OpType operation type}.
 *
 * @author Norman Fomferra
 */
public class GeneralFilterBand extends FilterBand {

    /**
     * Predefined operation types.
     */
    public enum OpType {
        MIN,
        MAX,
        MEDIAN,
        MEAN,
        STDDEV,
        /**
         * Morphological erosion (= min)
         */
        EROSION,
        /**
         * Morphological dilation (= max)
         */
        DILATION,
        OPENING,
        CLOSING,
    }

    private final OpType opType;
    private final Kernel structuringElement;
    private final int iterationCount;

    /**
     * Creates a GeneralFilterBand.
     *
     * @param name               the name of the band.
     * @param source             the source which shall be filtered.
     * @param opType             the predefined operation type.
     * @param structuringElement the structuring element (as used by morphological filters)
     */
    public GeneralFilterBand(String name, RasterDataNode source, OpType opType, Kernel structuringElement, int iterationCount) {
        super(name,
              source.getGeophysicalDataType() == ProductData.TYPE_FLOAT64 ? ProductData.TYPE_FLOAT64 : ProductData.TYPE_FLOAT32,
              source.getRasterWidth(),
              source.getRasterHeight(),
              source);
        Assert.notNull(opType, "opType");
        Assert.notNull(structuringElement, "structuringElement");
        this.opType = opType;
        this.structuringElement = structuringElement;
        this.iterationCount = iterationCount;
    }

    public OpType getOpType() {
        return opType;
    }

    public Kernel getStructuringElement() {
        return structuringElement;
    }

    /**
     * Returns the source level-image according the the
     *
     * @param sourceImage The geophysical source image. No-data is masked as NaN.
     * @param level       The image level.
     * @param rh          Rendering hints. JAI.KEY_BORDER_EXTENDER is set to BorderExtenderCopy.BORDER_COPY.
     * @return The resulting filtered level image.
     */
    @Override
    protected RenderedImage createSourceLevelImage(RenderedImage sourceImage, int level, RenderingHints rh) {
        int x = structuringElement.getXOrigin();
        int y = structuringElement.getYOrigin();
        int w = structuringElement.getWidth();
        int h = structuringElement.getHeight();
        boolean[] data = toStructuringElementData(structuringElement);
        RenderedImage targetImage = sourceImage;
        for (int i = 0; i < iterationCount; i++) {
            targetImage = createSourceLevelImage(targetImage, x, y, w, h, data, rh);
        }
        return targetImage;
    }

    private RenderedImage createSourceLevelImage(RenderedImage sourceImage, int x, int y, int w, int h, boolean[] data, RenderingHints rh) {
        RenderedImage targetImage;
        if (getOpType() == OpType.MIN) {
            targetImage = GeneralFilterDescriptor.create(sourceImage, new GeneralFilterFunction.Min(w, h, x, y, data), rh);
        } else if (getOpType() == OpType.MAX) {
            targetImage = GeneralFilterDescriptor.create(sourceImage, new GeneralFilterFunction.Max(w, h, x, y, data), rh);
        } else if (getOpType() == OpType.MEDIAN) {
            targetImage = GeneralFilterDescriptor.create(sourceImage, new GeneralFilterFunction.Median(w, h, x, y, data), rh);
        } else if (getOpType() == OpType.MEAN) {
            targetImage = GeneralFilterDescriptor.create(sourceImage, new GeneralFilterFunction.Mean(w, h, x, y, data), rh);
        } else if (getOpType() == OpType.STDDEV) {
            targetImage = GeneralFilterDescriptor.create(sourceImage, new GeneralFilterFunction.StdDev(w, h, x, y, data), rh);
        } else if (getOpType() == OpType.EROSION) {
            targetImage = GeneralFilterDescriptor.create(sourceImage, new GeneralFilterFunction.Erosion(w, h, x, y, data), rh);
        } else if (getOpType() == OpType.DILATION) {
            targetImage = GeneralFilterDescriptor.create(sourceImage, new GeneralFilterFunction.Dilation(w, h, x, y, data), rh);
        } else if (getOpType() == OpType.OPENING) {
            targetImage = GeneralFilterDescriptor.create(sourceImage, new GeneralFilterFunction.Erosion(w, h, x, y, data), rh);
            targetImage = GeneralFilterDescriptor.create(targetImage, new GeneralFilterFunction.Dilation(w, h, x, y, data), rh);
        } else if (getOpType() == OpType.CLOSING) {
            targetImage = GeneralFilterDescriptor.create(sourceImage, new GeneralFilterFunction.Dilation(w, h, x, y, data), rh);
            targetImage = GeneralFilterDescriptor.create(targetImage, new GeneralFilterFunction.Erosion(w, h, x, y, data), rh);
        } else {
            throw new IllegalStateException(String.format("Unsupported operation type '%s'", getOpType()));
        }
        return targetImage;
    }

    private static boolean[] toStructuringElementData(Kernel kernel) {
        double[] kernelElements = kernel.getKernelData(null);
        boolean[] structuringElement = new boolean[kernelElements.length];
        boolean hasFalse = false;
        boolean hasTrue = false;
        for (int i = 0; i < kernelElements.length; i++) {
            boolean b = kernelElements[i] != 0.0;
            if (b) {
                hasTrue = true;
            } else {
                hasFalse = true;
            }
            structuringElement[i] = b;
        }
        return hasTrue && hasFalse ? structuringElement : null;
    }

}
