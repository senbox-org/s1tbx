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


package org.esa.beam.dataio.modis.bandreader;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.dataio.modis.ModisConstants;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.util.math.Range;
import ucar.nc2.Variable;

import java.io.IOException;

abstract public class ModisBandReader {

    public static final int SCALE_UNKNOWN = 0;
    public static final int SCALE_LINEAR = 1;
    public static final int SCALE_EXPONENTIAL = 2;
    public static final int SCALE_POW_10 = 3;
    public static final int SCALE_SLOPE_INTERCEPT = 4;

    protected int layer;
    protected float scale;
    protected float offset;
    private String name;
    protected int[] start;
    protected int[] stride;
    protected int[] count;
    protected int xCoord;
    protected int yCoord;
    protected Range validRange;
    protected double fillValue;
    protected Variable variable;

    /**
     * Creates a band reader with given scientific dataset identifier
     *
     * @param variable the variable
     * @param layer the layer
     * @param is3d  true if the dataset is a 3d dataset
     */
    public ModisBandReader(Variable variable, final int layer, final boolean is3d) {
        this.variable = variable;
        this.layer = layer;

        if (is3d) {
            count = new int[3];
            stride = new int[3];
            start = new int[3];
            stride[0] = stride[1] = stride[2] = 1;
            start[0] = layer;
            count[0] = 1;

            xCoord = 2;
            yCoord = 1;
        } else {
            count = new int[2];
            stride = new int[2];
            start = new int[2];
            start[0] = layer;
            count[0] = 1;

            stride[0] = stride[1] = 1;
            xCoord = 1;
            yCoord = 0;
        }
    }

    /**
     * Sets the name of the band
     *
     * @param name the name for this band reader
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * Retrieves the name of the band
     *
     * @return name
     */
    public String getName() {
        return name;
    }

    /**
     * Converts a string to a scaling method enum value.
     *
     * @param scaleMethod the scale method as string
     * @return the enum value either {@link #SCALE_LINEAR}, {@link #SCALE_EXPONENTIAL},
     *         {@link #SCALE_POW_10}, {@link #SCALE_SLOPE_INTERCEPT} or {@link #SCALE_UNKNOWN} if the given string
     *         can not be converted.
     */
    public static int decodeScalingMethod(final String scaleMethod) {
        if (scaleMethod != null) {
            if (scaleMethod.equalsIgnoreCase(ModisConstants.LINEAR_SCALE_NAME)) {
                return SCALE_LINEAR;
            } else if (scaleMethod.equalsIgnoreCase(ModisConstants.EXPONENTIAL_SCALE_NAME)) {
                return SCALE_EXPONENTIAL;
            } else if (scaleMethod.equalsIgnoreCase(ModisConstants.POW_10_SCALE_NAME)) {
                return SCALE_POW_10;
            } else if (scaleMethod.equalsIgnoreCase(ModisConstants.SLOPE_INTERCEPT_SCALE_NAME)) {
                return SCALE_SLOPE_INTERCEPT;
            }
        }
        return SCALE_UNKNOWN;
    }

    /**
     * Sets scale and offset for scaled bands (scaling that cannot be handled by the product io framework)
     *
     * @param scale  the scaling used by this band reader
     * @param offset the offset used by this band reader
     */
    public void setScaleAndOffset(final float scale, final float offset) {
        this.scale = scale;
        this.offset = offset;
    }

    /**
     * Sets the valid range for this specific band reader, value in unscaled counts
     *
     * @param validRange the raw data valid range
     */
    public void setValidRange(Range validRange) {
        this.validRange = validRange;
    }

    abstract protected void prepareForReading(final int sourceOffsetX, final int sourceOffsetY,
                                              final int sourceWidth, final int sourceHeight,
                                              final int sourceStepX, final int sourceStepY,
                                              final ProductData destBuffer);

    abstract protected void readLine() throws IOException;

    abstract protected void validate(final int x);

    abstract protected void assign(final int x);

    /**
     * Sets the fill value, i.e. the value set where the measurement data is out-of-scope
     * value in unscaled measurements counts
     *
     * @param fillValue the fill value if any raw data is not in the valid range
     */
    public void setFillValue(double fillValue) {
        this.fillValue = fillValue;
    }

    /**
     * Retrieves the data type of the band
     *
     * @return the data type
     */
    abstract public int getDataType();

    /**
     * <p>The destination band, buffer and region parameters are exactly the ones passed to the original  call. Since
     * the <code>destOffsetX</code> and <code>destOffsetY</code> parameters are already taken into acount in the
     * <code>sourceOffsetX</code> and <code>sourceOffsetY</code> parameters, an implementor of this method is free to
     * ignore them.
     *
     * @param sourceOffsetX the absolute X-offset in source raster co-ordinates
     * @param sourceOffsetY the absolute Y-offset in source raster co-ordinates
     * @param sourceWidth   the width of region providing samples to be read given in source raster co-ordinates
     * @param sourceHeight  the height of region providing samples to be read given in source raster co-ordinates
     * @param sourceStepX   the sub-sampling in X direction within the region providing samples to be read
     * @param sourceStepY   the sub-sampling in Y direction within the region providing samples to be read
     * @param destBuffer    the destination buffer which receives the sample values to be read
     * @param pm            a monitor to inform the user about progress
     * @throws IOException -
     */
    public void readBandData(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight,
                             int sourceStepX, int sourceStepY, ProductData destBuffer, ProgressMonitor pm) throws IOException {
        start[yCoord] = sourceOffsetY;
        start[xCoord] = sourceOffsetX;
        count[yCoord] = 1;
        count[xCoord] = sourceWidth;
        stride[yCoord] = sourceStepY;
        stride[xCoord] = sourceStepX;

        prepareForReading(sourceOffsetX, sourceOffsetY, sourceWidth, sourceHeight,
                          sourceStepX, sourceStepY, destBuffer);

        pm.beginTask("Reading band '" + getName() + "'...", sourceHeight);
        // loop over lines
        try {
            for (int y = 0; y < sourceHeight; y += sourceStepY) {
                if (pm.isCanceled()) {
                    break;
                }
                readLine();
                for (int x = 0; x < sourceWidth; x++) {
                    validate(x);
                    assign(x);
                }
                start[yCoord] += sourceStepY;
                pm.worked(1);
            }
        } finally {
            pm.done();
        }
    }
}
