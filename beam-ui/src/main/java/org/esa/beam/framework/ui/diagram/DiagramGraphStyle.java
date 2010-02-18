package org.esa.beam.framework.ui.diagram;

import java.awt.Color;
import java.awt.Stroke;
import java.awt.Paint;

public interface DiagramGraphStyle {

    boolean isShowingPoints();

    Paint getFillPaint();

    Stroke getOutlineStroke();

    Color getOutlineColor();
}
