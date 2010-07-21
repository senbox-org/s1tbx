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

package org.esa.beam.framework.ui.layer;


import com.bc.ceres.glayer.CollectionLayer;
import com.bc.ceres.glayer.Layer;

import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.border.EmptyBorder;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.ArrayList;

public class CollectionLayerAssistantPage extends AbstractLayerSourceAssistantPage {

    private static final ArrayList<String> names = new ArrayList<String>();
    private JComboBox nameBox;

    static {
        // todo - load names from preferences
        names.add("");
    }

    CollectionLayerAssistantPage() {
        super("Set Layer Name");
    }

    @Override
    public Component createPageComponent() {
        nameBox = new JComboBox(names.toArray());
        nameBox.addItemListener(new NameBoxItemListener());
        nameBox.addActionListener(new NameBoxActionListener());
        nameBox.setEditable(true);

        final JPanel panel = new JPanel(new GridBagLayout());
        final GridBagConstraints constraints = new GridBagConstraints();
        panel.setBorder(new EmptyBorder(4, 4, 4, 4));

        constraints.fill = GridBagConstraints.HORIZONTAL;

        constraints.weighty = 0.1;
        constraints.weightx = 0.1;
        panel.add(new JLabel("Layer name:"), constraints);

        constraints.weightx = 0.9;
        panel.add(nameBox, constraints);

        return panel;
    }

    @Override
    public boolean validatePage() {
        return nameBox.getSelectedItem() != null && !nameBox.getSelectedItem().toString().trim().isEmpty();
    }

    @Override
    public boolean performFinish() {
        Layer layer = new CollectionLayer(nameBox.getSelectedItem().toString().trim());
        Layer rootLayer = getContext().getLayerContext().getRootLayer();
        rootLayer.getChildren().add(0, layer);
        if (!names.contains(layer.getName())) {
            names.add(1, layer.getName());
        }
        return true;
    }

    private class NameBoxItemListener implements ItemListener {

        @Override
        public void itemStateChanged(ItemEvent e) {
            getContext().updateState();
        }
    }

    private class NameBoxActionListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            getContext().updateState();
        }
    }
}