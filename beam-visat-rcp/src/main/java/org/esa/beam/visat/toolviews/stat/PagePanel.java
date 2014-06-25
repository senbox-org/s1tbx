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

import javax.swing.table.TableModel;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.help.HelpSys;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.application.ToolView;
import org.esa.beam.framework.ui.tool.ToolButtonFactory;
import org.esa.beam.util.SystemUtils;

import javax.swing.*;
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
public abstract class PagePanel extends JPanel implements ProductNodeListener {

    private final ToolView parentDialog;
    private final String helpId;
    private final String titlePrefix;

    private Product product;
    private boolean productChanged;

    private RasterDataNode raster;
    private boolean rasterChanged;

    private VectorDataNode vectorData;
    private boolean vectorDataChanged;

    private PagePanel alternativeView;

    PagePanel(ToolView parentDialog, String helpId, String titlePrefix) {
        super(new BorderLayout(4, 4));
        this.parentDialog = parentDialog;
        this.helpId = helpId;
        this.titlePrefix = titlePrefix;
        setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        setPreferredSize(new Dimension(600, 320));
    }

    public String getTitle() {
        return getTitlePrefix() + " - " + getProductNodeDisplayName();
    }

    public VectorDataNode getVectorDataNode() {
        return vectorData;
    }

    public ToolView getParentDialog() {
        return parentDialog;
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

    protected Container getParentDialogContentPane() {
        return getParentDialog().getContext().getPane().getControl();
    }

    protected final String getTitlePrefix() {
        return titlePrefix;
    }

    protected Product getProduct() {
        return product;
    }

    protected RasterDataNode getRaster() {
        return raster;
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

    protected void setRaster(RasterDataNode raster) {
        if (this.raster != raster) {
            this.raster = raster;
            rasterChanged = true;
        }
    }

    protected void setVectorDataNode(VectorDataNode vectorDataNode) {
        if (this.vectorData != vectorDataNode) {
            this.vectorData = vectorDataNode;
            vectorDataChanged = true;
        }
    }

    /**
     * @return {@code true} if {@link #handleNodeSelectionChanged} shall be called in a reaction to a node selection change.
     */
    protected boolean mustHandleSelectionChange() {
        return isRasterChanged() || isProductChanged();
    }

    /**
     * Called in reaction to a node selection change and if {@link #mustHandleSelectionChange()} returns {@code true}.
     * The default implementation calls {@link #updateComponents}.
     */
    protected void handleNodeSelectionChanged() {
        updateComponents();
    }

    /**
     * Called in reaction to a layer content change.
     * The default implementation does nothing.
     */
    protected void handleLayerContentChanged() {
    }

    /**
     * Initialises the panel's sub-components.
     */
    protected abstract void initComponents();

    /**
     * Updates the panel's sub-components as a reaction to a product node selection change.
     */
    protected abstract void updateComponents();

    protected abstract String getDataAsText();

    protected void handlePopupCreated(JPopupMenu popupMenu) {
    }

    protected boolean checkDataToClipboardCopy() {
        return true;
    }

    protected AbstractButton getHelpButton() {
        if (helpId != null) {
            final AbstractButton helpButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/Help22.png"),
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

    protected boolean hasAlternativeView(){
        return alternativeView != null;
    }

    protected void showAlternativeView() {
        final JPanel parent = (JPanel) this.getParent();
        parent.remove(this);
        this.setVisible(false);
        parent.add(alternativeView, BorderLayout.CENTER);
        alternativeView.setVisible(true);
        parent.revalidate();
    }

    protected void setAlternativeView(PagePanel alternativeView) {
        this.alternativeView = alternativeView;
    }

    protected PagePanel getAlternativeView() {
        return alternativeView;
    }

    void selectionChanged(Product product, RasterDataNode raster, VectorDataNode vectorDataNode) {
        if (raster != getRaster() || product != getProduct() || vectorDataNode != getVectorDataNode()) {
            setRaster(raster);
            setProduct(product);
            setVectorDataNode(vectorDataNode);
            if (mustHandleSelectionChange()) {
                handleNodeSelectionChanged();
                rasterChanged = false;
                productChanged = false;
                vectorDataChanged = false;
            }
        }
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

    private void setProduct(Product product) {
        if (this.product != product) {
            transferProductNodeListener(this.product, product);
            this.product = product;
            productChanged = true;
        }
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
}

