package org.esa.beam.framework.ui.diagram;

import java.awt.Color;
import java.awt.Stroke;

public interface DiagramGraphStyle {

    boolean isShowingPoints();

    Color getPointColor();

    Stroke getStroke();

    Color getColor();
}
