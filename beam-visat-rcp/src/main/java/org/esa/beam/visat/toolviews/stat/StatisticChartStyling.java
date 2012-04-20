package org.esa.beam.visat.toolviews.stat;

import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.VectorDataNode;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.axis.ValueAxis;
import org.opengis.feature.type.AttributeDescriptor;

import java.awt.*;
import java.awt.geom.Ellipse2D;

class StatisticChartStyling {

    static final Paint CORRELATIVE_POINT_PAINT = new Color(200, 0, 0);
    static final Paint CORRELATIVE_POINT_OUTLINE_PAINT = CORRELATIVE_POINT_PAINT;
    static final Paint CORRELATIVE_POINT_FILL_PAINT = new Color(255, 150, 150);
    static final Shape CORRELATIVE_POINT_SHAPE = new Ellipse2D.Float(-4, -4, 8, 8);
    static final boolean CORRELATIVE_POINT_SHAPES_FILLED = true;

    static final Paint SAMPLE_DATA_PAINT = new Color(0, 0, 200);
    static final Paint SAMPLE_DATA_FILL_PAINT = new Color(150, 150, 255);
    static final Shape SAMPLE_DATA_POINT_SHAPE = new Ellipse2D.Float(-4, -4, 8, 8);

    static ValueAxis updateScalingOfAxis(boolean logScaled, ValueAxis oldAxis, final boolean autoRangeIncludesZero) {
        ValueAxis newAxis = oldAxis;
        if (logScaled) {
            if (!(oldAxis instanceof CustomLogarithmicAxis)) {
                final CustomLogarithmicAxis logarithmicAxis = createLogarithmicAxis(oldAxis.getLabel());
                logarithmicAxis.setAutoRange(oldAxis.isAutoRange());
                newAxis = logarithmicAxis;
            }
        } else {
            if (oldAxis instanceof CustomLogarithmicAxis) {
                final NumberAxis numberAxis = createNumberAxis(oldAxis.getLabel(), autoRangeIncludesZero);
                numberAxis.setAutoRange(oldAxis.isAutoRange());
                newAxis = numberAxis;
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

    // todo - Check how we can draw an axis label that uses a sub-scripted "10" in "log10". (ts, nf)
    public static String getAxisLabel(RasterDataNode raster, String defaultVariableName, boolean log10Scaled) {
        if (raster != null) {
            if (log10Scaled) {
                return "log10 of " + raster.getName();
            }
            final String unit = raster.getUnit();
            if (unit != null && !unit.isEmpty()) {
                return raster.getName() + " in " + unit;
            }
            return raster.getName();
        } else {
            if (log10Scaled) {
                return "log10 of " + defaultVariableName;
            } else {
                return defaultVariableName;
            }
        }
    }

    private static String getAxisLabel0(boolean logScaled, RasterDataNode raster) {
        if (logScaled) {
            return "log10(" + raster.getName() + ")";
        }
        final String unit = raster.getUnit();
        if (unit != null && !unit.isEmpty()) {
            return raster.getName() + " (" + unit + ")";
        }
        return raster.getName();
    }

    public static String getCorrelativeDataLabel(VectorDataNode pointDataSource, AttributeDescriptor dataField1) {
        final String vdsName = pointDataSource.getName();
        final String dataFieldName = dataField1.getLocalName();
        return vdsName + "/" + dataFieldName;
    }
}
