package org.esa.beam.smos.visat;

import com.bc.ceres.core.Assert;
import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.glevel.MultiLevelImage;
import org.esa.beam.dataio.smos.SmosFile;
import org.esa.beam.dataio.smos.SmosProductReader;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.ui.PixelPositionListener;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.visat.VisatApp;

import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import java.awt.Container;
import java.awt.Rectangle;
import java.awt.event.MouseEvent;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.util.ArrayList;
import java.util.List;

public class SceneViewSelectionService {
    private final VisatApp visatApp;
    private final List<SelectionListener> selectionListeners;
    private final IFL ifl;
    private PPL ppl;

    private ProductSceneView selectedSceneView;

    public SceneViewSelectionService(VisatApp visatApp) {
        ifl = new IFL();
        ppl = new PPL();
        this.selectionListeners = new ArrayList<SelectionListener>();
        this.visatApp = visatApp;
        this.visatApp.addInternalFrameListener(ifl);
    }

    public void stop() {
        selectionListeners.clear();
        visatApp.removeInternalFrameListener(ifl);
    }

    public ProductSceneView getSelectedSceneView() {
        return selectedSceneView;
    }

    private void setSelectedSceneView(ProductSceneView view) {
        ProductSceneView oldView = selectedSceneView;
        if (oldView != null) {
            oldView.removePixelPositionListener(ppl);
        }
        if (oldView != view) {
            if (view != null) {
                Assert.argument(view.getProduct().getProductReader() instanceof SmosProductReader, "view");
            }
            selectedSceneView = view;
            fireSelectionChange(oldView, selectedSceneView);
            if (selectedSceneView != null) {
                selectedSceneView.addPixelPositionListener(ppl);
            }
        }
    }

    public Product getSelectedSmosProduct() {
        ProductSceneView sceneView = getSelectedSceneView();
        return sceneView != null ? sceneView.getProduct() : null;
    }

    public SmosFile getSelectedSmosFile() {
        Product product = getSelectedSmosProduct();
        if (product != null) {
            ProductReader productReader = product.getProductReader();
            Assert.state(productReader instanceof SmosProductReader, "productReader instanceof SmosProductReader");
            return ((SmosProductReader) productReader).getSmosFile();
        } else {
            return null;
        }
    }

    public int getGridPointId(int pixelX, int pixelY) {
        return getGridPointId(pixelX, pixelY, 0);
    }

    public int getGridPointId(int pixelX, int pixelY, int currentLevel) {
        Band gridPointIdBand = getSelectedSmosProduct().getBandAt(0); // Convention! Grid_Point_ID is always first!
        MultiLevelImage levelImage = (MultiLevelImage) gridPointIdBand.getSourceImage();
        RenderedImage image = levelImage.getImage(currentLevel);
        Raster data = image.getData(new Rectangle(pixelX, pixelY, 1, 1));
        return data.getSample(pixelX, pixelY, 0);
    }

    public synchronized void addSceneViewSelectionListener(SelectionListener selectionListener) {
        selectionListeners.add(selectionListener);
    }

    public synchronized void removeSceneViewSelectionListener(SelectionListener selectionListener) {
        selectionListeners.remove(selectionListener);
    }

    private void setView(ProductSceneView view) {
        if (selectedSceneView != view) {
            if (view != null) {
                if (view.getProduct().getProductReader() instanceof SmosProductReader) {
                    setSelectedSceneView(view);
                } else {
                    setSelectedSceneView(null);
                }
            } else {
                setSelectedSceneView(null);
            }
        }
    }

    private void fireSelectionChange(ProductSceneView oldView, ProductSceneView newView) {
        for (SelectionListener selectionListener : selectionListeners) {
            selectionListener.handleSceneViewSelectionChanged(oldView, newView);
        }
    }

    public interface SelectionListener {
        void handleSceneViewSelectionChanged(ProductSceneView oldView, ProductSceneView newView);
    }

    private class IFL extends InternalFrameAdapter {

        @Override
        public void internalFrameActivated(final InternalFrameEvent e) {
            final ProductSceneView view = getProductSceneViewByFrame(e);
            setView(view);
        }

        @Override
        public void internalFrameDeactivated(final InternalFrameEvent e) {
            if (getSelectedSceneView() == getProductSceneViewByFrame(e)) {
                setView(null);
            }
        }

        private ProductSceneView getProductSceneViewByFrame(final InternalFrameEvent e) {
            final Container content = getContent(e);
            if (content instanceof ProductSceneView) {
                return (ProductSceneView) content;
            } else {
                return null;
            }
        }

        private Container getContent(InternalFrameEvent e) {
            return e.getInternalFrame().getContentPane();
        }
    }

    private class PPL implements PixelPositionListener {
        @Override
        public void pixelPosChanged(ImageLayer baseImageLayer, int pixelX, int pixelY, int currentLevel, boolean pixelPosValid, MouseEvent e) {
            int seqnum = -1;
            if (pixelPosValid) {
                seqnum = getGridPointId(pixelX, pixelY, currentLevel);
            }
            SmosBox.getInstance().getGridPointSelectionService().setSelectedGridPointId(seqnum);
        }

        @Override
        public void pixelPosNotAvailable() {
            SmosBox.getInstance().getGridPointSelectionService().setSelectedGridPointId(-1);
        }
    }
}
