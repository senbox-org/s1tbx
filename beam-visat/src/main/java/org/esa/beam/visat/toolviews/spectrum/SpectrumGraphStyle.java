package org.esa.beam.visat.toolviews.spectrum;

import org.esa.beam.framework.ui.diagram.DiagramGraphStyle;

import java.awt.Color;
import java.awt.Stroke;
import java.awt.BasicStroke;


class SpectrumGraphStyle implements DiagramGraphStyle {
    Color color;
    boolean showingPoints;
    Color pointColor;
    Stroke stroke;

    public SpectrumGraphStyle() {
        color = Color.BLACK;
        pointColor = Color.WHITE;
        showingPoints = true;
        stroke = new BasicStroke(1.0f);
    }

    public Color getColor() {
        return color;
    }

    public void setColor(Color color) {
        this.color = color;
    }

    public Color getPointColor() {
        return pointColor;
    }

    public void setPointColor(Color pointColor) {
        this.pointColor = pointColor;
    }

    public boolean isShowingPoints() {
        return showingPoints;
    }

    public void setShowingPoints(boolean showingPoints) {
        this.showingPoints = showingPoints;
    }

    public Stroke getStroke() {
        return stroke;
    }

    public void setStroke(Stroke stroke) {
        this.stroke = stroke;
    }

}
