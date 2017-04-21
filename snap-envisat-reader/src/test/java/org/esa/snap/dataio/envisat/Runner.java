package org.esa.snap.dataio.envisat;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;

import java.io.IOException;

/**
 * @author Marco Peters
 */
public class Runner {

    public static void main(String[] args) throws IOException {
        Product product = ProductIO.readProduct("G:\\EOData\\AatsrL0\\AATSR Auxiliary Files\\ATS_GC1_AXVIEC20070720_093834_20020301_000000_20200101_000000");
        MetadataElement metadataRoot = product.getMetadataRoot();
        MetadataElement radiance_lut = metadataRoot.getElement("Temperature_To_Radiance_LUT");
        System.out.println();
    }
}
