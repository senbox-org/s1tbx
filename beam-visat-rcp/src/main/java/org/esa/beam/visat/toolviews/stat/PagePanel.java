package org.esa.beam.visat.toolviews.stat;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.ProductNodeEvent;
import org.esa.beam.framework.datamodel.ProductNodeListener;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.framework.help.HelpSys;
import org.esa.beam.framework.ui.TableLayout;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.application.PageComponent;
import org.esa.beam.framework.ui.application.ToolView;
import org.esa.beam.framework.ui.application.support.PageComponentListenerAdapter;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.framework.ui.product.ProductTree;
import org.esa.beam.framework.ui.product.ProductTreeListener;
import org.esa.beam.framework.ui.tool.ToolButtonFactory;
import org.esa.beam.util.SystemUtils;
import org.esa.beam.visat.VisatApp;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.data.Range;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.io.IOException;

/**
 * A general page within the statistics window.
 *
 * @author Marco Peters
 */
abstract class PagePanel extends JPanel implements ProductNodeListener {

    private RasterDataNode _raster;
    private Product _product;
    private boolean _rasterChanged;
    private boolean _productChanged;
    private final ToolView _parentDialog;
    private final String helpId;
    private final PagePanePTL _pagePanePTL = new PagePanePTL();
    private final InternalFrameAdapter _pagePaneIFL = new PagePaneIFL();

    PagePanel(ToolView parentDialog, String helpId) {
        super(new BorderLayout(4, 4));
        _parentDialog = parentDialog;
        this.helpId = helpId;
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        setPreferredSize(new Dimension(600, 320));
        final ProductTree productTree = VisatApp.getApp().getProductTree();
        _parentDialog.getContext().getPage().addPageComponentListener(new PageComponentListenerAdapter() {
            @Override
            public void componentOpened(PageComponent component) {
                productTree.addProductTreeListener(_pagePanePTL);
                transferProductNodeListener(getProduct(), null);
                VisatApp.getApp().addInternalFrameListener(_pagePaneIFL);
                updateCurrentSelection();
                transferProductNodeListener(null, _product);
                updateUI();
            }

            @Override
            public void componentClosed(PageComponent component) {
                productTree.removeProductTreeListener(_pagePanePTL);
                transferProductNodeListener(getProduct(), null);
                VisatApp.getApp().removeInternalFrameListener(_pagePaneIFL);
            }

        });

        updateCurrentSelection();
        initContent();
        transferProductNodeListener(null, _product);
    }

    protected Container getParentDialogContentPane() {
        return getParentDialog().getContext().getPane().getControl();
    }

    public String getHelpId() {
        return helpId;
    }

    private void updateCurrentSelection() {
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
        return _product;
    }

    private void setProduct(Product product) {
        if (_product != product) {
            transferProductNodeListener(_product, product);
            _product = product;
            _productChanged = true;
        }
    }

    protected RasterDataNode getRaster() {
        return _raster;
    }

    protected void setRaster(RasterDataNode raster) {
        if (_raster != raster) {
            _raster = raster;
            _rasterChanged = true;
        }
    }

    protected boolean isRasterChanged() {
        return _rasterChanged;
    }

    protected boolean isProductChanged() {
        return _productChanged;
    }

    public ToolView getParentDialog() {
        return _parentDialog;
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
            if (this.isShowing()) {
                _parentDialog.getDescriptor().setTitle(getTitle());
            }
            _rasterChanged = false;
            _productChanged = false;
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
        final TableLayout tableLayout = new TableLayout(3);
        JPanel buttonPane = new JPanel(tableLayout);
        buttonPane.setBorder(BorderFactory.createTitledBorder("Plot"));
        final AbstractButton zoomInButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/ZoomIn24.gif"),
                                                                           false);
        zoomInButton.setToolTipText("Zoom in.");
        zoomInButton.setName("zoomInButton.");
        zoomInButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                final Rectangle2D chartArea = chartPanel.getChartRenderingInfo().getChartArea();
                chartPanel.zoomInBoth(chartArea.getCenterX(), chartArea.getCenterY());
            }
        });
        final AbstractButton zoomOutButton = ToolButtonFactory.createButton(
                UIUtils.loadImageIcon("icons/ZoomOut24.gif"),
                false);
        zoomOutButton.setToolTipText("Zoom out.");
        zoomOutButton.setName("zoomOutButton.");
        zoomOutButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                final JFreeChart chart= chartPanel.getChart();
                final boolean rangeAxisOOR = isAxisOutOfRange(chartPanel, chart.getXYPlot().getRangeAxis());
                final boolean domainAxisOOR = isAxisOutOfRange(chartPanel, chart.getXYPlot().getDomainAxis());
                // prevent from zooming out to far
                if(rangeAxisOOR || domainAxisOOR) {
                    chartPanel.restoreAutoBounds();
                }else {
                    final Rectangle2D chartArea = chartPanel.getChartRenderingInfo().getChartArea();
                    chartPanel.zoomOutBoth(chartArea.getCenterX(), chartArea.getCenterY());
                }


            }
        });
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

        buttonPane.add(zoomInButton);
        buttonPane.add(zoomOutButton);
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
        if (_raster != null) {
            return _raster.getDisplayName();
        } else {
            if (_product != null) {
                return _product.getDisplayName();
            } else {
                return "";
            }
        }
    }

    private void selectionChanged(Product product, RasterDataNode raster) {
        if (raster != getRaster() || product != getProduct()) {
            setRaster(raster);
            setProduct(product);
            updateUI();
        }
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


    private class PagePanePTL implements ProductTreeListener {

        public void tiePointGridSelected(TiePointGrid tiePointGrid, int clickCount) {
            selectionChanged(tiePointGrid.getProduct(), tiePointGrid);
        }

        public void bandSelected(Band band, int clickCount) {
            selectionChanged(band.getProduct(), band);
        }

        public void productSelected(Product product, int clickCount) {
            selectionChanged(product, null);
        }

        public void metadataElementSelected(MetadataElement group, int clickCount) {
            selectionChanged(group.getProduct(), null);
        }

        public void productAdded(Product product) {
        }

        public void productRemoved(Product product) {
            selectionChanged(null, null);
        }

    }

    private class PagePaneIFL extends InternalFrameAdapter {

        @Override
        public void internalFrameActivated(InternalFrameEvent e) {
            final Container contentPane = e.getInternalFrame().getContentPane();
            if (contentPane instanceof ProductSceneView) {
                final ProductSceneView sceneView = (ProductSceneView) contentPane;
                selectionChanged(sceneView.getRaster().getProduct(), sceneView.getRaster());
            }
        }

        @Override
        public void internalFrameDeactivated(InternalFrameEvent e) {
            final Container contentPane = e.getInternalFrame().getContentPane();
            if (contentPane instanceof ProductSceneView) {
                final ProductSceneView sceneView = (ProductSceneView) contentPane;
                selectionChanged(sceneView.getRaster().getProduct(), null);
            }
        }
    }
}

