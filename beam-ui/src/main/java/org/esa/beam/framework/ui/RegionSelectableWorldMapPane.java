/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.framework.ui;

import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.glayer.swing.LayerCanvas;
import com.bc.ceres.grender.Rendering;
import com.bc.ceres.grender.Viewport;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.figure.Figure;
import com.bc.ceres.swing.figure.FigureCollection;
import com.bc.ceres.swing.figure.FigureEditor;
import com.bc.ceres.swing.figure.FigureEditorAware;
import com.bc.ceres.swing.figure.ShapeFigure;
import com.bc.ceres.swing.figure.ViewportInteractor;
import com.bc.ceres.swing.figure.support.DefaultFigureEditor;
import com.bc.ceres.swing.figure.support.DefaultFigureStyle;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Point;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Map;

/**
 * This class wraps a {@link WorldMapPane} and extends it by functionality to draw and resize a selection rectangle.
 *
 * @author Thomas Storm
 * @author Tonio Fincke
 */
public class RegionSelectableWorldMapPane {

    private static final int OFFSET = 6;

    private final BindingContext bindingContext;
    private final DefaultFigureEditor figureEditor;
    private final WorldMapPane worldMapPane;
    private final RegionSelectionInteractor regionSelectionInteractor;
    private final RegionSelectableWorldMapPane.CursorChanger cursorChanger = new CursorChanger();

    private Rectangle2D selectionRectangle;
    private Rectangle2D movableRectangle;
    private Rectangle2D.Double defaultRectangle;
    private Shape defaultShape;

    public RegionSelectableWorldMapPane(WorldMapPaneDataModel dataModel, BindingContext bindingContext) {
        this.bindingContext = bindingContext;
        worldMapPane = new FigureEditorAwareWorldMapPane(dataModel, new SelectionOverlay(dataModel));
        worldMapPane.setPanSupport(new RegionSelectionDecoratingPanSupport(worldMapPane.getLayerCanvas()));
        figureEditor = new DefaultFigureEditor(worldMapPane.getLayerCanvas());
        regionSelectionInteractor = new RegionSelectionInteractor();
        worldMapPane.getLayerCanvas().addMouseMotionListener(cursorChanger);
    }

    public JPanel createUI() {
        return worldMapPane;
    }

    private DefaultFigureStyle createFigureStyle() {
        DefaultFigureStyle figureStyle = (DefaultFigureStyle) figureEditor.getDefaultPolygonStyle();
        figureStyle.setFillColor(new Color(255, 200, 200));
        figureStyle.setFillOpacity(0.2);
        figureStyle.setStrokeColor(new Color(200, 0, 0));
        figureStyle.setStrokeWidth(2);
        return figureStyle;
    }

    private class CursorChanger implements MouseMotionListener {

        private final String DEFAULT = "default";
        private final String MOVE = "move";
        private final String NORTH = "north";
        private final String SOUTH = "south";
        private final String WEST = "west";
        private final String EAST = "east";
        private final String NORTH_WEST = "northWest";
        private final String NORTH_EAST = "northEast";
        private final String SOUTH_WEST = "southWest";
        private final String SOUTH_EAST = "southEast";

        private Map<String, Cursor> cursorMap;
        private Map<String, Rectangle2D.Double> rectangleMap;

        private CursorChanger() {
            cursorMap = new HashMap<String, Cursor>();
            cursorMap.put(DEFAULT, Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
            cursorMap.put(MOVE, Cursor.getPredefinedCursor(Cursor.MOVE_CURSOR));
            cursorMap.put(NORTH, Cursor.getPredefinedCursor(Cursor.N_RESIZE_CURSOR));
            cursorMap.put(SOUTH, Cursor.getPredefinedCursor(Cursor.S_RESIZE_CURSOR));
            cursorMap.put(WEST, Cursor.getPredefinedCursor(Cursor.W_RESIZE_CURSOR));
            cursorMap.put(EAST, Cursor.getPredefinedCursor(Cursor.E_RESIZE_CURSOR));
            cursorMap.put(NORTH_WEST, Cursor.getPredefinedCursor(Cursor.NW_RESIZE_CURSOR));
            cursorMap.put(NORTH_EAST, Cursor.getPredefinedCursor(Cursor.NE_RESIZE_CURSOR));
            cursorMap.put(SOUTH_WEST, Cursor.getPredefinedCursor(Cursor.SW_RESIZE_CURSOR));
            cursorMap.put(SOUTH_EAST, Cursor.getPredefinedCursor(Cursor.SE_RESIZE_CURSOR));

            rectangleMap = new HashMap<String, Rectangle2D.Double>();
            rectangleMap.put(DEFAULT, new Rectangle2D.Double());
            rectangleMap.put(MOVE, new Rectangle2D.Double());
            rectangleMap.put(NORTH, new Rectangle2D.Double());
            rectangleMap.put(SOUTH, new Rectangle2D.Double());
            rectangleMap.put(WEST, new Rectangle2D.Double());
            rectangleMap.put(EAST, new Rectangle2D.Double());
            rectangleMap.put(NORTH_EAST, new Rectangle2D.Double());
            rectangleMap.put(NORTH_WEST, new Rectangle2D.Double());
            rectangleMap.put(SOUTH_EAST, new Rectangle2D.Double());
            rectangleMap.put(SOUTH_WEST, new Rectangle2D.Double());
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            updateRectanglesForDragCursor();
            updateCursor(e);
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            updateCursor(e);
        }

        private void updateCursor(MouseEvent e) {
            final boolean cursorOutsideOfSelectionRectangle =
                    !rectangleMap.get(DEFAULT).contains(e.getPoint()) && worldMapPane.getCursor() != cursorMap.get(DEFAULT);
            if (cursorOutsideOfSelectionRectangle) {
                worldMapPane.setCursor(cursorMap.get(DEFAULT));
            } else {
                final String[] regionIdentifiers = {MOVE, NORTH, SOUTH, WEST, EAST, NORTH_WEST, NORTH_EAST, SOUTH_WEST, SOUTH_EAST};
                for (String region : regionIdentifiers) {
                    boolean cursorIsSet = setCursorWhenContained(cursorMap.get(region), rectangleMap.get(region), e.getPoint());
                    if (cursorIsSet) {
                        break;
                    }
                }
            }
        }

        private boolean setCursorWhenContained(Cursor cursor, Rectangle2D.Double rectangle, Point point) {
            if (rectangle.contains(point)) {
                if (worldMapPane.getCursor() != cursor) {
                    worldMapPane.setCursor(cursor);
                }
                return true;
            }
            return false;
        }

        private void updateRectanglesForDragCursor() {
            Rectangle2D.Double rectangleForDragCursor = new Rectangle2D.Double(movableRectangle.getX() + OFFSET,
                    movableRectangle.getY() + OFFSET,
                    movableRectangle.getWidth() - 2 * OFFSET,
                    movableRectangle.getHeight() - 2 * OFFSET);
            rectangleMap.get(MOVE).setRect(rectangleForDragCursor);

            final double x = rectangleForDragCursor.getX();
            final double y = rectangleForDragCursor.getY();
            final double width = rectangleForDragCursor.getWidth();
            final double height = rectangleForDragCursor.getHeight();

            setRectangle(DEFAULT, x - 2 * OFFSET, y - 2 * OFFSET, width + 4 * OFFSET, height + 4 * OFFSET);
            setRectangle(NORTH, x, y - 2 * OFFSET, width, 2 * OFFSET);
            setRectangle(SOUTH, x, y + height, width, 2 * OFFSET);
            setRectangle(WEST, x - 2 * OFFSET, y, 2 * OFFSET, height);
            setRectangle(EAST, x + width, y, 2 * OFFSET, height);
            setRectangle(NORTH_WEST, x - 2 * OFFSET, y - 2 * OFFSET, 2 * OFFSET, 2 * OFFSET);
            setRectangle(NORTH_EAST, x + width, y - 2 * OFFSET, 2 * OFFSET, 2 * OFFSET);
            setRectangle(SOUTH_WEST, x - 2 * OFFSET, y + height, 2 * OFFSET, 2 * OFFSET);
            setRectangle(SOUTH_EAST, x + width, y + height, 2 * OFFSET, 2 * OFFSET);
        }

        private void setRectangle(String rectangleIdentifier, double x, double y, double w, double h) {
            rectangleMap.get(rectangleIdentifier).setRect(x, y, w, h);
        }
    }

    private class FigureEditorAwareWorldMapPane extends WorldMapPane implements FigureEditorAware {

        private FigureEditorAwareWorldMapPane(WorldMapPaneDataModel dataModel, SelectionOverlay overlay) {
            super(dataModel, overlay);
            addScrollListener(new ScrollListener() {
                @Override
                public void scrolled() {
                    final FigureCollection figureCollection = figureEditor.getFigureCollection();
                    if (figureCollection.getFigureCount() == 0) {
                        return;
                    }
                    final Rectangle2D modelBounds = figureCollection.getFigure(0).getBounds();
                    final AffineTransform modelToViewTransform = worldMapPane.getLayerCanvas().getViewport().getModelToViewTransform();
                    final Shape transformedShape = modelToViewTransform.createTransformedShape(modelBounds);
                    movableRectangle.setRect(transformedShape.getBounds2D());
                    selectionRectangle.setRect(movableRectangle);
                    cursorChanger.updateRectanglesForDragCursor();
                }
            });
        }

        @Override
        public FigureEditor getFigureEditor() {
            return figureEditor;
        }

        @Override
        protected Action[] getOverlayActions() {
            final Action[] overlayActions = super.getOverlayActions();
            final Action[] actions = new Action[overlayActions.length + 1];
            System.arraycopy(overlayActions, 0, actions, 0, overlayActions.length);
            actions[actions.length - 1] = new ResetAction();
            return actions;
        }
    }

    private class SelectionOverlay extends BoundaryOverlay {

        private boolean firstTime = true;

        protected SelectionOverlay(WorldMapPaneDataModel dataModel) {
            super(dataModel);
        }

        @Override
        protected void handleSelectedProduct(Rendering rendering, Product selectedProduct) {
            if (firstTime) {
                ShapeFigure shapeFigure = createShapeFigure(selectedProduct);
                figureEditor.getFigureCollection().addFigure(shapeFigure);
                firstTime = false;
            }
            figureEditor.drawFigureCollection(rendering);
        }

        private ShapeFigure createShapeFigure(Product selectedProduct) {
            PixelPos upperLeftPixel = new PixelPos(0.5f, 0.5f);
            PixelPos lowerRightPixel = new PixelPos(
                    selectedProduct.getSceneRasterWidth() - 0.5f, selectedProduct.getSceneRasterHeight() - 0.5f);
            GeoCoding geoCoding = selectedProduct.getGeoCoding();
            GeoPos upperLeftGeoPos = geoCoding.getGeoPos(upperLeftPixel, null);
            GeoPos lowerRightGeoPos = geoCoding.getGeoPos(lowerRightPixel, null);

            Viewport viewport = worldMapPane.getLayerCanvas().getViewport();
            AffineTransform modelToViewTransform = viewport.getModelToViewTransform();
            Point2D.Double lowerRight = modelToView(lowerRightGeoPos, modelToViewTransform);
            Point2D.Double upperLeft = modelToView(upperLeftGeoPos, modelToViewTransform);

            Rectangle2D.Double rectangularShape = new Rectangle2D.Double(upperLeft.x, upperLeft.y,
                    lowerRight.x - upperLeft.x,
                    lowerRight.y - upperLeft.y);
            selectionRectangle = createRectangle(rectangularShape);
            movableRectangle = createRectangle(rectangularShape);
            defaultRectangle = createRectangle(rectangularShape);
            cursorChanger.updateRectanglesForDragCursor();
            defaultShape = viewport.getViewToModelTransform().createTransformedShape(rectangularShape);
            return figureEditor.getFigureFactory().createPolygonFigure(defaultShape, createFigureStyle());
        }

        private Rectangle2D.Double createRectangle(Rectangle2D.Double rectangularShape) {
            return new Rectangle2D.Double(rectangularShape.getX(), rectangularShape.getY(),
                    rectangularShape.getWidth(),rectangularShape.getHeight());
        }

        private Point2D.Double modelToView(GeoPos geoPos, AffineTransform modelToView) {
            Point2D.Double result = new Point2D.Double();
            modelToView.transform(new Point2D.Double(geoPos.getLon(), geoPos.getLat()), result);
            return result;
        }
    }

    private class RegionSelectionInteractor extends ViewportInteractor {

        private static final int NO_LONGITUDE_BORDER = -3;
        private static final int NO_LATITUDE_BORDER = -2;
        private static final int BORDER_UNKNOWN = -1;
        private static final int NORTH_BORDER = 0;
        private static final int EAST_BORDER = 1;
        private static final int SOUTH_BORDER = 2;
        private static final int WEST_BORDER = 3;
        private static final String NORTH_BOUND = "northBound";
        private static final String SOUTH_BOUND = "southBound";
        private static final String WEST_BOUND = "westBound";
        private static final String EAST_BOUND = "eastBound";

        private Point point;
        private int rectangleLongitude;
        private int rectangleLatitude;

        private RegionSelectionInteractor() {
            bindingContext.getPropertySet().getProperty(NORTH_BOUND).addPropertyChangeListener(new BoundsChangeListener(NORTH_BOUND));
            bindingContext.getPropertySet().getProperty(SOUTH_BOUND).addPropertyChangeListener(new BoundsChangeListener(SOUTH_BOUND));
            bindingContext.getPropertySet().getProperty(WEST_BOUND).addPropertyChangeListener(new BoundsChangeListener(WEST_BOUND));
            bindingContext.getPropertySet().getProperty(EAST_BOUND).addPropertyChangeListener(new BoundsChangeListener(EAST_BOUND));
        }

        @Override
        public void mousePressed(MouseEvent event) {
            point = event.getPoint();
            determineDraggedRectangleBorders(event);
        }

        private void determineDraggedRectangleBorders(MouseEvent e) {
            double x = e.getX();
            double y = e.getY();
            double x1 = selectionRectangle.getX();
            double y1 = selectionRectangle.getY();
            double x2 = selectionRectangle.getX() + selectionRectangle.getWidth();
            double y2 = selectionRectangle.getY() + selectionRectangle.getHeight();
            double dx1 = Math.abs(x1 - x);
            double dy1 = Math.abs(y1 - y);
            double dx2 = Math.abs(x2 - x);
            double dy2 = Math.abs(y2 - y);

            rectangleLongitude = BORDER_UNKNOWN;
            if (dx1 <= OFFSET) {
                rectangleLongitude = WEST_BORDER;
            } else if (dx2 <= OFFSET) {
                rectangleLongitude = EAST_BORDER;
            } else if (x >= x1 && x < x2) {
                rectangleLongitude = NO_LONGITUDE_BORDER;
            }

            rectangleLatitude = BORDER_UNKNOWN;
            if (dy1 <= OFFSET) {
                rectangleLatitude = NORTH_BORDER;
            } else if (dy2 <= OFFSET) {
                rectangleLatitude = SOUTH_BORDER;
            } else if (y > y1 && y < y2) {
                rectangleLatitude = NO_LATITUDE_BORDER;
            }
        }

        @Override
        public void mouseDragged(MouseEvent event) {
            double dx = event.getX() - point.getX();
            double dy = point.getY() - event.getY();

            double xOfUpdatedRectangle = selectionRectangle.getX();
            double yOfUpdatedRectangle = selectionRectangle.getY();
            double widthOfUpdatedRectangle = selectionRectangle.getWidth();
            double heightOfUpdatedRectangle = selectionRectangle.getHeight();

            if (rectangleLongitude == NO_LONGITUDE_BORDER && rectangleLatitude == NO_LATITUDE_BORDER) {
                xOfUpdatedRectangle = selectionRectangle.getX() + dx;
                yOfUpdatedRectangle = selectionRectangle.getY() - dy;
            }
            if (rectangleLongitude == WEST_BORDER) {
                xOfUpdatedRectangle += dx;
                widthOfUpdatedRectangle -= dx;
            } else if (rectangleLongitude == EAST_BORDER) {
                widthOfUpdatedRectangle += dx;
            }
            if (rectangleLatitude == NORTH_BORDER) {
                yOfUpdatedRectangle -= dy;
                heightOfUpdatedRectangle += dy;
            } else if (rectangleLatitude == SOUTH_BORDER) {
                heightOfUpdatedRectangle -= dy;
            }

            if (widthOfUpdatedRectangle > 2 && heightOfUpdatedRectangle > 2 &&
                    !(selectionRectangle.getX() == xOfUpdatedRectangle
                            && selectionRectangle.getY() == yOfUpdatedRectangle
                            && selectionRectangle.getWidth() == widthOfUpdatedRectangle
                            && selectionRectangle.getHeight() == heightOfUpdatedRectangle)) {
                setMovableRectangleInImageCoordinates(xOfUpdatedRectangle, yOfUpdatedRectangle,
                        widthOfUpdatedRectangle, heightOfUpdatedRectangle);
                Shape newFigureShape = getViewToModelTransform(event).createTransformedShape(movableRectangle);
                final Rectangle2D modelRectangle = newFigureShape.getBounds2D();

                adaptToModelRectangle(modelRectangle);
            }
        }

        @Override
        public void mouseReleased(MouseEvent event) {
            selectionRectangle.setRect(movableRectangle);
        }

        private void reset() {
            selectionRectangle.setRect(defaultRectangle);
            movableRectangle.setRect(defaultRectangle);
            cursorChanger.updateRectanglesForDragCursor();
            updateProperties(defaultShape.getBounds2D());
            updateFigure(defaultShape.getBounds2D());
            worldMapPane.zoomAll();
        }

        private void updateProperties(Rectangle2D modelRectangle) {
            try {
                bindingContext.getPropertySet().getProperty(NORTH_BOUND).setValue(modelRectangle.getMaxY());
                bindingContext.getPropertySet().getProperty(SOUTH_BOUND).setValue(modelRectangle.getMinY());
                bindingContext.getPropertySet().getProperty(WEST_BOUND).setValue(modelRectangle.getMinX());
                bindingContext.getPropertySet().getProperty(EAST_BOUND).setValue(modelRectangle.getMaxX());
            } catch (ValidationException e) {
                // should never come here
                throw new IllegalStateException(e);
            }
        }

        private void updateFigure(Rectangle2D modelRectangle) {
            Figure newFigure = figureEditor.getFigureFactory().createPolygonFigure(modelRectangle, createFigureStyle());
            figureEditor.getFigureCollection().removeAllFigures();
            figureEditor.getFigureCollection().addFigure(newFigure);
        }

        private void setMovableRectangleInImageCoordinates(double x, double y, double width, double height) {
            movableRectangle.setRect(x, y, width, height);
        }

        private void adaptToModelRectangle(Rectangle2D modelRectangle) {
            correctBoundsIfNecessary(modelRectangle);
            if (modelRectangle.getWidth() != 0 && modelRectangle.getHeight() != 0 &&
                    !modelRectangle.equals(figureEditor.getFigureCollection().getFigure(0).getBounds())) {
                updateFigure(modelRectangle);
                updateProperties(modelRectangle);
            }
        }

        private void correctBoundsIfNecessary(Rectangle2D newFigureShape) {
            double minX = newFigureShape.getMinX();
            double minY = newFigureShape.getMinY();
            double maxX = newFigureShape.getMaxX();
            double maxY = newFigureShape.getMaxY();
            minX = Math.min(maxX, Math.min(180, Math.max(-180, minX)));
            minY = Math.min(maxY, Math.min(90, Math.max(-90, minY)));
            maxX = Math.max(minX, Math.min(180, Math.max(-180, maxX)));
            maxY = Math.max(minY, Math.min(90, Math.max(-90, maxY)));
            minX = Math.min(maxX, Math.min(180, Math.max(-180, minX)));
            minY = Math.min(maxY, Math.min(90, Math.max(-90, minY)));
            if (newFigureShape.getMinX() != minX || newFigureShape.getMinY() != minY
                    || newFigureShape.getMaxX() != maxX || newFigureShape.getMaxY() != maxY) {
                newFigureShape.setRect(minX, minY, maxX - minX, maxY - minY);
            }
        }

        private class BoundsChangeListener implements PropertyChangeListener {

            private final String property;

            private BoundsChangeListener(String property) {
                this.property = property;
            }

            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                final Rectangle2D modelRectangle = figureEditor.getFigureCollection().getFigure(0).getBounds();
                final PropertySet propertySet = bindingContext.getPropertySet();
                double x = (property.equals(WEST_BOUND) ?
                        Double.parseDouble(propertySet.getProperty(WEST_BOUND).getValue().toString()) :
                        modelRectangle.getX());
                double y = (property.equals(SOUTH_BOUND) ?
                        Double.parseDouble(propertySet.getProperty(SOUTH_BOUND).getValue().toString()) :
                        modelRectangle.getY());
                double width = (property.equals(EAST_BOUND) || property.equals(WEST_BOUND) ?
                        Double.parseDouble(propertySet.getProperty(EAST_BOUND).getValue().toString()) - x :
                        modelRectangle.getWidth());
                double height = (property.equals(NORTH_BOUND) || property.equals(SOUTH_BOUND) ?
                        Double.parseDouble(propertySet.getProperty(NORTH_BOUND).getValue().toString()) - y :
                        modelRectangle.getHeight());
                modelRectangle.setRect(x, y, width, height);
                adaptToModelRectangle(modelRectangle);
            }
        }
    }

    private class RegionSelectionDecoratingPanSupport extends WorldMapPane.DefaultPanSupport {

        private Point p0;

        private RegionSelectionDecoratingPanSupport(LayerCanvas layerCanvas) {
            super(layerCanvas);
        }

        @Override
        public void panStarted(MouseEvent event) {
            super.panStarted(event);
            p0 = event.getPoint();
            final Rectangle2D.Double intersectionRectangle = createIntersectionRectangle();
            if (intersectionRectangle.contains(event.getPoint())) {
                regionSelectionInteractor.mousePressed(event);
            }
        }

        @Override
        public void performPan(MouseEvent event) {
            final Rectangle2D.Double intersectionRectangle = createIntersectionRectangle();
            if (intersectionRectangle.contains(p0)) {
                regionSelectionInteractor.mouseDragged(event);
            } else {
                super.performPan(event);
            }
        }

        @Override
        public void panStopped(MouseEvent event) {
            super.panStopped(event);
            updateRectangles();
        }

        private void updateRectangles() {
            AffineTransform modelToView = worldMapPane.getLayerCanvas().getViewport().getModelToViewTransform();
            selectionRectangle = modelToView.createTransformedShape(figureEditor.getFigureCollection().getFigure(0).getBounds()).getBounds2D();
            movableRectangle.setRect(selectionRectangle);
            cursorChanger.updateRectanglesForDragCursor();
        }

        private Rectangle2D.Double createIntersectionRectangle() {
            return new Rectangle2D.Double(selectionRectangle.getX() - OFFSET,
                    selectionRectangle.getY() - OFFSET,
                    selectionRectangle.getWidth() + 2 * OFFSET,
                    selectionRectangle.getHeight() + 2 * OFFSET);
        }

    }

    private class ResetAction extends AbstractAction {

        private ResetAction() {
            putValue(LARGE_ICON_KEY, UIUtils.loadImageIcon("icons/Undo24.gif"));
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            regionSelectionInteractor.reset();
        }
    }

}
