package org.esa.beam.dataio.chris.internal;

import com.bc.ceres.core.Assert;

import java.awt.Rectangle;
import static java.lang.Math.sqrt;

/**
 * The class {@code DropoutCorrection} encapsulates the dropout correction
 * algorithm developed by Luis Gomez Chova.
 *
 * @author Ralf Quast
 * @version $Revision: $ $Date: $
 */
public class DropoutCorrection {

    /**
     * The dropout correction types.
     */
    public enum Type {

        /**
         * This type includes the two neighboring pixels in along-track direction
         * only.
         */
        TWO,
        /**
         * This type includes the two neighboring pixels in both along and across
         * track directions, giving a total of four pixels.
         */
        FOUR,
        /**
         * This type includes all eight surrounding pixels.
         */
        EIGHT
    }

    public static final double[] M2 = {0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0};
    public static final double[] M4 = {0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0};
    public static final double[] M8 = {0.7, 1.0, 0.7, 1.0, 0.0, 1.0, 0.7, 1.0, 0.7};

    private static final int M_WIDTH = 3;
    private static final int M_HEIGHT = 3;

    private static final short VALID = 0;
    private static final short DROPOUT = 1;
    private static final short SATURATED = 2;
    private static final short CORRECTED_DROPOUT = 4;

    private double[] weights;
    private boolean cosmetic;

    /**
     * Constructs the default instance of this class.
     */
    public DropoutCorrection() {
        this(Type.FOUR);
    }

    /**
     * Constructs an instance of this class which performs a non-cosmetic
     * dropout correction with the given type.
     *
     * @param type
     */
    public DropoutCorrection(Type type) {
        this(type, false);
    }

    /**
     * Constructs an instance of this class.
     *
     * @param type     the correction type.
     * @param cosmetic indicates if the dropout correction should be cosmetic only.
     *                 If {@code true} the mask data are not modified.
     */
    public DropoutCorrection(Type type, boolean cosmetic) {
        switch (type) {
        case TWO:
            this.weights = M2;
            break;
        case FOUR:
            this.weights = M4;
            break;
        case EIGHT:
            this.weights = M8;
            break;
        }

        this.cosmetic = cosmetic;
    }

    /**
     * Performs the dropout correction for a given region of interest.
     *
     * @param rciData      the RCI raster data. The first array must hold the data which
     *                     are to be corrected.
     * @param maskData     the mask raster data. The first array must hold the data which
     *                     are to be corrected.
     * @param rasterWidth  the raster width.
     * @param rasterHeight the raster height.
     * @param roi          the region of interest inside the source raster.
     *
     * @throws IllegalArgumentException if RCI and mask data arrays do not have the same length.
     */
    public final void correct(int[][] rciData, short[][] maskData, int rasterWidth, int rasterHeight, Rectangle roi) {
        correct(rciData, maskData, rasterWidth, rasterHeight, roi, rciData[0], maskData[0], roi.x, roi.y, roi.width);
    }

    /**
     * Performs the dropout correction for a given region of interest.
     *
     * @param sourceRciData  the source RCI raster data. The first array must hold the data
     *                       which are to be corrected.
     * @param sourceMaskData the source mask raster data. The first array must hold the data
     *                       which are to be corrected.
     * @param sourceWidth    the width of the source raster.
     * @param sourceHeight   the height of the source raster.
     * @param sourceRoi      the region of interest inside the source raster.
     * @param targetRciData  the target RCI raster data. May be the same as the first array
     *                       of {@code sourceRciData}.
     * @param targetMaskData the target mask raster data. May be the same as the first array
     *                       of {@code sourceMaskData}.
     * @param targetOffsetX  the across-track offset inside the target raster.
     * @param targetOffsetY  the along-track offset inside the target raster.
     * @param targetWidth    the width of the target raster.
     *
     * @throws IllegalArgumentException if RCI and mask data arrays do not have the same length.
     */
    private void correct(int[][] sourceRciData,
                         short[][] sourceMaskData,
                         int sourceWidth,
                         int sourceHeight,
                         Rectangle sourceRoi,
                         int[] targetRciData,
                         short[] targetMaskData,
                         int targetOffsetX,
                         int targetOffsetY,
                         int targetWidth) {
        Assert.argument(sourceRciData.length == sourceMaskData.length);

        final double[] w = new double[weights.length];

        for (int sy = sourceRoi.y, ty = targetOffsetY; sy < sourceRoi.y + sourceRoi.height; ++sy, ++ ty) {
            for (int sx = sourceRoi.x, tx = targetOffsetX; sx < sourceRoi.x + sourceRoi.width; ++sx, ++ tx) {
                final int sxy = sy * sourceWidth + sx;
                final int txy = ty * targetWidth + tx;

                if (sourceMaskData[0][sxy] == DROPOUT) {
                    double ws = 0.0;
                    double xc = 0.0;

                    double ws2 = 0.0;
                    double xc2 = 0.0;

                    for (int i = 0; i < M_HEIGHT; ++i) {
                        final int ny = sy + (i - 1);
                        if (ny < 0 || ny >= sourceHeight) {
                            continue;
                        }
                        for (int j = 0; j < M_WIDTH; ++j) {
                            final int nx = sx + (j - 1);
                            if (nx < 0 || nx >= sourceWidth) {
                                continue;
                            }
                            final int ij = i * M_WIDTH + j;
                            final int nxy = ny * sourceWidth + nx;

                            if (weights[ij] != 0.0) {
                                switch (sourceMaskData[0][nxy]) {
                                case VALID:
                                    w[ij] = weights[ij] * calculateWeight(sxy, nxy, sourceRciData, sourceMaskData);
                                    ws += w[ij];
                                    xc += sourceRciData[0][nxy] * w[ij];
                                    break;

                                case SATURATED:
                                    ws2 += weights[ij];
                                    xc2 += sourceRciData[0][nxy] * weights[ij];
                                    break;
                                }
                            }
                        }
                    }
                    if (ws > 0.0) {
                        targetRciData[txy] = (int) (xc / ws);
                        if (!cosmetic) {
                            targetMaskData[txy] = CORRECTED_DROPOUT;
                        }
                    } else { // all neighbors are saturated
                        if (ws2 > 0.0) {
                            targetRciData[txy] = (int) (xc2 / ws2);
                            if (!cosmetic) {
                                targetMaskData[txy] = SATURATED;
                            }
                        } else { // all neighbors are dropouts
                            targetRciData[txy] = 0;
                        }
                    }
                } else { // not a dropout
                    targetRciData[txy] = sourceRciData[0][sxy];
                    targetMaskData[txy] = sourceMaskData[0][sxy];
                }
            }
        }
    }

    private double calculateWeight(int index, int neighborIndex, int[][] rciData, short[][] maskData) {
        double sum = 0.0;
        int count = 0;

        for (int i = 1; i < rciData.length; ++i) {
            if (maskData[i][index] == VALID && maskData[i][neighborIndex] == VALID && rciData[i][index] != 0) {
                final double d = (rciData[i][index] - rciData[i][neighborIndex]);

                sum += d * d;
                ++count;
            }
        }

        return count > 0 ? 1.0 / (1.0E-52 + sqrt(sum / count)) : 1.0;
    }

}
