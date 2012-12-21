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
package org.esa.nest.dat.wizards;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.visat.VisatApp;
import org.esa.nest.dat.dialogs.SourceProductPanel;

import javax.swing.*;
import java.awt.*;

/**

 */
public abstract class AbstractInputPanel extends WizardPanel {

    protected SourceProductPanel sourcePanel;

    public AbstractInputPanel() {
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
        final Product product = sourcePanel.getSelectedSourceProduct();
        if(product == null) {
            showErrorMsg("Please select a source product");
            return false;
        }
        return true;
    }

    public abstract WizardPanel getNextPanel();

    protected abstract String getInstructions();

    private void createPanel() {

        final JPanel textPanel = createTextPanel("Instructions", getInstructions());
        this.add(textPanel, BorderLayout.NORTH);

        sourcePanel = new SourceProductPanel(VisatApp.getApp());
        sourcePanel.initProducts();
        this.add(sourcePanel, BorderLayout.CENTER);
    }
}