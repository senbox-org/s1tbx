package org.esa.snap.gpf;

import org.esa.beam.framework.datamodel.CrsGeoCoding;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.util.StringUtils;
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

    public boolean isMultiSwath() {
        final String[] bandNames = product.getBandNames();
        return (contains(bandNames, "IW1") && contains(bandNames, "IW2")) ||
                (contains(bandNames, "EW1") && contains(bandNames, "EW2"));
    }

    public void checkIfTOPSARBurstProduct(final boolean shouldbe) throws OperatorException {
        final boolean isMultiSwath = isMultiSwath();
        if(shouldbe && !isMultiSwath) {
            throw new OperatorException("Source product should an SLC burst product");
        } else if(!shouldbe && isMultiSwath) {
            throw new OperatorException("Source product should should first be deburst");
        }
    }

    private static boolean contains(final String[] list, final String tag) {
        for(String s : list) {
            if(s.contains(tag))
                return true;
        }
        return false;
    }

    public void checkIfQuadPol() throws OperatorException {

    }

    public void checkIfMapProjected() throws OperatorException {
        if (OperatorUtils.isMapProjected(product)) {
            throw new OperatorException("Source product should not be map projected");
        }
    }
}
