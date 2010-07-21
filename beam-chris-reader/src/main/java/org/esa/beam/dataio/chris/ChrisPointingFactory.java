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

package org.esa.beam.dataio.chris;

import org.esa.beam.framework.datamodel.Pointing;
import org.esa.beam.framework.datamodel.PointingFactory;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.TiePointGridPointing;

public final class ChrisPointingFactory implements PointingFactory {

    @Override
    public String[] getSupportedProductTypes() {
        return new String[]{
                // uncorrected products
                "CHRIS_M0_GC",
                "CHRIS_M1_GC",
                "CHRIS_M2_GC",
                "CHRIS_M3_GC",
                "CHRIS_M4_GC",
                "CHRIS_M5_GC",
                "CHRIS_M20_GC",
                "CHRIS_M30_GC",
                "CHRIS_M3A_GC",
                // TOA reflectance products
                "CHRIS_M0_TOA_REFL_GC",
                "CHRIS_M1_TOA_REFL_GC",
                "CHRIS_M2_TOA_REFL_GC",
                "CHRIS_M3_TOA_REFL_GC",
                "CHRIS_M4_TOA_REFL_GC",
                "CHRIS_M5_TOA_REFL_GC",
                "CHRIS_M20_TOA_REFL_GC",
                "CHRIS_M30_TOA_REFL_GC",
                "CHRIS_M3A_TOA_REFL_GC",
                // noise-corrected products
                "CHRIS_M0_NR_GC",
                "CHRIS_M1_NR_GC",
                "CHRIS_M2_NR_GC",
                "CHRIS_M3_NR_GC",
                "CHRIS_M4_NR_GC",
                "CHRIS_M5_NR_GC",
                "CHRIS_M20_NR_GC",
                "CHRIS_M30_NR_GC",
                "CHRIS_M3A_NR_GC",
                // atmosphere-corrected products
                "CHRIS_M0_NR_AC_GC",
                "CHRIS_M1_NR_AC_GC",
                "CHRIS_M2_NR_AC_GC",
                "CHRIS_M3_NR_AC_GC",
                "CHRIS_M4_NR_AC_GC",
                "CHRIS_M5_NR_AC_GC",
                "CHRIS_M20_NR_AC_GC",
                "CHRIS_M30_NR_AC_GC",
                "CHRIS_M3A_NR_AC_GC",
                // nose-corrected TOA-reflectance products
                "CHRIS_M0_NR_TOA_REFL_GC",
                "CHRIS_M1_NR_TOA_REFL_GC",
                "CHRIS_M2_NR_TOA_REFL_GC",
                "CHRIS_M3_NR_TOA_REFL_GC",
                "CHRIS_M4_NR_TOA_REFL_GC",
                "CHRIS_M5_NR_TOA_REFL_GC",
                "CHRIS_M20_NR_TOA_REFL_GC",
                "CHRIS_M30_NR_TOA_REFL_GC",
                "CHRIS_M3A_NR_TOA_REFL_GC"
        };
    }

    @Override
    public Pointing createPointing(final RasterDataNode raster) {
        final Product product = raster.getProduct();
        return new TiePointGridPointing(product.getGeoCoding(),
                                        null,
                                        null,
                                        product.getTiePointGrid("vza"),
                                        product.getTiePointGrid("vaa"),
                                        null);
    }
}
