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

import java.util.Arrays;

/**
 * A general filter function.
 * This class is used as parameter for the
 * {@link com.bc.ceres.jai.operator.GeneralFilterDescriptor GeneralFilter} operation.
 */
public abstract class GeneralFilterFunction {

    private final int width;
    private final int height;
    private final int xOrigin;
    private final int yOrigin;

    protected GeneralFilterFunction(int size) {
        this(size, size);
    }

    protected GeneralFilterFunction(int width, int height) {
        this(width, height, width / 2, height / 2);
    }

    /**
     * Constructs a GeneralFilterFunction.
     *
     * @param width   the width of the kernel.
     * @param height  the height of the kernel.
     * @param xOrigin the X coordinate of the key kernel element.
     * @param yOrigin the Y coordinate of the key kernel element.
     * @throws IllegalArgumentException if width or height is not a positive number.
     */
    protected GeneralFilterFunction(int width, int height, int xOrigin, int yOrigin) {
        this.width = width;
        this.height = height;
        this.xOrigin = xOrigin;
        this.yOrigin = yOrigin;
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

    public abstract float filter(float[] fdata);

    public static class Min extends GeneralFilterFunction {
        public Min(int size) {
            super(size);
        }

        public Min(int width, int height) {
            super(width, height);
        }

        public Min(int width, int height, int xOrigin, int yOrigin) {
            super(width, height, xOrigin, yOrigin);
        }

        public float filter(float[] fdata) {
            float min = Float.POSITIVE_INFINITY;
            int n = 0;
            for (float v : fdata) {
                if (v < min) {
                    min = v;
                    n++;
                }
            }
            return n > 0 ? min : Float.NaN;
        }
    }

    public static class Max extends GeneralFilterFunction {
        public Max(int size) {
            super(size);
        }

        public Max(int width, int height) {
            super(width, height);
        }

        public Max(int width, int height, int xOrigin, int yOrigin) {
            super(width, height, xOrigin, yOrigin);
        }

        public float filter(float[] fdata) {
            float max = Float.NEGATIVE_INFINITY;
            int n = 0;
            for (float v : fdata) {
                if (v > max) {
                    max = v;
                    n++;
                }
            }
            return n > 0 ? max : Float.NaN;
        }
    }

    public static class Median extends GeneralFilterFunction {

        public Median(int size) {
            this(size, size);
        }

        public Median(int width, int height) {
            this(width, height, width / 2, height / 2);
        }

        public Median(int width, int height, int xOrigin, int yOrigin) {
            super(width, height, xOrigin, yOrigin);
        }

        public float filter(float[] fdata) {
            // Note: NaN's are moved to the end of the array
            Arrays.sort(fdata);
            int n = 0;
            for (float v : fdata) {
                if (!Float.isNaN(v))  {
                    n++;
                    break;
                }
            }
            if (n == 0) {
                return Float.NaN;
            } else if (n == 1) {
                return fdata[0];
            } else if (n % 2 == 1) {
                return fdata[n/2];
            } else {
                return 0.5F * (fdata[n/2] + fdata[n/2 + 1]);
            }
        }
    }

    public static class Mean extends GeneralFilterFunction {

        public Mean(int size) {
            this(size, size);
        }

        public Mean(int width, int height) {
            this(width, height, width / 2, height / 2);
        }

        public Mean(int width, int height, int xOrigin, int yOrigin) {
            super(width, height, xOrigin, yOrigin);
        }

        public float filter(float[] fdata) {
            float sum = 0F;
            int n = 0;
            for (float v : fdata) {
                if (!Float.isNaN(v))  {
                    sum += v;
                    n++;
                }
            }
            return n > 0 ? sum/n : Float.NaN;
        }
    }

    public static class StdDev extends GeneralFilterFunction {

        public StdDev(int size) {
            this(size, size);
        }

        public StdDev(int width, int height) {
            this(width, height, width / 2, height / 2);
        }

        public StdDev(int width, int height, int xOrigin, int yOrigin) {
            super(width, height, xOrigin, yOrigin);
        }

        @Override
        public float filter(float[] fdata) {
            float sum = 0F;
            int n = 0;
            for (float v : fdata) {
                if (!Float.isNaN(v)) {
                    sum += v;
                    n++;
                }
            }
            if (n > 0) {
                float mean = sum / n;
                float sqrSum = 0;
                for (float v : fdata) {
                    if (!Float.isNaN(v)) {
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
}
