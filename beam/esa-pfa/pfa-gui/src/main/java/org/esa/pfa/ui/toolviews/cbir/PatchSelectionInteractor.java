/*
 * Copyright (C) 2013 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.pfa.ui.toolviews.cbir;

import com.bc.ceres.swing.figure.FigureEditor;
import com.bc.ceres.swing.figure.FigureEditorInteractor;
import com.bc.ceres.swing.figure.ShapeFigure;

import java.awt.*;
import java.awt.event.InputEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RectangularShape;
import java.awt.image.BufferedImage;

public class PatchSelectionInteractor extends FigureEditorInteractor {
    private Point2D referencePoint;
    private boolean canceled;
    private ShapeFigure figure;
    private RectangularShape rectangularShape;
    private boolean started;

    private final int width;
    private final int height;
    private Cursor cursor;

    public PatchSelectionInteractor(final int w, final int h) {
        this.width = w;
        this.height = h;
        //cursor = createCursor(width, height, "name");
    }

    protected RectangularShape createRectangularShape(InputEvent event) {
        final Point2D pnt = toModelPoint(event, referencePoint);
        return new Rectangle2D.Double(pnt.getX() - width/2, pnt.getY() - height/2, width, height);
    }

    public Rectangle2D getPatchShape() {
        return figure.getBounds();
    }

    @Override
    public void cancelInteraction(InputEvent event) {
        if (!canceled) {
            canceled = true;
            figure.setShape(new Rectangle2D.Double(0, 0, 0, 0));
            getFigureEditor(event).getFigureSelection().removeAllFigures();
            getFigureEditor(event).getFigureCollection().removeFigure(figure);
            super.cancelInteraction(event);
        }
    }

    @Override
    protected void stopInteraction(InputEvent inputEvent) {
        super.stopInteraction(inputEvent);
        started = false;
    }

    @Override
    public void mouseMoved(MouseEvent event) {
        if(!started) {
            started = startInteraction(event);
            if (started) {
                final FigureEditor figureEditor = getFigureEditor(event);
                figureEditor.getFigureSelection().removeAllFigures();
                referencePoint = event.getPoint();
                canceled = false;
                rectangularShape = createRectangularShape(event);
                figure = figureEditor.getFigureFactory().createPolygonFigure(rectangularShape, //toModelShape(event, rectangularShape),
                        figureEditor.getDefaultPolygonStyle());
                figureEditor.getFigureCollection().addFigure(figure);
            }
        } else {
            referencePoint.setLocation(event.getX(), event.getY());

            updatePatchShape(event);
        }
    }

    @Override
    public void mouseExited(MouseEvent event) {
        cancelInteraction(event);
    }

    @Override
    public void mouseReleased(MouseEvent event) {
        if (started) {

            updatePatchShape(event);
            getFigureEditor(event).insertFigures(false, figure);

            stopInteraction(event);
        }
    }

    private void updatePatchShape(final MouseEvent event) {
        figure.setShape(createRectangularShape(event));
    }

    private static Cursor createCursor(final int w, final int h, final String cursorName) {
        final Toolkit defaultToolkit = Toolkit.getDefaultToolkit();

        final BufferedImage image = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g2 = image.createGraphics();
        g2.setColor(Color.red);
        g2.fillRect(2, 2, w-4, h-4);

        // this is necessary because on some systems the cursor is scaled but not the 'hot spot'
        final Dimension bestCursorSize = defaultToolkit.getBestCursorSize(w, h);
        final Point hotSpot = new Point((7 * bestCursorSize.width) / w, (7 * bestCursorSize.height) / h);

        return defaultToolkit.createCustomCursor(image, hotSpot, cursorName);
    }
}