package org.esa.snap.core.dataop.barithm;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.jexp.ParseException;

/**
 * Generates the combined uncertainty for a given band maths expression.
 *
 * @author Norman Fomferra
 * @since SNAP 2
 */
public interface UncertaintyGenerator {
    /**
     * Generates the combined uncertainty for a given band maths expression.
     *
     * @param product    The data product that serves the referenced rasters in the expression.
     * @param relation   Relation name of ancillary variables that represent uncertainties (NetCDF-U 'rel' attribute).
     * @param expression The band maths expression.
     * @return A new band maths expression representing the combined uncertainty.
     * @throws ParseException
     * @throws UnsupportedOperationException
     * @see RasterDataNode#addAncillaryVariable(RasterDataNode, String...)
     * @see RasterDataNode#setAncillaryRelations(String...)
     */
    String generateUncertainty(Product product, String relation, String expression) throws ParseException, UnsupportedOperationException;
}
