package org.esa.snap.statistics;

import org.esa.snap.core.datamodel.HistogramStxOp;

import javax.media.jai.Histogram;

public class HistogramExpanderTransmitter {

    static HistogramStxOp createExpandedHistogramOp(Histogram oldHistogram, double minimum, double maximum, boolean intHistogram, int initialBinCount) {
        final double oldMin = oldHistogram.getLowValue()[0];
        final double oldMax = oldHistogram.getHighValue()[0];
        final double newMin = Math.min(oldMin, minimum);
        final double newMax = Math.max(oldMax, maximum);
        final int oldBinCount = oldHistogram.getNumBins()[0];
        double oldBinWidth = computeBinWidth(oldMin, oldMax, oldBinCount);
        double numNewMinBins = 0;
        if (minimum < oldMin) {
            final double minDiff = oldMin - minimum;
            numNewMinBins = Math.ceil(minDiff / oldBinWidth);
        }
        double numNewMaxBins = 0;
        if (maximum > oldMax) {
            final double maxDiff = maximum - oldMax;
            numNewMaxBins = Math.ceil(maxDiff / oldBinWidth);
        }
        final HistogramStxOp histogramStxOp;
        if (oldBinWidth == 0 || numNewMinBins + numNewMaxBins > 200 * initialBinCount) {
            histogramStxOp = new HistogramStxOp(initialBinCount, newMin, newMax, intHistogram, false);
            migrateOldHistogramData(oldHistogram, histogramStxOp.getHistogram());
            return histogramStxOp;
        } else {
            double newMinimum = oldMin - numNewMinBins * oldBinWidth;
            double newMaximum = oldMax + numNewMaxBins * oldBinWidth;
            double newBinCount = oldBinCount + numNewMinBins + numNewMaxBins;

            final int binRatio;
            if (newBinCount > 2 * initialBinCount) {
                binRatio = (int) (newBinCount / initialBinCount);
                final int binRemainder = (int) (newBinCount % binRatio);
                newMaximum += binRemainder * oldBinWidth;
                newBinCount = (newBinCount + binRemainder) / binRatio;
            } else {
                binRatio = 1;
            }

            histogramStxOp = new HistogramStxOp((int) newBinCount, newMinimum, newMaximum, intHistogram, false);
            migrateOldHistogramData(oldHistogram, histogramStxOp.getHistogram(), (int) numNewMinBins, binRatio);
            return histogramStxOp;
        }
    }

    private static void migrateOldHistogramData(Histogram oldHistogram, Histogram newHistogram) {
        final double oldMin = oldHistogram.getLowValue(0);
        final double oldMax = oldHistogram.getHighValue(0);
        final int[] oldBins = oldHistogram.getBins(0);
        final int oldNumBins = oldBins.length;
        final double oldBinWidth = computeBinWidth(oldMin, oldMax, oldNumBins);

        final double newMin = newHistogram.getLowValue(0);
        final double newMax = newHistogram.getHighValue(0);
        final int[] newBins = newHistogram.getBins(0);
        final int newNumBins = newBins.length;
        final double newBinWidth = computeBinWidth(newMin, newMax, newNumBins);

        for (int i = 0; i < oldBins.length; i++) {
            int count = oldBins[i];
            if (count == 0) {
                continue;
            }
            final double binCenterValue = oldMin + oldBinWidth * i + oldBinWidth / 2;
            int newBinIndex = (int) Math.floor((binCenterValue - newMin) / newBinWidth);
            if (newBinIndex >= newNumBins) {
                newBinIndex = newNumBins - 1;
            }
            newBins[newBinIndex] += count;
        }
    }

    private static void migrateOldHistogramData(Histogram oldHistogram, Histogram newHistogram, int startOffset, int binRatio) {
        final int[] oldBins = oldHistogram.getBins(0);
        final int[] newBins = newHistogram.getBins(0);
        final int newMaxIndex = newBins.length - 1;
        for (int i = 0; i < oldBins.length; i++) {
            int newBinsIndex = (startOffset + i) / binRatio;
            if (newBinsIndex > newMaxIndex) {
                newBinsIndex = newMaxIndex;
            }
            newBins[newBinsIndex] += oldBins[i];
        }
    }

    private static double computeBinWidth(double min, double max, int binCount) {
        return (max - min) / binCount;
    }
}
