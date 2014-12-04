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
package org.esa.snap.gpf.ui.worldmap;

import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.snap.db.ProductEntry;

/**

 */
public class WorldMapUI {

    private final NestWorldMapPaneDataModel worldMapDataModel;
    private final NestWorldMapPane worlMapPane;

    public WorldMapUI() {
        worldMapDataModel = new NestWorldMapPaneDataModel();
        worlMapPane = new NestWorldMapPane(worldMapDataModel);
    }

    public GeoPos[] getSelectionBox() {
        return worldMapDataModel.getSelectionBox();
    }

    public void setSelectionStart(final float lat, final float lon) {
        worldMapDataModel.setSelectionBoxStart(lat, lon);
    }

    public void setSelectionEnd(final float lat, final float lon) {
        worldMapDataModel.setSelectionBoxEnd(lat, lon);
    }

    public NestWorldMapPane getWorlMapPane() {
        return worlMapPane;
    }

    public void setProductEntryList(final ProductEntry[] productEntryList) {
        if (productEntryList == null) return;
        final GeoPos[][] geoBoundaries = new GeoPos[productEntryList.length][4];
        int i = 0;
        for (ProductEntry entry : productEntryList) {
            geoBoundaries[i++] = entry.getGeoBoundary();
        }

        worldMapDataModel.setAdditionalGeoBoundaries(geoBoundaries);
    }

    public NestWorldMapPaneDataModel getModel() {
        return worldMapDataModel;
    }
}
