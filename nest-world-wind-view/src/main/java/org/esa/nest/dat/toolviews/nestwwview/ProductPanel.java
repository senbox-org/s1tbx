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
package org.esa.nest.dat.toolviews.nestwwview;

import gov.nasa.worldwind.WorldWindow;

import javax.swing.*;
import javax.swing.border.CompoundBorder;
import javax.swing.border.TitledBorder;
import java.awt.*;
import java.awt.event.ActionEvent;

class ProductPanel extends JPanel {
    private final ProductLayer productLayer;
    private JPanel layersPanel;
    private JPanel westPanel;
    private JScrollPane scrollPane;
    private Font defaultFont = null;

    public ProductPanel(WorldWindow wwd, ProductLayer prodLayer) {
        super(new BorderLayout());
        productLayer = prodLayer;
        this.makePanel(wwd, new Dimension(100, 400));
    }

    public ProductPanel(WorldWindow wwd, Dimension size, ProductLayer prodLayer) {
        super(new BorderLayout());
        productLayer = prodLayer;
        this.makePanel(wwd, size);
    }

    private void makePanel(WorldWindow wwd, Dimension size) {
        // Make and fill the panel holding the layer titles.
        this.layersPanel = new JPanel(new GridLayout(0, 1, 0, 4));
        this.layersPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        this.fill(wwd);

        // Must put the layer grid in a container to prevent scroll panel from stretching their vertical spacing.
        final JPanel dummyPanel = new JPanel(new BorderLayout());
        dummyPanel.add(this.layersPanel, BorderLayout.NORTH);

        // Put the name panel in a scroll bar.
        this.scrollPane = new JScrollPane(dummyPanel);
        this.scrollPane.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        if (size != null)
            this.scrollPane.setPreferredSize(size);

        // Add the scroll bar and name panel to a titled panel that will resize with the main window.
        westPanel = new JPanel(new GridLayout(0, 1, 0, 10));
        westPanel.setBorder(
                new CompoundBorder(BorderFactory.createEmptyBorder(9, 9, 9, 9), new TitledBorder("Products")));
        westPanel.setToolTipText("Products to Show");
        westPanel.add(scrollPane);
        this.add(westPanel, BorderLayout.CENTER);
    }

    private void fill(WorldWindow wwd) {
        final String[] productNames = productLayer.getProductNames();
        for(String name : productNames) {
            final LayerAction action = new LayerAction(productLayer, wwd, name, productLayer.getOpacity(name) != 0);
            final JCheckBox jcb = new JCheckBox(action);
            jcb.setSelected(action.selected);
            this.layersPanel.add(jcb);

            if (defaultFont == null) {
                this.defaultFont = jcb.getFont();
            }
        }
    }

    public void update(WorldWindow wwd) {
        // Replace all the layer names in the layers panel with the names of the current layers.
        this.layersPanel.removeAll();
        this.fill(wwd);
        this.westPanel.revalidate();
        this.westPanel.repaint();
    }

    @Override
    public void setToolTipText(String string) {
        this.scrollPane.setToolTipText(string);
    }

    private static class LayerAction extends AbstractAction {
        final WorldWindow wwd;
        private final ProductLayer layer;
        private final boolean selected;
        private final String name;

        public LayerAction(ProductLayer layer, WorldWindow wwd, String name, boolean selected) {
            super(name);
            this.wwd = wwd;
            this.layer = layer;
            this.name = name;
            this.selected = selected;
            this.layer.setEnabled(this.selected);
        }

        public void actionPerformed(ActionEvent actionEvent) {
            // Simply enable or disable the layer based on its toggle button.
            if (((JCheckBox) actionEvent.getSource()).isSelected())
                this.layer.setOpacity(name, this.layer.getOpacity());
            else
                this.layer.setOpacity(name, 0);

            wwd.redraw();
        }
    }
}