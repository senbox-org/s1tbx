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

import com.bc.ceres.jai.GeneralFilterFunction;
import com.bc.ceres.jai.operator.GeneralFilterDescriptor;

import java.awt.RenderingHints;
import java.awt.image.RenderedImage;

/**
 * A band that obtains its input data from an underlying source raster and filters
 * its data using an arbitrary {@link Operator algorithm}.
 * <p/>
 * <p><i>Note that this class is not yet public API and may change in future releases.</i></p>
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 */
public class GeneralFilterBand extends FilterBand {

    public static final Operator MIN = new Min();
    public static final Operator MAX = new Max();
    public static final Operator MEDIAN = new Median();
    public static final Operator MEAN = new Mean();
    public static final Operator STDDEV = new StdDev();

    private static final Operator[] operators = {MIN, MAX, MEDIAN, MEAN, STDDEV};

    private final int subWindowSize;
    private final Operator operator;

    /**
     * Creates a GeneralFilterBand.
     *
     * @param name          the name of the band.
     * @param source        the source which shall be filtered.
     * @param subWindowSize the window size (width/height) used by the filter
     * @param operator      the operator which performs the filter operation
     */
    public GeneralFilterBand(String name, RasterDataNode source, int subWindowSize, Operator operator) {
        super(name,
              source.getGeophysicalDataType() == ProductData.TYPE_FLOAT64 ? ProductData.TYPE_FLOAT64 : ProductData.TYPE_FLOAT32,
              source.getSceneRasterWidth(),
              source.getSceneRasterHeight(),
              source);
        this.subWindowSize = subWindowSize;
        this.operator = operator;
    }

    /**
     * Creates an instance {@link Operator} by the given class name
     *
     * @param operatorClassName the class name
     * @return instance of {@link Operator}
     */
    public static Operator createOperator(String operatorClassName) {
        for (Operator operator : operators) {
            if (operator.getClass().getName().equals(operatorClassName)) {
                return operator;
            }
        }
        return null;
    }

    public int getSubWindowSize() {
        return subWindowSize;
    }

    public Operator getOperator() {
        return operator;
    }

    @Override
    protected RenderedImage createSourceLevelImage(RenderedImage sourceImage, int level, RenderingHints rh) {
        if (getOperator() == MIN) {
            return GeneralFilterDescriptor.create(sourceImage, new GeneralFilterFunction.Min(subWindowSize), rh);
        } else if (getOperator() == MAX) {
            return GeneralFilterDescriptor.create(sourceImage, new GeneralFilterFunction.Max(subWindowSize), rh);
        } else if (getOperator() == MEDIAN) {
            return GeneralFilterDescriptor.create(sourceImage, new GeneralFilterFunction.Median(subWindowSize), rh);
        } else if (getOperator() == MEAN) {
            return GeneralFilterDescriptor.create(sourceImage, new GeneralFilterFunction.Mean(subWindowSize), rh);
        } else if (getOperator() == STDDEV) {
            return GeneralFilterDescriptor.create(sourceImage, new GeneralFilterFunction.StdDev(subWindowSize), rh);
        }
        throw new IllegalStateException(
                String.format("Operator class %s not supported.", getOperator().getClass()));

    }


    /**
     * An operator which performs an operation on an array of pixel values extracted from a raster sub-window.
     * <p/>
     * Note: since BEAM 5, this serves as just a marker interface.
     */
    public static interface Operator {
    }

    public static class Min implements Operator {
    }

    public static class Max implements Operator {
    }

    public static class Mean implements Operator {
    }

    public static class Median implements Operator {
    }

    public static class StdDev implements Operator {
    }

}