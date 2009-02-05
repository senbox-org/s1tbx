package org.esa.beam.visat.toolviews.pixelinfo;

import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.swing.dock.DockablePane;
import org.esa.beam.framework.datamodel.Pin;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.ProductNodeEvent;
import org.esa.beam.framework.datamodel.ProductNodeListener;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.ui.PixelInfoView;
import org.esa.beam.framework.ui.PixelPositionListener;
import org.esa.beam.framework.ui.TableLayout;
import org.esa.beam.framework.ui.tool.ToolButtonFactory;
import org.esa.beam.framework.ui.application.support.AbstractToolView;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.util.PropertyMap;
import org.esa.beam.util.math.MathUtils;
import org.esa.beam.visat.VisatApp;

import javax.swing.*;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.*;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;

/**
 * The tool window which displays the pixel info view.
 */
public class PixelInfoToolView extends AbstractToolView {
    public static final String ID = PixelInfoToolView.class.getName();

    private PixelInfoView pixelInfoView;
    private JCheckBox pinCheckbox;
    private PinSelectionChangedListener pinSelectionChangedListener;
    private ProductSceneView currentView;
    private HashMap<ProductSceneView, PixelInfoPPL> pixelPosListeners;
    private AbstractButton coordToggleButton;
    private AbstractButton timeToggleButton;
    private AbstractButton bandsToggleButton;
    private AbstractButton tpgsToggleButton;
    private AbstractButton flagsToggleButton;

    public PixelInfoToolView() {
    }

    @Override
    public JComponent createControl() {
        final VisatApp visatApp = VisatApp.getApp();

        pixelInfoView = new PixelInfoView(visatApp);
        final DisplayFilter bandDisplayValidator = new DisplayFilter(visatApp);
        pixelInfoView.setPreferredSize(new Dimension(320, 480));
        pixelInfoView.setDisplayFilter(bandDisplayValidator);
        final PropertyMap preferences = visatApp.getPreferences();
        preferences.addPropertyChangeListener(new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                final String propertyName = evt.getPropertyName();
                if (PixelInfoView.PROPERTY_KEY_SHOW_ONLY_DISPLAYED_BAND_PIXEL_VALUES.equals(propertyName)) {
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
        pixelInfoViewPanel.add(pixelInfoView);

        pinCheckbox = new JCheckBox("Snap to selected pin");
        pinCheckbox.setName("pinCheckbox");
        pinCheckbox.setSelected(false);
        pinCheckbox.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (pinCheckbox.isSelected()) {
                    currentView = visatApp.getSelectedProductSceneView();
                    setToSelectedPin(currentView);
                }
            }
        });

        final TableLayout layout = new TableLayout(5);
        layout.setTableAnchor(TableLayout.Anchor.WEST);
        layout.setTableFill(TableLayout.Fill.HORIZONTAL);
        layout.setTablePadding(new Insets(2, 2, 2, 2));
        layout.setTableWeightX(1.0);
        layout.setCellColspan(1,0, 5);
        final JPanel optionPanel = new JPanel(layout);


        coordToggleButton = createToggleButton(pixelInfoView, PixelInfoView.DockablePaneKey.GEOLOCATION, true);
        timeToggleButton = createToggleButton(pixelInfoView, PixelInfoView.DockablePaneKey.SCANLINE, true);
        tpgsToggleButton = createToggleButton(pixelInfoView, PixelInfoView.DockablePaneKey.TIEPOINTS, true);
        bandsToggleButton = createToggleButton(pixelInfoView, PixelInfoView.DockablePaneKey.BANDS, true);
        flagsToggleButton = createToggleButton(pixelInfoView, PixelInfoView.DockablePaneKey.FLAGS, false);

        optionPanel.add(coordToggleButton);
        optionPanel.add(timeToggleButton);
        optionPanel.add(tpgsToggleButton);
        optionPanel.add(bandsToggleButton);
        optionPanel.add(flagsToggleButton);

        optionPanel.add(pinCheckbox);

        pixelInfoViewPanel.add(optionPanel, BorderLayout.SOUTH);

        visatApp.addInternalFrameListener(new PixelInfoIFL());
        initOpenedFrames();

        return pixelInfoViewPanel;
    }

    private AbstractButton createToggleButton(PixelInfoView pixelInfoView, PixelInfoView.DockablePaneKey paneKey,
                                              boolean initSelectedState) {
        final DockablePane dockablePane = pixelInfoView.getDockablePane(paneKey);
        final DockablePanelToggleAction panelToggleAction = new DockablePanelToggleAction(pixelInfoView, paneKey);
        final AbstractButton button = ToolButtonFactory.createButton(panelToggleAction, true);
        button.setText(null);
        button.setIcon(dockablePane.getIcon());

        button.setName(getContext().getPane().getControl().getName() + "." + paneKey + ".button");
        button.setToolTipText("Show/hide " + dockablePane.getTitle());
        button.setSelected(initSelectedState);

        dockablePane.addComponentListener(new ComponentListener() {

            public void componentResized(ComponentEvent e) { }

            public void componentMoved(ComponentEvent e) { }

            public void componentShown(ComponentEvent e) {
                button.setSelected(dockablePane.isContentShown());
            }

            public void componentHidden(ComponentEvent e) {
                button.setSelected(dockablePane.isContentShown());
            }
        });
        return button;
    }

    @Override
    public boolean isVisible() {
        return super.isVisible() || pixelInfoView.isAnyDockablePaneVisible();
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
            currentView = productSceneView;
        }
    }

    private void setPixelOffsetY(final PropertyMap preferences) {
        pixelInfoView.setPixelOffsetY((float) preferences.getPropertyDouble(
                VisatApp.PROPERTY_KEY_PIXEL_OFFSET_FOR_DISPLAY_Y,
                VisatApp.PROPERTY_DEFAULT_PIXEL_OFFSET_FOR_DISPLAY));
    }

    private void setPixelOffsetX(final PropertyMap preferences) {
        pixelInfoView.setPixelOffsetX((float) preferences.getPropertyDouble(
                VisatApp.PROPERTY_KEY_PIXEL_OFFSET_FOR_DISPLAY_X,
                VisatApp.PROPERTY_DEFAULT_PIXEL_OFFSET_FOR_DISPLAY));
    }

    private void setShowPixelPosDecimals(final PropertyMap preferences) {
        pixelInfoView.setShowPixelPosDecimals(preferences.getPropertyBool(
                VisatApp.PROPERTY_KEY_PIXEL_OFFSET_FOR_DISPLAY_SHOW_DECIMALS,
                VisatApp.PROPERTY_DEFAULT_PIXEL_OFFSET_FOR_DISPLAY_SHOW_DECIMALS));
    }

    private void setShowOnlyLoadedBands(final PropertyMap preferences, DisplayFilter validator) {
        final boolean showOnlyLoadedOrDisplayedBands = preferences.getPropertyBool(
                PixelInfoView.PROPERTY_KEY_SHOW_ONLY_DISPLAYED_BAND_PIXEL_VALUES,
                PixelInfoView.PROPERTY_DEFAULT_SHOW_DISPLAYED_BAND_PIXEL_VALUES);
        validator.setShowOnlyLoadedOrDisplayedBands(showOnlyLoadedOrDisplayedBands);
    }

    private PinSelectionChangedListener getOrCreatePinSelectionChangedListener() {
        if (pinSelectionChangedListener == null) {
            pinSelectionChangedListener = new PinSelectionChangedListener();
        }
        return pinSelectionChangedListener;
    }

    private boolean isSnapToPin() {
        return pinCheckbox.isSelected();
    }

    private void setToSelectedPin(ProductSceneView sceneView) {
        if (sceneView != null) {
            final Product product = sceneView.getProduct();
            final Pin pin = product.getPinGroup().getSelectedNode();
            if (pin == null) {
                pixelInfoView.updatePixelValues(sceneView, -1, -1, 0, false);
            } else {
                final PixelPos pos = pin.getPixelPos();
                final int x = MathUtils.floorInt(pos.x);
                final int y = MathUtils.floorInt(pos.y);
                pixelInfoView.updatePixelValues(sceneView, x, y, 0, true);
            }
        }
    }

    private PixelInfoPPL registerPPL(ProductSceneView view) {
        if (pixelPosListeners == null) {
            pixelPosListeners = new HashMap<ProductSceneView, PixelInfoPPL>();
        }
        final PixelInfoPPL listener = new PixelInfoPPL(view);
        pixelPosListeners.put(view, listener);
        return listener;
    }

    private PixelInfoPPL unregisterPPL(ProductSceneView view) {
        if (pixelPosListeners != null) {
            return pixelPosListeners.remove(view);
        }
        return null;
    }

    private class PixelInfoIFL extends InternalFrameAdapter {

        @Override
        public void internalFrameOpened(InternalFrameEvent e) {
            Container content = getContent(e);
            if (content instanceof ProductSceneView) {
                initView((ProductSceneView) content);
            }
        }

        @Override
        public void internalFrameActivated(InternalFrameEvent e) {
            Container content = getContent(e);
            if (content instanceof ProductSceneView) {
                currentView = (ProductSceneView) content;
                final Product product = currentView.getProduct();
                product.addProductNodeListener(getOrCreatePinSelectionChangedListener());
                if (isSnapToPin()) {
                    setToSelectedPin(currentView);
                }
            }
        }

        @Override
        public void internalFrameClosing(InternalFrameEvent e) {
            final Container content = getContent(e);
            if (content instanceof ProductSceneView) {
                final ProductSceneView view = (ProductSceneView) content;
                view.removePixelPositionListener(unregisterPPL(view));
                removePinSelectionChangedListener(e);
            }
            pixelInfoView.clearProductNodeRefs();
        }

        @Override
        public void internalFrameDeactivated(InternalFrameEvent e) {
            removePinSelectionChangedListener(e);
        }

        private void removePinSelectionChangedListener(InternalFrameEvent e) {
            final Container content = getContent(e);
            if (content instanceof ProductSceneView) {
                final ProductSceneView view = (ProductSceneView) content;
                final Product product = view.getProduct();
                product.removeProductNodeListener(getOrCreatePinSelectionChangedListener());
                if (isSnapToPin()) {
                    pixelInfoView.updatePixelValues(view, -1, -1, 0, false);
                }
            }
        }

        private Container getContent(InternalFrameEvent e) {
            return e.getInternalFrame().getContentPane();
        }
    }

    private class PixelInfoPPL implements PixelPositionListener {

        private final ProductSceneView view;

        private PixelInfoPPL(ProductSceneView view) {
            this.view = view;
        }

        public void pixelPosChanged(ImageLayer imageLayer, int pixelX, int pixelY, int currentLevel, boolean pixelPosValid, MouseEvent e) {
            if (isActive()) {
                pixelInfoView.updatePixelValues(view, pixelX, pixelY, currentLevel, pixelPosValid);
            }
        }

        public void pixelPosNotAvailable() {
            if (isActive()) {
                pixelInfoView.updatePixelValues(view, -1, -1, 0, false);
            }
        }

        private boolean isActive() {
            return  isVisible() && !isSnapToPin();
        }
    }

    private class PinSelectionChangedListener implements ProductNodeListener {

        public void nodeChanged(ProductNodeEvent event) {
            if (isActive()) {
                if (!Pin.PROPERTY_NAME_SELECTED.equals(event.getPropertyName())) {
                    return;
                }
                updatePin(event);
            }
        }

        public void nodeDataChanged(ProductNodeEvent event) {
            if (isActive()) {
                updatePin(event);
            }
        }

        public void nodeAdded(ProductNodeEvent event) {
            if (isActive()) {
                updatePin(event);
            }
        }

        public void nodeRemoved(ProductNodeEvent event) {
            if (isActive()) {
                ProductNode sourceNode = event.getSourceNode();
                if (sourceNode instanceof Pin && sourceNode.isSelected()) {
                    setToSelectedPin(currentView);
                }
            }
        }

        private void updatePin(ProductNodeEvent event) {
            final ProductNode sourceNode = event.getSourceNode();
            if (sourceNode instanceof Pin && sourceNode.isSelected()) {
                setToSelectedPin(currentView);
            }
        }

        private boolean isActive() {
            return isSnapToPin();
        }
    }

    private class DisplayFilter extends PixelInfoView.DisplayFilter {

        private final VisatApp app;
        private boolean showOnlyLoadedOrDisplayedBands;

        private DisplayFilter(VisatApp app) {
            this.app = app;
        }

        public void setShowOnlyLoadedOrDisplayedBands(boolean v) {
            if (showOnlyLoadedOrDisplayedBands != v) {
                final boolean oldValue = showOnlyLoadedOrDisplayedBands;
                showOnlyLoadedOrDisplayedBands = v;
                firePropertyChange("showOnlyLoadedOrDisplayedBands", oldValue, v);
            }
        }

        @Override
        public boolean accept(ProductNode node) {
            if (node instanceof RasterDataNode) {
                final RasterDataNode rdn = (RasterDataNode) node;
                if (showOnlyLoadedOrDisplayedBands) {
                    if (rdn.hasRasterData()) {
                        return true;
                    }
                    final JInternalFrame internalFrame = app.findInternalFrame(rdn);
                    return internalFrame != null && internalFrame.getContentPane() instanceof ProductSceneView;
                }
            }
            return true;
        }
    }

    private class DockablePanelToggleAction extends AbstractAction {

        private final PixelInfoView piv;
        private final PixelInfoView.DockablePaneKey panelKey;

        private DockablePanelToggleAction(PixelInfoView pixelInfoView, PixelInfoView.DockablePaneKey dockablePanelKey) {
            piv = pixelInfoView;
            panelKey = dockablePanelKey;
        }

        public void actionPerformed(ActionEvent e) {
            final DockablePane dockablePane = piv.getDockablePane(panelKey);
            final boolean isShown = dockablePane.isContentShown();
            piv.showDockablePanel(panelKey, !isShown);
        }
    }
}
