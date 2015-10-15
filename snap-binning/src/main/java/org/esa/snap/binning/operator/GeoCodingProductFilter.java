package org.esa.snap.binning.operator;

import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.Product;

class GeoCodingProductFilter extends BinningProductFilter {

    @Override
    protected boolean acceptForBinning(Product product) {
        final GeoCoding geoCoding = product.getSceneGeoCoding();
        if (geoCoding != null && geoCoding.canGetGeoPos()) {
            return true;
        }
        setReason("Rejected because it does not contain a proper geo coding.");
        return false;
    }
}
