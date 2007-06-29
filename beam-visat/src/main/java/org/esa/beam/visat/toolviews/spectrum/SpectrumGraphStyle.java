package org.esa.beam.visat.toolviews.spectrum;

import org.esa.beam.framework.ui.diagram.DiagramGraphStyle;

import java.awt.Color;
import java.awt.Stroke;
import java.awt.BasicStroke;
import java.awt.Paint;


class SpectrumGraphStyle implements DiagramGraphStyle {
    Color outlineColor;
    boolean showingPoints;
    Paint fillPaint;
    Stroke outlineStroke;

    public SpectrumGraphStyle() {
        outlineColor = Color.BLACK;
        fillPaint = Color.WHITE;
        showingPoints = true;
        outlineStroke = new BasicStroke(1.0f);
    }

    public Color getOutlineColor() {
        return outlineColor;
    }

    public void setOutlineColor(Color outlineColor) {
        this.outlineColor = outlineColor;
    }

    public Paint getFillPaint() {
        return fillPaint;
    }

    public void setFillPaint(Paint fillPaint) {
        this.fillPaint = fillPaint;
    }

    public boolean isShowingPoints() {
        return showingPoints;
    }

    public void setShowingPoints(boolean showingPoints) {
        this.showingPoints = showingPoints;
    }

    public Stroke getOutlineStroke() {
        return outlineStroke;
    }

    public void setOutlineStroke(Stroke stroke) {
        this.outlineStroke = stroke;
    }

}
