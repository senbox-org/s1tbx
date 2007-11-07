package org.esa.beam.visat.toolviews.pixelinfo;

import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.ui.PixelInfoView;
import org.esa.beam.framework.ui.PixelPositionListener;
import org.esa.beam.framework.ui.application.support.AbstractToolView;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.util.PropertyMap;
import org.esa.beam.util.math.MathUtils;
import org.esa.beam.visat.VisatApp;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JInternalFrame;
import javax.swing.JPanel;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.image.RenderedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;

/**
 * The tool window which displays the pixel info view.
 */
public class PixelInfoToolView extends AbstractToolView {
    public static final String ID = PixelInfoToolView.class.getName();

    private PixelInfoView _pixelInfoView;
    private JCheckBox _pinCheckbox;
    private PinSelectionChangedListener _pinSelectionChangedListener;
    private ProductSceneView _currentView;
    private HashMap<ProductSceneView, PixelInfoPPL> _pixelPosListeners;

    public PixelInfoToolView() {
    }

    public JComponent createControl() {
        _pinCheckbox = new JCheckBox("Snap to selected pin");
        _pinCheckbox.setName("pinCheckbox");
        _pinCheckbox.setSelected(false);
        _pinCheckbox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (_pinCheckbox.isSelected()) {
                    _currentView = VisatApp.getApp().getSelectedProductSceneView();
                    setToSelectedPin(_currentView);
                }
            }
        });

        _pixelInfoView = new PixelInfoView();
        final DisplayFilter bandDisplayValidator = new DisplayFilter(VisatApp.getApp());
        _pixelInfoView.setPreferredSize(new Dimension(320, 480));
        _pixelInfoView.setDisplayFilter(bandDisplayValidator);
        final PropertyMap preferences = VisatApp.getApp().getPreferences();
        preferences.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                final String propertyName = evt.getPropertyName();
                if (PixelInfoView.PROPERTY_KEY_SHOW_ONLY_LOADED_OR_DISPLAYED_BAND_PIXEL_VALUES.equals(propertyName)) {
                    setShowOnlyLoadedBands(preferences, bandDisplayValidator);
                } else if (VisatApp.PROPERTY_KEY_PIXEL_OFFSET_FOR_DISPLAY_SHOW_DECIMALS.equals(propertyName)) {
                    setShowPixelPosDecimals(preferences);
                } else if (VisatApp.PROPERTY_KEY_PIXEL_OFFSET_FOR_DISPLAY_X.equals(propertyName)) {
                    setPixelOffsetX(preferences);
                } else if (VisatApp.PROPERTY_KEY_PIXEL_OFFSET_FOR_DISPLAY_Y.equals(propertyName)) {
                    setPixelOffsetY(preferences);
                }
            }
        });
        setShowOnlyLoadedBands(preferences, bandDisplayValidator);
        setShowPixelPosDecimals(preferences);
        setPixelOffsetX(preferences);
        setPixelOffsetY(preferences);

        final JPanel pixelInfoViewPanel = new JPanel(new BorderLayout());
        pixelInfoViewPanel.add(_pixelInfoView);
        pixelInfoViewPanel.add(_pinCheckbox, BorderLayout.SOUTH);

        VisatApp.getApp().addInternalFrameListener(new PixelInfoIFL());
        initOpenedFrames();

        return pixelInfoViewPanel;
    }

    private void initOpenedFrames() {
        JInternalFrame[] internalFrames = VisatApp.getApp().getAllInternalFrames();
        for (JInternalFrame internalFrame : internalFrames) {
            Container contentPane = internalFrame.getContentPane();
            if (contentPane instanceof ProductSceneView) {
                initView((ProductSceneView) contentPane);
            }
        }
    }

    private void initView(ProductSceneView productSceneView) {
        productSceneView.addPixelPositionListener(registerPPL(productSceneView));
        final Product product = productSceneView.getProduct();
        product.addProductNodeListener(getOrCreatePinSelectionChangedListener());
        if (isSnapToPin()) {
            setToSelectedPin(productSceneView);
        }
        if (productSceneView == VisatApp.getApp().getSelectedProductSceneView()) {
            _currentView = productSceneView;
        }
    }

    private void setPixelOffsetY(final PropertyMap preferences) {
        _pixelInfoView.setPixelOffsetY((float) preferences.getPropertyDouble(
                VisatApp.PROPERTY_KEY_PIXEL_OFFSET_FOR_DISPLAY_Y,
                VisatApp.PROPERTY_DEFAULT_PIXEL_OFFSET_FOR_DISPLAY));
    }

    private void setPixelOffsetX(final PropertyMap preferences) {
        _pixelInfoView.setPixelOffsetX((float) preferences.getPropertyDouble(
                VisatApp.PROPERTY_KEY_PIXEL_OFFSET_FOR_DISPLAY_X,
                VisatApp.PROPERTY_DEFAULT_PIXEL_OFFSET_FOR_DISPLAY));
    }

    private void setShowPixelPosDecimals(final PropertyMap preferences) {
        _pixelInfoView.setShowPixelPosDecimals(preferences.getPropertyBool(
                VisatApp.PROPERTY_KEY_PIXEL_OFFSET_FOR_DISPLAY_SHOW_DECIMALS,
                VisatApp.PROPERTY_DEFAULT_PIXEL_OFFSET_FOR_DISPLAY_SHOW_DECIMALS));
    }

    private void setShowOnlyLoadedBands(final PropertyMap preferences, DisplayFilter validator) {
        final boolean showOnlyLoadedOrDisplayedBands = preferences.getPropertyBool(
                PixelInfoView.PROPERTY_KEY_SHOW_ONLY_LOADED_OR_DISPLAYED_BAND_PIXEL_VALUES,
                PixelInfoView.PROPERTY_DEFAULT_SHOW_ONLY_LOADED_OR_DISPLAYED_BAND_PIXEL_VALUES);
        validator.setShowOnlyLoadedOrDisplayedBands(showOnlyLoadedOrDisplayedBands);
    }

    private PinSelectionChangedListener getOrCreatePinSelectionChangedListener() {
        if (_pinSelectionChangedListener == null) {
            _pinSelectionChangedListener = new PinSelectionChangedListener();
        }
        return _pinSelectionChangedListener;
    }

    private boolean isSnapToPin() {
        return _pinCheckbox.isSelected();
    }

    private void setToSelectedPin(ProductSceneView sceneView) {
        if (sceneView != null) {
            final Product product = sceneView.getProduct();
            final Pin pin = product.getPinGroup().getSelectedNode();
            if (pin == null) {
                _pixelInfoView.updatePixelValues(sceneView, -1, -1, false);
            } else {
                final PixelPos pos = pin.getPixelPos();
                final int x = MathUtils.floorInt(pos.x);
                final int y = MathUtils.floorInt(pos.y);
                _pixelInfoView.updatePixelValues(sceneView, x, y, true);
            }
        }
    }

    private PixelInfoPPL registerPPL(ProductSceneView view) {
        if (_pixelPosListeners == null) {
            _pixelPosListeners = new HashMap<ProductSceneView, PixelInfoPPL>();
        }
        final PixelInfoPPL listener = new PixelInfoPPL(view);
        _pixelPosListeners.put(view, listener);
        return listener;
    }

    private PixelInfoPPL unregisterPPL(ProductSceneView view) {
        if (_pixelPosListeners != null) {
            return _pixelPosListeners.remove(view);
        }
        return null;
    }

    private class PixelInfoIFL extends InternalFrameAdapter {

        @Override
        public void internalFrameOpened(InternalFrameEvent e) {
            Container contentPane = e.getInternalFrame().getContentPane();
            if (contentPane instanceof ProductSceneView) {
                initView((ProductSceneView) contentPane);
            }
        }

        @Override
        public void internalFrameActivated(InternalFrameEvent e) {
            Container contentPane = e.getInternalFrame().getContentPane();
            if (contentPane instanceof ProductSceneView) {
                _currentView = (ProductSceneView) contentPane;
                final Product product = _currentView.getProduct();
                product.addProductNodeListener(getOrCreatePinSelectionChangedListener());
                if (isSnapToPin()) {
                    setToSelectedPin(_currentView);
                }
            }
        }

        @Override
        public void internalFrameClosing(InternalFrameEvent e) {
            final Container contentPane = e.getInternalFrame().getContentPane();
            if (contentPane instanceof ProductSceneView) {
                final ProductSceneView view = (ProductSceneView) contentPane;
                view.removePixelPositionListener(unregisterPPL(view));
                removePinSelectionChangedListener(e);
            }
            _pixelInfoView.clearProductNodeRefs();
        }

        @Override
        public void internalFrameDeactivated(InternalFrameEvent e) {
            removePinSelectionChangedListener(e);
        }

        private void removePinSelectionChangedListener(InternalFrameEvent e) {
            final Container contentPane = e.getInternalFrame().getContentPane();
            if (contentPane instanceof ProductSceneView) {
                final ProductSceneView view = (ProductSceneView) contentPane;
                final Product product = view.getProduct();
                product.removeProductNodeListener(getOrCreatePinSelectionChangedListener());
                if (isSnapToPin()) {
                    _pixelInfoView.updatePixelValues(view, -1, -1, false);
                }
            }
        }
    }

    private class PixelInfoPPL implements PixelPositionListener {

        private final ProductSceneView _view;

        public PixelInfoPPL(ProductSceneView view) {
            _view = view;
        }

        public void pixelPosChanged(RenderedImage sourceImage, int pixelX, int pixelY, boolean pixelPosValid, MouseEvent e) {
            if (isExecute()) {
                _pixelInfoView.updatePixelValues(_view, pixelX, pixelY, pixelPosValid);
            }
        }

        public void pixelPosNotAvailable(RenderedImage sourceImage) {
            if (isExecute()) {
                _pixelInfoView.updatePixelValues(_view, -1, -1, false);
            }
        }

        private boolean isExecute() {
            return _pixelInfoView.isVisible() && !isSnapToPin();
        }
    }

    private class PinSelectionChangedListener implements ProductNodeListener {

        public void nodeChanged(ProductNodeEvent event) {
            if (isExecute()) {
                if (!Pin.PROPERTY_NAME_SELECTED.equals(event.getPropertyName())) {
                    return;
                }
                updatePin(event);
            }
        }

        public void nodeDataChanged(ProductNodeEvent event) {
            if (isExecute()) {
                updatePin(event);
            }
        }

        public void nodeAdded(ProductNodeEvent event) {
            if (isExecute()) {
                updatePin(event);
            }
        }

        public void nodeRemoved(ProductNodeEvent event) {
            if (isExecute()) {
                ProductNode sourceNode = event.getSourceNode();
                if (sourceNode instanceof Pin && sourceNode.isSelected()) {
                    setToSelectedPin(_currentView);
                }
            }
        }

        private void updatePin(ProductNodeEvent event) {
            final ProductNode sourceNode = event.getSourceNode();
            if (sourceNode instanceof Pin && sourceNode.isSelected()) {
                setToSelectedPin(_currentView);
            }
        }

        private boolean isExecute() {
            return isSnapToPin();
        }
    }

    private class DisplayFilter extends PixelInfoView.DisplayFilter {

        private final VisatApp _app;
        private boolean _showOnlyLoadedOrDisplayedBands;

        public DisplayFilter(VisatApp app) {
            _app = app;
        }

        public void setShowOnlyLoadedOrDisplayedBands(boolean v) {
            if (_showOnlyLoadedOrDisplayedBands != v) {
                final boolean oldValue = _showOnlyLoadedOrDisplayedBands;
                _showOnlyLoadedOrDisplayedBands = v;
                firePropertyChange("showOnlyLoadedOrDisplayedBands", oldValue, v);
            }
        }

        @Override
        public boolean accept(ProductNode node) {
            if (node instanceof RasterDataNode) {
                final RasterDataNode rdn = (RasterDataNode) node;
                if (_showOnlyLoadedOrDisplayedBands) {
                    if (rdn.hasRasterData()) {
                        return true;
                    }
                    final JInternalFrame internalFrame = _app.findInternalFrame(rdn);
                    return internalFrame != null && internalFrame.getContentPane() instanceof ProductSceneView;
                }
            }
            return true;
        }
    }
}
