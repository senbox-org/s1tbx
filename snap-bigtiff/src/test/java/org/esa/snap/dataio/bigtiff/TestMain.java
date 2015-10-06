package org.esa.snap.dataio.bigtiff;

import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.junit.Ignore;

import java.io.File;
import java.io.IOException;

/**
 * Created by Tom on 27.01.2015.
 */
@Ignore
public class TestMain {

    // C:\Data\delete\subset_L3_2012-03-01_2012-03-31_compressed.tif
    // C:\Data\reader_tests\applications\generic_reader\GeoTIFF\tiny99.tif

    // /home/tom/delete/subset_L3_2011-02-01_2011-02-28_1200m.tif
    // /home/tom/delete/subset_L3_2011-02-01_uncompressed.tif
    // /usr/local/data/reader_acceptance_tests/applications/generic_reader/GeoTIFF/tiny99.tif

    public static void main(String[] args) throws IOException {
        final Product bigGeoTiff = ProductIO.readProduct(new File(args[0]), BigGeoTiffProductReaderPlugIn.FORMAT_NAME);

        final int numBands = bigGeoTiff.getNumBands();
        for (int i = 0; i < numBands; i++) {
            final Band band = bigGeoTiff.getBandAt(i);
            final String name = band.getName();
            final int dataType = band.getDataType();
            final String typeString = ProductData.getTypeString(dataType);


            System.out.println(name + " " + typeString + " : " + band.getSourceImage().getData().getSampleFloat(109, 109, 0));
            System.out.println("---------------------------------------------------------------");
            System.out.println();


        }

        if (bigGeoTiff != null) {
            bigGeoTiff.dispose();
        }
    }
}
