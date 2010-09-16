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

package org.esa.beam.pet.visat;

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

    private CheckBoxList productsList;

    ProductChooser(Window parent, String title, int buttonMask, String helpID, Product[] products) {
        super(parent, title, buttonMask, helpID);

        TableLayout layout = new TableLayout(1);
        layout.setTableFill(TableLayout.Fill.BOTH);
        layout.setRowWeightY(0, 1.0);
        layout.setRowWeightY(1, 0.0);
        layout.setTableWeightX(1.0);
        JPanel panel = new JPanel(layout);

        DefaultListModel listModel = new ProductListModel();
        productsList = new CheckBoxList(listModel);
        productsList.setCellRenderer(new ProductListCellRenderer());
        for (Product product : products) {
            listModel.addElement(product);
        }

        panel.add(new JScrollPane(productsList));
        panel.add(createButtonsPanel());

        setContent(panel);
    }

    List<Product> getSelectedProducts() {
        List<Product> selectedProducts = new ArrayList<Product>();
        for (int i = 0; i < productsList.getModel().getSize(); i++) {
            if (productsList.getCheckBoxListSelectionModel().isSelectedIndex(i)) {
                selectedProducts.add((Product) productsList.getModel().getElementAt(i));
            }
        }
        return selectedProducts;
    }

    private JPanel createButtonsPanel() {
        JPanel buttonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        final JCheckBox selectAll = new JCheckBox("Select all");
        final JCheckBox selectNone = new JCheckBox("Select none");
        selectAll.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                selectAll.setEnabled(false);
                selectNone.setEnabled(true);
                selectNone.setSelected(false);
                productsList.getCheckBoxListSelectionModel().setSelectionInterval(0, productsList.getModel().getSize());
            }
        });
        selectNone.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                selectNone.setEnabled(false);
                selectAll.setEnabled(true);
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

    private static class ProductListCellRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
                                                      boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            Product product = (Product) value;
            label.setText("[" + product.getRefNo() + "] " + product.getName());
            return label;
        }

    }

    private static class ProductListModel extends DefaultListModel {

        @Override
        public void addElement(Object obj) {
            if (!(obj instanceof Product)) {
                throw new IllegalArgumentException(
                        "Only elements of type org.esa.beam.framework.datamodel.Product allowed.");
            }
            boolean alreadyContained = false;
            for (int i = 0; i < getSize(); i++) {
                String currentProductName = ((Product) get(i)).getName();
                String newProductName = ((Product) obj).getName();
                alreadyContained |= currentProductName.equals(newProductName);
            }

            if (!alreadyContained) {
                super.addElement(obj);
            }
        }
    }


}
