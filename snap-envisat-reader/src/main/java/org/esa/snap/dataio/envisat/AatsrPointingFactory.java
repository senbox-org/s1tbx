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

package org.esa.snap.dataio.envisat;

import org.esa.snap.core.datamodel.Pointing;
import org.esa.snap.core.datamodel.PointingFactory;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.core.datamodel.TiePointGridPointing;

/**
 * The {@link PointingFactory} for Envisat AATSR Level 1b/2 products.
 */
public class AatsrPointingFactory implements PointingFactory {

    private static final String[] PRODUCT_TYPES = new String[]{
            EnvisatConstants.AATSR_L2_NR_PRODUCT_TYPE_NAME,
            EnvisatConstants.AATSR_L1B_TOA_PRODUCT_TYPE_NAME
    };

    /**
     * Retrieves the product types for which this instance can create {@link Pointing pointings}.
     *
     * @return the product types
     */
    public String[] getSupportedProductTypes() {
        return PRODUCT_TYPES;
    }

    public Pointing createPointing(RasterDataNode raster) {
        final Product product = raster.getProduct();
        if (raster.getName().toLowerCase().indexOf("_nadir") != -1) {
            return new TiePointGridPointing(raster.getGeoCoding(),
                                            toZenithTiePointGrid(product,
                                                                 EnvisatConstants.AATSR_SUN_ELEV_NADIR_DS_NAME),
                                            product.getTiePointGrid(EnvisatConstants.AATSR_SUN_AZIMUTH_NADIR_DS_NAME),
                                            toZenithTiePointGrid(product,
                                                                 EnvisatConstants.AATSR_VIEW_ELEV_NADIR_DS_NAME),
                                            product.getTiePointGrid(EnvisatConstants.AATSR_VIEW_AZIMUTH_NADIR_DS_NAME),
                                            product.getTiePointGrid(EnvisatConstants.AATSR_ALTITUDE_DS_NAME));
        } else if (raster.getName().toLowerCase().indexOf("_fward") != -1) {
            return new TiePointGridPointing(raster.getGeoCoding(),
                                            toZenithTiePointGrid(product,
                                                                 EnvisatConstants.AATSR_SUN_ELEV_FWARD_DS_NAME),
                                            product.getTiePointGrid(EnvisatConstants.AATSR_SUN_AZIMUTH_FWARD_DS_NAME),
                                            toZenithTiePointGrid(product,
                                                                 EnvisatConstants.AATSR_VIEW_ELEV_FWARD_DS_NAME),
                                            product.getTiePointGrid(EnvisatConstants.AATSR_VIEW_AZIMUTH_FWARD_DS_NAME),
                                            product.getTiePointGrid(EnvisatConstants.AATSR_ALTITUDE_DS_NAME));
        }
        return null;
    }

    private TiePointGrid toZenithTiePointGrid(final Product product, final String name) {
        final TiePointGrid base = product.getTiePointGrid(name);
        return base != null ? TiePointGrid.createZenithFromElevationAngleTiePointGrid(base) : null;
    }
}
