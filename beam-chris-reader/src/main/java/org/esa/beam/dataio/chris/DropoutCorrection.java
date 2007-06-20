package org.esa.beam.dataio.chris;

import com.bc.ceres.core.Assert;

/**
 * The class {@code DropoutCorrection} encapsulates the dropout correction
 * algorithm developed by Luis Gomez Chova.
 *
 * @author Ralf Quast
 * @version $Revision: 1.5 $ $Date: 2007/04/18 16:01:35 $
 */
class DropoutCorrection {

    public static final double[] N_VERTICAL = {0, 1, 0, 0, 0, 0, 0, 1, 0};

    private static final int NEIGHBOURHOOD_WIDTH = 3;
    private static final int NEIGHBOURHOOD_HEIGHT = 3;
    private static final int NEIGHBOURHOOD_SIZE = NEIGHBOURHOOD_WIDTH * NEIGHBOURHOOD_HEIGHT;

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
    public DropoutCorrection(final double[] neighbourhoodMatrix, final int neighbourhoodBandCount,
                             final int sourceWidth, final int sourceHeight) {
        Assert.argument(neighbourhoodMatrix.length == NEIGHBOURHOOD_SIZE);
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
     * @param xOffset
     * @param yOffset
     * @param width
     * @param height
     */
    public void perform(final int[][] data, final short[][] mask, final int bandIndex,
                        final int xOffset, final int yOffset, final int width, final int height) {

        Assert.argument(data.length == mask.length);
        Assert.argument(data[bandIndex].length == mask[bandIndex].length);

        final double[] w = new double[neighbourhoodMatrix.length];

        // x = offset of current row
        // y = offset of current column 
        for (int y = yOffset; y < yOffset + height; y++) {
            for (int x = xOffset; x < xOffset + width; x++) {
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
                    for (int ny = y - 1; ny <= y + 1; ny++) {
                        if (ny < 0 || ny >= sourceHeight) {
                            // this is not an adjacent row, so
                            nIndex += NEIGHBOURHOOD_WIDTH;
                            continue;
                        }

                        // nx = current column of neighbourhood matrix
                        for (int nx = x - 1; nx <= x + 1; nx++, nIndex++) {
                            if (nx < 0 || nx >= sourceWidth) {
                                // this is not an adjacent column, so
                                continue;
                            }
                            // offset of neighbourhood matrix element
                            final int nyx = ny * sourceWidth + nx;

                            if (neighbourhoodMatrix[nIndex] != 0.0) {
                                switch (mask[bandIndex][nyx]) {
                                    case VALID:
                                        w[nIndex] = neighbourhoodMatrix[nIndex] * calculateWeight(yx, nyx, data, mask, bandIndex);
                                        ws += w[nIndex];
                                        xc += data[bandIndex][nyx] * w[nIndex];
                                        break;

                                    case SATURATED:
                                        ws2 += neighbourhoodMatrix[nIndex];
                                        xc2 += data[bandIndex][nyx] * neighbourhoodMatrix[nIndex];
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

    private static boolean isAnyAdjacentBandUsable(final int offset,
                                                   final int count,
                                                   final int stride,
                                                   final short[] mask) {
        int l = offset;
        int h = offset;

        for (int k = 0; k < count; ++k) {
            l -= stride;
            h += stride;

            if (l > 0 && mask[l] == VALID || h < mask.length && mask[h] == VALID) {
                return true;
            }
        }

        return false;
    }

}
