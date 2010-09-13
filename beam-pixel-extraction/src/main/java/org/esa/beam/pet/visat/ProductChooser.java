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
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.ModalDialog;

import javax.swing.DefaultListModel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.Window;

/**
 * @author Thomas Storm
 */
public class ProductChooser extends ModalDialog {

    private final DefaultListModel listModel;

    public ProductChooser(Window parent, String title, int buttonMask, String helpID, Product[] products) {
        super(parent, title, buttonMask, helpID);

        TableLayout layout = new TableLayout(2);
        JPanel panel = new JPanel(layout);
        listModel = new DefaultListModel();
        panel.add(new JScrollPane(new JList(listModel)));

        for (Product product : products) {
            listModel.addElement(product);
        }

        setContent(panel);
    }

    public Product[] getSelectedProducts() {
        Product[] result = new Product[listModel.size()];
        for (int i = 0; i < result.length; i++) {
            result[i] = (Product) listModel.get(i);

        }
        return result;
    }
}
