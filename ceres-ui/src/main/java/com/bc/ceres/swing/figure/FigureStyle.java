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

    /**
     * Gets the effective stroke style used for drawing the exterior of a lineal or polygonal shape.
     * The effective paint may result from a number of different style properties.
     *
     * @return The effective stroke style used for drawing.
     */
    Stroke getStroke();

    /**
     * Gets the effective stroke style used for drawing the exterior of a lineal or polygonal shape.
     * The effective paint may result from a number of different style properties.
     *
     * @param scale The current model-to-view scaling.
     * @return The effective stroke style used for drawing.
     */
    Stroke getStroke(double scale);

    /**
     * Gets the effective stroke paint used for drawing the exterior of a lineal or polygonal shape.
     * The effective paint may result from a number of different style properties.
     *
     * @return The effective stroke paint used for drawing.
     */
    Paint getStrokePaint();

    Color getStrokeColor();

    double getStrokeOpacity();

    double getStrokeWidth();

    /**
     * Gets the effective fill paint used for drawing the interior of a polygonal shape.
     * The effective paint may result from a number of different style properties.
     *
     * @return The effective fill paint used for drawing.
     */
    Paint getFillPaint();

    Color getFillColor();

    double getFillOpacity();

    String toCssString();

    void fromCssString(String css);
}
