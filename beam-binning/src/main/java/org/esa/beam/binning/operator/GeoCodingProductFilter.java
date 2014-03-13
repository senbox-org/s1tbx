package org.esa.beam.binning.operator;

import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.Product;

public class GeoCodingProductFilter extends BinningProductFilter {

    @Override
    protected boolean acceptForBinning(Product product) {
        final GeoCoding geoCoding = product.getGeoCoding();
        if (geoCoding != null && geoCoding.canGetGeoPos()) {
            return true;
        }
        setReason("Rejected because it does not contain a proper geo coding.");
        return false;
    }
}
