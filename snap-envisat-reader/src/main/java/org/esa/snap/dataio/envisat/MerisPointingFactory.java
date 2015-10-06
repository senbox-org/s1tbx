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
import org.esa.snap.core.datamodel.TiePointGridPointing;

/**
 * The {@link PointingFactory} for Envisat MERIS Level 1b/2 products.
 */
public class MerisPointingFactory implements PointingFactory {

    private static final String[] PRODUCT_TYPES = new String[]{
            EnvisatConstants.MERIS_RR_L1B_PRODUCT_TYPE_NAME,
            EnvisatConstants.MERIS_RR_L2_PRODUCT_TYPE_NAME,
            EnvisatConstants.MERIS_FR_L1B_PRODUCT_TYPE_NAME,
            EnvisatConstants.MERIS_FR_L2_PRODUCT_TYPE_NAME,
            EnvisatConstants.MERIS_FRS_L1B_PRODUCT_TYPE_NAME,
            EnvisatConstants.MERIS_FRS_L2_PRODUCT_TYPE_NAME,
            EnvisatConstants.MERIS_FSG_L1B_PRODUCT_TYPE_NAME,
            EnvisatConstants.MERIS_FRG_L1B_PRODUCT_TYPE_NAME
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
        return new TiePointGridPointing(raster.getGeoCoding(),
                                        product.getTiePointGrid(EnvisatConstants.MERIS_SUN_ZENITH_DS_NAME),
                                        product.getTiePointGrid(EnvisatConstants.MERIS_SUN_AZIMUTH_DS_NAME),
                                        product.getTiePointGrid(EnvisatConstants.MERIS_VIEW_ZENITH_DS_NAME),
                                        product.getTiePointGrid(EnvisatConstants.MERIS_VIEW_AZIMUTH_DS_NAME),
                                        product.getTiePointGrid(EnvisatConstants.MERIS_DEM_ALTITUDE_DS_NAME));
    }
}
