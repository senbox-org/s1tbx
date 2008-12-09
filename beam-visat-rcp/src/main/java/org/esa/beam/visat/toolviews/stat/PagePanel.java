package org.esa.beam.visat.toolviews.stat;

import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.help.HelpSys;
import org.esa.beam.framework.ui.TableLayout;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.application.ToolView;
import org.esa.beam.framework.ui.tool.ToolButtonFactory;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.util.SystemUtils;
import org.esa.beam.visat.VisatApp;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.data.Range;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.image.RenderedImage;
import java.io.IOException;

/**
 * A general page within the statistics window.
 *
 * @author Marco Peters
 */
abstract class PagePanel extends JPanel implements ProductNodeListener {

    private Product product;
    private boolean productChanged;
    private RasterDataNode raster;
    private boolean rasterChanged;

    private final ToolView parentDialog;
    private final String helpId;

    PagePanel(ToolView parentDialog, String helpId) {
        super(new BorderLayout(4, 4));
        this.parentDialog = parentDialog;
        this.helpId = helpId;
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        setPreferredSize(new Dimension(600, 320));
        updateCurrentSelection();
        initContent();
        transferProductNodeListener(null, product);
    }

    protected Container getParentDialogContentPane() {
        return getParentDialog().getContext().getPane().getControl();
    }

    public String getHelpId() {
        return helpId;
    }

    void updateCurrentSelection() {
        final ProductNode selectedNode = VisatApp.getApp().getSelectedProductNode();
        if (selectedNode instanceof Product) {
            setProduct((Product) selectedNode);
        } else if (selectedNode instanceof RasterDataNode) {
            setRaster((RasterDataNode) selectedNode);
            setProduct(selectedNode.getProduct());
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

    protected abstract String getTitlePrefix();

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

    protected RenderedImage getRoiImage(RasterDataNode rdn) {
        return ImageManager.getInstance().createRoiMaskImage(rdn, 0);
    }

    protected boolean isRasterChanged() {
        return rasterChanged;
    }

    protected boolean isProductChanged() {
        return productChanged;
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
            parentDialog.getDescriptor().setTitle(getTitle());
            rasterChanged = false;
            productChanged = false;
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
        if (getHelpId() != null) {
            final AbstractButton helpButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/Help24.gif"),
                                                                             false);
            helpButton.setToolTipText("Help.");
            helpButton.setName("helpButton");
            HelpSys.enableHelpOnButton(helpButton, getHelpId());
            HelpSys.enableHelpKey(getParentDialogContentPane(), getHelpId());
            return helpButton;
        }

        return null;
    }

    protected JMenuItem createCopyDataToClipboardMenuItem() {
        final JMenuItem menuItem = new JMenuItem("Copy Data to Clipboard"); /*I18N*/
        menuItem.addActionListener(new ActionListener() {

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

    protected static JPanel createChartButtonPanel(final ChartPanel chartPanel) {
        final TableLayout tableLayout = new TableLayout(2);
        JPanel buttonPane = new JPanel(tableLayout);
        buttonPane.setBorder(BorderFactory.createTitledBorder("Plot"));
        final AbstractButton zoomAllButton = ToolButtonFactory.createButton(
                UIUtils.loadImageIcon("icons/ZoomAll24.gif"),
                false);
        zoomAllButton.setToolTipText("Zoom all.");
        zoomAllButton.setName("zoomAllButton.");
        zoomAllButton.addActionListener(new ActionListener() {
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
            public void actionPerformed(ActionEvent e) {
                chartPanel.createChartPrintJob();
            }
        });

        buttonPane.add(zoomAllButton);
        buttonPane.add(propertiesButton);
        buttonPane.add(saveButton);
        buttonPane.add(printButton);
        return buttonPane;
    }

    private static boolean isAxisOutOfRange(ChartPanel chartPanel, ValueAxis axis) {
        final Range currentRange = axis.getRange();
        final Range defaultRange = chartPanel.getChart().getXYPlot().getDataRange(axis);
        final double outFactor = chartPanel.getZoomOutFactor();
        final Range nextRange = Range.scale(currentRange, outFactor);
        return nextRange.getLowerBound() < defaultRange.getLowerBound() ||
                nextRange.getUpperBound() > defaultRange.getUpperBound();
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

    void selectionChanged(Product product, RasterDataNode raster) {
        if (raster != getRaster() || product != getProduct()) {
            setRaster(raster);
            setProduct(product);
            SwingUtilities.invokeLater(new Runnable() {
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
    public void nodeAdded(ProductNodeEvent event) {
    }

    /**
     * Notified when a node changed.
     *
     * @param event the product node which the listener to be notified
     */
    public void nodeChanged(ProductNodeEvent event) {
    }

    /**
     * Notified when a node's data changed.
     *
     * @param event the product node which the listener to be notified
     */
    public void nodeDataChanged(ProductNodeEvent event) {
    }

    /**
     * Notified when a node was removed.
     *
     * @param event the product node which the listener to be notified
     */
    public void nodeRemoved(ProductNodeEvent event) {
    }

}

