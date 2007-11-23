/*
 * $Id: ProductExpressionPane.java,v 1.1 2006/10/10 14:47:37 norman Exp $
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
package org.esa.beam.framework.ui.product;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.swing.Box;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.framework.dataop.barithm.BandArithmetic;
import org.esa.beam.framework.ui.ExpressionPane;
import org.esa.beam.util.PropertyMap;

import com.bc.jexp.Namespace;
import com.bc.jexp.impl.ParserImpl;

/**
 * An expression pane to be used in conjunction with {@link Product}s in order to edit and assemple band arithmetic expressions.
 */
public class ProductExpressionPane extends ExpressionPane {

    private Product[] products;
    private Product currentProduct;
    private JComboBox productBox;
    private JList nodeList;
    private JCheckBox inclBandsCheck;
    private JCheckBox inclGridsCheck;
    private JCheckBox inclFlagsCheck;

    protected ProductExpressionPane(boolean booleanExpr, Product[] products, Product currentProduct,
                                    PropertyMap preferences) {
        super(booleanExpr, null, preferences);
        if (products == null || products.length == 0) {
            throw new IllegalArgumentException("no products given");
        }
        this.products = products;
        this.currentProduct = currentProduct != null ? currentProduct : this.products[0];
        init();
    }

    public static ProductExpressionPane createBooleanExpressionPane(Product[] products, Product currentProduct,
                                                                    PropertyMap preferences) {
        return new ProductExpressionPane(true, products, currentProduct, preferences);
    }

    public static ProductExpressionPane createGeneralExpressionPane(Product[] products, Product currentProduct,
                                                                    PropertyMap preferences) {
        return new ProductExpressionPane(false, products, currentProduct, preferences);
    }

    public Product getCurrentProduct() {
        return currentProduct;
    }

    protected void init() {

        final int defaultIndex = Arrays.asList(products).indexOf(currentProduct);
        Namespace namespace = BandArithmetic.createDefaultNamespace(products,
                                                                    defaultIndex == -1 ? 0 : defaultIndex);
        // todo - make type checking an option (checkbox) in UI
        setParser(new ParserImpl(namespace, false));

        final ActionListener resetNodeListAL = new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (e.getSource() == productBox) {
                    setCurrentProduct();
                }
                resetNodeList();
            }
        };

        inclBandsCheck = new JCheckBox("Show bands"); /*I18N*/
        inclBandsCheck.addActionListener(resetNodeListAL);
        if (!isBooleanExpressionPreferred()) {
            inclBandsCheck.setSelected(true);
        }

        inclGridsCheck = new JCheckBox("Show tie point grids"); /*I18N*/
        inclGridsCheck.addActionListener(resetNodeListAL);

        inclFlagsCheck = new JCheckBox("Show single flags"); /*I18N*/
        inclFlagsCheck.addActionListener(resetNodeListAL);
        if (isBooleanExpressionPreferred()) {
            inclFlagsCheck.setSelected(true);
        }

        nodeList = createPatternList();
        JScrollPane scrollableNodeList = new JScrollPane(nodeList);

        Box inclNodeBox = Box.createVerticalBox();
        inclNodeBox.add(inclBandsCheck);
        inclNodeBox.add(inclGridsCheck);
        inclNodeBox.add(inclFlagsCheck);

        JPanel nodeListPane = new JPanel(new BorderLayout());
        nodeListPane.add(new JLabel("Data sources: "), BorderLayout.NORTH); /*I18N*/
        nodeListPane.add(scrollableNodeList, BorderLayout.CENTER);
        nodeListPane.add(inclNodeBox, BorderLayout.SOUTH);

        JPanel accessoryPane = createDefaultAccessoryPane(nodeListPane);
        setLeftAccessory(accessoryPane);

        if (!isBooleanExpressionPreferred()) {

            List<String> nameList = new ArrayList<String>(products.length);
            for (Product product : products) {
                String productName = product.getDisplayName();
                nameList.add(productName);
            }
            String currentProductName = currentProduct.getDisplayName();
            final String[] productNames = new String[nameList.size()];
            nameList.toArray(productNames);
            productBox = new JComboBox(productNames);
            productBox.setEditable(false);
            productBox.setEnabled(products.length > 1);
            productBox.addActionListener(resetNodeListAL);
            productBox.setSelectedItem(currentProductName);

            JPanel productPane = new JPanel(new BorderLayout());
            productPane.add(new JLabel("Product: "), BorderLayout.WEST);
            productPane.add(productBox, BorderLayout.CENTER);

            setTopAccessory(productPane);
        }

        resetNodeList();
    }

    @Override
    public void dispose() {
        products = null;
        currentProduct = null;
        productBox = null;
        nodeList = null;
        inclBandsCheck = null;
        inclGridsCheck = null;
        inclFlagsCheck = null;
        super.dispose();
    }

    private void resetNodeList() {
        setCurrentProduct();
        List<String> listEntries = new ArrayList<String>(64);
        if (currentProduct != null) {
            String[] flagNames = currentProduct.getAllFlagNames();
            boolean hasBands = currentProduct.getNumBands() > 0;
            boolean hasGrids = currentProduct.getNumTiePointGrids() > 0;
            boolean hasFlags = flagNames.length > 0;
            boolean inclBands = inclBandsCheck.isSelected();
            boolean inclGrids = inclGridsCheck.isSelected();
            boolean inclFlags = inclFlagsCheck.isSelected();
            inclBandsCheck.setEnabled(hasBands);
            inclGridsCheck.setEnabled(hasGrids);
            inclFlagsCheck.setEnabled(hasFlags);
            if (!hasBands && inclBands) {
                inclBandsCheck.setSelected(false);
                inclBands = false;
            }
            if (!hasGrids && inclGrids) {
                inclGridsCheck.setSelected(false);
                inclGrids = false;
            }
            if (!hasFlags && inclFlags) {
                inclFlagsCheck.setSelected(false);
                inclFlags = false;
            }
            nodeList.setEnabled(inclBands || inclGrids || inclFlags);
            final String namePrefix = getNodeNamePrefix();
            if (inclBands) {
                addBandNameRefs(currentProduct, namePrefix, listEntries);
            }
            if (inclGrids) {
                addGridNameRefs(currentProduct, namePrefix, listEntries);
            }
            if (inclFlags) {
                addFlagNameRefs(namePrefix, flagNames, listEntries);
            }
        } else {
            nodeList.setEnabled(false);
            inclBandsCheck.setEnabled(false);
            inclGridsCheck.setEnabled(false);
            inclFlagsCheck.setEnabled(false);
        }
        nodeList.setListData(listEntries.toArray());
    }

    private void setCurrentProduct() {
        if (productBox != null) {
            int index = productBox.getSelectedIndex();
            if (index != -1) {
                currentProduct = products[index];
            } else {
                currentProduct = null;
            }
        }
    }

    private String getNodeNamePrefix() {
        final String namePrefix;
        if (products.length > 1) {
            namePrefix = BandArithmetic.getProductNodeNamePrefix(currentProduct);
        } else {
            namePrefix = "";
        }
        return namePrefix;
    }

    private static void addBandNameRefs(Product product, String namePrefix, List<String> list) {
        for (int j = 0; j < product.getNumBands(); j++) {
            Band band = product.getBandAt(j);
            list.add(namePrefix + band.getName());
        }
    }

    private static void addGridNameRefs(Product product, String namePrefix, List<String> list) {
        for (int j = 0; j < product.getNumTiePointGrids(); j++) {
            TiePointGrid grid = product.getTiePointGridAt(j);
            list.add(namePrefix + grid.getName());
        }
    }

    private static void addFlagNameRefs(String namePrefix, String[] flagNames, List<String> list) {
        for (String flagName : flagNames) {
            list.add(namePrefix + flagName);
        }
    }

}
