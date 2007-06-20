package org.esa.beam.visat.toolviews.stat;

import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.framework.ui.product.ProductTree;
import org.esa.beam.framework.ui.product.ProductTreeListener;
import org.esa.beam.framework.ui.application.PageComponent;
import org.esa.beam.framework.ui.application.ToolView;
import org.esa.beam.framework.ui.application.support.PageComponentListenerAdapter;
import org.esa.beam.util.SystemUtils;
import org.esa.beam.visat.VisatApp;

import javax.swing.*;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * A general page within the statistics window.
 *
 * @author Marco Peters
 */
abstract class PagePane extends JPanel implements ProductNodeListener{

    private RasterDataNode _raster;
    private Product _product;
    private boolean _rasterChanged;
    private boolean _productChanged;
    private ToolView _parentDialog;
    private final PagePanePTL _pagePanePTL = new PagePanePTL();
    private final InternalFrameAdapter _pagePaneIFL = new PagePaneIFL();

    public PagePane(final ToolView parentDialog) {
        super(new BorderLayout(4, 4));
        _parentDialog = parentDialog;
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

    private void updateCurrentSelection() {
        final ProductNode selectedNode = VisatApp.getApp().getSelectedProductNode();
        if(selectedNode instanceof Product) {
            setProduct((Product) selectedNode);
        } else if(selectedNode instanceof RasterDataNode) {
            setRaster((RasterDataNode) selectedNode);
            setProduct(selectedNode.getProduct());
        }
    }

    private void transferProductNodeListener(final Product oldProduct, final Product newProduct) {
        if (oldProduct != newProduct) {
            if (oldProduct != null) {
                oldProduct.removeProductNodeListener(this);
            }
            if(newProduct != null) {
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

    private void setProduct(final Product product) {
        if(_product != product) {
            transferProductNodeListener(_product, product);
            _product = product;
            _productChanged = true;
        }
    }

    protected RasterDataNode getRaster() {
        return _raster;
    }

    protected void setRaster(final RasterDataNode raster) {
        if(_raster != raster) {
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
            if(this.isShowing()) {
                _parentDialog.getDescriptor().setTitle(getTitle());
            }
            _rasterChanged = false;
            _productChanged = false;
        }

    }

    protected boolean mustUpdateContent(){
        return (isRasterChanged() || isProductChanged());
    }

    protected abstract void initContent();

    protected abstract void updateContent();

    protected abstract String getDataAsText();

    protected void handlePopupCreated(final JPopupMenu popupMenu) {
    }

    protected boolean checkDataToClipboardCopy() {
        return true;
    }

    private void maybeOpenPopup(final MouseEvent mouseEvent) {
        if (mouseEvent.isPopupTrigger()) {
            final JPopupMenu popupMenu = new JPopupMenu();
            final JMenuItem menuItem = new JMenuItem("Copy Data to Clipboard"); /*I18N*/
            menuItem.addActionListener(new ActionListener() {

                public void actionPerformed(final ActionEvent actionEvent) {
                    if (checkDataToClipboardCopy()) {
                        copyToClipboardImpl();
                    }
                }
            });
            popupMenu.add(menuItem);
            handlePopupCreated(popupMenu);
            final Point point = SwingUtilities.convertPoint(mouseEvent.getComponent(), mouseEvent.getPoint(), this);
            popupMenu.show(this, point.x, point.y);
        }
    }

    private void copyToClipboardImpl() {
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

    class PopupHandler extends MouseAdapter {

        @Override
        public void mousePressed(final MouseEvent e) {
            maybeOpenPopup(e);
        }

        @Override
        public void mouseReleased(final MouseEvent e) {
            maybeOpenPopup(e);
        }

        @Override
        public void mouseClicked(final MouseEvent e) {
            maybeOpenPopup(e);
        }
    }

    private String getProductNodeDisplayName() {
        if (_raster != null) {
            return _raster.getDisplayName();
        } else {
            if(_product != null) {
                return _product.getDisplayName();
            } else {
                return "";
            }
        }
    }

    private void selectionChanged(final Product product, final RasterDataNode raster) {
        if(raster != getRaster() || product != getProduct()) {
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

        public void tiePointGridSelected(final TiePointGrid tiePointGrid, final int clickCount) {
            selectionChanged(tiePointGrid.getProduct(), tiePointGrid);
        }

        public void bandSelected(final Band band, final int clickCount) {
            selectionChanged(band.getProduct(), band);
        }

        public void productSelected(final Product product, final int clickCount) {
            selectionChanged(product, null);
        }

        public void metadataElementSelected(final MetadataElement group, final int clickCount) {
            selectionChanged(group.getProduct(), null);
        }

        public void productAdded(final Product product) {                }
        public void productRemoved(final Product product) {
            selectionChanged(null, null);
        }

    }

    private class PagePaneIFL extends InternalFrameAdapter {
        @Override
        public void internalFrameActivated(final InternalFrameEvent e) {
            final Container contentPane = e.getInternalFrame().getContentPane();
            if (contentPane instanceof ProductSceneView) {
                final ProductSceneView sceneView = (ProductSceneView) contentPane;
                selectionChanged(sceneView.getRaster().getProduct(), sceneView.getRaster());
            }
        }

        @Override
        public void internalFrameDeactivated(final InternalFrameEvent e) {
            final Container contentPane = e.getInternalFrame().getContentPane();
            if (contentPane instanceof ProductSceneView) {
                final ProductSceneView sceneView = (ProductSceneView) contentPane;
                selectionChanged(sceneView.getRaster().getProduct(), null);
            }
        }
    }
}

