package org.esa.beam.dataio.avhrr.noaa.pod;

import com.bc.ceres.binio.SequenceData;
import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.dataio.avhrr.BandReader;
import org.esa.beam.dataio.avhrr.calibration.Calibrator;
import org.esa.beam.framework.datamodel.ProductData;

import java.io.IOException;

/**
 * @author Ralf Quast
 */
final class PodBandReader implements BandReader {

    private static final int POINTS_PER_SCAN = 2048;
    private static final int WORDS_PER_SCAN = 3414;

    private static final int TEN_BITS = 0b1111111111;
    private static final int[] FIRST = {0, 0, 0, 1, 1};
    private static final int[][] INCREMENT = {{1, 2, 2}, {2, 1, 2}, {2, 2, 1}, {1, 2, 2}, {2, 1, 2}};
    private static final int[][] SHIFT = {{20, 0, 10}, {10, 20, 0}, {0, 10, 20}, {20, 0, 10}, {10, 20, 0}};

    private final VideoDataProvider videoDataProvider;
    private final int channelIndex;
    private final CalibratorFactory calibratorFactory;

    public PodBandReader(int channelIndex, VideoDataProvider videoDataProvider, CalibratorFactory calibratorFactory) {
        this.channelIndex = channelIndex;
        this.videoDataProvider = videoDataProvider;
        this.calibratorFactory = calibratorFactory;
    }

    @Override
    public String getBandName() {
        return calibratorFactory.getBandName();
    }

    @Override
    public String getBandUnit() {
        return calibratorFactory.getBandUnit();
    }

    @Override
    public String getBandDescription() {
        return calibratorFactory.getBandDescription();
    }

    @Override
    public double getScalingFactor() {
        return 1.0;
    }

    @Override
    public int getDataType() {
        return ProductData.TYPE_FLOAT32;
    }

    @Override
    public void readBandRasterData(int sourceOffsetX,
                                   int sourceOffsetY,
                                   int sourceWidth,
                                   int sourceHeight,
                                   int sourceStepX,
                                   int sourceStepY,
                                   ProductData targetBuffer, ProgressMonitor pm) throws IOException {
        final int minX = sourceOffsetX;
        final int maxX = sourceOffsetX + sourceWidth - 1;
        final int minY = sourceOffsetY;
        final int maxY = sourceOffsetY + sourceHeight - 1;
        final int targetStart = 0;
        final int targetIncrement = 1;

        final float[] targetData = (float[]) targetBuffer.getElems();
        final int[] videoData = new int[WORDS_PER_SCAN];
        final int[] countData = new int[POINTS_PER_SCAN];

        int targetIdx = targetStart;
        pm.beginTask("Reading AVHRR band '" + getBandName() + "'...", maxY - minY);
        try {
            for (int y = minY; y <= maxY; y += sourceStepY) {
                if (pm.isCanceled()) {
                    break;
                }
                boolean valid = videoDataProvider.isValid(y);
                if (valid) {
                    readVideoData(y, videoData);
                    decodeVideoData(channelIndex, videoData, countData);
                    try {
                        final Calibrator calibrator = calibratorFactory.createCalibrator(y);
                        for (int x = minX; x <= maxX; x += sourceStepX) {
                            targetData[targetIdx] = calibrator.calibrate(countData[x]);
                            targetIdx += targetIncrement;
                        }
                    } catch (IOException e) {
                        valid = false;
                    }
                }
                if (!valid) {
                    for (int x = minX; x <= maxX; x += sourceStepX) {
                        targetData[targetIdx] = Float.NaN;
                        targetIdx += targetIncrement;
                    }
                }
                pm.worked(1);
            }
        } finally {
            pm.done();
        }
    }

    private void readVideoData(int y, int[] videoData) throws IOException {
        final SequenceData videoDataSequence = videoDataProvider.getVideoData(y);
        for (int i = 0; i < videoDataSequence.getElementCount(); i++) {
            videoData[i] = videoDataSequence.getInt(i);
        }
    }

    private void decodeVideoData(int channelIndex, int[] videoData, int[] countData) {
        final int[] shifts = SHIFT[channelIndex];
        final int[] increments = INCREMENT[channelIndex];

        for (int i = 0, j = 0, rawIndex = FIRST[channelIndex]; i < countData.length; i++) {
            countData[i] = (videoData[rawIndex] & (TEN_BITS << shifts[j])) >> shifts[j];
            rawIndex += increments[j];
            if (j == 2) {
                j = 0;
            } else {
                j++;
            }
        }
    }

}
