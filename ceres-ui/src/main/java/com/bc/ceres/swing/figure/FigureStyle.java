package com.bc.ceres.swing.figure;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertySet;

import java.awt.Color;
import java.awt.Paint;
import java.awt.Stroke;

public interface FigureStyle extends PropertySet {
    //
    //  The following property descriptors are SVG/CSS standards (see http://www.w3.org/TR/SVG/styling.html)
    //

    // todo - change to PropertyDescriptor with defaultValue set (nf)
    Property FILL_COLOR = Property.create("fill", Color.class, Color.BLACK, false);
    Property FILL_OPACITY = Property.create("fill-opacity", Double.class, 1.0, false);

    Property STROKE_COLOR = Property.create("stroke", Color.class, null, false);
    Property STROKE_OPACITY = Property.create("stroke-opacity", Double.class, 1.0, false);
    Property STROKE_WIDTH = Property.create("stroke-width", Double.class, 0.0, false);

    String getName();

    Stroke getStroke();

    Stroke getStroke(double scale);

    Color getStrokeColor();

    double getStrokeOpacity();

    double getStrokeWidth();

    Color getFillColor();

    double getFillOpacity();

    String toCssString();

    void fromCssString(String css);
}
