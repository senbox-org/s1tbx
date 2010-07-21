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

package org.esa.beam.visat.toolviews.pixelinfo;

import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.swing.TableLayout;
import com.bc.swing.dock.DockablePane;
import com.jidesoft.docking.DockableFrame;
import com.jidesoft.docking.event.DockableFrameAdapter;
import com.jidesoft.docking.event.DockableFrameEvent;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Placemark;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.ProductNodeEvent;
import org.esa.beam.framework.datamodel.ProductNodeListener;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.ui.PixelInfoView;
import org.esa.beam.framework.ui.PixelPositionListener;
import org.esa.beam.framework.ui.application.support.AbstractToolView;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.framework.ui.tool.ToolButtonFactory;
import org.esa.beam.util.PropertyMap;
import org.esa.beam.util.math.MathUtils;
import org.esa.beam.visat.VisatApp;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JInternalFrame;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
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
    private PinChangedListener pinChangedListener;
    private PropertyChangeListener pinSelectionChangeListener;
    private ProductSceneView currentView;
    private HashMap<ProductSceneView, PixelInfoPPL> pixelPosListeners;

    @Override
    public JComponent createControl() {
        final VisatApp visatApp = VisatApp.getApp();

        pinSelectionChangeListener = new PinSelectionChangeListener();
        pixelInfoView = new PixelInfoView(visatApp);
        final DisplayFilter bandDisplayValidator = new DisplayFilter(visatApp);
        pixelInfoView.setPreferredSize(new Dimension(320, 480));
        pixelInfoView.setDisplayFilter(bandDisplayValidator);
        final PropertyMap preferences = visatApp.getPreferences();
        preferences.addPropertyChangeListener(new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                final String propertyName = evt.getPropertyName();
                if (PixelInfoView.PROPERTY_KEY_SHOW_ONLY_DISPLAYED_BAND_PIXEL_VALUES.equals(propertyName)) {
                    setShowOnlyLoadedBands(preferences, bandDisplayValidator);
                } else if (VisatApp.PROPERTY_KEY_PIXEL_OFFSET_FOR_DISPLAY_SHOW_DECIMALS.equals(propertyName)) {
                    setShowPixelPosDecimals(preferences);
                } else if (VisatApp.PROPERTY_KEY_DISPLAY_GEOLOCATION_AS_DECIMAL.equals(propertyName)) {
                    setShowGeoPosDecimal(preferences);
                } else if (VisatApp.PROPERTY_KEY_PIXEL_OFFSET_FOR_DISPLAY_X.equals(propertyName)) {
                    setPixelOffsetX(preferences);
                } else if (VisatApp.PROPERTY_KEY_PIXEL_OFFSET_FOR_DISPLAY_Y.equals(propertyName)) {
                    setPixelOffsetY(preferences);
                }
            }
        });

        DockableFrame dockableFrame = (DockableFrame) getContext().getPane().getControl();
        dockableFrame.addDockableFrameListener(new DockableFrameAdapter() {
            @Override
            public void dockableFrameTabShown(DockableFrameEvent e) {
                updatePixelInfo();
            }

            @Override
            public void dockableFrameShown(DockableFrameEvent e) {
                updatePixelInfo();
            }
        });

        setShowOnlyLoadedBands(preferences, bandDisplayValidator);
        setShowPixelPosDecimals(preferences);
        setShowGeoPosDecimal(preferences);
        setPixelOffsetX(preferences);
        setPixelOffsetY(preferences);

        final JPanel pixelInfoViewPanel = new JPanel(new BorderLayout());
        pixelInfoViewPanel.add(pixelInfoView);

        pinCheckbox = new JCheckBox("Snap to selected pin");
        pinCheckbox.setName("pinCheckbox");
        pinCheckbox.setSelected(false);
        pinCheckbox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updatePixelInfo();
            }
        });

        final TableLayout layout = new TableLayout(5);
        layout.setTableAnchor(TableLayout.Anchor.WEST);
        layout.setTableFill(TableLayout.Fill.HORIZONTAL);
        layout.setTablePadding(new Insets(2, 2, 2, 2));
        layout.setTableWeightX(1.0);
        layout.setCellColspan(1, 0, 5);
        final JPanel optionPanel = new JPanel(layout);


        AbstractButton coordToggleButton = createToggleButton(pixelInfoView, PixelInfoView.DockablePaneKey.GEOLOCATION,
                                                              true);
        AbstractButton timeToggleButton = createToggleButton(pixelInfoView, PixelInfoView.DockablePaneKey.SCANLINE,
                                                             true);
        AbstractButton tpgsToggleButton = createToggleButton(pixelInfoView, PixelInfoView.DockablePaneKey.TIEPOINTS,
                                                             true);
        AbstractButton bandsToggleButton = createToggleButton(pixelInfoView, PixelInfoView.DockablePaneKey.BANDS, true);
        AbstractButton flagsToggleButton = createToggleButton(pixelInfoView, PixelInfoView.DockablePaneKey.FLAGS,
                                                              false);

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

    private void updatePixelInfo() {
        if (isSnapToSelectedPin()) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    snapToSelectedPin(currentView);
                }
            });
        } else {
            pixelInfoView.updatePixelValues(currentView, -1, -1, 0, false);
        }
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

            @Override
            public void componentResized(ComponentEvent e) {
            }

            @Override
            public void componentMoved(ComponentEvent e) {
            }

            @Override
            public void componentShown(ComponentEvent e) {
                button.setSelected(dockablePane.isContentShown());
            }

            @Override
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
        product.addProductNodeListener(getOrCreatePinChangedListener());

        updatePixelInfo();

        if (productSceneView == VisatApp.getApp().getSelectedProductSceneView()) {
            if (currentView != null) {
                currentView.removePropertyChangeListener(ProductSceneView.PROPERTY_NAME_SELECTED_PIN,
                                                         pinSelectionChangeListener);
            }
            currentView = productSceneView;
            currentView.addPropertyChangeListener(ProductSceneView.PROPERTY_NAME_SELECTED_PIN,
                                                  pinSelectionChangeListener);
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

    private void setShowGeoPosDecimal(final PropertyMap preferences) {
        pixelInfoView.setShowGeoPosDecimal(preferences.getPropertyBool(
                VisatApp.PROPERTY_KEY_DISPLAY_GEOLOCATION_AS_DECIMAL,
                VisatApp.PROPERTY_DEFAULT_DISPLAY_GEOLOCATION_AS_DECIMAL));
    }

    private void setShowOnlyLoadedBands(final PropertyMap preferences, DisplayFilter validator) {
        final boolean showOnlyLoadedOrDisplayedBands = preferences.getPropertyBool(
                PixelInfoView.PROPERTY_KEY_SHOW_ONLY_DISPLAYED_BAND_PIXEL_VALUES,
                PixelInfoView.PROPERTY_DEFAULT_SHOW_DISPLAYED_BAND_PIXEL_VALUES);
        validator.setShowOnlyLoadedOrDisplayedBands(showOnlyLoadedOrDisplayedBands);
    }

    private PinChangedListener getOrCreatePinChangedListener() {
        if (pinChangedListener == null) {
            pinChangedListener = new PinChangedListener();
        }
        return pinChangedListener;
    }

    private boolean isSnapToSelectedPin() {
        return pinCheckbox.isSelected();
    }

    private void snapToSelectedPin(ProductSceneView sceneView) {
        if (sceneView != null) {
            final Placemark pin = sceneView.getSelectedPin();
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
                if (currentView != null) {
                    currentView.removePropertyChangeListener(ProductSceneView.PROPERTY_NAME_SELECTED_PIN,
                                                             pinSelectionChangeListener);
                }
                currentView = (ProductSceneView) content;
                currentView.addPropertyChangeListener(ProductSceneView.PROPERTY_NAME_SELECTED_PIN,
                                                      pinSelectionChangeListener);
                final Product product = currentView.getProduct();
                product.addProductNodeListener(getOrCreatePinChangedListener());
                updatePixelInfo();
            }
        }

        @Override
        public void internalFrameClosing(InternalFrameEvent e) {
            final Container content = getContent(e);
            if (content instanceof ProductSceneView) {
                final ProductSceneView view = (ProductSceneView) content;
                view.removePropertyChangeListener(ProductSceneView.PROPERTY_NAME_SELECTED_PIN,
                                                  pinSelectionChangeListener);
                view.removePixelPositionListener(unregisterPPL(view));
                removePinChangedListener(e);
            }
            pixelInfoView.clearProductNodeRefs();
        }

        @Override
        public void internalFrameDeactivated(InternalFrameEvent e) {
            removePinChangedListener(e);
        }

        private void removePinChangedListener(InternalFrameEvent e) {
            final Container content = getContent(e);
            if (content instanceof ProductSceneView) {
                final ProductSceneView view = (ProductSceneView) content;
                final Product product = view.getProduct();
                product.removeProductNodeListener(getOrCreatePinChangedListener());
                if (isSnapToSelectedPin()) {
                    pixelInfoView.updatePixelValues(view, -1, -1, 0, false);
                }
            }
        }

        private Container getContent(InternalFrameEvent e) {
            return e.getInternalFrame().getContentPane();
        }
    }

    private class PixelInfoPPL implements PixelPositionListener {
        // (mp,ts - 09.04.2010) todo - this listener should also make use of updatePixelInfo()
        private final ProductSceneView view;

        private PixelInfoPPL(ProductSceneView view) {
            this.view = view;
        }

        @Override
        public void pixelPosChanged(ImageLayer imageLayer, int pixelX, int pixelY, int currentLevel,
                                    boolean pixelPosValid, MouseEvent e) {
            if (isActive()) {
                pixelInfoView.updatePixelValues(view, pixelX, pixelY, currentLevel, pixelPosValid);
            }
        }

        @Override
        public void pixelPosNotAvailable() {
            if (isActive()) {
                pixelInfoView.updatePixelValues(view, -1, -1, 0, false);
            }
        }

        private boolean isActive() {
            return isVisible() && !isSnapToSelectedPin();
        }
    }

    private class PinChangedListener implements ProductNodeListener {

        @Override
        public void nodeChanged(ProductNodeEvent event) {
            if (Placemark.PROPERTY_NAME_PIXELPOS.equals(event.getPropertyName())) {
                handlePinEvent(event);
            }
        }

        @Override
        public void nodeDataChanged(ProductNodeEvent event) {
            handlePinEvent(event);
        }

        @Override
        public void nodeAdded(ProductNodeEvent event) {
            handlePinEvent(event);
        }

        @Override
        public void nodeRemoved(ProductNodeEvent event) {
            handlePinEvent(event);
        }

        private void handlePinEvent(ProductNodeEvent event) {
            if (event.getSourceNode() == currentView.getSelectedPin()) {
                updatePixelInfo();
            }
        }
    }

    private class PinSelectionChangeListener implements PropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            if (isVisible()) {
                updatePixelInfo();
            }
        }

    }

    private static class DisplayFilter extends PixelInfoView.DisplayFilter {

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

    private static class DockablePanelToggleAction extends AbstractAction {

        private final PixelInfoView piv;
        private final PixelInfoView.DockablePaneKey panelKey;

        private DockablePanelToggleAction(PixelInfoView pixelInfoView, PixelInfoView.DockablePaneKey dockablePanelKey) {
            piv = pixelInfoView;
            panelKey = dockablePanelKey;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            final DockablePane dockablePane = piv.getDockablePane(panelKey);
            final boolean isShown = dockablePane.isContentShown();
            piv.showDockablePanel(panelKey, !isShown);
        }
    }
}
