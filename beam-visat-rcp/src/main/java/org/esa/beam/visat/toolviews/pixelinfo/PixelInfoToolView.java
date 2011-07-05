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

package org.esa.beam.visat.toolviews.pixelinfo;

import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.swing.TableLayout;
import com.bc.swing.dock.DockablePane;
import com.jidesoft.docking.DockableFrame;
import com.jidesoft.docking.event.DockableFrameAdapter;
import com.jidesoft.docking.event.DockableFrameEvent;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Placemark;
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

/**
 * The tool window which displays the pixel info view.
 */
public class PixelInfoToolView extends AbstractToolView {

    public static final String ID = PixelInfoToolView.class.getName();

    private PinChangedListener pinChangedListener;
    private PropertyChangeListener pinSelectionChangeListener;
    private PixelInfoPPL pixelPositionListener;
    private ProductSceneView currentView;
    private PixelInfoView pixelInfoView;
    private JCheckBox pinCheckbox;

    @Override
    public JComponent createControl() {
        final VisatApp visatApp = VisatApp.getApp();
        pinChangedListener = new PinChangedListener();
        pinSelectionChangeListener = new PinSelectionChangeListener();
        pixelPositionListener = new PixelInfoPPL();

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

        setCurrentView(VisatApp.getApp().getSelectedProductSceneView());

        return pixelInfoViewPanel;
    }

    private void updatePixelInfo() {
        if (isSnapToSelectedPin()) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    snapToSelectedPin();
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

    private void setCurrentView(ProductSceneView view) {
        if (currentView == view) {
            return;
        }

        if (currentView != null) {
            currentView.removePixelPositionListener(pixelPositionListener);
            currentView.removePropertyChangeListener(ProductSceneView.PROPERTY_NAME_SELECTED_PIN,
                                                     pinSelectionChangeListener);
            currentView.getProduct().removeProductNodeListener(pinChangedListener);
        } else {
            pixelInfoView.clearProductNodeRefs();
        }

        currentView = view;

        if (currentView != null) {
            currentView.addPixelPositionListener(pixelPositionListener);
            currentView.addPropertyChangeListener(ProductSceneView.PROPERTY_NAME_SELECTED_PIN,
                                                  pinSelectionChangeListener);
            currentView.getProduct().addProductNodeListener(pinChangedListener);

            updatePixelInfo();
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

    private boolean isSnapToSelectedPin() {
        return pinCheckbox.isSelected();
    }

    private void snapToSelectedPin() {
        final Placemark pin = currentView != null ? currentView.getSelectedPin() : null;
        if (pin != null) {
            final PixelPos pos = pin.getPixelPos();
            final int x = MathUtils.floorInt(pos.x);
            final int y = MathUtils.floorInt(pos.y);
            pixelInfoView.updatePixelValues(currentView, x, y, 0, true);
        } else {
            pixelInfoView.updatePixelValues(currentView, -1, -1, 0, false);
        }
    }

    private class PixelInfoIFL extends InternalFrameAdapter {

        @Override
        public void internalFrameActivated(InternalFrameEvent e) {
            Container content = getContent(e);
            if (content instanceof ProductSceneView) {
                setCurrentView((ProductSceneView) content);
            }
        }

        @Override
        public void internalFrameDeactivated(InternalFrameEvent e) {
            Container content = getContent(e);
            if (content == currentView) {
                setCurrentView(null);
            }
        }

        private Container getContent(InternalFrameEvent e) {
            return e.getInternalFrame().getContentPane();
        }
    }

    private class PixelInfoPPL implements PixelPositionListener {

        @Override
        public void pixelPosChanged(ImageLayer imageLayer, int pixelX, int pixelY, int currentLevel,
                                    boolean pixelPosValid, MouseEvent e) {
            if (isActive()) {
                pixelInfoView.updatePixelValues(currentView, pixelX, pixelY, currentLevel, pixelPosValid);
            }
        }

        @Override
        public void pixelPosNotAvailable() {
            if (isActive()) {
                pixelInfoView.updatePixelValues(currentView, -1, -1, 0, false);
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
            if (currentView != null
                    && event.getSourceNode() == currentView.getSelectedPin()) {
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
