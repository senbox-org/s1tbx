/*
 * Copyright (C) 2013 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.dat.dialogs;

import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.ModelessDialog;
import org.esa.beam.visat.VisatApp;
import org.esa.nest.dat.toolviews.Projects.ProductSet;

import javax.swing.*;
import java.awt.*;

/**
 * Created by IntelliJ IDEA.
 * User: lveci
 * Date: Jun 5, 2008
 * To change this template use File | Settings | File Templates.
 */
public class ProductSetDialog extends ModelessDialog {

    private final FileTable productSetTable = new FileTable();
    private final JTextField nameField;
    private final ProductSet productSet;

    private boolean ok = false;

    public ProductSetDialog(String title, ProductSet prodSet) {
        super(VisatApp.getApp().getMainFrame(), title, ModalDialog.ID_OK_CANCEL, null);
        productSet = prodSet;

        productSetTable.setFiles(productSet.getFileList());

        final JComponent content =  ProductSetPanel.createComponent(productSetTable, true);

        final JPanel topPanel = new JPanel(new BorderLayout(4, 4));
        final JLabel nameLabel = new JLabel("Name:");
        topPanel.add(nameLabel, BorderLayout.WEST);
        nameField = new JTextField(productSet.getName());
        topPanel.add(nameField, BorderLayout.CENTER);

        content.add(topPanel, BorderLayout.NORTH);

        setContent(content);
    }

    @Override
    protected void onOK() {
        productSet.setName(nameField.getText());
        productSet.setFileList(productSetTable.getFileList());
        productSet.Save();
        
        ok = true;
        hide();
    }

    public boolean IsOK() {
        return ok;
    }

}