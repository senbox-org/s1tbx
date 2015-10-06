package org.esa.snap.core.gpf.descriptor;

import org.esa.snap.core.datamodel.Product;

/**
 * Target product element metadata.
 *
 * @author Norman Fomferra
 * @since BEAM 5
 */
public interface TargetProductDescriptor extends DataElementDescriptor {
    /**
     * @return The target product type.
     * Defaults to {@link Product}.
     */
    Class<? extends Product> getDataType();
}
