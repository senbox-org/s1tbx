package com.bc.ceres.swing.figure.support;

import junit.framework.TestCase;

import java.awt.Rectangle;

import com.bc.ceres.swing.figure.support.DefaultShapeFigure;
import com.bc.ceres.swing.figure.support.DefaultFigureStyle;
import com.bc.ceres.swing.figure.FigureSelection;
import com.bc.ceres.swing.figure.Figure;

public class FigureSelectionTest extends TestCase {

    public void testThatFigureSelectionCannotBeSelected() {
        FigureSelection figureSelection = new DefaultFigureSelection();
        assertEquals(false, figureSelection.isSelected());
        figureSelection.setSelected(true);
        assertEquals(false, figureSelection.isSelected());
    }

    public void testPropagateSelectionState() {
        Figure figure = new DefaultShapeFigure(new Rectangle(0, 0, 10, 10), true, new DefaultFigureStyle());

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