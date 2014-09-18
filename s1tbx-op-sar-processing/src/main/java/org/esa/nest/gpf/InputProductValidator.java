package org.esa.nest.gpf;

import org.esa.beam.framework.datamodel.CrsGeoCoding;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.snap.datamodel.AbstractMetadata;
import org.esa.snap.gpf.OperatorUtils;

/**
 * Validates input products using commonly used verifications
 */
public class InputProductValidator {

    private final Product product;
    private final MetadataElement absRoot;

    public InputProductValidator(final Product product) throws OperatorException {
        this.product = product;
        absRoot = AbstractMetadata.getAbstractedMetadata(product);
    }

    public void checkIfSLC() throws OperatorException {

    }

    public void checkIfTOPSARDeburst() throws OperatorException {

    }

    public void checkIfQuadPol() throws OperatorException {

    }

    public void checkIfMapProjected() throws OperatorException {
        if (OperatorUtils.isMapProjected(product)) {
            throw new OperatorException("Source product should not be map projected");
        }
    }
}
