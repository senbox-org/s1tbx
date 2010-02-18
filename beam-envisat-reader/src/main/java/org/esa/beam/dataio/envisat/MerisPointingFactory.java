package org.esa.beam.dataio.envisat;

import org.esa.beam.framework.datamodel.Pointing;
import org.esa.beam.framework.datamodel.PointingFactory;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.TiePointGridPointing;

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
     * Retrieves the product types for which this instance can create {@link org.esa.beam.framework.datamodel.Pointing pointings}.
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
