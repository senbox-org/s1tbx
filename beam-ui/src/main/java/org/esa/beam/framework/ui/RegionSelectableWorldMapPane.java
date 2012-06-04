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

import com.bc.ceres.glayer.swing.LayerCanvas;
import com.bc.ceres.grender.Rendering;
import com.bc.ceres.grender.Viewport;
import com.bc.ceres.swing.binding.BindingContext;
import com.bc.ceres.swing.figure.Figure;
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

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Point;
import java.awt.Shape;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

/**
 * This class wraps a {@link WorldMapPane} and extends it by functionality to draw and resize a selection rectangle.
 *
 * @author Thomas Storm
 * @author Tonio Fincke
 */
public class RegionSelectableWorldMapPane {

    private final BindingContext bindingContext;
    private final DefaultFigureEditor figureEditor;
    private final WorldMapPane worldMapPane;
    private final RegionSelectionInteractor regionSelectionInteractor;

    private Rectangle2D selectionRectangle;

    public RegionSelectableWorldMapPane(WorldMapPaneDataModel dataModel, BindingContext bindingContext) {
        this.bindingContext = bindingContext;
        worldMapPane = new FigureEditorAwareWorldMapPane(dataModel, new SelectionOverlay(dataModel));
        worldMapPane.setPanSupport(new RegionSelectionDecoratingPanSupport(worldMapPane.getLayerCanvas()));
        figureEditor = new DefaultFigureEditor(worldMapPane.getLayerCanvas());
        regionSelectionInteractor = new RegionSelectionInteractor();
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

    private class FigureEditorAwareWorldMapPane extends WorldMapPane implements FigureEditorAware {

        private FigureEditorAwareWorldMapPane(WorldMapPaneDataModel dataModel, SelectionOverlay overlay) {
            super(dataModel, overlay);
        }

        @Override
        public FigureEditor getFigureEditor() {
            return figureEditor;
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
            PixelPos lowerRightPixel = new PixelPos(selectedProduct.getSceneRasterWidth() - 0.5f, selectedProduct.getSceneRasterHeight() - 0.5f);
            GeoCoding geoCoding = selectedProduct.getGeoCoding();
            GeoPos upperLeftGeoPos = geoCoding.getGeoPos(upperLeftPixel, null);
            GeoPos lowerRightGeoPos = geoCoding.getGeoPos(lowerRightPixel, null);

            Viewport viewport = worldMapPane.getLayerCanvas().getViewport();
            AffineTransform modelToViewTransform = viewport.getModelToViewTransform();
            Point2D.Double lowerRight = modelToView(lowerRightGeoPos, modelToViewTransform);
            Point2D.Double upperLeft = modelToView(upperLeftGeoPos, modelToViewTransform);

            Rectangle2D.Double rectangularShape = new Rectangle2D.Double(upperLeft.x, upperLeft.y, lowerRight.x - upperLeft.x, lowerRight.y - upperLeft.y);
            Shape transformedShape = viewport.getViewToModelTransform().createTransformedShape(rectangularShape);
            if (selectionRectangle == null) {
                selectionRectangle = rectangularShape;
            }
            return figureEditor.getFigureFactory().createPolygonFigure(transformedShape, createFigureStyle());
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

        private Point point;
        private int rectangleLongitude;
        private int rectangleLatitude;
        private static final int HANDLE_SIZE = 6;

        @Override
        public void mousePressed(MouseEvent event) {
            point = event.getPoint();
            AffineTransform modelToViewTransform = getModelToViewTransform(event);
            selectionRectangle = modelToViewTransform.createTransformedShape(figureEditor.getFigureCollection().getFigure(0).getBounds()).getBounds2D();
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
            if (dx1 <= HANDLE_SIZE) {
                rectangleLongitude = WEST_BORDER;
            } else if (dx2 <= HANDLE_SIZE) {
                rectangleLongitude = EAST_BORDER;
            } else if (x >= x1 && x < x2) {
                rectangleLongitude = NO_LONGITUDE_BORDER;
            }

            rectangleLatitude = BORDER_UNKNOWN;
            if (dy1 <= HANDLE_SIZE) {
                rectangleLatitude = NORTH_BORDER;
            } else if (dy2 <= HANDLE_SIZE) {
                rectangleLatitude = SOUTH_BORDER;
            } else if (y > y1 && y < y2) {
                rectangleLatitude = NO_LATITUDE_BORDER;
            }
        }

        @Override
        public void mouseDragged(MouseEvent event) {
            modifySelectionRectangle(event);
        }

        private void modifySelectionRectangle(MouseEvent event) {
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
                setSelectionRectangleBounds(xOfUpdatedRectangle, yOfUpdatedRectangle, widthOfUpdatedRectangle,
                        heightOfUpdatedRectangle, getViewToModelTransform(event));
            }
        }

        private void setSelectionRectangleBounds(double x, double y, double width, double height, AffineTransform transform) {
            Shape newFigureShape = transform.createTransformedShape(new Rectangle2D.Double(x, y, width, height));
            Figure newFigure = figureEditor.getFigureFactory().createPolygonFigure(newFigureShape, createFigureStyle());
            figureEditor.getFigureCollection().removeAllFigures();
            figureEditor.getFigureCollection().addFigure(newFigure);
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
            AffineTransform modelToView = worldMapPane.getLayerCanvas().getViewport().getModelToViewTransform();
            selectionRectangle = modelToView.createTransformedShape(figureEditor.getFigureCollection().getFigure(0).getBounds()).getBounds2D();
            if (selectionRectangle.contains(event.getPoint())) {
                regionSelectionInteractor.mousePressed(event);
            }
        }

        @Override
        public void performPan(MouseEvent event) {
            if (selectionRectangle.contains(p0)) {
                regionSelectionInteractor.mouseDragged(event);
            } else {
                super.performPan(event);
            }
        }
    }
}
