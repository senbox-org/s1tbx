package com.bc.ceres.jai;

/**
 * A general filter function.
 * This class is used as parameter for the
 * {@link com.bc.ceres.jai.operator.GeneralFilterDescriptor GeneralFilter} operation.
 */
public abstract class GeneralFilterFunction {
    public static final GeneralFilterFunction MIN_3X3 = new Min(3);
    public static final GeneralFilterFunction MAX_3X3 = new Max(3);
    public static final GeneralFilterFunction MEAN_3X3 = new Mean(3);

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

    private static class Min extends GeneralFilterFunction {
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
            float min = Float.MAX_VALUE;
            for (float v : fdata) {
                if (v < min) {
                    min = v;
                }
            }
            return min;
        }
    }

    private  static class Max extends GeneralFilterFunction {
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
            float max = -Float.MAX_VALUE;
            for (float v : fdata) {
                if (v > max) {
                    max = v;
                }
            }
            return max;
        }
    }

    private static class Mean extends GeneralFilterFunction {
        private final float n;

        public Mean(int size) {
            this(size, size);
        }

        public Mean(int width, int height) {
            this(width, height, width / 2, height / 2);
        }

        public Mean(int width, int height, int xOrigin, int yOrigin) {
            super(width, height, xOrigin, yOrigin);
            n = width * height;
        }

        public float filter(float[] fdata) {
            final float a = 1.0F / n;
            float sum = 0.0F;
            for (float v : fdata) {
                sum += a * v;
            }
            return sum;
        }
    }
}
