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
package org.esa.nest.dat.layers.maptools;

import org.esa.nest.util.DialogUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * map tools options
 */
public class MapToolsOptions {

    private final JCheckBox northArrow = new JCheckBox("Show North Arrow", true);
    private final JCheckBox latLonGrid = new JCheckBox("Show Lat/lon Grid", false);
    private final JCheckBox lookDirection = new JCheckBox("Show Look Direction", false);
    private final JCheckBox mapOverview = new JCheckBox("Show Map Overview", false);
    private final JCheckBox info = new JCheckBox("Show Product Info", false);
    private final JCheckBox scale = new JCheckBox("Show Scale", true);
    private final JCheckBox nestLogo = new JCheckBox("Show NEST logo", true);
    private MapToolsLayer layer = null;

    public MapToolsOptions() {
        ActionListener updateStateListenser = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                updateState();
            }
        };
        northArrow.addActionListener(updateStateListenser);
        latLonGrid.addActionListener(updateStateListenser);
        lookDirection.addActionListener(updateStateListenser);
        mapOverview.addActionListener(updateStateListenser);
        info.addActionListener(updateStateListenser);
        scale.addActionListener(updateStateListenser);
        nestLogo.addActionListener(updateStateListenser);
    }

    public void setLayer(MapToolsLayer layer) {
        this.layer = layer;
    }

    public boolean showNorthArrow() {
        return northArrow.isSelected();
    }

    public boolean showLookDirection() {
        return lookDirection.isSelected();
    }

    public boolean showLatLonGrid() {
        return latLonGrid.isSelected();
    }

    public boolean showMapOverview() {
        return mapOverview.isSelected();
    }

    public boolean showInfo() {
        return info.isSelected();
    }

    public boolean showScale() {
        return scale.isSelected();
    }

    public boolean showNestLogo() {
        return nestLogo.isSelected();
    }

    public JPanel createPanel() {
        final JPanel panel = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = DialogUtils.createGridBagConstraints();

        panel.add(northArrow, gbc);
        gbc.gridy++;
        //panel.add(latLonGrid, gbc);
        //gbc.gridy++;
        panel.add(lookDirection, gbc);
        gbc.gridy++;
        //panel.add(info, gbc);
        //gbc.gridy++;
        panel.add(scale, gbc);
        gbc.gridy++;
        panel.add(nestLogo, gbc);

        return panel;
    }

    private void updateState() {
        if(layer != null) {
            layer.regenerate();
        }
    }


}
