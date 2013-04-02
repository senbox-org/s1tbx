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
package org.esa.beam.framework.ui;

import com.bc.swing.dock.DockablePane;
import com.bc.swing.dock.FloatingComponentFactory;
import com.bc.swing.dock.FloatingDockableFrame;
import com.jidesoft.docking.DockingManager;
import com.jidesoft.swing.JideSplitPane;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.ProductNodeEvent;
import org.esa.beam.framework.datamodel.ProductNodeListener;
import org.esa.beam.framework.datamodel.ProductNodeListenerAdapter;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.help.HelpSys;
import org.esa.beam.framework.ui.product.ProductSceneView;

import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableModel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

/**
 * The pixel info view component is used to display the geophysical values for the pixel at a given pixel position
 * (x,y). The pixel info view can simultaneously display band, tie point grid and flag values.
 *
 * @author Norman Fomferra
 * @author Sabine Embacher
 * @version 1.2
 */
public class PixelInfoView extends JPanel {

    public static final String HELP_ID = "pixelInfoView";
    /**
     * Preferences key for show all band pixel values in pixel info view
     */
    public static final String PROPERTY_KEY_SHOW_ONLY_DISPLAYED_BAND_PIXEL_VALUES = "pixelview.showOnlyDisplayedBands";
    public static final boolean PROPERTY_DEFAULT_SHOW_DISPLAYED_BAND_PIXEL_VALUES = true;

    private static final int _NAME_COLUMN = 0;
    private static final int _VALUE_COLUMN = 1;
    private static final int _UNIT_COLUMN = 2;
    private JideSplitPane multiSplitPane;
    private boolean showGeoPosDecimal;

    public enum DockablePaneKey {

        GEOLOCATION, SCANLINE, BANDS, TIEPOINTS, FLAGS
    }

    private final PropertyChangeListener _displayFilterListener;
    private final ProductNodeListener _productNodeListener;

    private DockablePane geolocInfoPane;
    private DockablePane scanLineInfoPane;
    private DockablePane bandPixelInfoPane;
    private DockablePane tiePointGridPixelInfoPane;
    private DockablePane flagPixelInfoPane;
    private boolean _showPixelPosDecimals;
    private float _pixelOffsetX;
    private float _pixelOffsetY;
    private DisplayFilter _displayFilter;
    private final BasicApp app;
    private Map<DockablePaneKey, DockablePane> dockablePaneMap;

    private final PixelInfoViewTableModel geolocModel;
    private final PixelInfoViewTableModel scanlineModel;
    private final PixelInfoViewTableModel bandModel;
    private final PixelInfoViewTableModel tiePointModel;
    private final PixelInfoViewTableModel flagModel;

    private final PixelInfoViewModelUpdater modelUpdater;
    private final PixelInfoUpdateService updateService;


    /**
     * Constructs a new pixel info view.
     */
    public PixelInfoView(BasicApp app) {
        super(new BorderLayout());
        this.app = app;
        _displayFilterListener = createDisplayFilterListener();
        _productNodeListener = createProductNodeListener();
        dockablePaneMap = new HashMap<DockablePaneKey, DockablePane>(5);
        geolocModel = new PixelInfoViewTableModel(new String[]{"Coordinate", "Value", "Unit"});
        scanlineModel = new PixelInfoViewTableModel(new String[]{"Time", "Value", "Unit"});
        bandModel = new PixelInfoViewTableModel(new String[]{"Band", "Value", "Unit"});
        tiePointModel = new PixelInfoViewTableModel(new String[]{"Tie Point Grid", "Value", "Unit"});
        flagModel = new PixelInfoViewTableModel(new String[]{"Flag", "Value",});
        modelUpdater = new PixelInfoViewModelUpdater(geolocModel, scanlineModel, bandModel, tiePointModel, flagModel,
                                                     this);
        updateService = new PixelInfoUpdateService(modelUpdater);
        createUI();
    }

    ProductNodeListener getProductNodeListener() {
        return _productNodeListener;
    }

    private ProductNodeListener createProductNodeListener() {
        return new ProductNodeListenerAdapter() {
            @Override
            public void nodeChanged(ProductNodeEvent event) {
                updateService.requestUpdate();
            }

            @Override
            public void nodeAdded(ProductNodeEvent event) {
                updateService.requestUpdate();
            }

            @Override
            public void nodeRemoved(ProductNodeEvent event) {
                updateService.requestUpdate();
            }
        };
    }

    private PropertyChangeListener createDisplayFilterListener() {
        return new PropertyChangeListener() {
            public void propertyChange(PropertyChangeEvent evt) {
                if (getCurrentProduct() != null) {
                    updateService.requestUpdate();
                    clearSelectionInRasterTables();
                }
            }
        };
    }

    /**
     * Returns the current product
     *
     * @return the current Product
     */
    public Product getCurrentProduct() {
        return modelUpdater.getCurrentProduct();
    }

    /**
     * Returns the current raster
     *
     * @return the current raster
     */
    public RasterDataNode getCurrentRaster() {
        return modelUpdater.getCurrentRaster();
    }

    /**
     * Sets the filter to be used to filter the displayed bands. <p/>
     *
     * @param displayFilter the filter, can be null
     */
    public void setDisplayFilter(DisplayFilter displayFilter) {
        if (_displayFilter != displayFilter) {
            if (_displayFilter != null) {
                _displayFilter.removePropertyChangeListener(_displayFilterListener);
            }
            _displayFilter = displayFilter;
            _displayFilter.addPropertyChangeListener(_displayFilterListener);
        }
    }

    /**
     * Returns the display filter
     *
     * @return the display filter, can be null
     */
    public DisplayFilter getDisplayFilter() {
        return _displayFilter;
    }

    public void setShowPixelPosDecimals(boolean showPixelPosDecimals) {
        if (_showPixelPosDecimals != showPixelPosDecimals) {
            _showPixelPosDecimals = showPixelPosDecimals;
            updateService.requestUpdate();
        }
    }

    boolean showPixelPosDecimal() {
        return _showPixelPosDecimals;
    }

    public void setShowGeoPosDecimal(boolean showGeoPosDecimal) {
        if (this.showGeoPosDecimal != showGeoPosDecimal) {
            this.showGeoPosDecimal = showGeoPosDecimal;
            updateService.requestUpdate();
        }
    }

    boolean showGeoPosDecimal() {
        return showGeoPosDecimal;
    }

    public void setPixelOffsetX(float pixelOffsetX) {
        if (_pixelOffsetX != pixelOffsetX) {
            _pixelOffsetX = pixelOffsetX;
            updateService.requestUpdate();
        }
    }

    float getPixelOffsetX() {
        return _pixelOffsetX;
    }

    public void setPixelOffsetY(float pixelOffsetY) {
        if (_pixelOffsetY != pixelOffsetY) {
            _pixelOffsetY = pixelOffsetY;
            updateService.requestUpdate();
        }
    }

    float getPixelOffsetY() {
        return _pixelOffsetY;
    }

    public void updatePixelValues(ProductSceneView view, int pixelX, int pixelY, int level, boolean pixelPosValid) {
        updateService.updateState(view, pixelX, pixelY, level, pixelPosValid);
    }

    public boolean isAnyDockablePaneVisible() {
        for (DockablePaneKey paneKey : dockablePaneMap.keySet()) {
            if (isDockablePaneVisible(paneKey)) {
                return true;
            }
        }
        return false;
    }

    public void showDockablePanel(DockablePaneKey key, boolean show) {
        final DockablePane dockablePane = dockablePaneMap.get(key);
        if (multiSplitPane.indexOfPane(dockablePane) < 0 && show) {
            multiSplitPane.addPane(dockablePane);
            multiSplitPane.invalidate();
        }
        dockablePane.setShown(show);
    }

    public DockablePane getDockablePane(DockablePaneKey key) {
        return dockablePaneMap.get(key);
    }

    private void createUI() {
        geolocInfoPane = createDockablePane("Geo-location", 0, UIUtils.loadImageIcon("icons/WorldMap16.gif"),
                                            geolocModel);
        scanLineInfoPane = createDockablePane("Time Info", 1, UIUtils.loadImageIcon("icons/Clock16.gif"),
                                              scanlineModel);
        bandPixelInfoPane = createDockablePane("Bands", 2, UIUtils.loadImageIcon("icons/RsBandAsSwath16.gif"),
                                               bandModel);
        tiePointGridPixelInfoPane = createDockablePane("Tie Point Grids", 3,
                                                       UIUtils.loadImageIcon("icons/RsBandAsTiePoint16.gif"),
                                                       tiePointModel);
        flagPixelInfoPane = createDockablePane("Flags", 4, UIUtils.loadImageIcon("icons/RsBandFlags16.gif"), flagModel);

        geolocInfoPane.setPreferredSize(new Dimension(128, 128));
        scanLineInfoPane.setPreferredSize(new Dimension(128, 128));
        bandPixelInfoPane.setPreferredSize(new Dimension(128, 512));
        tiePointGridPixelInfoPane.setPreferredSize(new Dimension(128, 128));
        flagPixelInfoPane.setPreferredSize(new Dimension(128, 128));
        flagPixelInfoPane.setVisible(false);

        dockablePaneMap.put(DockablePaneKey.GEOLOCATION, geolocInfoPane);
        dockablePaneMap.put(DockablePaneKey.SCANLINE, scanLineInfoPane);
        dockablePaneMap.put(DockablePaneKey.TIEPOINTS, tiePointGridPixelInfoPane);
        dockablePaneMap.put(DockablePaneKey.BANDS, bandPixelInfoPane);
        dockablePaneMap.put(DockablePaneKey.FLAGS, flagPixelInfoPane);

        multiSplitPane = new JideSplitPane();
        multiSplitPane.setOrientation(JideSplitPane.VERTICAL_SPLIT);
        multiSplitPane.addPane(geolocInfoPane);
        multiSplitPane.addPane(scanLineInfoPane);
        multiSplitPane.addPane(tiePointGridPixelInfoPane);
        multiSplitPane.addPane(bandPixelInfoPane);
        // Flags are not added, they are only displayed on request
//        multiSplitPane.addPane(flagPixelInfoPane);

        final JTable flagsTable = getTable(flagPixelInfoPane);
        flagsTable.setDefaultRenderer(String.class, new FlagCellRenderer());
        flagsTable.setDefaultRenderer(Object.class, new FlagCellRenderer());

        addComponentListener();
        add(multiSplitPane, BorderLayout.CENTER);

        HelpSys.enableHelpKey(this, HELP_ID);
    }

    private static JTable getTable(DockablePane pane) {
        return (JTable) ((JScrollPane) pane.getContent()).getViewport().getView();
    }

    private void addComponentListener() {
        addComponentListener(new ComponentAdapter() {

            @Override
            public void componentResized(ComponentEvent e) {
                super.componentResized(e);
                setPreferredSize(getSize());
            }
        });
    }

    private DockablePane createDockablePane(String name, int index, ImageIcon icon, TableModel tableModel) {
        JTable table = new JTable(tableModel);
        table.setCellSelectionEnabled(false);
        table.setColumnSelectionAllowed(false);
        table.setRowSelectionAllowed(true);
        table.setTableHeader(null);
        table.removeEditor();
        JScrollPane scrollPane = new JScrollPane(table,
                                                 JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                                 JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        scrollPane.setBorder(null);
        scrollPane.setViewportBorder(null);

        final DockingManager dockingManager = app.getMainFrame().getDockingManager();
        final FloatingComponentFactory componentFactory = FloatingDockableFrame.getFactory(dockingManager);
        return new DockablePane(name, icon, scrollPane, index, true, componentFactory);
    }

    void clearSelectionInRasterTables() {
        final JTable bandTable = getTable(bandPixelInfoPane);
        final JTable tiePointGridTable = getTable(tiePointGridPixelInfoPane);
        bandTable.clearSelection();
        tiePointGridTable.clearSelection();
        final RasterDataNode raster = modelUpdater.getCurrentRaster();
        if (raster != null) {
            final String rasterName = raster.getName();
            if (!selectCurrentRaster(rasterName, bandTable)) {
                selectCurrentRaster(rasterName, tiePointGridTable);
            }
        }
    }

    public void clearProductNodeRefs() {
        modelUpdater.clearProductNodeRefs();
        updateService.clearState();
    }

    boolean isDockablePaneVisible(DockablePaneKey key) {
        final DockablePane dockablePane = dockablePaneMap.get(key);
        return dockablePane.isContentShown();
    }

    private boolean selectCurrentRaster(String rasterName, JTable table) {
        final TableModel model = table.getModel();
        for (int i = 0; i < model.getRowCount(); i++) {
            final String s = model.getValueAt(i, _NAME_COLUMN).toString();
            if (rasterName.equals(s)) {
                table.changeSelection(i, _NAME_COLUMN, false, false);
                return true;
            }
        }
        return false;
    }

    private static class FlagCellRenderer extends DefaultTableCellRenderer {

        /**
         * Returns the default table cell renderer.
         *
         * @param table      the <code>JTable</code>
         * @param value      the value to assign to the cell at <code>[row, column]</code>
         * @param isSelected true if cell is selected
         * @param hasFocus   true if cell has focus
         * @param row        the row of the cell to render
         * @param column     the column of the cell to render
         *
         * @return the default table cell renderer
         */
        @Override
        public Component getTableCellRendererComponent(JTable table,
                                                       Object value,
                                                       boolean isSelected,
                                                       boolean hasFocus,
                                                       int row,
                                                       int column) {
            Component c = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
            c.setForeground(Color.black);
            c.setBackground(Color.white);
            if (column == _VALUE_COLUMN && value != null) {
                if (value.equals("true")) {
                    c.setForeground(UIUtils.COLOR_DARK_RED);
                    final Color very_light_blue = new Color(230, 230, 255);
                    c.setBackground(very_light_blue);
                } else if (value.equals("false")) {
                    c.setForeground(UIUtils.COLOR_DARK_BLUE);
                    final Color very_light_red = new Color(255, 230, 230);
                    c.setBackground(very_light_red);
                }
            }
            return c;
        }
    }

    public static abstract class DisplayFilter {

        private final Vector<PropertyChangeListener> _pcl = new Vector<PropertyChangeListener>();

        public abstract boolean accept(final ProductNode node);

        public void addPropertyChangeListener(PropertyChangeListener displayFilterListener) {
            if (displayFilterListener != null && !_pcl.contains(displayFilterListener)) {
                _pcl.add(displayFilterListener);
            }
        }

        public void removePropertyChangeListener(PropertyChangeListener displayFilterListener) {
            if (displayFilterListener != null && _pcl.contains(displayFilterListener)) {
                _pcl.remove(displayFilterListener);
            }
        }

        protected void firePropertyChange(String propertyName, Object oldValue, Object newValue) {
            final PropertyChangeEvent event = new PropertyChangeEvent(this, propertyName, oldValue, newValue);
            for (int i = 0; i < _pcl.size(); i++) {
                (_pcl.elementAt(i)).propertyChange(event);
            }
        }
    }
}
