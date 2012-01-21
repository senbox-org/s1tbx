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

package org.esa.beam.visat.toolviews.placemark;

import com.bc.ceres.swing.selection.AbstractSelectionChangeListener;
import com.bc.ceres.swing.selection.SelectionChangeEvent;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.ui.application.support.AbstractToolView;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.framework.ui.product.ProductTreeListenerAdapter;
import org.esa.beam.framework.ui.product.SimpleFeatureFigure;
import org.esa.beam.visat.VisatApp;

import javax.swing.*;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import java.awt.*;

/**
 * A dialog used to manage the list of pins or ground control points associated
 * with a selected product.
 */
public class PlacemarkEditorToolView extends AbstractToolView {

    private VisatApp visatApp;

    private Product product;
    private VectorDataNode vectorDataNode;
    private ProductSceneView view;
    private JLabel editor;
    private final PlacemarkEditorToolView.IFL ifl;
    private final PTL ptl;
    private final PNL pl;
    private final SCL scl;
    private String titleBase;

    public PlacemarkEditorToolView() {
        visatApp = VisatApp.getApp();
        ptl = new PTL();
        ifl = new IFL();
        pl = new PNL();
        scl = new SCL();
    }

    @Override
    public JComponent createControl() {
        titleBase = getTitle();
        editor = new JLabel();
        return editor;
    }

    @Override
    public void componentOpened() {

        setView(visatApp.getSelectedProductSceneView());
        setProduct(visatApp.getSelectedProduct());
        updateEditor();

        visatApp.addInternalFrameListener(ifl);
        visatApp.getProductTree().addProductTreeListener(ptl);
    }

    @Override
    public void componentHidden() {
        visatApp.removeInternalFrameListener(ifl);
        visatApp.getProductTree().removeProductTreeListener(ptl);
    }

    private void setView(ProductSceneView view) {
        if (this.view != view) {
            ProductSceneView oldView = this.view;
            this.view = view;
            handleViewChanged(oldView, this.view);
        }
    }

    private void handleViewChanged(ProductSceneView oldView, ProductSceneView newView) {
        if (oldView != null) {
            oldView.getSelectionContext().removeSelectionChangeListener(scl);
        }
        if (newView != null) {
            newView.getSelectionContext().addSelectionChangeListener(scl);
        }
    }

    private Product getProduct() {
        return product;
    }

    public void setProduct(Product product) {
        if (this.product != product) {
            Product oldProduct = this.product;
            this.product = product;
            handleProductChanged(oldProduct, this.product);
        }
    }

    private void handleProductChanged(Product oldProduct, Product newProduct) {
        if (oldProduct != null) {
            oldProduct.removeProductNodeListener(pl);
        }
        if (newProduct != null) {
            newProduct.addProductNodeListener(pl);
        }
    }

    public void setVectorDataNode(VectorDataNode vectorDataNode) {
        if (this.vectorDataNode != vectorDataNode) {
            VectorDataNode oldVectorDataNode = this.vectorDataNode;
            this.vectorDataNode = vectorDataNode;
            handleVectorDataNodeChanged(oldVectorDataNode, this.vectorDataNode);
        }
    }

    private void handleVectorDataNodeChanged(VectorDataNode oldVectorDataNode, VectorDataNode newVectorDataNode) {

        if (newVectorDataNode != null) {
            setProduct(newVectorDataNode.getProduct());
        } else {
            setProduct(null);
        }

        if (vectorDataNode != null) {
            setTitle(titleBase + " - " + vectorDataNode.getName());
        } else {
            setTitle(titleBase);
        }

        updateEditor();
    }


    private PlacemarkGroup getPlacemarkGroup() {
        if (vectorDataNode != null) {
            return vectorDataNode.getPlacemarkGroup();
        }
        return null;
    }


    private void updateEditor() {
        if (vectorDataNode != null) {
            int selectedFigureCount = 0;
            if (view != null) {
                SimpleFeatureFigure[] selectedFeatureFigures = view.getSelectedFeatureFigures();
                selectedFigureCount = selectedFeatureFigures.length;
            }

            editor.setText(String.format("<html>" +
                    "Vector data node <b>%s</b><br>" +
                    "%d placemark(s)<br>" +
                    "%d feature(s)<br>" +
                    "%d figure(s) selected</html>",
                    vectorDataNode.getName(),
                    vectorDataNode.getPlacemarkGroup().getNodeCount(),
                    vectorDataNode.getFeatureCollection().size(),
                    selectedFigureCount));
        } else {
            editor.setText("No selection.");
        }
    }

    private class SCL extends AbstractSelectionChangeListener {
        @Override
        public void selectionChanged(SelectionChangeEvent event) {
            updateEditor();
        }
    }

    private class PNL implements ProductNodeListener {

        @Override
        public void nodeChanged(ProductNodeEvent event) {
            ProductNode sourceNode = event.getSourceNode();
            if (sourceNode.getOwner() == getPlacemarkGroup() && sourceNode instanceof Placemark) {
                updateEditor();
            }
        }

        @Override
        public void nodeDataChanged(ProductNodeEvent event) {
            ProductNode sourceNode = event.getSourceNode();
            if (sourceNode.getOwner() == getPlacemarkGroup() && sourceNode instanceof Placemark) {
                updateEditor();
            }
        }

        @Override
        public void nodeAdded(ProductNodeEvent event) {
            ProductNode sourceNode = event.getSourceNode();
            if (sourceNode.getOwner() == getPlacemarkGroup() && sourceNode instanceof Placemark) {
                updateEditor();
            }
        }

        @Override
        public void nodeRemoved(ProductNodeEvent event) {
            ProductNode sourceNode = event.getSourceNode();
            if (sourceNode.getOwner() == getPlacemarkGroup() && sourceNode instanceof Placemark) {
                updateEditor();
            }
        }

    }

    private class IFL extends InternalFrameAdapter {

        @Override
        public void internalFrameActivated(InternalFrameEvent e) {
            Container contentPane = e.getInternalFrame().getContentPane();
            if (contentPane instanceof ProductSceneView) {
                setView((ProductSceneView) contentPane);
            }
        }

        @Override
        public void internalFrameDeactivated(InternalFrameEvent e) {
            Container contentPane = e.getInternalFrame().getContentPane();
            if (contentPane instanceof ProductSceneView) {
                ProductSceneView sceneView = (ProductSceneView) contentPane;
                if (sceneView == view) {
                    setView(null);
                }
            }
        }

    }

    private class PTL extends ProductTreeListenerAdapter {

        @Override
        public void productRemoved(Product product) {
            if (product == getProduct()) {
                setProduct(null);
            }
        }

        @Override
        public void vectorDataSelected(VectorDataNode vectorDataNode, int clickCount) {
            setVectorDataNode(vectorDataNode);
        }
    }


}
