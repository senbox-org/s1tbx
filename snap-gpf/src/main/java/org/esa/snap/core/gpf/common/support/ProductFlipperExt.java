package org.esa.snap.core.gpf.common.support;

import org.esa.snap.core.dataio.ProductFlipper;
import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;

/**
 * Override ProductFlipper
 */
public class ProductFlipperExt extends ProductFlipper {

    public ProductFlipperExt(int flipType, boolean sourceProductOwner) {
        super(flipType, sourceProductOwner);
    }

    @Override
    protected void addMetadataToProduct(Product product) {
        super.addMetadataToProduct(product);
        updateTargetProductMetadata(product);
    }

    private void updateTargetProductMetadata(final Product targetProduct) {

        try {
            final int flipType = getFlipType();
            if (flipType == FLIP_VERTICAL || flipType == FLIP_BOTH) {
                final MetadataElement rootTgt = targetProduct.getMetadataRoot();
                if (rootTgt == null) {
                    return;
                }

                final MetadataElement absTgt = rootTgt.getElement("Abstracted_Metadata");
                if (absTgt == null) {
                    return;
                }

                final MetadataElement rootSrc = sourceProduct.getMetadataRoot();
                if (rootSrc == null) {
                    return;
                }

                final MetadataElement absSrc = rootSrc.getElement("Abstracted_Metadata");
                if (absSrc == null) {
                    return;
                }

                final MetadataAttribute firstLineTimeAttr = absSrc.getAttribute("first_line_time");
                if (firstLineTimeAttr != null) {
                    final ProductData.UTC firstLineTime = ProductData.UTC.parse(firstLineTimeAttr.getData().getElemString());
                    if (firstLineTime != null) {
                        absTgt.getAttribute("last_line_time").getData().setElems(firstLineTime.getArray());
                    }
                }

                final MetadataAttribute lastLineTimeAttr = absSrc.getAttribute("last_line_time");
                if (lastLineTimeAttr != null) {
                    final ProductData.UTC lastLineTime = ProductData.UTC.parse(lastLineTimeAttr.getData().getElemString());
                    if (lastLineTime != null) {
                        absTgt.getAttribute("first_line_time").getData().setElems(lastLineTime.getArray());
                    }
                }

                final MetadataAttribute lineTimeIntervalAttr = absSrc.getAttribute("line_time_interval");
                if (lineTimeIntervalAttr != null) {
                    final double lineTimeInterval = lineTimeIntervalAttr.getData().getElemDouble();
                    absTgt.getAttribute("line_time_interval").getData().setElemDouble(-lineTimeInterval);
                }
            }
        } catch (Throwable e) {
            e.printStackTrace();
        }
    }
}
