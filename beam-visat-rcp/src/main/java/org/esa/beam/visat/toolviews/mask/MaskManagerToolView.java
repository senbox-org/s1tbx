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

import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.ProductNodeEvent;
import org.esa.beam.framework.datamodel.ProductNodeListener;
import org.esa.beam.framework.datamodel.ProductNodeListenerAdapter;
import org.esa.beam.framework.help.HelpSys;
import org.esa.beam.framework.ui.application.support.AbstractToolView;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.visat.VisatApp;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import java.awt.Container;

public class MaskManagerToolView extends AbstractToolView {
    public static final String ID = MaskManagerToolView.class.getName();

    private final ProductNodeListener titleUpdater;
    private ProductSceneView productSceneView;
    private MaskManagerForm form;

    public MaskManagerToolView() {
        this.titleUpdater = createTitleUpdater();
    }

    public void setProductSceneView(final ProductSceneView productSceneView) {
        final ProductSceneView productSceneViewOld = this.productSceneView;
        if (productSceneViewOld == productSceneView) {
            return;
        }
        if (this.productSceneView != null) {
            this.productSceneView.getProduct().removeProductNodeListener(titleUpdater);
        }
        this.productSceneView = productSceneView;
        if (this.productSceneView != null) {
            this.productSceneView.getProduct().addProductNodeListener(titleUpdater);
        }

        form.setSceneView(this.productSceneView);

        updateTitle();
    }

    private void updateTitle() {
        final String titleAddtion;
        if (productSceneView != null) {
            if (productSceneView.isRGB()) {
                titleAddtion = " - " + productSceneView.getProduct().getProductRefString() + " RGB";
            } else {
                titleAddtion = " - " + productSceneView.getRaster().getDisplayName();
            }
        } else {
            titleAddtion = "";
        }
        setTitle(getDescriptor().getTitle() + titleAddtion);
    }

    private void updateFormState() {
        form.updateState();
    }

    private ProductNodeListener createTitleUpdater() {
        return new ProductNodeListenerAdapter() {
            @Override
            public void nodeChanged(final ProductNodeEvent event) {
                if (event.getPropertyName().equalsIgnoreCase(ProductNode.PROPERTY_NAME_NAME)) {
                    final ProductNode sourceNode = event.getSourceNode();
                    if ((productSceneView.isRGB() && sourceNode == productSceneView.getProduct())
                            || sourceNode == productSceneView.getRaster()) {
                        updateTitle();
                    }
                }
            }
        };
    }

    @Override
    public JComponent createControl() {

        form = new MaskManagerForm();

        AbstractButton helpButton = form.getHelpButton();
        helpButton.setName("helpButton");
        if (getDescriptor().getHelpId() != null) {
            HelpSys.enableHelpKey(getPaneControl(), getDescriptor().getHelpId());
            HelpSys.enableHelpOnButton(helpButton, getDescriptor().getHelpId());
        }

        JPanel contentPanel = form.createContentPanel();

        setProductSceneView(VisatApp.getApp().getSelectedProductSceneView());

        // Add an internal frame listsner to VISAT so that we can update our
        // mask manager with the information of the currently activated
        // product scene view.
        VisatApp.getApp().addInternalFrameListener(new MaskManagerIFL());

        updateFormState();

        return contentPanel;
    }

    private class MaskManagerIFL extends InternalFrameAdapter {

        @Override
        public void internalFrameActivated(InternalFrameEvent e) {
            final Container content = getContent(e);
            if (content instanceof ProductSceneView) {
                setProductSceneView((ProductSceneView) content);
            } else {
                setProductSceneView(null);
            }
        }

        @Override
        public void internalFrameDeactivated(InternalFrameEvent e) {
            if (getContent(e) instanceof ProductSceneView) {
                setProductSceneView(null);
            }
        }

        private Container getContent(InternalFrameEvent e) {
            return e.getInternalFrame().getContentPane();
        }
    }
}