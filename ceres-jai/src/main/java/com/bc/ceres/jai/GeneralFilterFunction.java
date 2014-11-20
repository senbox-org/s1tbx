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

package com.bc.ceres.jai;

import com.bc.ceres.core.Assert;

import java.util.Arrays;

/**
 * A general filter function.
 * This class is used as parameter for the
 * {@link com.bc.ceres.jai.operator.GeneralFilterDescriptor GeneralFilter} operation.
 */
public abstract class GeneralFilterFunction {

    protected final int width;
    protected final int height;
    protected final int xOrigin;
    protected final int yOrigin;
    protected final boolean[] structuringElement;

    /**
     * Constructs a GeneralFilterFunction.
     *
     * @param width              the width of the kernel.
     * @param height             the height of the kernel.
     * @param xOrigin            the X coordinate of the key kernel element.
     * @param yOrigin            the Y coordinate of the key kernel element.
     * @param structuringElement The structuring element with a length equal to {@code width * height}. May be {@code null}.
     */
    protected GeneralFilterFunction(int width, int height, int xOrigin, int yOrigin, boolean[] structuringElement) {
        Assert.argument(width > 0, "width must be positive");
        Assert.argument(height > 0, "height must be positive");
        Assert.argument(xOrigin >= 0 && xOrigin < width, "xOrigin out of bounds");
        Assert.argument(yOrigin >= 0 && yOrigin < height, "yOrigin out of bounds");
        this.width = width;
        this.height = height;
        this.xOrigin = xOrigin;
        this.yOrigin = yOrigin;
        if (structuringElement != null) {
            Assert.argument(structuringElement.length == width * height, "structuringElement has illegal size");
            this.structuringElement = structuringElement.clone();
        } else {
            this.structuringElement = null;
        }
    }

    /**
     * @return the width of the kernel.
     */
    public final int getWidth() {
        return width;
    }

    /**
     * @return the height of the kernel.
     */
    public final int getHeight() {
        return height;
    }

    /**
     * @return the X coordinate of the key kernel element.
     */
    public final int getXOrigin() {
        return xOrigin;
    }

    /**
     * @return the Y coordinate of the key kernel element.
     */
    public final int getYOrigin() {
        return yOrigin;
    }

    /**
     * @return the number of pixels required to the left of the key element.
     */
    public final int getLeftPadding() {
        return xOrigin;
    }

    /**
     * @return the number of pixels required to the right of the key element.
     */
    public final int getRightPadding() {
        return width - xOrigin - 1;
    }

    /**
     * @return the number of pixels required above the key element.
     */
    public final int getTopPadding() {
        return yOrigin;
    }

    /**
     * @return the number of pixels required below the key element.
     */
    public final int getBottomPadding() {
        return height - yOrigin - 1;
    }

    public boolean[] getStructuringElement() {
        return structuringElement;
    }

    public abstract float filter(float[] fdata);

    public static final class Min extends GeneralFilterFunction {
        public Min(int width, int height, int xOrigin, int yOrigin, boolean[] structuringElement) {
            super(width, height, xOrigin, yOrigin, structuringElement);
        }

        public float filter(float[] fdata) {
            final boolean[] se = structuringElement;
            float min = Float.POSITIVE_INFINITY;
            int n = 0;
            for (int i = 0; i < fdata.length; i++) {
                float v = fdata[i];
                if ((se == null || se[i]) && v < min) {
                    min = v;
                    n++;
                }
            }
            return n > 0 ? min : Float.NaN;
        }
    }

    public static final class Max extends GeneralFilterFunction {
        public Max(int width, int height, int xOrigin, int yOrigin, boolean[] structuringElement) {
            super(width, height, xOrigin, yOrigin, structuringElement);
        }

        public float filter(float[] fdata) {
            final boolean[] se = structuringElement;
            float max = Float.NEGATIVE_INFINITY;
            int n = 0;
            for (int i = 0; i < fdata.length; i++) {
                float v = fdata[i];
                if ((se == null || se[i]) && v > max) {
                    max = v;
                    n++;
                }
            }
            return n > 0 ? max : Float.NaN;
        }
    }

    public static final class Median extends GeneralFilterFunction {

        public Median(int width, int height, int xOrigin, int yOrigin, boolean[] structuringElement) {
            super(width, height, xOrigin, yOrigin, structuringElement);
        }

        public float filter(float[] fdata) {
            final boolean[] se = structuringElement;
            // Note: NaN's are moved to the end of the array
            Arrays.sort(fdata);
            int n = 0;
            for (int i = 0; i < fdata.length; i++) {
                float v = fdata[i];
                if ((se == null || se[i]) && !Float.isNaN(v)) {
                    n++;
                }
            }
            if (n == 0) {
                return Float.NaN;
            } else if (n == 1) {
                return fdata[0];
            } else if (n % 2 == 1) {
                return fdata[n / 2];
            } else {
                return 0.5F * (fdata[n / 2 - 1] + fdata[n / 2]);
            }
        }
    }

    public static final class Mean extends GeneralFilterFunction {

        public Mean(int width, int height, int xOrigin, int yOrigin, boolean[] structuringElement) {
            super(width, height, xOrigin, yOrigin, structuringElement);
        }

        public float filter(float[] fdata) {
            final boolean[] se = structuringElement;
            float sum = 0F;
            int n = 0;
            for (int i = 0; i < fdata.length; i++) {
                float v = fdata[i];
                if ((se == null || se[i]) && !Float.isNaN(v)) {
                    sum += v;
                    n++;
                }
            }
            return n > 0 ? sum / n : Float.NaN;
        }
    }

    public static final class StdDev extends GeneralFilterFunction {

        public StdDev(int width, int height, int xOrigin, int yOrigin, boolean[] structuringElement) {
            super(width, height, xOrigin, yOrigin, structuringElement);
        }

        @Override
        public float filter(float[] fdata) {
            final boolean[] se = structuringElement;
            float sum = 0F;
            int n = 0;
            for (int i = 0; i < fdata.length; i++) {
                float v = fdata[i];
                if ((se == null || se[i]) && !Float.isNaN(v)) {
                    sum += v;
                    n++;
                }
            }
            if (n > 0) {
                float mean = sum / n;
                float sqrSum = 0;
                for (int i = 0; i < fdata.length; i++) {
                    float v = fdata[i];
                    if ((se == null || se[i]) && !Float.isNaN(v)) {
                        float delta = v - mean;
                        sqrSum += delta * delta;
                    }
                }
                return (float) Math.sqrt(sqrSum / n);
            } else {
                return Float.NaN;
            }
        }
    }

    public static final class Erosion extends GeneralFilterFunction {

        public Erosion(int width, int height, int xOrigin, int yOrigin, boolean[] structuringElement) {
            super(width, height, xOrigin, yOrigin, structuringElement);
        }

        public float filter(float[] fdata) {
            final boolean[] se = structuringElement;
            float min = Float.POSITIVE_INFINITY;
            int n = 0;
            for (int i = 0; i < fdata.length; i++) {
                float v = fdata[i];
                if ((se == null || se[i]) && v < min) {
                    min = v;
                    n++;
                }
            }
            return n > 0 ? min : Float.NaN;
        }
    }

    public static final class Dilation extends GeneralFilterFunction {
        public Dilation(int width, int height, int xOrigin, int yOrigin, boolean[] structuringElement) {
            super(width, height, xOrigin, yOrigin, structuringElement);
        }

        public float filter(float[] fdata) {
            final boolean[] se = structuringElement;
            float max = Float.NEGATIVE_INFINITY;
            int n = 0;
            for (int i = 0; i < fdata.length; i++) {
                float v = fdata[i];
                if ((se == null || se[i]) && v > max) {
                    max = v;
                    n++;
                }
            }
            return n > 0 ? max : Float.NaN;
        }
    }
}
