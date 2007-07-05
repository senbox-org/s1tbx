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

    public enum Type {VERTICAL, FOUR_CONNECTED, EIGHT_CONNECTED}

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
     * Constructs an instance of this class, which performs a non-cosmetic
     * dropout correction.
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
        case VERTICAL:
            this.weights = M2;
            break;
        case FOUR_CONNECTED:
            this.weights = M4;
            break;
        case EIGHT_CONNECTED:
            this.weights = M8;
            break;
        }

        this.cosmetic = cosmetic;
    }

    /**
     * Performs the dropout correction.
     *
     * @param rciData      the RCI raster data.
     * @param maskData     the mask raster data.
     * @param rasterWidth  the raster width.
     * @param rasterHeight the raster height.
     * @param roi          the rater region of interest.
     *
     * @throws IllegalArgumentException if RCI and mask data arrays do not have the same length.
     */
    public void perform(int[][] rciData, short[][] maskData, int rasterWidth, int rasterHeight, Rectangle roi) {
        Assert.argument(rciData.length == maskData.length);

        final double[] w = new double[weights.length];

        for (int y = roi.y; y < roi.y + roi.height; ++y) {
            for (int x = roi.x; x < roi.x + roi.width; ++x) {
                // offset of current pixel
                final int xy = y * rasterWidth + x;

                if (maskData[0][xy] == DROPOUT) {
                    double ws = 0.0;
                    double xc = 0.0;

                    double ws2 = 0.0;
                    double xc2 = 0.0;

                    for (int i = 0; i < M_HEIGHT; ++i) {
                        final int ny = y + (i - 1);
                        if (ny < 0 || ny >= rasterHeight) {
                            continue;
                        }
                        for (int j = 0; j < M_WIDTH; ++j) {
                            final int nx = x + (j - 1);
                            if (nx < 0 || nx >= rasterWidth) {
                                continue;
                            }
                            final int ij = i * M_WIDTH + j;
                            final int nxy = ny * rasterWidth + nx;

                            if (weights[ij] != 0.0) {
                                switch (maskData[0][nxy]) {
                                case VALID:
                                    w[ij] = weights[ij] * calculateWeight(xy, nxy, rciData, maskData);
                                    ws += w[ij];
                                    xc += rciData[0][nxy] * w[ij];
                                    break;

                                case SATURATED:
                                    ws2 += weights[ij];
                                    xc2 += rciData[0][nxy] * weights[ij];
                                    break;
                                }
                            }
                        }
                    }

                    if (ws > 0.0) {
                        rciData[0][xy] = (int) (xc / ws);
                        if (!cosmetic) {
                            maskData[0][xy] = CORRECTED_DROPOUT;
                        }
                    } else if (ws2 > 0.0) {
                        rciData[0][xy] = (int) (xc2 / ws2);
                        if (!cosmetic) {
                            maskData[0][xy] = SATURATED;
                        }
                    } else {
                        rciData[0][xy] = 0;
                    }
                }
            }
        }
    }

    private double calculateWeight(int index, int neighborIndex, int[][] radianceData, short[][] maskData) {
        double sum = 0.0;
        int count = 0;

        for (int i = 1; i < radianceData.length; ++i) {
            if (maskData[i][index] == VALID && maskData[i][neighborIndex] == VALID && radianceData[i][index] != 0) {
                final double d = (radianceData[i][index] - radianceData[i][neighborIndex]);

                sum += d * d;
                ++count;
            }
        }

        return count > 0 ? 1.0 / (1.0E-52 + sqrt(sum / count)) : 1.0;
    }

}
