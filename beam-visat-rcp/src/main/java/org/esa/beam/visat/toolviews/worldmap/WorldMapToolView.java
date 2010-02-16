/*
 * $Id: WorldMapToolView.java,v 1.1 2007/04/19 10:41:39 norman Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
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
package org.esa.beam.visat.toolviews.worldmap;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.ui.WorldMapPane;
import org.esa.beam.framework.ui.WorldMapPaneDataModel;
import org.esa.beam.framework.ui.application.support.AbstractToolView;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.framework.ui.product.ProductTreeListener;
import org.esa.beam.framework.ui.product.ProductTreeListenerAdapter;
import org.esa.beam.visat.VisatApp;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;

/**
 * The window displaying the world map.
 *
 * @author Sabine Embacher
 * @author Norman Fomferra
 * @author Marco Peters
 * @version $Revision$ $Date$
 */
public class WorldMapToolView extends AbstractToolView {

    public static final String ID = WorldMapToolView.class.getName();

    private VisatApp visatApp;
    private WorldMapPaneDataModel worldMapDataModel;


    public WorldMapToolView() {
        visatApp = VisatApp.getApp();
    }

    @Override
    public JComponent createControl() {
        final JPanel mainPane = new JPanel(new BorderLayout(4, 4));
        mainPane.setPreferredSize(new Dimension(320, 160));

        worldMapDataModel = new WorldMapPaneDataModel();
        final WorldMapPane worldMapPane = new WorldMapPane(worldMapDataModel);
        worldMapPane.setNavControlVisible(true);
        mainPane.add(worldMapPane, BorderLayout.CENTER);

        visatApp.addProductTreeListener(new WorldMapPTL());

        // Add an internal frame listener to VISAT so that we can update our
        // world map window with the information of the currently activated
        // product scene view.
        //
        visatApp.addInternalFrameListener(new WorldMapIFL());
        setProducts(visatApp.getProductManager().getProducts());
        setSelectedProduct(visatApp.getSelectedProduct());

        return mainPane;
    }

    public void setSelectedProduct(Product product) {
        worldMapDataModel.setSelectedProduct(product);
    }

    public Product getSelectedProduct() {
        return worldMapDataModel.getSelectedProduct();
    }


    public void setProducts(Product[] products) {
        worldMapDataModel.setProducts(products);
    }

    public void setPathesToDisplay(GeoPos[][] geoBoundaries) {
        worldMapDataModel.setAdditionalGeoBoundaries(geoBoundaries);
    }

    public void packIfNeeded() {
        getPaneWindow().pack();
    }

    private class WorldMapPTL extends ProductTreeListenerAdapter {

        private WorldMapPTL() {
        }

        @Override
        public void productAdded(final Product product) {
            worldMapDataModel.addProduct(product);
            setSelectedProduct(product);
        }

        @Override
        public void productRemoved(final Product product) {
            if (getSelectedProduct() == product) {
                setSelectedProduct(null);
            }
            worldMapDataModel.removeProduct(product);
        }

        @Override
        public void productNodeSelected(ProductNode productNode, int clickCount) {
            setSelectedProduct(productNode.getProduct());
        }
    }

    private class WorldMapIFL extends InternalFrameAdapter {

        @Override
        public void internalFrameActivated(final InternalFrameEvent e) {
            final Container contentPane = e.getInternalFrame().getContentPane();
            Product product = null;
            if (contentPane instanceof ProductSceneView) {
                product = ((ProductSceneView) contentPane).getProduct();
            }
            setSelectedProduct(product);
        }
    }
}
