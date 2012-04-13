/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.visat.toolviews.stat;

import com.bc.ceres.swing.TableLayout;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.ProductNodeEvent;
import org.esa.beam.framework.datamodel.ProductNodeListener;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.VectorDataNode;
import org.esa.beam.framework.help.HelpSys;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.application.ToolView;
import org.esa.beam.framework.ui.tool.ToolButtonFactory;
import org.esa.beam.util.SystemUtils;
import org.esa.beam.visat.VisatApp;
import org.jfree.chart.ChartPanel;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.IOException;

/**
 * A general page within the statistics window.
 *
 * @author Marco Peters
 */
abstract class PagePanel extends JPanel implements ProductNodeListener {

    protected static final String ZOOM_TIP_MESSAGE = "TIP: To zoom within the chart, draw a rectangle\n" +
            "with the mouse or use the context menu.";

    private Product product;
    private boolean productChanged;
    private RasterDataNode raster;
    private boolean rasterChanged;
    private VectorDataNode vectorData;
    private boolean vectorDataChanged;

    private final ToolView parentDialog;
    private final String helpId;
    private final String titlePrefix;

    PagePanel(ToolView parentDialog, String helpId, String titlePrefix) {
        super(new BorderLayout(4, 4));
        this.parentDialog = parentDialog;
        this.helpId = helpId;
        this.titlePrefix = titlePrefix;
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        setPreferredSize(new Dimension(600, 320));
        setCurrentSelection();
    }

    protected Container getParentDialogContentPane() {
        return getParentDialog().getContext().getPane().getControl();
    }

    void setCurrentSelection() {
        final ProductNode selectedNode = VisatApp.getApp().getSelectedProductNode();
        if (selectedNode != null && selectedNode.getProduct() != null) {
            setProduct(selectedNode.getProduct());
        }
        if (selectedNode instanceof RasterDataNode) {
            setRaster((RasterDataNode) selectedNode);
        } else if (selectedNode instanceof VectorDataNode) {
            setVectorDataNode((VectorDataNode) selectedNode);
        }
    }

    private void transferProductNodeListener(Product oldProduct, Product newProduct) {
        if (oldProduct != newProduct) {
            if (oldProduct != null) {
                oldProduct.removeProductNodeListener(this);
            }
            if (newProduct != null) {
                newProduct.addProductNodeListener(this);
            }
        }
    }

    public String getTitle() {
        return getTitlePrefix() + " - " + getProductNodeDisplayName();
    }

    protected final String getTitlePrefix() {
        return titlePrefix;
    }

    protected Product getProduct() {
        return product;
    }

    private void setProduct(Product product) {
        if (this.product != product) {
            transferProductNodeListener(this.product, product);
            this.product = product;
            productChanged = true;
        }
    }

    protected RasterDataNode getRaster() {
        return raster;
    }

    protected void setRaster(RasterDataNode raster) {
        if (this.raster != raster) {
            this.raster = raster;
            rasterChanged = true;
        }
    }

    public VectorDataNode getVectorDataNode() {
        return vectorData;
    }

    protected void setVectorDataNode(VectorDataNode vectorDataNode) {
        if (this.vectorData != vectorDataNode) {
            this.vectorData = vectorDataNode;
            vectorDataChanged = true;
        }
    }

    protected boolean isRasterChanged() {
        return rasterChanged;
    }

    protected boolean isProductChanged() {
        return productChanged;
    }

    protected boolean isVectorDataNodeChanged() {
        return vectorDataChanged;
    }

    public ToolView getParentDialog() {
        return parentDialog;
    }


    /**
     * Resets the UI property with a value from the current look and feel.
     *
     * @see javax.swing.JComponent#updateUI
     */
    @Override
    public void updateUI() {
        super.updateUI();
        if (mustUpdateContent()) {
            updateContent();
            rasterChanged = false;
            productChanged = false;
            vectorDataChanged = false;
        }

    }

    protected boolean mustUpdateContent() {
        return isRasterChanged() || isProductChanged();
    }

    protected abstract void initContent();

    protected abstract void updateContent();

    protected abstract String getDataAsText();

    protected void handlePopupCreated(JPopupMenu popupMenu) {
    }

    protected boolean checkDataToClipboardCopy() {
        return true;
    }

    protected AbstractButton getHelpButton() {
        if (helpId != null) {
            final AbstractButton helpButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/Help24.gif"),
                                                                             false);
            helpButton.setToolTipText("Help.");
            helpButton.setName("helpButton");
            HelpSys.enableHelpOnButton(helpButton, helpId);
            HelpSys.enableHelpKey(getParentDialogContentPane(), helpId);
            return helpButton;
        }

        return null;
    }

    protected JMenuItem createCopyDataToClipboardMenuItem() {
        final JMenuItem menuItem = new JMenuItem("Copy Data to Clipboard"); /*I18N*/
        menuItem.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent actionEvent) {
                if (checkDataToClipboardCopy()) {
                    copyTextDataToClipboard();
                }
            }
        });
        return menuItem;
    }

    private void maybeOpenPopup(MouseEvent mouseEvent) {
        if (mouseEvent.isPopupTrigger()) {
            final JPopupMenu popupMenu = new JPopupMenu();
            popupMenu.add(createCopyDataToClipboardMenuItem());
            handlePopupCreated(popupMenu);
            final Point point = SwingUtilities.convertPoint(mouseEvent.getComponent(), mouseEvent.getPoint(), this);
            popupMenu.show(this, point.x, point.y);
        }
    }

    protected void copyTextDataToClipboard() {
        final Cursor oldCursor = getCursor();
        try {
            setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
            final String dataAsText = getDataAsText();
            if (dataAsText != null) {
                SystemUtils.copyToClipboard(dataAsText);
            }
        } finally {
            setCursor(oldCursor);
        }
    }

    @Deprecated
    protected static JPanel createChartButtonPanel(final ChartPanel chartPanel) {

        final AbstractButton zoomAllButton = ToolButtonFactory.createButton(
                UIUtils.loadImageIcon("icons/ZoomAll24.gif"),
                false);
        zoomAllButton.setToolTipText("Zoom all.");
        zoomAllButton.setName("zoomAllButton.");
        zoomAllButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                chartPanel.restoreAutoBounds();
            }
        });

        final AbstractButton propertiesButton = ToolButtonFactory.createButton(
                UIUtils.loadImageIcon("icons/Edit24.gif"),
                false);
        propertiesButton.setToolTipText("Edit properties.");
        propertiesButton.setName("propertiesButton.");
        propertiesButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                chartPanel.doEditChartProperties();
            }
        });

        final AbstractButton saveButton = ToolButtonFactory.createButton(
                UIUtils.loadImageIcon("icons/Export24.gif"),
                false);
        saveButton.setToolTipText("Save chart as image.");
        saveButton.setName("saveButton.");
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    chartPanel.doSaveAs();
                } catch (IOException e1) {
                    JOptionPane.showMessageDialog(chartPanel,
                                                  "Could not save chart:\n" + e1.getMessage(),
                                                  "Error",
                                                  JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        final AbstractButton printButton = ToolButtonFactory.createButton(
                UIUtils.loadImageIcon("icons/Print24.gif"),
                false);
        printButton.setToolTipText("Print chart.");
        printButton.setName("printButton.");
        printButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                chartPanel.createChartPrintJob();
            }
        });


        final TableLayout tableLayout = new TableLayout(2);
        JPanel buttonPane = new JPanel(tableLayout);
        buttonPane.setBorder(BorderFactory.createTitledBorder("Plot"));

        buttonPane.add(zoomAllButton);
        buttonPane.add(propertiesButton);
        buttonPane.add(saveButton);
        buttonPane.add(printButton);
        return buttonPane;
    }

    protected JPanel createTopPanel(){
        final AbstractButton refreshButton = ToolButtonFactory.createButton(
            UIUtils.loadImageIcon("icons/ZoomAll24.gif"),
                    false);
        refreshButton.setToolTipText("Refresh View");
        refreshButton.setName("refreshButton");
        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

            }
        });

        final AbstractButton switchToTableButton = ToolButtonFactory.createButton(
                UIUtils.loadImageIcon("icons/ZoomAll24.gif"),
                false);
        switchToTableButton.setToolTipText("Switch to Table View");
        switchToTableButton.setName("switchToTableButton");
        switchToTableButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

            }
        });

        final AbstractButton switchToChartButton = ToolButtonFactory.createButton(
                UIUtils.loadImageIcon("icons/ZoomAll24.gif"),
                false);
        switchToChartButton.setToolTipText("Switch to Chart View");
        switchToChartButton.setName("switchToChartButton");
        switchToChartButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {

            }
        });

        final TableLayout tableLayout = new TableLayout(6);
        tableLayout.setColumnFill(4, TableLayout.Fill.HORIZONTAL);
        tableLayout.setColumnWeightX(4, 1.0);
        JPanel buttonPanel = new JPanel(tableLayout);
        buttonPanel.add(refreshButton);
        buttonPanel.add(switchToTableButton);
        buttonPanel.add(new JPanel());
        buttonPanel.add(new JPanel());
        buttonPanel.add(new JPanel());

        return buttonPanel;
    }

    protected JPanel createChartButtonPanel2(final ChartPanel chartPanel) {

        final AbstractButton zoomAllButton = ToolButtonFactory.createButton(
                UIUtils.loadImageIcon("icons/ZoomAll24.gif"),
                false);
        zoomAllButton.setToolTipText("Zoom all.");
        zoomAllButton.setName("zoomAllButton.");
        zoomAllButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                chartPanel.restoreAutoBounds();
            }
        });

        final AbstractButton propertiesButton = ToolButtonFactory.createButton(
                UIUtils.loadImageIcon("icons/Edit24.gif"),
                false);
        propertiesButton.setToolTipText("Edit properties.");
        propertiesButton.setName("propertiesButton.");
        propertiesButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                chartPanel.doEditChartProperties();
            }
        });

        final AbstractButton saveButton = ToolButtonFactory.createButton(
                UIUtils.loadImageIcon("icons/Export24.gif"),
                false);
        saveButton.setToolTipText("Save chart as image.");
        saveButton.setName("saveButton.");
        saveButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                try {
                    chartPanel.doSaveAs();
                } catch (IOException e1) {
                    JOptionPane.showMessageDialog(chartPanel,
                                                  "Could not save chart:\n" + e1.getMessage(),
                                                  "Error",
                                                  JOptionPane.ERROR_MESSAGE);
                }
            }
        });

        final AbstractButton printButton = ToolButtonFactory.createButton(
                UIUtils.loadImageIcon("icons/Print24.gif"),
                false);
        printButton.setToolTipText("Print chart.");
        printButton.setName("printButton.");
        printButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                chartPanel.createChartPrintJob();
            }
        });

        final TableLayout tableLayout = new TableLayout(6);
        tableLayout.setColumnFill(4, TableLayout.Fill.HORIZONTAL);
        tableLayout.setColumnWeightX(4, 1.0);
        JPanel buttonPanel = new JPanel(tableLayout);
        buttonPanel.add(zoomAllButton);
        buttonPanel.add(propertiesButton);
        buttonPanel.add(saveButton);
        buttonPanel.add(printButton);
        buttonPanel.add(new JPanel());
        buttonPanel.add(getHelpButton());

        return buttonPanel;
    }


    class PopupHandler extends MouseAdapter {

        @Override
        public void mousePressed(MouseEvent e) {
            maybeOpenPopup(e);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            maybeOpenPopup(e);
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            maybeOpenPopup(e);
        }
    }

    private String getProductNodeDisplayName() {
        if (raster != null) {
            return raster.getDisplayName();
        } else {
            if (product != null) {
                return product.getDisplayName();
            } else {
                return "";
            }
        }
    }

    void selectionChanged(Product product, RasterDataNode raster, VectorDataNode vectorDataNode) {
        if (raster != getRaster() || product != getProduct() || vectorDataNode != getVectorDataNode()) {
            setRaster(raster);
            setProduct(product);
            setVectorDataNode(vectorDataNode);
            invokeUpdateUI();
        }
    }

    private void invokeUpdateUI() {
        if (SwingUtilities.isEventDispatchThread()) {
            updateUI();
        } else {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    updateUI();
                }
            });
        }
    }

    void handleLayerContentChanged() {
    }

    /**
     * Notified when a node was added.
     *
     * @param event the product node which the listener to be notified
     */
    @Override
    public void nodeAdded(ProductNodeEvent event) {
    }

    /**
     * Notified when a node changed.
     *
     * @param event the product node which the listener to be notified
     */
    @Override
    public void nodeChanged(ProductNodeEvent event) {
    }

    /**
     * Notified when a node's data changed.
     *
     * @param event the product node which the listener to be notified
     */
    @Override
    public void nodeDataChanged(ProductNodeEvent event) {
    }

    /**
     * Notified when a node was removed.
     *
     * @param event the product node which the listener to be notified
     */
    @Override
    public void nodeRemoved(ProductNodeEvent event) {
    }

}

