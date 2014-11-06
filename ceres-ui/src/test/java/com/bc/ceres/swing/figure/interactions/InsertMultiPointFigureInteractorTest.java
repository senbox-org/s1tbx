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

package com.bc.ceres.swing.figure.interactions;

import com.bc.ceres.swing.figure.ShapeFigure;
import com.bc.ceres.swing.figure.support.DefaultFigureCollection;
import com.bc.ceres.swing.figure.support.DefaultFigureFactory;
import com.bc.ceres.swing.figure.support.FigureEditorPanel;
import org.junit.Before;
import org.junit.Test;

import java.awt.event.MouseEvent;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

public class InsertMultiPointFigureInteractorTest {

    private InsertMultiPointFigureInteractor interactor;
    private static FigureEditorPanel figureEditorPanel;

    @Before
    public void setup() {
        interactor = new InsertMultiPointFigureInteractor(true);
        interactor.activate();
        figureEditorPanel = new FigureEditorPanel(null,
                                            new DefaultFigureCollection(),
                                            new DefaultFigureFactory());

    }

    @Test
    public void testInteraction3Points() {
        interactor.mouseReleased(createMouseEvent(0, 0, 1));
        // expecting at least 4 points, because of special JTS polygon expecting
        // at least 4 points at construction time
        Point2D[] expectedPoints = new Point2D[]{
                new Point2D.Double(0, 0),
                new Point2D.Double(0, 0),
                new Point2D.Double(0, 0),
                new Point2D.Double(0, 0)
        };
        Point2D[] actualPoints = interactor.getPoints();
        performArrayTest(expectedPoints, actualPoints);

        interactor.mouseMoved(createMouseEvent(10, 10, 1));
        expectedPoints = new Point2D[]{
                new Point2D.Double(0, 0),
                new Point2D.Double(0, 0),
                new Point2D.Double(0, 0),
                new Point2D.Double(10, 10)
        };
        actualPoints = interactor.getPoints();
        performArrayTest(expectedPoints, actualPoints);

        interactor.mouseReleased(createMouseEvent(12, 12, 1));
        expectedPoints = new Point2D[]{
                new Point2D.Double(0, 0),
                new Point2D.Double(0, 0),
                new Point2D.Double(0, 0),
                new Point2D.Double(12, 12),
                new Point2D.Double(12, 12),
        };
        actualPoints = interactor.getPoints();
        performArrayTest(expectedPoints, actualPoints);

        interactor.mouseReleased(createMouseEvent(13, 16, 1));
        expectedPoints = new Point2D[]{
                new Point2D.Double(0, 0),
                new Point2D.Double(0, 0),
                new Point2D.Double(0, 0),
                new Point2D.Double(12, 12),
                new Point2D.Double(13, 16),
                new Point2D.Double(13, 16),
        };
        actualPoints = interactor.getPoints();
        performArrayTest(expectedPoints, actualPoints);

        interactor.mouseClicked(createMouseEvent(29, 5, 2));
        performFinalFigureTest(new Point2D[]{
                new Point2D.Double(0, 0),
                new Point2D.Double(12, 12),
                new Point2D.Double(13, 16),
                new Point2D.Double(0, 0), // close path
        });
    }

    @Test
    public void testInteraction2Points() {
        interactor.mouseReleased(createMouseEvent(0, 0, 1));
        // expecting at least 4 points, because of special JTS polygon
        Point2D[] expectedPoints = new Point2D[]{
                new Point2D.Double(0, 0),
                new Point2D.Double(0, 0),
                new Point2D.Double(0, 0),
                new Point2D.Double(0, 0)
        };
        Point2D[] actualPoints = interactor.getPoints();
        performArrayTest(expectedPoints, actualPoints);

        interactor.mouseReleased(createMouseEvent(13, 16, 1));
        expectedPoints = new Point2D[]{
                new Point2D.Double(0, 0),
                new Point2D.Double(0, 0),
                new Point2D.Double(0, 0),
                new Point2D.Double(13, 16),
                new Point2D.Double(13, 16),
        };
        actualPoints = interactor.getPoints();
        performArrayTest(expectedPoints, actualPoints);

        interactor.mouseClicked(createMouseEvent(29, 5, 2));
        performFinalFigureTest(new Point2D[]{
                new Point2D.Double(0, 0),
                new Point2D.Double(0, 0), 
                new Point2D.Double(13, 16),
                new Point2D.Double(0, 0), // close path
        });
    }

    private void performFinalFigureTest(Point2D[] expectedPoints) {
        final ShapeFigure figure = (ShapeFigure) figureEditorPanel.getFigureEditor().getFigureCollection().getFigure(0);
        final PathIterator pathIterator = figure.getShape().getPathIterator(null);
        List<Point2D> segList = new ArrayList<Point2D>();
        while(!pathIterator.isDone()) {
            final double[] seg = new double[6];
            pathIterator.currentSegment(seg);
            segList.add(new Point2D.Double(seg[0], seg[1]));
            pathIterator.next();
        }

        performArrayTest(expectedPoints, segList.toArray(new Point2D[segList.size()]));
    }

    private void performArrayTest(Point2D[] expectedPoints, Point2D[] actualPoints) {
        if (expectedPoints.length != actualPoints.length) {
            failTest(expectedPoints, actualPoints);
        }
        for (int i = 0, expectedPointsLength = expectedPoints.length; i < expectedPointsLength; i++) {
            Point2D expectedPoint = expectedPoints[i];
            Point2D actualPoint = actualPoints[i];
            if(!expectedPoint.equals(actualPoint)){
                failTest(expectedPoints, actualPoints, i);
            }
        }
    }

    private void failTest(Point2D[] expectedPoints, Point2D[] actualPoints, int index) {
        fail("Arrays not equal at <"+index+">:\n" +
             "expected:" + Arrays.toString(expectedPoints) + "\n" +
             "actual:" + Arrays.toString(actualPoints));
    }

    private void failTest(Point2D[] expectedPoints, Point2D[] actualPoints) {
        fail("Arrays not equal:\n" +
             "expected:" + Arrays.toString(expectedPoints) + "\n" +
             "actual:" + Arrays.toString(actualPoints));
    }


    private MouseEvent createMouseEvent(int x, int y, int clickCount) {
        return new MouseEvent(figureEditorPanel, 0, System.currentTimeMillis(), 0, x, y, clickCount, false);
    }

}
