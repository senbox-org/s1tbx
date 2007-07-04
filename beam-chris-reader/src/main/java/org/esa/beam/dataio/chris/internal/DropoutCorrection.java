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
    private final int sceneWidth;
    private final int sceneHeight;
    private final boolean cosmetic;


    public DropoutCorrection(int type, int sourceWidth, int sourceHeight) {
        this(type, sourceWidth, sourceHeight, false);
    }

    /**
     * Constructor.
     * todo - complete
     *
     * @param type
     * @param sourceWidth
     * @param sourceHeight
     * @param cosmetic
     */
    public DropoutCorrection(int type, int sourceWidth, int sourceHeight, boolean cosmetic) {
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

        this.sceneWidth = sourceWidth;
        this.sceneHeight = sourceHeight;
        this.cosmetic = cosmetic;
    }

    /**
     * Performs the dropout correction.
     * todo - complete
     *
     * @param data
     * @param mask
     */
    public void perform(int[][] data, short[][] mask, int bandIndex, int neighboringBandCount, Rectangle rectangle) {
        Assert.argument(data.length == mask.length);
        Assert.argument(data[bandIndex].length == mask[bandIndex].length);

        int imax = min(bandIndex + neighboringBandCount, data.length - 1);
        int imin = max(bandIndex - neighboringBandCount, 0);

        int[][] nd = new int[imax - imin][];
        short[][] nm = new short[imax - imin][];

        for (int i = imin, j = 0; i <= imax; ++i) {
            if (i != bandIndex) {
                nd[j] = data[i];
                nm[j] = mask[i];
                ++j;
            }
        }

        perform(data[bandIndex], mask[bandIndex], nd, nm, data[bandIndex], mask[bandIndex], rectangle);
    }

    /**
     * Performs the dropout correction.
     * todo - complete
     *
     * @param sourceData
     * @param sourceMask
     */
    public void perform(int[] sourceData, short[] sourceMask, int[][] neighborData, short[][] neighborMask,
                        int[] targetData, short[] targetMask,
                        Rectangle targetRectangle) {
        Assert.argument(sourceData.length == sourceMask.length);

        final double[] w = new double[weights.length];

        for (int y = targetRectangle.y; y < targetRectangle.y + targetRectangle.height; ++y) {
            for (int x = targetRectangle.x; x < targetRectangle.x + targetRectangle.width; ++x) {
                // offset of current pixel
                final int xy = y * sceneWidth + x;

                targetData[xy] = sourceData[xy];
                targetMask[xy] = sourceMask[xy];

                if (sourceMask[xy] == DROPOUT) {
                    double ws = 0.0;
                    double xc = 0.0;

                    double ws2 = 0.0;
                    double xc2 = 0.0;

                    int imin = (y - 1 < 0) ? 1 : 0;
                    int imax = (y + 1 < sceneHeight) ? M_HEIGHT : M_HEIGHT - 1;
                    int jmin = (x - 1 < 0) ? 1 : 0;
                    int jmax = (x + 1 < sceneWidth) ? M_WIDTH : M_WIDTH - 1;

                    for (int i = imin; i < imax; ++i) {
                        for (int j = jmin; j < jmax; ++j) {
                            final int ij = i * M_WIDTH + j;
                            final int ni = xy + (j - 1) + (i - 1) * sceneWidth;

                            if (weights[ij] != 0.0) {
                                switch (sourceMask[ni]) {
                                case VALID:
                                    w[ij] = weights[ij] * calculateWeight(xy, ni, neighborData, neighborMask);
                                    ws += w[ij];
                                    xc += sourceData[ni] * w[ij];
                                    break;

                                case SATURATED:
                                    ws2 += weights[ij];
                                    xc2 += sourceData[ni] * weights[ij];
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
