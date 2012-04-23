/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

import org.esa.beam.framework.datamodel.VectorDataNode;
import org.esa.beam.framework.ui.BasicApp;
import org.esa.beam.framework.ui.ModalDialog;
import org.opengis.feature.type.AttributeType;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Olaf Danne
 * @author Thomas Storm
 */
class SeparateGeometriesDialog extends ModalDialog {

    private final JComboBox comboBox;

    public SeparateGeometriesDialog(BasicApp.MainFrame mainFrame, VectorDataNode vectorDataNode, String helpId, String text) {
        super(mainFrame, "Import Geometry", ModalDialog.ID_YES_NO_HELP, helpId);
        JPanel content = new JPanel(new BorderLayout());
        content.add(new JLabel(text), BorderLayout.NORTH);

        List<AttributeType> types = vectorDataNode.getFeatureType().getTypes();
        ArrayList<String> names = new ArrayList<String>();
        for (AttributeType type : types) {
            if (type.getBinding().equals(String.class)) {
                names.add(type.getName().getLocalPart());
            }
        }
        comboBox = new JComboBox(names.toArray(new String[names.size()]));
        if (names.size() > 0) {
            JPanel content2 = new JPanel(new BorderLayout());
            content2.add(new JLabel("Attribute for mask/layer naming: "), BorderLayout.WEST);
            content2.add(comboBox, BorderLayout.CENTER);
            content.add(content2, BorderLayout.SOUTH);
        }
        setContent(content);

    }

    String getSelectedAttributeName() {
        if (comboBox.getItemCount() > 0) {
            return comboBox.getSelectedItem().toString();
        } else {
            return null;
        }
    }
}
