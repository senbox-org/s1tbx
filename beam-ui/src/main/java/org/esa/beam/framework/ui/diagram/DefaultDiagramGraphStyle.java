/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.beam.framework.ui.diagram;

import org.esa.beam.framework.ui.diagram.DiagramGraphStyle;

import java.awt.Color;
import java.awt.Stroke;
import java.awt.BasicStroke;
import java.awt.Paint;


public class DefaultDiagramGraphStyle implements DiagramGraphStyle {
    private Color outlineColor;
    private boolean showingPoints;
    private Paint fillPaint;
    private Stroke outlineStroke;

    public DefaultDiagramGraphStyle() {
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
