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
import org.esa.beam.framework.ui.BoundaryOverlay;
import org.esa.beam.framework.ui.WorldMapPane;
import org.esa.beam.framework.ui.WorldMapPaneDataModel;

import javax.swing.JPanel;
import java.awt.Color;
import java.awt.Point;
import java.awt.Rectangle;
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
    private DefaultFigureEditor figureEditor;
    private final WorldMapPane worldMapPane;

    public RegionSelectableWorldMapPane(WorldMapPaneDataModel dataModel, BindingContext bindingContext) {
        this.bindingContext = bindingContext;
        worldMapPane = new FigureEditorAwareWorldMapPane(dataModel, new SelectionOverlay(dataModel));
        figureEditor = new DefaultFigureEditor(worldMapPane.getLayerCanvas());
        figureEditor.setInteractor(new RegionSelectionInteractor());
    }

    public JPanel createUI() {
        return worldMapPane;
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
            return figureEditor.getFigureFactory().createPolygonFigure(transformedShape, createFigureStyle());
        }

        private Point2D.Double modelToView(GeoPos geoPos, AffineTransform modelToView) {
            Point2D.Double result = new Point2D.Double();
            modelToView.transform(new Point2D.Double(geoPos.getLon(), geoPos.getLat()), result);
            return result;
        }

        private DefaultFigureStyle createFigureStyle() {
            DefaultFigureStyle figureStyle = (DefaultFigureStyle)figureEditor.getDefaultPolygonStyle();
            figureStyle.setFillOpacity(0);
            figureStyle.setStrokeColor(new Color(200,0,0));
            figureStyle.setStrokeWidth(2);
            return figureStyle;
        }

    }

    private class RegionSelectionInteractor extends ViewportInteractor {

        private Point point;
        private int rectangleSectionX;
        private int rectangleSectionY;
        private int HANDLE_SIZE = 6;

        @Override
        public void mousePressed(MouseEvent event) {
            point = event.getPoint();
            Rectangle2D figurebounds = figureEditor.getFigureCollection().getFigure(0).getBounds();
            computeSliderSections(event, figurebounds);
        }

        private void computeSliderSections(MouseEvent e, Rectangle2D figureBounds) {

            double x = e.getX();
            double y = e.getY();
            double x1 = figureBounds.getX();
            double y1 = figureBounds.getY();
            double x2 = figureBounds.getX() + figureBounds.getWidth();
            double y2 = figureBounds.getY() + figureBounds.getHeight();
            double dx1 = Math.abs(x1 - x);
            double dy1 = Math.abs(y1 - y);
            double dx2 = Math.abs(x2 - x);
            double dy2 = Math.abs(y2 - y);

            rectangleSectionX = -1;
            if (dx1 <= HANDLE_SIZE) {
                rectangleSectionX = 0;   // left rectangle handle selected
            } else if (dx2 <= HANDLE_SIZE) {
                rectangleSectionX = 2;   // right rectangle handle selected
            } else if (x >= x1 && x < x2) {
                rectangleSectionX = 1;   // center rectangle handle selected
            }

            rectangleSectionY = -1;
            if (dy1 <= HANDLE_SIZE) {
                rectangleSectionY = 0; // upper rectangle handle selected
            } else if (dy2 <= HANDLE_SIZE) {
                rectangleSectionY = 2; // lower rectangle handle selected
            } else if (y > y1 && y < y2) {
                rectangleSectionY = 1; // center rectangle handle selected
            }
        }

        private void modifySliderBox(MouseEvent e) {
            int x = e.getX();
            int y = e.getY();
            int dx = x - point.x;
            int dy = y - point.y;

            Rectangle figureBounds = figureEditor.getFigureCollection().getFigure(0).getBounds().getBounds();

            int sbx = 0;
            int sby = 0;
            int sbw = 0;
            int sbh = 0;
            boolean validMode = false;

            if (rectangleSectionX == 0 && rectangleSectionY == 0) {
                sbx = figureBounds.x + dx;
                sby = figureBounds.y + dy;
                sbw = figureBounds.width - dx;
                sbh = figureBounds.height - dy;
                validMode = true;
            } else if (rectangleSectionX == 1 && rectangleSectionY == 0) {
                sbx = figureBounds.x;
                sby = figureBounds.y + dy;
                sbw = figureBounds.width;
                sbh = figureBounds.height - dy;
                validMode = true;
            } else if (rectangleSectionX == 2 && rectangleSectionY == 0) {
                sbx = figureBounds.x;
                sby = figureBounds.y + dy;
                sbw = figureBounds.width + dx;
                sbh = figureBounds.height - dy;
                validMode = true;
            } else if (rectangleSectionX == 0 && rectangleSectionY == 1) {
                sbx = figureBounds.x + dx;
                sby = figureBounds.y;
                sbw = figureBounds.width - dx;
                sbh = figureBounds.height;
                validMode = true;
            } else if (rectangleSectionX == 1 && rectangleSectionY == 1) {
                sbx = figureBounds.x + dx;
                sby = figureBounds.y + dy;
                sbw = figureBounds.width;
                sbh = figureBounds.height;
                validMode = true;
            } else if (rectangleSectionX == 2 && rectangleSectionY == 1) {
                sbx = figureBounds.x;
                sby = figureBounds.y;
                sbw = figureBounds.width + dx;
                sbh = figureBounds.height;
                validMode = true;
            } else if (rectangleSectionX == 0 && rectangleSectionY == 2) {
                sbx = figureBounds.x + dx;
                sby = figureBounds.y;
                sbw = figureBounds.width - dx;
                sbh = figureBounds.height + dy;
                validMode = true;
            } else if (rectangleSectionX == 1 && rectangleSectionY == 2) {
                sbx = figureBounds.x;
                sby = figureBounds.y;
                sbw = figureBounds.width;
                sbh = figureBounds.height + dy;
                validMode = true;
            } else if (rectangleSectionX == 2 && rectangleSectionY == 2) {
                sbx = figureBounds.x;
                sby = figureBounds.y;
                sbw = figureBounds.width + dx;
                sbh = figureBounds.height + dy;
                validMode = true;
            }

            if (validMode && sbw > 2 && sbh > 2) {
                setSliderBoxBounds(sbx, sby, sbw, sbh, true);
            }
        }

        private void setSliderBoxBounds(int x, int y, int width, int height, boolean fireEvent) {
            Rectangle figureBounds = figureEditor.getFigureCollection().getFigure(0).getBounds().getBounds();
            if (figureBounds.getX() == x
                    && figureBounds.getY() == y
                    && figureBounds.getWidth() == width
                    && figureBounds.getHeight() == height) {
                return;
            }
            DefaultFigureStyle figureStyle = (DefaultFigureStyle)figureEditor.getDefaultPolygonStyle();
            figureStyle.setFillOpacity(0);
            figureStyle.setStrokeColor(new Color(200,0,0));
            figureStyle.setStrokeWidth(2);
            if(rectangleSectionX == 1 && rectangleSectionY == 1) {
                figureEditor.getFigureCollection().getFigure(0).move(x-figureBounds.x, y-figureBounds.y);
            }
            else {
                Figure newFigure = figureEditor.getFigureFactory().createPolygonFigure(new Rectangle(x, y, width, height), figureStyle);
                figureEditor.getFigureCollection().removeAllFigures();
                figureEditor.getFigureCollection().addFigure(newFigure);
            }
//            if (sliderBoxChangeListener != null && fireEvent) {
//                sliderBoxChangeListener.sliderBoxChanged(sliderBox.getBounds());
//            }
        }

        @Override
        public void mouseDragged(MouseEvent event) {
            modifySliderBox(event);
        }
    }
}
