package org.esa.snap.gpf;

import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.util.StringUtils;
import org.esa.snap.datamodel.AbstractMetadata;

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

    public void checkIfCoregisteredStack() throws OperatorException {
        if (!StackUtils.isCoregisteredStack(product)) {
            throw new OperatorException("Input should be a coregistered stack");
        }
    }

    public void checkIfSLC() throws OperatorException {

    }

    public boolean isMultiSwath() {
        final String[] bandNames = product.getBandNames();
        return (contains(bandNames, "IW1") && contains(bandNames, "IW2")) ||
                (contains(bandNames, "EW1") && contains(bandNames, "EW2"));
    }

    public void checkIfSentinel1Product() throws OperatorException {
        final String mission = absRoot.getAttributeString(AbstractMetadata.MISSION);
        if (!mission.startsWith("SENTINEL-1")) {
            throw new OperatorException("Input should be a Sentinel-1 product.");
        }
    }

    public void checkProductType(final String[] validProductTypes) throws OperatorException {
        final String productType = absRoot.getAttributeString(AbstractMetadata.PRODUCT_TYPE);
        for (String validProductType : validProductTypes) {
            if (productType.equals(validProductType))
                return;
        }
        throw new OperatorException(productType + " is not a valid product type from: " + StringUtils.arrayToString(validProductTypes, ","));
    }

    public void checkAcquisitionMode(final String[] validModes) throws OperatorException {
        final String acquisitionMode = absRoot.getAttributeString(AbstractMetadata.ACQUISITION_MODE);
        for (String validMode : validModes) {
            if (acquisitionMode.equals(validMode))
                return;
        }
        throw new OperatorException(acquisitionMode + " is not a valid acquisition mode from: " + StringUtils.arrayToString(validModes, ","));
    }

    public void checkIfTOPSARBurstProduct(final boolean shouldbe) throws OperatorException {
        final boolean isMultiSwath = isMultiSwath();
        if (shouldbe && !isMultiSwath) {
            throw new OperatorException("Source product should be an SLC burst product");
        } else if (!shouldbe && isMultiSwath) {
            throw new OperatorException("Source product should first be deburst");
        }
    }

    private static boolean contains(final String[] list, final String tag) {
        for (String s : list) {
            if (s.contains(tag))
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
