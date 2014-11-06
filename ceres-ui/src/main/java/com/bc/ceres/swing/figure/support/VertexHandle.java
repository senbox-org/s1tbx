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

package com.bc.ceres.swing.figure.support;

import com.bc.ceres.swing.figure.AbstractHandle;
import com.bc.ceres.swing.figure.Figure;
import com.bc.ceres.swing.figure.FigureStyle;
import static com.bc.ceres.swing.figure.support.StyleDefaults.*;

import java.awt.Shape;
import java.awt.geom.Path2D;
import java.awt.geom.Ellipse2D;


/**
 * A {@link com.bc.ceres.swing.figure.Handle Handle} that can be used to change vertex positions.
 *
 * @author Norman Fomferra
 * @since Ceres 0.10
 */
public class VertexHandle extends AbstractHandle {
    private int segmentIndex;

    public VertexHandle(Figure figure,
                        int vertexIndex,
                        FigureStyle normalStyle,
                        FigureStyle selectedStyle) {
        super(figure, normalStyle, selectedStyle);
        this.segmentIndex = vertexIndex;
        updateLocation();
        setShape(createHandleShape());
    }

    public int getSegmentIndex() {
        return segmentIndex;
    }

    public void setSegmentIndex(int segmentIndex) {
        this.segmentIndex = segmentIndex;
    }

    @Override
    public void updateLocation() {
        final double[] segment = getFigure().getSegment(segmentIndex);
        if (segment != null) {
            setLocation(segment[0], segment[1]);
        }
    }

    @Override
    public void move(double dx, double dy) {
        setLocation(getX() + dx, getY() + dy);
        final double[] segment = getFigure().getSegment(segmentIndex);
        if (segment != null) {
            segment[0] += dx;
            segment[1] += dy;
            getFigure().setSegment(segmentIndex, segment);
        }

    }

    private static Shape createHandleShape() {
        return new Ellipse2D.Double(-0.5 * VERTEX_HANDLE_SIZE, -0.5 * VERTEX_HANDLE_SIZE, VERTEX_HANDLE_SIZE, VERTEX_HANDLE_SIZE);
    }
}