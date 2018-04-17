package org.esa.s1tbx.io.sentinel1;

import org.esa.s1tbx.commons.io.ImageIOFile;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;

import java.io.IOException;
import java.text.DateFormat;

/**
 * Supports reading directories for level1, level2, and level0
 */
public interface Sentinel1Directory {

    DateFormat sentinelDateFormat = ProductData.UTC.createDateFormat("yyyy-MM-dd HH:mm:ss");

    void close() throws IOException;

    void readProductDirectory() throws IOException;

    Product createProduct() throws IOException;

    ImageIOFile.BandInfo getBandInfo(final Band destBand);

    boolean isSLC();

    default MetadataElement getMetadataObject(final MetadataElement origProdRoot, final String metadataObjectName) {

        final MetadataElement metadataSection = origProdRoot.getElement("XFDU").getElement("metadataSection");
        final MetadataElement[] metadataObjects = metadataSection.getElements();

        for (MetadataElement elem : metadataObjects) {
            if (elem.getAttribute("ID").getData().getElemString().equals(metadataObjectName)) {
                return elem;
            }
        }
        return null;
    }
}
