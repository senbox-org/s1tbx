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

import com.bc.ceres.swing.figure.Figure;
import com.bc.ceres.swing.figure.FigureSelection;
import com.bc.ceres.swing.figure.TestFigure;
import org.junit.Test;

import java.awt.Rectangle;

import static org.junit.Assert.*;

public class DefaultFigureSelectionTest {

    @Test
    public void testThatFigureSelectionCannotBeSelected() {
        FigureSelection figureSelection = new DefaultFigureSelection();
        assertEquals(false, figureSelection.isSelectable());
        assertEquals(false, figureSelection.isSelected());
        figureSelection.setSelected(true);
        assertEquals(false, figureSelection.isSelected());
    }

    @Test
    public void testThatNonSelectableFiguresAreNotSelected() {
        FigureSelection figureSelection = new DefaultFigureSelection();
        TestFigure f1 = new TestFigure(true);
        TestFigure f2 = new TestFigure(false);
        TestFigure f3 = new TestFigure(false);
        TestFigure f4 = new TestFigure(true);

        assertSame(true, f1.isSelectable());
        assertSame(false, f2.isSelectable());
        assertSame(false, f3.isSelectable());
        assertSame(true, f4.isSelectable());

        assertSame(false, f1.isSelected());
        assertSame(false, f2.isSelected());
        assertSame(false, f3.isSelected());
        assertSame(false, f4.isSelected());

        figureSelection.addFigures(f1, f2, f3, f4);
        assertEquals(2, figureSelection.getFigureCount());
        assertSame(f1, figureSelection.getFigure(0));
        assertSame(f4, figureSelection.getFigure(1));
        assertSame(true, f1.isSelected());
        assertSame(false, f2.isSelected());
        assertSame(false, f3.isSelected());
        assertSame(true, f4.isSelected());

        figureSelection.removeAllFigures();
        assertEquals(0, figureSelection.getFigureCount());
        assertSame(false, f1.isSelected());
        assertSame(false, f2.isSelected());
        assertSame(false, f3.isSelected());
        assertSame(false, f4.isSelected());
    }

    @Test
    public void testPropagateSelectionState() {
        Figure figure = new DefaultShapeFigure(new Rectangle(0, 0, 10, 10), Figure.Rank.AREA, new DefaultFigureStyle());

        FigureSelection figureSelection = new DefaultFigureSelection();

        assertEquals(false, figureSelection.isSelected());
        assertEquals(false, figure.isSelected());

        figureSelection.addFigure(figure);
        assertEquals(false, figureSelection.isSelected());
        assertEquals(true, figure.isSelected());

        figureSelection.removeFigure(figure);
        assertEquals(false, figureSelection.isSelected());
        assertEquals(false, figure.isSelected());
    }
}