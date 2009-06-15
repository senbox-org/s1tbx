package org.esa.beam.visat.toolviews.nav;

import com.bc.ceres.glayer.support.ImageLayer;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.ui.PixelPositionListener;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.visat.VisatApp;

import javax.swing.JInternalFrame;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import java.awt.Container;
import java.awt.event.MouseEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

class CursorSynchronizer {

    private static final GeoPos INVALID_GEO_POS = new GeoPos(Float.NaN, Float.NaN);
    private static final String SYNC_CURSOR_OVERLAY_ID = "syncCursorOverlay";

    private final VisatApp visatApp;

    private final List<ProductSceneView> psvList;
    private final Map<ProductSceneView, ViewPPL> viewPplMap;
    private PsvListUpdater psvOverlayMapUpdater;
    private boolean enabled;

    CursorSynchronizer(VisatApp visatApp) {
        this.visatApp = visatApp;
        psvList = new ArrayList<ProductSceneView>();
        viewPplMap = new HashMap<ProductSceneView, ViewPPL>();
        psvOverlayMapUpdater = new PsvListUpdater();
        enabled = false;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        if (this.enabled != enabled) {
            if (enabled) {
                initPsvOverlayMap();
                visatApp.addInternalFrameListener(psvOverlayMapUpdater);
            } else {
                visatApp.removeInternalFrameListener(psvOverlayMapUpdater);
                clearPsvOverlayMap();
            }
            this.enabled = enabled;
        }
    }

    private void updateCursorOverlays(ProductSceneView sourceView, GeoPos geoPos) {
        for (ProductSceneView view : psvList) {
            CursorOverlay overlay = new CursorOverlay(view, geoPos);
            if (view != sourceView) {
                view.getLayerCanvas().addOverlay(SYNC_CURSOR_OVERLAY_ID, overlay);
            } else {
                view.getLayerCanvas().removeOverlay(SYNC_CURSOR_OVERLAY_ID);
            }
        }
    }

    private void initPsvOverlayMap() {
        JInternalFrame[] internalFrames = visatApp.getAllInternalFrames();
        for (JInternalFrame internalFrame : internalFrames) {
            Container contentPane = internalFrame.getContentPane();
            if (contentPane instanceof ProductSceneView) {
                ProductSceneView view = (ProductSceneView) contentPane;
                psvList.add(view);
                addPPL(view);
            }
        }
    }

    private void clearPsvOverlayMap() {
        for (ProductSceneView view : psvList) {
            removePPL(view);
            view.getLayerCanvas().removeOverlay(SYNC_CURSOR_OVERLAY_ID);
        }
        psvList.clear();
    }

    private void addPPL(ProductSceneView view) {
        ViewPPL ppl = new ViewPPL(view);
        viewPplMap.put(view, ppl);
        view.addPixelPositionListener(ppl);
    }

    private void removePPL(ProductSceneView view) {
        ViewPPL ppl = viewPplMap.get(view);
        viewPplMap.remove(view);
        view.removePixelPositionListener(ppl);
    }

    private class PsvListUpdater extends InternalFrameAdapter {

        @Override
        public void internalFrameOpened(InternalFrameEvent e) {
            Container contentPane = e.getInternalFrame().getContentPane();
            if (contentPane instanceof ProductSceneView) {
                ProductSceneView view = (ProductSceneView) contentPane;
                psvList.add(view);
                addPPL(view);
            }
        }

        @Override
        public void internalFrameClosed(InternalFrameEvent e) {
            Container contentPane = e.getInternalFrame().getContentPane();
            if (contentPane instanceof ProductSceneView) {
                ProductSceneView view = (ProductSceneView) contentPane;
                removePPL(view);
            }
        }

    }

    private class ViewPPL implements PixelPositionListener {

        private final ProductSceneView view;

        private ViewPPL(ProductSceneView view) {
            this.view = view;
        }

        @Override
        public void pixelPosChanged(ImageLayer baseImageLayer, int pixelX, int pixelY, int currentLevel,
                                    boolean pixelPosValid, MouseEvent e) {
            PixelPos pixelPos = computeLevelZeroPixelPos(baseImageLayer, pixelX, pixelY, currentLevel);
            GeoPos geoPos = view.getRaster().getGeoCoding().getGeoPos(pixelPos, null);
            updateCursorOverlays(view, geoPos);
        }

        private PixelPos computeLevelZeroPixelPos(ImageLayer imageLayer, int pixelX, int pixelY, int currentLevel) {
            if (currentLevel != 0) {
                AffineTransform i2mTransform = imageLayer.getImageToModelTransform(currentLevel);
                Point2D modelP = i2mTransform.transform(new Point2D.Double(pixelX + 0.5, pixelY + 0.5), null);
                AffineTransform m2iTransform = imageLayer.getModelToImageTransform();
                Point2D imageP = m2iTransform.transform(modelP, null);

                return new PixelPos(new Float(imageP.getX()), new Float(imageP.getY()));
            } else {
                return new PixelPos(pixelX + 0.5f, pixelY + 0.5f);
            }
        }

        @Override
        public void pixelPosNotAvailable() {
            updateCursorOverlays(null, INVALID_GEO_POS);
        }
    }
}
