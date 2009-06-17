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

import com.bc.ceres.swing.TableLayout;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.framework.help.HelpSys;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.WorldMapPane;
import org.esa.beam.framework.ui.application.support.AbstractToolView;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.framework.ui.product.ProductTreeListener;
import org.esa.beam.framework.ui.tool.ToolButtonFactory;
import org.esa.beam.visat.VisatApp;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Arrays;
import java.util.List;

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

    private WorldMapPane worldMapPane;
    private JCheckBox checkBoxAuto;
    private JTextField productRefField;
    private AbstractButton nextButton;
    private AbstractButton prevButton;


    public WorldMapToolView() {
        visatApp = VisatApp.getApp();
    }

    @Override
    public JComponent createControl() {
        final JPanel mainPane = new JPanel(new BorderLayout(4, 4));
        mainPane.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        mainPane.setPreferredSize(new Dimension(320, 200));

        worldMapPane = new WorldMapPane();
        mainPane.add(worldMapPane, BorderLayout.CENTER);

        final TableLayout tableLayout = new TableLayout(1);
        tableLayout.setTableFill(TableLayout.Fill.BOTH);
        tableLayout.setTableWeightX(1.0);
        tableLayout.setTableWeightY(0.0);
        tableLayout.setTablePadding(4, 4);
        final JPanel controlPanel = new JPanel(tableLayout);

        checkBoxAuto = new JCheckBox("Autoc.");
        checkBoxAuto.setName("checkBoxAuto");
        checkBoxAuto.setToolTipText("Automatically center selected product");/*I18N*/
        checkBoxAuto.setSelected(true);
        checkBoxAuto.setEnabled(false);
        controlPanel.add(checkBoxAuto);

        final JButton buttonCenter = new JButton("Center");
        buttonCenter.setName("buttonCenter");
        buttonCenter.setToolTipText("Center selected product"); /*I18N*/
        buttonCenter.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                centerSelectedProduct();
            }
        });
        controlPanel.add(buttonCenter);

        tableLayout.setCellPadding(2, 0, new Insets(10, 4, 4, 4));
        controlPanel.add(createProductIterationPanel());

        tableLayout.setRowFill(3, TableLayout.Fill.BOTH);
        tableLayout.setRowWeightY(3, 1.0);
        controlPanel.add(new JPanel());

        final AbstractButton helpButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/Help24.gif"),
                                                                         false);
        helpButton.setToolTipText("Help."); /*I18N*/
        helpButton.setName("helpButton");
        if (getDescriptor().getHelpId() != null) {
            HelpSys.enableHelpOnButton(helpButton, getDescriptor().getHelpId());
            HelpSys.enableHelpKey(mainPane, getDescriptor().getHelpId());
        }
        controlPanel.add(helpButton);

        mainPane.add(controlPanel, BorderLayout.EAST);

        visatApp.addProductTreeListener(new WorldMapPTL());

        // Add an internal frame listener to VISAT so that we can update our
        // world map window with the information of the currently activated
        // product scene view.
        //
        visatApp.addInternalFrameListener(new WorldMapIFL());
        setProducts(visatApp);
        setSelectedProduct(visatApp.getSelectedProduct());

        return mainPane;
    }

    private JPanel createProductIterationPanel() {
        final JPanel panel = new JPanel(new BorderLayout(2, 2));
        productRefField = new JTextField("-");
        productRefField.setEditable(false);
        productRefField.setHorizontalAlignment(JTextField.CENTER);
        final Dimension preferredSize = productRefField.getPreferredSize();
        preferredSize.setSize(30, preferredSize.getHeight());
        productRefField.setPreferredSize(preferredSize);
        panel.add(productRefField, BorderLayout.CENTER);

        nextButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/Right24.gif"),
                                                    false);
        nextButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final Product product = getSelectedProduct();
                setSelectedProduct(getNextProduct(product));
            }
        });
        prevButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/Left24.gif"),
                                                    false);
        prevButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final Product product = getSelectedProduct();
                setSelectedProduct(getPreviousProduct(product));
            }
        });
        final JPanel buttonPanel = new JPanel(new BorderLayout(1, 1));
        buttonPanel.add(prevButton, BorderLayout.WEST);
        buttonPanel.add(nextButton, BorderLayout.EAST);

        panel.add(buttonPanel, BorderLayout.SOUTH);

        return panel;
    }

    private Product getPreviousProduct(Product product) {
        final List<Product> products = Arrays.asList(worldMapPane.getProducts());
        int i = products.indexOf(product) - 1;
        if (i < 0) {
            i = products.size() - 1;
        }
        return products.get(i);
    }

    private Product getNextProduct(Product product) {
        final List<Product> products = Arrays.asList(worldMapPane.getProducts());
        int i = products.indexOf(product) + 1;
        if (i > products.size() - 1) {
            i = 0;
        }
        return products.get(i);
    }

    public void setSelectedProduct(Product product) {
        worldMapPane.setSelectedProduct(product);
        if (checkBoxAuto.isSelected()) {
            centerSelectedProduct();
        }

        if (worldMapPane.getProducts().length == 0) {
            nextButton.setToolTipText("No products available.");
            nextButton.setEnabled(false);
            prevButton.setToolTipText("No products available.");
            prevButton.setEnabled(false);
        } else {
            nextButton.setEnabled(true);
            prevButton.setEnabled(true);
            if (product != null) {
                nextButton.setToolTipText(getNextProduct(product).getDisplayName());
                prevButton.setToolTipText(getPreviousProduct(product).getDisplayName());
            }
        }
        if (product != null) {
            productRefField.setText(product.getProductRefString());
            productRefField.setToolTipText(product.getDisplayName());
        } else {
            productRefField.setText("-");
            productRefField.setToolTipText("No product selected.");
        }
    }

    public Product getSelectedProduct() {
        return worldMapPane.getSelectedProduct();
    }

    private void setProducts(final VisatApp visatApp) {
        setProducts(visatApp.getProductManager().getProducts());
    }


    public void setProducts(Product[] products) {
        worldMapPane.setProducts(products);
        checkBoxAuto.setEnabled(products != null && products.length > 0);
    }

    public void setPathesToDisplay(GeoPos[][] geoBoundaries) {
        worldMapPane.setPathesToDisplay(geoBoundaries);
    }

    private void centerSelectedProduct() {
        final Product product = worldMapPane.getSelectedProduct();
        if (product != null) {
            worldMapPane.zoomToProductCenter(product);
        }
    }

    public void packIfNeeded() {
        getPaneWindow().pack();
    }

    private class WorldMapPTL implements ProductTreeListener {

        private WorldMapPTL() {
        }

        @Override
        public void productAdded(final Product product) {
            setSelectedProduct(product);
            setProducts(visatApp);
        }

        @Override
        public void productRemoved(final Product product) {
            if (getSelectedProduct() == product) {
                setSelectedProduct(null);
            }
            setProducts(visatApp);
        }

        @Override
        public void productSelected(final Product product, final int clickCount) {
            setSelectedProduct(product);
        }

        @Override
        public void metadataElementSelected(final MetadataElement group, final int clickCount) {
            final Product product = group.getProduct();
            setSelectedProduct(product);
        }

        @Override
        public void tiePointGridSelected(final TiePointGrid tiePointGrid, final int clickCount) {
            final Product product = tiePointGrid.getProduct();
            setSelectedProduct(product);
        }

        @Override
        public void bandSelected(final Band band, final int clickCount) {
            final Product product = band.getProduct();
            setSelectedProduct(product);
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

        @Override
        public void internalFrameDeactivated(final InternalFrameEvent e) {
        }
    }
}
