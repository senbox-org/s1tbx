package org.esa.beam.dataio.envisat;

import org.esa.beam.framework.datamodel.Pointing;
import org.esa.beam.framework.datamodel.PointingFactory;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.framework.datamodel.TiePointGridPointing;

/**
 * The {@link PointingFactory} for Envisat AATSR Level 1b/2 products.
 */
public class AatsrPointingFactory implements PointingFactory {

    private static final String[] PRODUCT_TYPES = new String[]{
            EnvisatConstants.AATSR_L2_NR_PRODUCT_TYPE_NAME,
            EnvisatConstants.AATSR_L1B_TOA_PRODUCT_TYPE_NAME
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
