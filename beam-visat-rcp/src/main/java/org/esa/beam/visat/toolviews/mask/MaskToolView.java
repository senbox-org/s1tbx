/*
 * $Id: MaskManagerToolView.java,v 1.1 2007/04/19 10:41:38 norman Exp $
 *
 * Copyright (C) 2009 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.visat.toolviews.mask;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.ProductNodeEvent;
import org.esa.beam.framework.datamodel.ProductNodeListener;
import org.esa.beam.framework.datamodel.ProductNodeListenerAdapter;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.TiePointGrid;
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

    private final ProductNodeListener titleUpdater;
    private ProductSceneView sceneView;
    private MaskForm maskForm;
    private String prefixTitle;

    public MaskToolView() {
        this.titleUpdater = createTitleUpdater();
    }

    private void setSceneView(final ProductSceneView sceneView) {
        if (this.sceneView != sceneView) {
            if (this.sceneView != null) {
                this.sceneView.getProduct().removeProductNodeListener(titleUpdater);
            }
            this.sceneView = sceneView;
            if (this.sceneView != null) {
                this.sceneView.getProduct().addProductNodeListener(titleUpdater);
            }

            if (this.sceneView != null) {
                maskForm.reconfigureMaskTable(this.sceneView.getProduct(), this.sceneView.getRaster());
            } else {
                maskForm.clearMaskTable();
            }
        }
        updateTitle();
    }

    private void updateTitle() {
        final String titleAddtion;
        if (sceneView != null) {
            if (sceneView.isRGB()) {
                titleAddtion = " - " + sceneView.getProduct().getProductRefString() + " RGB";
            } else {
                titleAddtion = " - " + sceneView.getRaster().getDisplayName();
            }
        } else {
            if (maskForm.getRaster() != null) {
                titleAddtion = " - " + maskForm.getRaster().getDisplayName();
            } else if (maskForm.getProduct() != null) {
                titleAddtion = " - " + maskForm.getProduct().getDisplayName();
            } else {
                titleAddtion = "";
            }
        }
        getDescriptor().setTitle(prefixTitle + titleAddtion);
    }

    private ProductNodeListener createTitleUpdater() {
        return new ProductNodeListenerAdapter() {
            @Override
            public void nodeChanged(final ProductNodeEvent event) {
                if (event.getPropertyName().equalsIgnoreCase(ProductNode.PROPERTY_NAME_NAME)) {
                    final ProductNode sourceNode = event.getSourceNode();
                    if ((sceneView.isRGB() && sourceNode == sceneView.getProduct())
                            || sourceNode == sceneView.getRaster()) {
                        updateTitle();
                    }
                }
            }
        };
    }

    @Override
    public JComponent createControl() {
        prefixTitle = getDescriptor().getTitle();
        maskForm = createMaskForm(this, new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                if (sceneView != null) {
                    Mask selectedMask = maskForm.getSelectedMask();
                    if (selectedMask != null) {
                        VectorDataNode vectorDataNode = Mask.VectorDataType.getVectorData(selectedMask);
                        if (vectorDataNode != null) {
                            VisatApp.getApp().setSelectedProductNode(vectorDataNode);
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

        ProductSceneView selectedProductSceneView = VisatApp.getApp().getSelectedProductSceneView();
        if (selectedProductSceneView != null) {
            setSceneView(selectedProductSceneView);
        } else {
            ProductNode selectedProductNode = VisatApp.getApp().getSelectedProductNode();
            if (selectedProductNode instanceof Product) {
                Product product = (Product) selectedProductNode;
                maskForm.reconfigureMaskTable(product, null);
            } else if (selectedProductNode instanceof RasterDataNode) {
                RasterDataNode rdn = (RasterDataNode) selectedProductNode;
                maskForm.reconfigureMaskTable(rdn.getProduct(), rdn);
            }
        }
        updateTitle();

        // Add an internal frame listsner to VISAT so that we can update our
        // mask manager with the information of the currently activated
        // product scene view.
        VisatApp.getApp().addInternalFrameListener(new MaskIFL());
        VisatApp.getApp().addProductTreeListener(new MaskPTL());

        maskForm.updateState();

        return maskForm.createContentPanel();
    }

    protected abstract MaskForm createMaskForm(AbstractToolView maskToolView, ListSelectionListener selectionListener);

    private class MaskIFL extends InternalFrameAdapter {

        @Override
        public void internalFrameActivated(InternalFrameEvent e) {
            setSceneView(getSceneView(e));
        }

        @Override
        public void internalFrameDeactivated(InternalFrameEvent e) {
            ProductSceneView sceneView = getSceneView(e);
            if (sceneView != null) {
                setSceneView(null);
            }
        }

        private ProductSceneView getSceneView(InternalFrameEvent e) {
            Container contentPane = e.getInternalFrame().getContentPane();
            if (contentPane instanceof ProductSceneView) {
                return (ProductSceneView) contentPane;
            }
            return null;
        }
    }

    private class MaskPTL extends ProductTreeListenerAdapter {
        @Override
        public void productSelected(Product product, int clickCount) {
            maskForm.reconfigureMaskTable(product, null);
            setSceneView(null);
        }

        @Override
        public void bandSelected(Band band, int clickCount) {
            maskForm.reconfigureMaskTable(band.getProduct(), band);
            setSceneView(null);
        }
        
        @Override
        public void tiePointGridSelected(TiePointGrid tiePointGrid, int clickCount) {
            maskForm.reconfigureMaskTable(tiePointGrid.getProduct(), tiePointGrid);
            setSceneView(null);
        }
    }
}