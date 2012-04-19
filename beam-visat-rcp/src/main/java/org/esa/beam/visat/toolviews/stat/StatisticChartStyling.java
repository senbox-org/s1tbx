package org.esa.beam.visat.toolviews.stat;

import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;

import java.awt.*;
import java.awt.geom.Ellipse2D;

class StatisticChartStyling {

    static final Paint INSITU_PAINT = new Color(200, 0, 0);
    static final Paint INSITU_OUTLINE_PAINT = INSITU_PAINT;
    static final Paint INSITU_FILL_PAINT = new Color(255, 150, 150);
    static final Shape INSITU_SHAPE = new Ellipse2D.Float(-4, -4, 8, 8);
    static final boolean INSITU_SHAPES_FILLED = true;
    static final Paint DATA_PAINT = new Color(0, 0, 200);
    static final Paint DATA_FILL_PAINT = new Color(150, 150, 255);
    static final Shape DATA_POINT_SHAPE = new Ellipse2D.Float(-4, -4, 8, 8);

    static ValueAxis updateScalingOfAxis(boolean logScaled, ValueAxis oldAxis, final boolean autoRangeIncludesZero) {
        ValueAxis newAxis = oldAxis;
        if (logScaled) {
            if (!(oldAxis instanceof CustomLogarithmicAxis)) {
                newAxis = createLogarithmicAxis(oldAxis.getLabel());
            }
        } else {
            if (oldAxis instanceof CustomLogarithmicAxis) {
                newAxis = createNumberAxis(oldAxis.getLabel(), autoRangeIncludesZero);
            }
        }
        return newAxis;
    }

    static NumberAxis createNumberAxis(String label, boolean autoRangeIncludesZero) {
        final NumberAxis numberAxis = new NumberAxis(label);
        numberAxis.setAutoRangeIncludesZero(autoRangeIncludesZero);
        return numberAxis;
    }

    static CustomLogarithmicAxis createLogarithmicAxis(String label) {
        CustomLogarithmicAxis logAxis = new CustomLogarithmicAxis(label);
        logAxis.setAllowNegativesFlag(false);
        logAxis.setLog10TickLabelsFlag(true);
        logAxis.setMinorTickCount(10);
        return logAxis;
    }
}
