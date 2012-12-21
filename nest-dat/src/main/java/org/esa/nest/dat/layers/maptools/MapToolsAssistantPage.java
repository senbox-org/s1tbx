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

import org.esa.beam.framework.ui.layer.AbstractLayerSourceAssistantPage;
import org.esa.nest.util.DialogUtils;

import javax.swing.*;
import java.awt.*;

class MapToolsAssistantPage extends AbstractLayerSourceAssistantPage {

    private final JCheckBox compass = new JCheckBox("Show Compass", true);
    private final JCheckBox latLonGrid = new JCheckBox("Show Lat/lon Grid", false);
    private final JCheckBox lookDirection = new JCheckBox("Show Look Direction", true);
    private final JCheckBox nestLogo = new JCheckBox("Show NEST logo", true);

    MapToolsAssistantPage() {
        super("Map Tools Options");
    }

    @Override
    public boolean validatePage() {
        return true;
    }

    @Override
    public boolean hasNextPage() {
        return false;
    }

    @Override
    public AbstractLayerSourceAssistantPage getNextPage() {
        return null;
    }

    @Override
    public boolean canFinish() {
        return true;
    }

    @Override
    public boolean performFinish() {
        final MapToolsOptions options = new MapToolsOptions(compass.isSelected(),
                                                            latLonGrid.isSelected(),
                                                            lookDirection.isSelected(),
                                                            false,
                                                            nestLogo.isSelected());
        MapToolsLayerSource.createLayer(getContext(), options);
        return true;
    }

    @Override
    public Component createPageComponent() {
        final JPanel panel = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = DialogUtils.createGridBagConstraints();

        panel.add(compass, gbc);
        gbc.gridy++;
        //panel.add(latLonGrid, gbc);
        //gbc.gridy++;
        panel.add(lookDirection, gbc);
        gbc.gridy++;
        panel.add(nestLogo, gbc);

        return panel;
    }
}