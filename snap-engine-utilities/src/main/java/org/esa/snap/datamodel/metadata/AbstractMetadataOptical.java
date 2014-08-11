package org.esa.snap.datamodel.metadata;

import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;

import java.io.IOException;

/**
 * Creates a generic interface to optical metadata
 */
public class AbstractMetadataOptical extends AbstractMetadataBase implements AbstractMetadataInterface {
    /**
     * If AbstractedMetadata is modified by adding new attributes then this version number needs to be incremented
     */
    private static final String METADATA_VERSION = "6.0";
    private static final String abstracted_metadata_version = "optical_metadata_version";
    private static final String OPTICAL_METADATA_ROOT = "Optical_Metadata";

    /**
     * Get abstracted metadata.
     *
     * @param sourceProduct the product
     * @return AbstractMetadata object
     */
    public static AbstractMetadataOptical getOpticalAbstractedMetadata(final Product sourceProduct) throws IOException {
        AbstractMetadata abstractMetadata = AbstractMetadata.getAbstractedMetadata(sourceProduct);
        if (abstractMetadata == null) {
            throw new IOException("no metadata found in product");
        }
        MetadataElement absRoot = abstractMetadata.getAbsRoot();
        return new AbstractMetadataOptical(absRoot, absRoot.getElement(AbstractMetadataOptical.OPTICAL_METADATA_ROOT));
    }

    private AbstractMetadataOptical(final MetadataElement root, final MetadataElement abstractedMetadata) {
        super(root, abstractedMetadata);
    }

    protected boolean isCurrentVersion() {
        // check if version has changed
        final String version = absRoot.getAttributeString(abstracted_metadata_version, "");
        return (version.equals(METADATA_VERSION));
    }

    protected void migrateToCurrentVersion(final MetadataElement abstractedMetadata) {
        if (isCurrentVersion())
            return;

        //todo
    }

    /**
     * Abstract common metadata from products to be used uniformly by all operators
     *
     * @param root the product metadata root
     * @return abstracted metadata root
     */
    protected MetadataElement addAbstractedMetadataHeader(MetadataElement root) {
        MetadataElement absRoot;
        if (root == null) {
            absRoot = new MetadataElement(OPTICAL_METADATA_ROOT);
        } else {
            absRoot = root.getElement(OPTICAL_METADATA_ROOT);
            if (absRoot == null) {
                absRoot = new MetadataElement(OPTICAL_METADATA_ROOT);
                root.addElementAt(absRoot, 0);
            }
        }

        MetadataAttribute att = addAbstractedAttribute(absRoot, abstracted_metadata_version, ProductData.TYPE_ASCII, "", "AbsMetadata version");
        att.getData().setElems(METADATA_VERSION);

        return absRoot;
    }
}
