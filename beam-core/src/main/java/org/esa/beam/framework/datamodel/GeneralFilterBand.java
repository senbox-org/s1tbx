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
package org.esa.beam.framework.datamodel;

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
    }

    private final int subWindowSize;
    private final OpType opType;

    /**
     * Creates a GeneralFilterBand.
     *
     * @param name             the name of the band.
     * @param source           the source which shall be filtered.
     * @param subWindowSize    the window size (width/height) used by the filter
     * @param opType the predefined operation type.
     */
    public GeneralFilterBand(String name, RasterDataNode source, int subWindowSize, OpType opType) {
        super(name,
              source.getGeophysicalDataType() == ProductData.TYPE_FLOAT64 ? ProductData.TYPE_FLOAT64 : ProductData.TYPE_FLOAT32,
              source.getSceneRasterWidth(),
              source.getSceneRasterHeight(),
              source);
        Assert.notNull(opType, "opType");
        this.subWindowSize = subWindowSize;
        this.opType = opType;
    }

    public int getSubWindowSize() {
        return subWindowSize;
    }

    public OpType getOpType() {
        return opType;
    }

    /**
     *
     * Returns the source lvel-image according the the
     * @param sourceImage The geophysical source image. No-data is masked as NaN.
     * @param level       The image level.
     * @param rh          Rendering hints. JAI.KEY_BORDER_EXTENDER is set to BorderExtenderCopy.BORDER_COPY.
     * @return The resulting filtered level image.
     */
    @Override
    protected RenderedImage createSourceLevelImage(RenderedImage sourceImage, int level, RenderingHints rh) {
        if (getOpType() == OpType.MIN) {
            return GeneralFilterDescriptor.create(sourceImage, new GeneralFilterFunction.Min(subWindowSize), rh);
        } else if (getOpType() == OpType.MAX) {
            return GeneralFilterDescriptor.create(sourceImage, new GeneralFilterFunction.Max(subWindowSize), rh);
        } else if (getOpType() == OpType.MEDIAN) {
            return GeneralFilterDescriptor.create(sourceImage, new GeneralFilterFunction.Median(subWindowSize), rh);
        } else if (getOpType() == OpType.MEAN) {
            return GeneralFilterDescriptor.create(sourceImage, new GeneralFilterFunction.Mean(subWindowSize), rh);
        } else if (getOpType() == OpType.STDDEV) {
            return GeneralFilterDescriptor.create(sourceImage, new GeneralFilterFunction.StdDev(subWindowSize), rh);
        }
        throw new IllegalStateException(String.format("Unsupported operation type '%s'", getOpType()));
    }
}