/*
 * Copyright (C) 2012 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.dat.wizards;

import org.esa.nest.dat.toolviews.productlibrary.WorldMapUI;
import org.esa.nest.db.ProductEntry;

import javax.swing.*;
import java.awt.*;
import java.io.File;

/**
   Map Panel
 */
public abstract class AbstractMapPanel extends WizardPanel {

    protected final File[] productFileList;
    protected final File targetFolder;

    public AbstractMapPanel(final File[] productFileList, final File targetFolder) {
        super("Viewing the footprint");
        this.productFileList = productFileList;
        this.targetFolder = targetFolder;

        createPanel(productFileList);
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

    public abstract WizardPanel getNextPanel();

    public boolean validateInput() {
        return true;
    }

    protected String getInstructions() {
        return  "View the footprint of the input products on the world map\n"+
                "Use the mouse wheel to zoom in and out. Hold and drag the right mouse button to pan\n";
    }

    private void createPanel(final File[] productFileList) {

        final JPanel textPanel = createTextPanel("Instructions", getInstructions());
        this.add(textPanel, BorderLayout.NORTH);

        final WorldMapUI worldMapUI = new WorldMapUI();
        this.add(worldMapUI.getWorlMapPane(), BorderLayout.CENTER);

        worldMapUI.setProductEntryList(ProductEntry.createProductEntryList(productFileList));
    }
}