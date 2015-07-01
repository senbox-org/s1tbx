package org.esa.snap.framework.dataop.barithm;

import com.bc.jexp.ParseException;
import com.bc.jexp.Term;
import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.framework.datamodel.RasterDataNode;

/**
 * Generates the combined uncertainty for a given band maths expression.
 *
 * @author Norman Fomferra
 * @see org.esa.snap.framework.datamodel.RasterDataNode#addAncillaryVariable(RasterDataNode)
 * @since SNAP 2
 */
public interface UncertaintyGenerator {
    /**
     * Generates the combined uncertainty for a given band maths expression.
     *
     * @param product    The data product that serves the referenced rasters in the expression.
     * @param relation   Relation name of ancillary variables that represent uncertainties (NetCDF-U 'rel' attribute).
     * @param expression The band maths expression.
     * @return A term representing the combined uncertainty.
     * @throws ParseException
     * @throws UnsupportedOperationException
     * @see org.esa.snap.framework.datamodel.RasterDataNode#addAncillaryVariable(RasterDataNode, String)
     * @see org.esa.snap.framework.datamodel.RasterDataNode#setAncillaryRelation(String)
     */
    Term generateUncertainty(Product product, String relation, String expression) throws ParseException, UnsupportedOperationException;
}
