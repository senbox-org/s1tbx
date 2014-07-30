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

package org.esa.beam.visat.actions.rangefinder;

import com.bc.ceres.glayer.swing.LayerCanvas;
import com.bc.ceres.grender.Rendering;
import com.bc.ceres.grender.Viewport;
import com.bc.ceres.swing.figure.ViewportInteractor;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.dataop.maptransf.Ellipsoid;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.util.math.MathUtils;
import org.esa.beam.visat.VisatApp;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingUtilities;
import java.awt.BasicStroke;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Stroke;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

/**
 * A tool representing the range finder.
 *
 * @author Sabine Embacher
 * @author Ralf Quast
 * @author Marco Zuehlke
 */
public class RangeFinderInteractor extends ViewportInteractor {

    public static final String TITLE = "Range Finder Tool";

    private static class ModelPoint extends Point2D.Double {

        private static ModelPoint create(Viewport viewport, Point point) {
            return new ModelPoint(viewport.getViewToModelTransform().transform(point, new Point2D.Double()));
        }

        private ModelPoint() {
            super();
        }

        private ModelPoint(Point2D p) {
            super(p.getX(), p.getY());
        }

        private Point toViewPoint(Viewport viewport) {
            return (Point) viewport.getModelToViewTransform().transform(this, new Point());
        }
    }

    private final List<ModelPoint> modelPointList;
    private final ModelPoint currentModelPoint;
    private final Cursor cursor;

    private RangeFinderOverlay overlay;

    public RangeFinderInteractor() {
        modelPointList = new ArrayList<>();
        currentModelPoint = new ModelPoint();
        ImageIcon cursorIcon = UIUtils.loadImageIcon("cursors/RangeFinder.gif");
        cursor = createRangeFinderCursor(cursorIcon);
    }

    @Override
    public Cursor getCursor() {
        return cursor;
    }

    @Override
    public void mouseDragged(MouseEvent e) {
        handleDragAndMove(e);
    }

    @Override
    public void mouseMoved(MouseEvent e) {
        handleDragAndMove(e);
    }

    @Override
    public void mouseClicked(MouseEvent e) {
        final ProductSceneView view = getProductSceneView(e);
        if (view == null) {
            return;
        }
        if (overlay != null && view != overlay.view) {
            removeOverlay();
        }
        if (overlay == null) {
            createOverlay(view);
        }
        if (e.getClickCount() == 1) {
            final Point viewPoint = e.getPoint();
            final ModelPoint modelPoint = ModelPoint.create(view.getViewport(), viewPoint);
            modelPointList.add(modelPoint);
            currentModelPoint.setLocation(modelPoint);
            overlay.repaint();
        }
        if (e.getClickCount() == 2 && modelPointList.size() > 0) {
            showDetailsDialog(view);
            modelPointList.clear();
            removeOverlay();
        }
    }

    private void handleDragAndMove(MouseEvent e) {
        if (modelPointList.size() > 0 && overlay != null) {
            final ProductSceneView view = getProductSceneView(e);
            if (view != null) {
                final Point viewPoint = e.getPoint();
                final ModelPoint modelPoint = ModelPoint.create(view.getViewport(), viewPoint);
                currentModelPoint.setLocation(modelPoint);
                overlay.repaint();
            }
        }
    }

    private void createOverlay(ProductSceneView view) {
        overlay = new RangeFinderOverlay(view);
        view.getLayerCanvas().addOverlay(overlay);
    }

    private void removeOverlay() {
        overlay.view.getLayerCanvas().removeOverlay(overlay);
        overlay = null;
    }

    private ProductSceneView getProductSceneView(MouseEvent event) {
        final Component eventComponent = event.getComponent();
        if (eventComponent instanceof ProductSceneView) {
            return (ProductSceneView) eventComponent;
        }
        final Container parentComponent = eventComponent.getParent();
        if (parentComponent instanceof ProductSceneView) {
            return (ProductSceneView) parentComponent;
        }
        // Case: Scroll bars are displayed
        if (parentComponent.getParent() instanceof ProductSceneView) {
            return (ProductSceneView) parentComponent.getParent();
        }
        return null;
    }

    private void showDetailsDialog(ProductSceneView view) {
        GeoCoding geoCoding = view.getProduct().getGeoCoding();

        float distance = 0;
        float distanceError = 0;
        final AffineTransform m2i = view.getBaseImageLayer().getModelToImageTransform();

        final Point imagePoint1 = new Point();
        final Point imagePoint2 = new Point();
        final DistanceData[] distanceData = new DistanceData[modelPointList.size() - 1];
        for (int i = 0; i < distanceData.length; i++) {
            m2i.transform(modelPointList.get(i), imagePoint1);
            m2i.transform(modelPointList.get(i + 1), imagePoint2);

            final DistanceData segmentData = new DistanceData(geoCoding, imagePoint1, imagePoint2);
            distance += segmentData.distance;
            distanceError += segmentData.distanceError;
            distanceData[i] = segmentData;
        }


        final JButton detailsButton = new JButton("Details...");
        detailsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final Window parentWindow = SwingUtilities.getWindowAncestor(detailsButton);
                createDetailsDialog(parentWindow, distanceData).show();
            }
        });

        final JPanel buttonPane = new JPanel(new BorderLayout());
        buttonPane.add(detailsButton, BorderLayout.EAST);

        final JPanel messagePane = new JPanel(new BorderLayout(0, 6));
        messagePane.add(new JLabel("Distance: " + distance + " +/- " + distanceError + " km"));
        messagePane.add(buttonPane, BorderLayout.SOUTH);

        JOptionPane.showMessageDialog(VisatApp.getApp().getMainFrame(), messagePane,
                                      TITLE,
                                      JOptionPane.INFORMATION_MESSAGE);
    }

    private static ModalDialog createDetailsDialog(final Window parentWindow, final DistanceData[] dds) {
        float distance = 0;
        float distanceError = 0;

        final StringBuilder message = new StringBuilder();
        for (int i = 0; i < dds.length; i++) {
            final DistanceData dd = dds[i];
            distance += dd.distance;
            distanceError += dd.distanceError;
            message.append(
                    "Distance between points " + i + " to " + (i + 1) + " in pixels:\n" +
                    "XH[" + dd.xH + "] to XN[" + dd.xN + "]: " + Math.abs(dd.xH - dd.xN) + "\n" +
                    "YH[" + dd.yH + "] to YN[" + dd.yN + "]: " + Math.abs(dd.yH - dd.yN) + "\n" +
                    "\n" +
                    "LonH: " + dd.lonH + "   LatH: " + dd.latH + "\n" +
                    "LonN: " + dd.lonN + "   LatN: " + dd.latN + "\n" +
                    "\n" +
                    "LamH: " + dd.lamH + "   PhiH: " + dd.phiH + "\n" +
                    "LamN: " + dd.lamN + "   PhiN: " + dd.phiN + "\n" +
                    "\n" +
                    "Mean earth radius used: " + DistanceData.MEAN_EARTH_RADIUS_KM + " km" + "\n" +
                    "\n" +
                    "Distance: " + dd.distance + " +/- " + dd.distanceError + " km\n" +
                    "\n\n"           /*I18N*/
            );
        }
        message.insert(0, "Total distance: " + distance + " +/- " + distanceError + " km\n" +
                          "\n" +
                          "computed as described below:\n\n");

        final JScrollPane content = new JScrollPane(new JTextArea(message.toString()));
        content.setPreferredSize(new Dimension(300, 150));

        final ModalDialog detailsWindow = new ModalDialog(parentWindow,
                                                          TITLE + " - Details",
                                                          /*I18N*/
                                                          ModalDialog.ID_OK,
                                                          null);
        detailsWindow.setContent(content);
        return detailsWindow;
    }

    private static Cursor createRangeFinderCursor(ImageIcon cursorIcon) {
        Toolkit defaultToolkit = Toolkit.getDefaultToolkit();
        final String cursorName = "rangeFinder";

        // this is necessary because on some systems the cursor is scaled but not the 'hot spot'
        Dimension bestCursorSize = defaultToolkit.getBestCursorSize(cursorIcon.getIconWidth(),
                                                                    cursorIcon.getIconHeight());
        Point hotSpot = new Point((7 * bestCursorSize.width) / cursorIcon.getIconWidth(),
                                  (7 * bestCursorSize.height) / cursorIcon.getIconHeight());

        return defaultToolkit.createCustomCursor(cursorIcon.getImage(), hotSpot, cursorName);
    }

    private class RangeFinderOverlay implements LayerCanvas.Overlay {

        private final ProductSceneView view;

        RangeFinderOverlay(ProductSceneView view) {
            this.view = view;
        }

        void repaint() {
            view.getLayerCanvas().repaint();
        }

        @Override
        public void paintOverlay(LayerCanvas canvas, Rendering rendering) {
            if (modelPointList.size() == 0) {
                return;
            }
            Graphics2D g2d = rendering.getGraphics();
            final Stroke strokeOld = g2d.getStroke();
            g2d.setStroke(new BasicStroke(0.1f));
            final Color colorOld = g2d.getColor();
            g2d.setColor(Color.red);
            g2d.translate(0.5, 0.5);

            final int r = 3;
            final int r2 = r * 2;

            Point viewPoint1 = null;
            Point viewPoint2 = null;
            final Viewport viewport = canvas.getViewport();
            for (final ModelPoint modelPoint : modelPointList) {
                viewPoint1 = modelPoint.toViewPoint(viewport);

                g2d.drawOval(viewPoint1.x - r, viewPoint1.y - r, r2, r2);
                g2d.drawLine(viewPoint1.x, viewPoint1.y - r2, viewPoint1.x, viewPoint1.y - r);
                g2d.drawLine(viewPoint1.x, viewPoint1.y + r2, viewPoint1.x, viewPoint1.y + r);
                g2d.drawLine(viewPoint1.x - r2, viewPoint1.y, viewPoint1.x - r, viewPoint1.y);
                g2d.drawLine(viewPoint1.x + r2, viewPoint1.y, viewPoint1.x + r, viewPoint1.y);

                if (viewPoint2 != null) {
                    g2d.drawLine(viewPoint1.x, viewPoint1.y, viewPoint2.x, viewPoint2.y);
                }

                viewPoint2 = viewPoint1;
            }
            if (viewPoint1 != null) {
                viewPoint2 = currentModelPoint.toViewPoint(canvas.getViewport());
                g2d.drawLine(viewPoint1.x, viewPoint1.y, viewPoint2.x, viewPoint2.y);
            }

            g2d.translate(-0.5, -0.5);
            g2d.setStroke(strokeOld);
            g2d.setColor(colorOld);
        }
    }

    private static class DistanceData {

        final static float MIN_EARTH_RADIUS = (float) Ellipsoid.WGS_84.getSemiMinor();
        final static float MAX_EARTH_RADIUS = (float) Ellipsoid.WGS_84.getSemiMajor();
        final static float MEAN_EARTH_RADIUS_M = 6371000;
        final static float MEAN_EARTH_RADIUS_KM = MEAN_EARTH_RADIUS_M * 0.001f;
        final static float MEAN_ERROR_FACTOR = MIN_EARTH_RADIUS / MAX_EARTH_RADIUS;

        final int xH;
        final int yH;
        final int xN;
        final int yN;
        final float lonH;
        final float latH;
        final float lonN;
        final float latN;
        final float lamH;
        final float phiH;
        final float lamN;
        final float phiN;
        final float distance;
        final float distanceError;

        public DistanceData(GeoCoding geoCoding, final Point pH, final Point pN) {
            this.xH = pH.x;
            this.yH = pH.y;
            this.xN = pN.x;
            this.yN = pN.y;
            final GeoPos geoPosH = geoCoding.getGeoPos(new PixelPos(xH, yH), null);
            final GeoPos geoPosN = geoCoding.getGeoPos(new PixelPos(xN, yN), null);
            this.lonH = geoPosH.getLon();
            this.latH = geoPosH.getLat();
            this.lonN = geoPosN.getLon();
            this.latN = geoPosN.getLat();
            this.lamH = (float) (MathUtils.DTOR * lonH);
            this.phiH = (float) (MathUtils.DTOR * latH);
            this.lamN = (float) (MathUtils.DTOR * lonN);
            this.phiN = (float) (MathUtils.DTOR * latN);
            this.distance = (float) MathUtils.sphereDistance(MEAN_EARTH_RADIUS_KM, lamH, phiH, lamN, phiN);
            this.distanceError = distance * (1 - MEAN_ERROR_FACTOR);
        }
    }
}