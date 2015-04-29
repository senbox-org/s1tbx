/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.s1tbx.dat.dialogs;

import org.esa.s1tbx.dat.toolviews.Projects.ProductSet;
import org.esa.snap.graphbuilder.rcp.dialogs.support.FileTable;
import org.esa.snap.graphbuilder.rcp.dialogs.ProductSetPanel;
import org.esa.snap.framework.ui.ModalDialog;
import org.esa.snap.framework.ui.ModelessDialog;
import org.esa.snap.rcp.SnapApp;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.BorderLayout;

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

    public ProductSetDialog(final String title, final ProductSet prodSet) {
        super(SnapApp.getDefault().getMainFrame(), title, ModalDialog.ID_OK_CANCEL, null);
        productSet = prodSet;

        productSetTable.setFiles(productSet.getFileList());

        final ProductSetPanel content = new ProductSetPanel(new SnapApp.SnapContext(), "", productSetTable, false, true);

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
