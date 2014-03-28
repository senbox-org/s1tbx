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

package org.esa.beam.framework.ui.product;

import com.bc.ceres.swing.TableLayout;
import com.jidesoft.swing.CheckBoxList;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.ModalDialog;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListCellRenderer;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Thomas Storm
 */
class ProductChooser extends ModalDialog {

    private final CheckBoxList productsList;
    private final JCheckBox selectAll;
    private final JCheckBox selectNone;

    ProductChooser(Window parent, String title, int buttonMask, String helpID, Product[] products) {
        super(parent, title, buttonMask, helpID);

        TableLayout layout = new TableLayout(1);
        layout.setTableFill(TableLayout.Fill.BOTH);
        layout.setRowWeightY(0, 1.0);
        layout.setRowWeightY(1, 0.0);
        layout.setTableWeightX(1.0);
        JPanel panel = new JPanel(layout);

        ProductListModel listModel = new ProductListModel();
        selectAll = new JCheckBox("Select all");
        selectNone = new JCheckBox("Select none", true);

        productsList = new CheckBoxList(listModel);
        productsList.setCellRenderer(new ProductListCellRenderer());
        productsList.getCheckBoxListSelectionModel().addListSelectionListener(new ListSelectionListener() {

            @Override
            public void valueChanged(ListSelectionEvent e) {
                final int length = productsList.getCheckBoxListSelectedIndices().length;
                if (length == 0) {
                    selectNone.setSelected(true);
                } else if (length == productsList.getModel().getSize()) {
                    selectAll.setSelected(true);
                } else {
                    selectNone.setSelected(false);
                    selectAll.setSelected(false);
                }
            }
        });
        for (Product product : products) {
            listModel.addElement(product);
        }

        panel.add(new JScrollPane(productsList));
        panel.add(createButtonsPanel());

        setContent(panel);
    }

    List<Product> getSelectedProducts() {
        List<Product> selectedProducts = new ArrayList<>();
        for (int i = 0; i < productsList.getModel().getSize(); i++) {
            if (productsList.getCheckBoxListSelectionModel().isSelectedIndex(i)) {
                selectedProducts.add((Product) productsList.getModel().getElementAt(i));
            }
        }
        return selectedProducts;
    }

    private JPanel createButtonsPanel() {
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        selectAll.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                selectNone.setSelected(false);
                productsList.getCheckBoxListSelectionModel().setSelectionInterval(0,
                                                                                  productsList.getModel().getSize() - 1);
            }
        });
        selectNone.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                selectAll.setSelected(false);
                productsList.getCheckBoxListSelectionModel().clearSelection();
            }
        });
        selectAll.setMnemonic('a');
        selectNone.setMnemonic('n');
        buttonsPanel.add(selectAll);
        buttonsPanel.add(selectNone);
        return buttonsPanel;
    }

    private static class ProductListCellRenderer implements ListCellRenderer<Product> {

        private DefaultListCellRenderer delegate = new DefaultListCellRenderer();

        @Override
        public Component getListCellRendererComponent(JList list, Product value, int index, boolean isSelected,
                                                      boolean cellHasFocus) {
            JLabel label = (JLabel) delegate.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            label.setText(value.getDisplayName());
            return label;
        }

    }

    private static class ProductListModel extends DefaultListModel<Product> {

        @Override
        public void addElement(Product product) {
            boolean alreadyContained = false;
            for (int i = 0; i < getSize(); i++) {
                String currentProductName = get(i).getName();
                String newProductName = product.getName();
                alreadyContained |= currentProductName.equals(newProductName);
            }

            if (!alreadyContained) {
                super.addElement(product);
            }
        }
    }


}
