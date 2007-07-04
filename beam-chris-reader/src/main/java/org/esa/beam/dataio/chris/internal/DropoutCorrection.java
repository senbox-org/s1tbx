package org.esa.beam.dataio.chris.internal;

import com.bc.ceres.core.Assert;

import java.awt.Rectangle;
import static java.lang.Math.*;

/**
 * The class {@code DropoutCorrection} encapsulates the dropout correction
 * algorithm developed by Luis Gomez Chova.
 *
 * @author Ralf Quast
 * @version $Revision: $ $Date: $
 */
public class DropoutCorrection {

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
    private final boolean cosmetic;


    public DropoutCorrection(int type) {
        this(type, false);
    }

    /**
     * Constructor.
     * todo - complete
     *
     * @param type
     * @param cosmetic
     */
    public DropoutCorrection(int type, boolean cosmetic) {
        Assert.argument(type == 2 || type == 4 || type == 8);

        switch (type) {
        case 2 :
            this.weights = M2;
            break;
        case 4 :
            this.weights = M4;
            break;
        case 8 :
            this.weights = M8;
            break;
        }

        this.cosmetic = cosmetic;
    }

    /**
     * Performs the dropout correction.
     * todo - complete
     *
     * @param sourceData
     * @param sourceMask
     */
    public void perform(int[] sourceData, short[] sourceMask, int[][] neighborData, short[][] neighborMask,
                        Rectangle sourceRectangle, int[] targetData, short[] targetMask,
                        Rectangle targetRectangle) {
        Assert.argument(sourceData.length == sourceMask.length);

        final double[] w = new double[weights.length];

        for (int y = targetRectangle.y; y < targetRectangle.y + targetRectangle.height; ++y) {
            for (int x = targetRectangle.x; x < targetRectangle.x + targetRectangle.width; ++x) {
                // offset of current pixel
                final int xy = y * sourceRectangle.width + x;

                targetData[xy] = sourceData[xy];
                targetMask[xy] = sourceMask[xy];

                if (sourceMask[xy] == DROPOUT) {
                    double ws = 0.0;
                    double xc = 0.0;

                    double ws2 = 0.0;
                    double xc2 = 0.0;

                    for (int i = 0; i < M_HEIGHT; ++i) {
                        final int ny = y + (i - 1);
                        if (ny < sourceRectangle.y || ny >= sourceRectangle.y + sourceRectangle.height) {
                            continue;
                        }
                        for (int j = 0; j < M_WIDTH; ++j) {
                            final int nx = x + (j - 1);
                            if (nx < sourceRectangle.x || nx >= sourceRectangle.x + sourceRectangle.width) {
                                continue;
                            }
                            final int ij = i * M_WIDTH + j;
                            final int nxy = ny * sourceRectangle.width + nx;

                            if (weights[ij] != 0.0) {
                                switch (sourceMask[nxy]) {
                                case VALID:
                                    w[ij] = weights[ij] * calculateWeight(xy, nxy, neighborData, neighborMask);
                                    ws += w[ij];
                                    xc += sourceData[nxy] * w[ij];
                                    break;

                                case SATURATED:
                                    ws2 += weights[ij];
                                    xc2 += sourceData[nxy] * weights[ij];
                                    break;
                                }
                            }
                        }
                    }

                    if (ws > 0.0) {
                        targetData[xy] = (int) (xc / ws);
                        if (!cosmetic) {
                            targetMask[xy] = CORRECTED_DROPOUT;
                        }
                    } else if (ws2 > 0.0) {
                        targetData[xy] = (int) (xc2 / ws2);
                        if (!cosmetic) {
                            targetMask[xy] = SATURATED;
                        }
                    } else {
                        targetData[xy] = 0;
                    }
                }
            }
        }
    }

    private double calculateWeight(int index, int neighborIndex, int[][] data, short[][] mask) {
        double sum = 0.0;
        int count = 0;

        for (int i = 0; i < data.length; ++i) {
            if (mask[i][index] == VALID && mask[i][neighborIndex] == VALID && data[i][index] != 0) {
                final double d = (data[i][index] - data[i][neighborIndex]);

                sum += d * d;
                ++count;
            }
        }
        if (count > 0) {
            return 1.0 / (1.0E-52 + sqrt(sum / count)); // avoid an infinite return value for sum -> 0
        } else {
            return 1.0;
        }
    }

}
