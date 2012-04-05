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
package org.esa.beam.visat.toolviews.mask;

import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.VectorDataNode;
import org.esa.beam.framework.help.HelpSys;
import org.esa.beam.framework.ui.application.support.AbstractToolView;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.framework.ui.product.ProductTreeListenerAdapter;
import org.esa.beam.visat.VisatApp;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.Container;

// todo - a stripped version of this class may serve as base class for all VISAT tool views (nf)

public abstract class MaskToolView extends AbstractToolView {

    private MaskForm maskForm;
    private String prefixTitle;

    @Override
    public JComponent createControl() {
        prefixTitle = getDescriptor().getTitle();
        maskForm = createMaskForm(this, new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                final ProductSceneView sceneView = VisatApp.getApp().getSelectedProductSceneView();
                if (sceneView != null) {
                    Mask selectedMask = maskForm.getSelectedMask();
                    if (selectedMask != null) {
                        VectorDataNode vectorDataNode = Mask.VectorDataType.getVectorData(selectedMask);
                        if (vectorDataNode != null) {
                            sceneView.selectVectorDataLayer(vectorDataNode);
                        }
                    }
                }
            }
        });

        AbstractButton helpButton = maskForm.getHelpButton();
        if (helpButton != null) {
            helpButton.setName("helpButton");
            if (getDescriptor().getHelpId() != null) {
                HelpSys.enableHelpKey(getPaneControl(), getDescriptor().getHelpId());
                HelpSys.enableHelpOnButton(helpButton, getDescriptor().getHelpId());
            }
        }

        updateMaskForm(VisatApp.getApp().getSelectedProductSceneView());

        VisatApp.getApp().addProductTreeListener(new MaskPTL());
        VisatApp.getApp().addInternalFrameListener(new MaskIFL());

        maskForm.updateState();

        return maskForm.createContentPanel();
    }

    private void updateMaskForm(ProductSceneView view) {
        if (view == null) {
            final ProductNode selectedProductNode = VisatApp.getApp().getSelectedProductNode();
            if (selectedProductNode instanceof RasterDataNode) {
                final RasterDataNode rdn = (RasterDataNode) selectedProductNode;
                maskForm.reconfigureMaskTable(rdn.getProduct(), rdn);
            } else if (selectedProductNode instanceof Product) {
                final Product product = (Product) selectedProductNode;
                maskForm.reconfigureMaskTable(product, null);
            } else if (selectedProductNode != null && selectedProductNode.getProduct() != null) {
                maskForm.reconfigureMaskTable(selectedProductNode.getProduct(), null);
            } else {
                maskForm.clearMaskTable();
            }
            updateTitle(selectedProductNode != null ? selectedProductNode.getDisplayName() : null);
        } else {
            maskForm.reconfigureMaskTable(view.getProduct(), view.getRaster());
            if (view.isRGB()) {
                updateTitle("RGB");
            } else {
                updateTitle(view.getRaster().getDisplayName());
            }
        }
    }

    private void updateTitle(String suffix) {
        if (suffix != null) {
            getDescriptor().setTitle(prefixTitle + " - " + suffix);
        } else {
            getDescriptor().setTitle(prefixTitle);
        }
    }

    protected abstract MaskForm createMaskForm(AbstractToolView maskToolView, ListSelectionListener selectionListener);

    private class MaskPTL extends ProductTreeListenerAdapter {

        @Override
        public void productNodeSelected(ProductNode productNode, int clickCount) {
            updateMaskForm(VisatApp.getApp().getSelectedProductSceneView());
        }

        @Override
        public void productRemoved(Product product) {
            if (maskForm.getProduct() == product) {
                updateMaskForm(VisatApp.getApp().getSelectedProductSceneView());
            }
        }
    }

    private class MaskIFL extends InternalFrameAdapter {

        @Override
        public void internalFrameClosed(InternalFrameEvent internalFrameEvent) {
            updateMaskForm(VisatApp.getApp().getSelectedProductSceneView());
        }

        @Override
        public void internalFrameActivated(InternalFrameEvent internalFrameEvent) {
            final Container contentPane = internalFrameEvent.getInternalFrame().getContentPane();
            if (contentPane instanceof ProductSceneView) {
                updateMaskForm((ProductSceneView) contentPane);
            }
        }
    }
}
