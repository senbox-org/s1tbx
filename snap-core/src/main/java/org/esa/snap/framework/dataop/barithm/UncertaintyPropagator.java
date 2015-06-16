package org.esa.snap.framework.dataop.barithm;

import com.bc.jexp.ParseException;
import com.bc.jexp.Term;
import org.esa.snap.framework.datamodel.Product;

/**
 * @author Norman Fomferra
 * @since SNAP 2
 */
public interface UncertaintyPropagator {
    Term propagateUncertainties(Product product, String expression) throws ParseException, UnsupportedOperationException;
}
