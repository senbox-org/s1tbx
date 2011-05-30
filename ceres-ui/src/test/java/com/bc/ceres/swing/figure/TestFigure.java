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

package com.bc.ceres.swing.figure;

import com.bc.ceres.swing.figure.support.DefaultFigureStyle;
import com.bc.ceres.swing.figure.support.DefaultShapeFigure;
import org.junit.Ignore;

import java.awt.geom.Ellipse2D;

@Ignore
public class TestFigure extends DefaultShapeFigure {
    public TestFigure() {
        this(false);
    }

    public TestFigure(boolean selectable) {
        super(new Ellipse2D.Double(0, 0, 10, 10), Figure.Rank.AREA, new DefaultFigureStyle());
        setSelectable(selectable);
    }
}
