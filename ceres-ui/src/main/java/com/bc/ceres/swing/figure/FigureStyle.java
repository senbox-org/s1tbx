package com.bc.ceres.swing.figure;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertySet;

import java.awt.Color;
import java.awt.Paint;
import java.awt.Stroke;

public interface FigureStyle extends PropertySet {
    //
    //  The following property descriptors are SVG standards (see http://www.w3.org/TR/SVG/styling.html)
    //

    Property FILL = Property.create("fill", Paint.class, Color.BLACK, false);
    Property FILL_OPACITY = Property.create("fill-opacity", Number.class, 1.0f, false);

    Property STROKE = Property.create("stroke", Paint.class, null, false);
    Property STROKE_OPACITY = Property.create("stroke-opacity", Number.class, 1.0f, false);
    Property STROKE_WIDTH = Property.create("stroke-width", Number.class, 0.0f, false);

    String getName();

    FigureStyle getDefaultStyle();

    Stroke getStroke();

    Stroke getStroke(double scale);

    Paint getStrokePaint();

    Paint getFillPaint();
}
