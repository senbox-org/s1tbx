package org.esa.beam.dataio.chris.internal;

import com.bc.ceres.core.Assert;

import java.awt.Rectangle;

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

    private static final int N_WIDTH = 3;
    private static final int N_HEIGHT = 3;
    private static final int N_SIZE = N_WIDTH * N_HEIGHT;

    private static final short VALID = 0;
    private static final short DROPOUT = 1;
    private static final short SATURATED = 2;
    private static final short CORRECTED_DROPOUT = 4;

    private double[] matrix;
    private int neighbourBandCount;
    private final int sourceWidth;
    private final int sourceHeight;
    private final boolean cosmetic;


    public DropoutCorrection(int type, int neighbourBandCount, int sourceWidth, int sourceHeight) {
        this(type, neighbourBandCount, sourceWidth, sourceHeight, false);
    }

    /**
     * Constructor.
     * todo - complete
     *
     * @param type
     * @param neighbourBandCount
     * @param sourceWidth
     * @param sourceHeight
     * @param cosmetic
     */
    public DropoutCorrection(int type, int neighbourBandCount, int sourceWidth, int sourceHeight,
                             boolean cosmetic) {
        Assert.argument(type == 2 || type == 4 || type == 8);
        Assert.argument(neighbourBandCount >= 0);

        switch (type) {
        case 2 :
            this.matrix = M2;
            break;
        case 4 :
            this.matrix = M4;
            break;
        case 8 :
            this.matrix = M8;
            break;
        }

        this.neighbourBandCount = neighbourBandCount;
        this.sourceWidth = sourceWidth;
        this.sourceHeight = sourceHeight;
        this.cosmetic = cosmetic;
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

        final double[] w = new double[matrix.length];

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

                            if (matrix[nIndex] != 0.0) {
                                switch (mask[bandIndex][ij]) {
                                case VALID:
                                    w[nIndex] = matrix[nIndex] * calculateWeight(yx, ij, data, mask,
                                                                                 bandIndex);
                                    ws += w[nIndex];
                                    xc += data[bandIndex][ij] * w[nIndex];
                                    break;

                                case SATURATED:
                                    ws2 += matrix[nIndex];
                                    xc2 += data[bandIndex][ij] * matrix[nIndex];
                                    break;
                                }
                            }
                        }
                    }

                    if (ws > 0.0) {
                        data[bandIndex][yx] = (int) (xc / ws);
                        if (!cosmetic) {
                            mask[bandIndex][yx] = CORRECTED_DROPOUT;
                        }
                    } else if (ws2 > 0.0) {
                        data[bandIndex][yx] = (int) (xc2 / ws2);
                        if (!cosmetic) {
                            mask[bandIndex][yx] = SATURATED;
                        }
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

        for (int k = 0; k < neighbourBandCount; ++k) {
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
