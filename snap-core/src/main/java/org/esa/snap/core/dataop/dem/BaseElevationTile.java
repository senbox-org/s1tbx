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
package org.esa.snap.core.dataop.dem;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.runtime.Config;

import java.util.Arrays;

public class BaseElevationTile implements ElevationTile {

    protected final ElevationModel demModel;
    protected Product product;
    private final Band band;
    private final int bandWidth;
    protected final float noDataValue;
    private final float[][] objectArray;
    private final boolean useDEMGravitationalModel;

    public BaseElevationTile(final ElevationModel demModel, final Product product) {
        this.demModel = demModel;
        this.product = product;
        this.band = product.getBandAt(0);
        this.bandWidth = band.getRasterWidth();
        noDataValue = demModel.getDescriptor().getNoDataValue();
        objectArray = new float[band.getRasterHeight() + 1][];
        useDEMGravitationalModel = Config.instance().preferences().getBoolean("snap.useDEMGravitationalModel", true);
        //System.out.println("Dem Tile "+product.getName());
    }

    public final void clearCache() {
        if (objectArray != null) {
            Arrays.fill(objectArray, 0, objectArray.length, null);
        }
    }

    public final float getSample(final int pixelX, final int pixelY) throws Exception {

        float[] line = objectArray[pixelY];
        if (line == null) {
            line = band.readPixels(0, pixelY, bandWidth, 1, new float[bandWidth], ProgressMonitor.NULL);
            if (useDEMGravitationalModel) {
                addGravitationalModel(pixelY, line);
            }
            objectArray[pixelY] = line;
        }
        return line[pixelX];
    }

    public void dispose() {
        clearCache();
        if (product != null) {
            product.dispose();
            product = null;
        }
    }

    protected void addGravitationalModel(final int index, final float[] line) throws Exception {
    }
}
