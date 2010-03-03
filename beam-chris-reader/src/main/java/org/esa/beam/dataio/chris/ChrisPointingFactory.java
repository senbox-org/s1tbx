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
                "CHRIS_M3A_NR_AC_GC"
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
