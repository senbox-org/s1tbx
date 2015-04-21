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
package org.esa.s1tbx.dat.wizards;

import org.esa.snap.dat.dialogs.FileTable;
import org.esa.snap.dat.dialogs.ProductSetPanel;
import org.esa.snap.rcp.SnapApp;

import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.io.File;

/**
 * Input Panel
 */
public abstract class AbstractMultipleInputPanel extends WizardPanel {

    protected ProductSetPanel productSetPanel;

    public AbstractMultipleInputPanel() {
        super("Input");

        createPanel();
    }

    public void returnFromLaterStep() {
    }

    public boolean canRedisplayNextPanel() {
        return false;
    }

    public boolean hasNextPanel() {
        return true;
    }

    public boolean canFinish() {
        return false;
    }

    public boolean validateInput() {
        final File[] fileList = productSetPanel.getFileList();
        if (fileList.length == 0 || (fileList.length == 1 && !fileList[0].exists())) {
            showErrorMsg("Please add some products to the table");
            return false;
        }
        return true;
    }

    public abstract WizardPanel getNextPanel();

    protected String getInstructions() {
        return "Browse for input products with the Add button, use the Add All Open button to add every product opened " +
                "or drag and drop products into the table.\n" +
                "Specify the target folder where the products will be written to.\n";
    }

    private void createPanel() {

        final JPanel textPanel = createTextPanel("Instructions", getInstructions());
        this.add(textPanel, BorderLayout.NORTH);

        productSetPanel = new ProductSetPanel(new SnapApp.SnapContext(), null, new FileTable(), true, true);
        this.add(productSetPanel, BorderLayout.CENTER);
    }
}
