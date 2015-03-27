package org.esa.snap.gpf;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.util.StringUtils;
import org.esa.snap.datamodel.AbstractMetadata;
import org.esa.snap.datamodel.Unit;

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

    public boolean isComplex() {
        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);
        if (absRoot != null) {
            final String sampleType = absRoot.getAttributeString(AbstractMetadata.SAMPLE_TYPE, AbstractMetadata.NO_METADATA_STRING).trim();
            if (sampleType.equalsIgnoreCase("complex"))
                return true;
        }
        return false;
    }

    public void checkIfSLC() throws OperatorException {
        if (!isComplex()) {
            throw new OperatorException("Input should be a single look complex SLC product");
        }
    }

    public boolean isMultiSwath() {
        final String[] bandNames = product.getBandNames();
        return (contains(bandNames, "IW1") && contains(bandNames, "IW2")) ||
                (contains(bandNames, "EW1") && contains(bandNames, "EW2"));
    }

    public boolean isSentinel1Product() throws OperatorException {
        final String mission = absRoot.getAttributeString(AbstractMetadata.MISSION);
        return mission.startsWith("SENTINEL-1");
    }

    public void checkIfSentinel1Product() throws OperatorException {
        if (!isSentinel1Product()) {
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

    public boolean isTOPSARProduct() {
        final String[] bandNames = product.getBandNames();
        return (contains(bandNames, "IW1") || contains(bandNames, "IW2") || contains(bandNames, "IW3") ||
                contains(bandNames, "EW1") || contains(bandNames, "EW2") || contains(bandNames, "EW3") ||
                contains(bandNames, "EW4") || contains(bandNames, "EW5"));
    }

    public void checkIfTOPSARBurstProduct(final boolean shouldbe) throws OperatorException {

        final boolean isTOPSARProduct = isTOPSARProduct();
        if (shouldbe && !isTOPSARProduct) {
            // It should be a TOP SAR Burst product but it is not even a TOP SAR Product
            throw new OperatorException("Source product should be an SLC burst product");
        } else if (shouldbe && isTOPSARProduct && isDebursted()) {
            // It should be a TOP SAR Burst product and it is a TOP SAR product but it has been deburst
            throw new OperatorException("Source product should NOT be a deburst product");
        } else if (!shouldbe && isTOPSARProduct && !isDebursted()) {
            // It should not be a TOP SAR burst product but it is.
            throw new OperatorException("Source product should first be deburst");
        }
    }

    public void checkIfMultiSwathTOPSARProduct() throws OperatorException {
        if (!isMultiSwath()) {
            throw new OperatorException("Source product should be multi sub-swath SLC burst product");
        }
    }

    public void checkIfDeburstedProduct() throws OperatorException {
        if (!isDebursted()) {
            throw new OperatorException("Source product should be a debursted product");
        }
    }

    public boolean isDebursted() {
        if(!isSentinel1Product())
            return true;

        boolean isDebursted = true;
        final MetadataElement origProdRoot = AbstractMetadata.getOriginalProductMetadata(product);
        MetadataElement annotation = origProdRoot.getElement("annotation");
        if (annotation == null) {
            return true;
        }

        final MetadataElement[] elems = annotation.getElements();
        for (MetadataElement elem : elems) {
            final MetadataElement product = elem.getElement("product");
            final MetadataElement swathTiming = product.getElement("swathTiming");
            final MetadataElement burstList = swathTiming.getElement("burstList");
            final int count = Integer.parseInt(burstList.getAttributeString("count"));
            if (count != 0) {
                isDebursted = false;
                break;
            }
        }
        return isDebursted;
    }

    public void checkIfSentinel1DeburstProduct() throws OperatorException {

        // First off, it must be SLC
        checkIfSLC();

        // Then...
        // If it is not Sentinel1, we don't have to check.
        // If it is single-path (i.e., not multi-path), we don't have to check.
        // If it is multi-path, then it needs to be deburst.
        // Now isMultiPath() returns true if it is multi-path AND not deburst
        // So we want to throw if isMultiPath() returns true in isTOPSARBurstProduct()
        if (isTOPSARBurstProduct()) {
            throw new OperatorException("Source product should first be deburst");
        }
    }

    public boolean isTOPSARBurstProduct() throws OperatorException {

        final String mission = absRoot.getAttributeString(AbstractMetadata.MISSION);
        if (!mission.startsWith("SENTINEL-1")) {
            return false;
        }

        final String[] bandNames = product.getBandNames();
        for (String bandName : bandNames) {
            if (bandName.contains("IW1") || bandName.contains("IW2") || bandName.contains("IW3") ||
                bandName.contains("EW1") || bandName.contains("EW2") || bandName.contains("EW3") ||
                bandName.contains("EW4") || bandName.contains("EW5")) {
                return true;
            }
        }

        return false;
    }

    private static boolean contains(final String[] list, final String tag) {
        for (String s : list) {
            if (s.contains(tag))
                return true;
        }
        return false;
    }

    public boolean isFullPolSLC() {

        int validBandCnt = 0;
        for (final Band band : product.getBands()) {

            final Unit.UnitType bandUnit = Unit.getUnitType(band);
            if (!(bandUnit == Unit.UnitType.REAL || bandUnit == Unit.UnitType.IMAGINARY))
                continue;
            final String pol = OperatorUtils.getPolarizationFromBandName(band.getName());
            if(pol == null)
                continue;

            if (pol.contains("hh") || pol.contains("hv") || pol.contains("vh") || pol.contains("vv")) {
                ++validBandCnt;
            }
        }

        return validBandCnt == 8;
    }

    public void checkIfQuadPolSLC() throws OperatorException {
        if (!isFullPolSLC()) {
            throw new OperatorException("Input should be a full pol SLC product");
        }
    }

    public void checkIfMapProjected() throws OperatorException {
        if (OperatorUtils.isMapProjected(product)) {
            throw new OperatorException("Source product should not be map projected");
        }
    }

    public void checkIfCalibrated() throws OperatorException {
        if (OperatorUtils.isCalibrated(product)) {
            throw new OperatorException("Source product has already been calibrated");
        }
    }

    public void checkIfNotCalibrated() throws OperatorException {
        if (!OperatorUtils.isCalibrated(product)) {
            throw new OperatorException("Source product should be calibrated");
        }
    }
}
