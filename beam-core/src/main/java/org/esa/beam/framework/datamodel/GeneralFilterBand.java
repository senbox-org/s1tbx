/*
 * $Id: GeneralFilterBand.java,v 1.3 2007/03/19 15:52:28 marcop Exp $
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
package org.esa.beam.framework.datamodel;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;

import java.io.IOException;
import java.util.Arrays;

/**
 * A band that obtains its input data from an underlying source raster and filters
 * its data using an arbitrary {@link GeneralFilterBand.Operator algorithm}.
 * <p/>
 * <p><i>Note that this class is not yet public API and may change in future releases.</i></p>
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 */
public class GeneralFilterBand extends FilterBand {

    public static final Operator MIN = new Min();
    public static final Operator MAX = new Max();
    public static final Operator MEAN = new Mean();
    public static final Operator MEDIAN = new Median();
    public static final Operator STDDEV = new StandardDeviation();
    public static final Operator RMS = new RootMeanSquare();

    private int _subWindowWidth;
    private int _subWindowHeight;
    private Operator _operator;

    public GeneralFilterBand(String name, RasterDataNode source, int subWindowWidth, int subWindowHeight,
                             Operator operator) {
        super(name,
              ProductData.TYPE_FLOAT32,
              source.getSceneRasterWidth(),
              source.getSceneRasterHeight(),
              source);
        setOwner(source.getProduct());
        _subWindowWidth = subWindowWidth;
        _subWindowHeight = subWindowHeight;
        _operator = operator;
        setGeophysicalNoDataValue(-9999);
        setNoDataValueUsed(true);
    }

    // TODO - (nf) Convoluted bands are currently always read entirely. However, this method wont work anymore for
    // spatial subsets with width != sceneRasterWidth or height != sceneRasterHeight

    /**
     * Reads raster data from this dataset into the user-supplied raster data buffer.
     * <p/>
     * <p>This method always directly (re-)reads this band's data from its associated data source into the given data
     * buffer.
     *
     * @param offsetX    the X-offset in the raster co-ordinates where reading starts
     * @param offsetY    the Y-offset in the raster co-ordinates where reading starts
     * @param width      the width of the raster data buffer
     * @param height     the height of the raster data buffer
     * @param rasterData a raster data buffer receiving the pixels to be read
     * @param pm         a monitor to inform the user about progress
     *
     * @throws java.io.IOException      if an I/O error occurs
     * @throws IllegalArgumentException if the raster is null
     * @throws IllegalStateException    if this product raster was not added to a product so far, or if the product to
     *                                  which this product raster belongs to, has no associated product reader
     * @see org.esa.beam.framework.dataio.ProductReader#readBandRasterData(Band, int, int, int, int, ProductData, com.bc.ceres.core.ProgressMonitor) 
     */
    @Override
    public void readRasterData(int offsetX, int offsetY, int width, int height, ProductData rasterData,
                               ProgressMonitor pm) throws IOException {
        final RasterDataNode source = getSource();
        final ProductData sourceData = ProductData.createInstance(ProductData.TYPE_FLOAT64, width * height);
        pm.beginTask("Reading band data", 2);
        try {
            source.readPixels(offsetX, offsetY, width, height, (double[]) sourceData.getElems(),
                              SubProgressMonitor.create(pm, 1));

            final int kw = _subWindowWidth;
            final int kh = _subWindowHeight;
            final int kx0 = (kw - 1) >> 1;
            final int ky0 = (kh - 1) >> 1;
            int numSourceValues;
            final double[] sourceValues = new double[kw * kh];
            double targetValue;

            ProgressMonitor subPm = SubProgressMonitor.create(pm, 1);
            subPm.beginTask("Applying filter...", height);
            try {
                for (int y = 0; y < height; y++) {
                    if (subPm.isCanceled()) {
                        return;
                    }
                    for (int x = 0; x < width; x++) {
                        numSourceValues = 0;
                        for (int ky = 0; ky < kh; ky++) {
                            final int sourceY = y + (ky - ky0);
                            if (sourceY >= 0 && sourceY < height) {
                                for (int kx = 0; kx < kw; kx++) {
                                    final int sourceX = x + (kx - kx0);
                                    if (sourceX >= 0 && sourceX < width) {
                                        if (source.isPixelValid(sourceX, sourceY)) {
                                            sourceValues[numSourceValues++] = source.scale(
                                                    sourceData.getElemDoubleAt(sourceY * width + sourceX));
                                        }
                                    }
                                }
                            }
                        }
                        if (numSourceValues > 0) {
                            targetValue = _operator.evaluate(sourceValues, numSourceValues);
                        } else {
                            targetValue = getGeophysicalNoDataValue();
                        }
                        rasterData.setElemDoubleAt(y * width + x, targetValue);
                    }
                    subPm.worked(1);
                }
            } finally {
                subPm.done();
            }
        } finally {
            pm.done();
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void writeRasterData(int offsetX, int offsetY,
                                int width, int height,
                                ProductData rasterData, ProgressMonitor pm) throws IOException {
        throw new IllegalStateException("write not supported for filtered band");
    }

    /**
     * Creates an instance {@link Operator} by the given class name
     *
     * @param operatorClassName the class name
     *
     * @return instance of {@link Operator}
     */
    public static Operator createOperator(String operatorClassName) {
        Operator operator = null;
        try {
            final Class operatorClass = Class.forName(operatorClassName);
            operator = (Operator) operatorClass.newInstance();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InstantiationException e) {
            e.printStackTrace();
        }
        return operator;
    }

    public int getSubWindowWidth() {
        return _subWindowWidth;
    }

    public int getSubWindowHeight() {
        return _subWindowHeight;
    }

    public Operator getOperator() {
        return _operator;
    }

    /**
     * An operator which performs an operation on an array of pixel values extracted from a raster sub-window.
     */
    public static interface Operator {

        /**
         * Performs the operation.
         *
         * @param values an array of pixel values extracted from a raster sub-window
         * @param n      the number of values in the array
         *
         * @return the result of the operation
         */
        double evaluate(double[] values, int n);
    }

    public static class Min implements Operator {

        public final double evaluate(final double[] values, final int n) {
            if (n == 0) {
                return Double.NaN;
            }
            double x = Double.MAX_VALUE;
            for (int i = 0; i < n; i++) {
                if (values[i] < x) {
                    x = values[i];
                }
            }
            return x;
        }
    }

    public static class Max implements Operator {

        public final double evaluate(final double[] values, final int n) {
            if (n == 0) {
                return Double.NaN;
            }
            double x = -Double.MAX_VALUE;
            for (int i = 0; i < n; i++) {
                if (values[i] > x) {
                    x = values[i];
                }
            }
            return x;
        }
    }

    public static class Mean implements Operator {

        public final double evaluate(final double[] values, final int n) {
            if (n == 0) {
                return Double.NaN;
            }
            double sum = 0;
            for (int i = 0; i < n; i++) {
                sum += values[i];
            }
            return sum / n;
        }
    }

    public static class Median implements Operator {

        public final double evaluate(final double[] values, final int n) {
            if (n == 0) {
                return Double.NaN;
            } else if (n == 1) {
                return values[0];
            }
            Arrays.sort(values);
            final int i = n / 2;
            if (n % 2 != 0) {
                return values[i];
            }
            return 0.5 * (values[i] + values[i + 1]);
        }
    }

    public static class StandardDeviation implements Operator {

        public final double evaluate(final double[] values, final int n) {
            if (n == 0) {
                return Double.NaN;
            } else if (n == 1) {
                return 0;
            }
            double sum = 0;
            for (int i = 0; i < n; i++) {
                sum += values[i];
            }
            final double mean = sum / n;
            sum = 0;
            for (int i = 0; i < n; i++) {
                sum += (values[i] - mean) * (values[i] - mean);
            }
            return Math.sqrt(sum / (n - 1));
        }
    }

    public static class RootMeanSquare implements Operator {

        public final double evaluate(final double[] values, final int n) {
            if (n == 0) {
                return Double.NaN;
            }
            double sum = 0;
            for (int i = 0; i < n; i++) {
                sum += values[i] * values[i];
            }
            return Math.sqrt(sum / n);
        }
    }
}
