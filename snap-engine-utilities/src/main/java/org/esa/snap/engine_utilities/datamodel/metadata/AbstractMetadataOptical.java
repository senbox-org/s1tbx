/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.snap.engine_utilities.datamodel.metadata;

import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;

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
