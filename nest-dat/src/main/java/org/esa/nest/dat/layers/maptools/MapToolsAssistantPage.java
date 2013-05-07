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

import java.awt.*;

class MapToolsAssistantPage extends AbstractLayerSourceAssistantPage {

    private final MapToolsOptions options = new MapToolsOptions();

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
        MapToolsLayerSource.createLayer(getContext(), options);
        return true;
    }

    @Override
    public Component createPageComponent() {
        return options.createPanel();
    }
}