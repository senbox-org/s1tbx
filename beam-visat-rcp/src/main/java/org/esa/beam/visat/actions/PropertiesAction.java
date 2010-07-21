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

package org.esa.beam.visat.actions;

import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.visat.VisatApp;
import org.esa.beam.visat.dialogs.PropertyEditor;

public class PropertiesAction extends ExecCommand {

    public PropertiesAction() {

    }

    @Override
    public void actionPerformed(CommandEvent event) {
        showEditor(VisatApp.getApp());
    }

    @Override
    public void updateState(CommandEvent event) {
        final ProductNode node = VisatApp.getApp().getSelectedProductNode();
        event.getCommand().setEnabled(PropertyEditor.isValidNode(node));
    }

    private static void showEditor(final VisatApp visatApp) {
        final ProductNode selectedProductNode = visatApp.getSelectedProductNode();
        if (selectedProductNode != null) {
            final PropertyEditor propertyEditor = new PropertyEditor(visatApp);
            propertyEditor.show(selectedProductNode);
        }
        visatApp.updateState();
    }
}
