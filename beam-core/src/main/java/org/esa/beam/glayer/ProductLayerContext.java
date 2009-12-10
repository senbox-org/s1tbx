package org.esa.beam.glayer;

import com.bc.ceres.glayer.LayerContext;
import com.bc.ceres.glayer.LayerType;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNode;

/**
 * A {@link LayerContext} for layers requiring access to a certain {@link Product} or a
 * certain {@link ProductNode}.
 * <p/>
 * For {@link LayerType}s requiring this context, {@link LayerType#isValidFor(LayerContext)}
 * must return {@code true} if, and only if, the argument is an instance of this class.
 *
 * @author Ralf Quast
 * @version $Revision $ $Date $
 * @since BEAM 4.7
 */
public interface ProductLayerContext extends LayerContext {

    /**
     * Returns the product provided by this context.
     *
     * @return the product provided by this context.
     */
    Product getProduct();
}
