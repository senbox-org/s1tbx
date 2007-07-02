package org.esa.beam.dataio.chris.internal;

import com.bc.ceres.core.Assert;

import java.awt.Rectangle;

/**
 * The class {@code DropoutCorrection} encapsulates the dropout correction
 * algorithm developed by Luis Gomez Chova.
 *
 * @author Ralf Quast
 * @version $Revision: 1.5 $ $Date: 2007/04/18 16:01:35 $
 */
public class DropoutCorrection {

    public static final double[] N2 = {0.0, 1.0, 0.0, 0.0, 0.0, 0.0, 0.0, 1.0, 0.0};
    public static final double[] N4 = {0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0, 1.0, 0.0};
    public static final double[] N8 = {0.7, 1.0, 0.7, 1.0, 0.0, 1.0, 0.7, 1.0, 0.7};

    private static final int N_WIDTH = 3;
    private static final int N_HEIGHT = 3;
    private static final int N_SIZE = N_WIDTH * N_HEIGHT;

    private static final short VALID = 0;
    private static final short DROPOUT = 1;
    private static final short SATURATED = 2;
    private static final short CORRECTED_DROPOUT = 5;
    private static final short CORRECTED_DROPOUT_SATURATED = 7;

    private double[] neighbourhoodMatrix;
    private int neighbourhoodBandCount;
    private final int sourceWidth;
    private final int sourceHeight;


    /**
     * Constructor.
     * todo - complete
     *
     * @param neighbourhoodMatrix
     * @param neighbourhoodBandCount
     * @param sourceWidth
     * @param sourceHeight
     */
    public DropoutCorrection(double[] neighbourhoodMatrix, int neighbourhoodBandCount,
                             int sourceWidth, int sourceHeight) {
        Assert.argument(neighbourhoodMatrix.length == N_SIZE);
        Assert.argument(neighbourhoodMatrix[4] == 0.0);
        Assert.argument(neighbourhoodBandCount >= 0);

        this.neighbourhoodMatrix = neighbourhoodMatrix;
        this.neighbourhoodBandCount = neighbourhoodBandCount;
        this.sourceWidth = sourceWidth;
        this.sourceHeight = sourceHeight;
    }

    /**
     * Performs the dropout correction.
     * todo - complete
     *
     * @param data
     * @param mask
     */
    public void perform(int[][] data, short[][] mask, int bandIndex, Rectangle rectangle) {
        Assert.argument(data.length == mask.length);
        Assert.argument(data[bandIndex].length == mask[bandIndex].length);

        final double[] w = new double[neighbourhoodMatrix.length];

        // x = offset of current row
        // y = offset of current column 
        for (int y = rectangle.y; y < rectangle.y + rectangle.height; ++y) {
            for (int x = rectangle.x; x < rectangle.x + rectangle.width; ++x) {
                // offset of current pixel
                final int yx = y * sourceWidth + x;

                if (mask[bandIndex][yx] == DROPOUT) {
                    double ws = 0.0;
                    double xc = 0.0;

                    double ws2 = 0.0;
                    double xc2 = 0.0;

                    // offset of neighbourhood pixel
                    int nIndex = 0;

                    // ny = current row of neighbourhood matrix
                    for (int i = y - 1; i <= y + 1; ++i) {
                        if (i < 0 || i >= sourceHeight) {
                            // this is not an adjacent row, so
                            nIndex += N_WIDTH;
                            continue;
                        }

                        // nx = current column of neighbourhood matrix
                        for (int j = x - 1; j <= x + 1; ++j, nIndex++) {
                            if (j < 0 || j >= sourceWidth) {
                                // this is not an adjacent column, so
                                continue;
                            }
                            // offset of neighbourhood matrix element
                            final int ij = i * sourceWidth + j;

                            if (neighbourhoodMatrix[nIndex] != 0.0) {
                                switch (mask[bandIndex][ij]) {
                                    case VALID:
                                        w[nIndex] = neighbourhoodMatrix[nIndex] * calculateWeight(yx, ij, data, mask, bandIndex);
                                        ws += w[nIndex];
                                        xc += data[bandIndex][ij] * w[nIndex];
                                        break;

                                    case SATURATED:
                                        ws2 += neighbourhoodMatrix[nIndex];
                                        xc2 += data[bandIndex][ij] * neighbourhoodMatrix[nIndex];
                                        break;
                                }
                            }
                        }
                    }

                    if (ws > 0.0) {
                        data[bandIndex][yx] = (int) (xc / ws);
                        mask[bandIndex][yx] = CORRECTED_DROPOUT;
                    } else if (ws2 > 0.0) {
                        data[bandIndex][yx] = (int) (xc2 / ws2);
                        mask[bandIndex][yx] = CORRECTED_DROPOUT_SATURATED;
                    } else {
                        data[bandIndex][yx] = 0;
                    }
                }
            }
        }
    }

    private double calculateWeight(final int yx,
                                   final int nyx,
                                   final int[][] data,
                                   final short[][] mask,
                                   final int bandIndex) {
        double sum = 0.0;
        int validCount = 0;

        for (int k = 0; k < neighbourhoodBandCount; ++k) {
            int lowerBand = bandIndex - (k + 1);

            if (lowerBand > 0 &&
                mask[lowerBand][yx] == VALID &&
                mask[lowerBand][nyx] == VALID &&
                data[lowerBand][yx] != 0) {
                final double d = (data[lowerBand][yx] - data[lowerBand][nyx]);

                sum += d * d;
                ++validCount;
            }

            int upperBand = bandIndex + (k + 1);
            if (upperBand < mask.length &&
                mask[upperBand][yx] == VALID &&
                mask[upperBand][nyx] == VALID &&
                data[upperBand][yx] != 0) {
                final double d = (data[upperBand][yx] - data[upperBand][nyx]);

                sum += d * d;
                ++validCount;
            }
        }

        if (validCount > 0) {
            final double eps = 1.0E-52;
            final double rms = Math.sqrt(sum / validCount);

            return 1.0 / (eps + rms); // avoid an infinite return value for sum -> 0
        } else {
            return 1.0;
        }
    }

}
