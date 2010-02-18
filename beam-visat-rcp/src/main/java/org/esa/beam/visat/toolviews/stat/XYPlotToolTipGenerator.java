package org.esa.beam.visat.toolviews.stat;

import org.jfree.chart.labels.CustomXYToolTipGenerator;
import org.jfree.data.xy.XYDataset;


class XYPlotToolTipGenerator extends CustomXYToolTipGenerator {

    @Override
    public String generateToolTip(XYDataset data, int series, int item) {
        final Comparable key = data.getSeriesKey(series);
        final double valueX = data.getXValue(series, item);
        final double valueY = data.getYValue(series, item);
        return String.format("%s: X = %6.2f, Y = %6.2f", key, valueX, valueY);
    }

}
