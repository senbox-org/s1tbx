package org.esa.snap.dataio.bigtiff;

import org.esa.snap.framework.dataio.EncodeQualification;
import org.esa.snap.framework.dataio.ProductWriter;
import org.esa.snap.framework.dataio.ProductWriterPlugIn;
import org.esa.snap.framework.datamodel.CrsGeoCoding;
import org.esa.snap.framework.datamodel.GeoCoding;
import org.esa.snap.framework.datamodel.MapGeoCoding;
import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.util.io.SnapFileFilter;

import java.io.File;
import java.util.Locale;

public class BigGeoTiffProductWriterPlugIn implements ProductWriterPlugIn {

    public static final String FORMAT_NAME = BigGeoTiffProductReaderPlugIn.FORMAT_NAME;

    private static final Class[] OUTPUT_TYPES = new Class[]{String.class, File.class,};

    @Override
    public EncodeQualification getEncodeQualification(Product product) {
        GeoCoding geoCoding = product.getGeoCoding();
        if (geoCoding == null) {
            return new EncodeQualification(EncodeQualification.Preservation.PARTIAL,
                                           "The product is not geo-coded. A usual TIFF file will be written instead.");
        } else if (!(geoCoding instanceof MapGeoCoding) && !(geoCoding instanceof CrsGeoCoding)) {
            return new EncodeQualification(EncodeQualification.Preservation.PARTIAL,
                                           "The product is geo-coded but seems not rectified. Geo-coding information may not be properly preserved.");
        } else {
            return new EncodeQualification(EncodeQualification.Preservation.FULL);
        }
    }

    @Override
    public Class[] getOutputTypes() {
        return OUTPUT_TYPES;
    }

    @Override
    public ProductWriter createWriterInstance() {
        return new BigGeoTiffProductWriter(this);
    }

    @Override
    public String[] getFormatNames() {
        return new String[]{FORMAT_NAME};
    }

    @Override
    public String[] getDefaultFileExtensions() {
        return Constants.FILE_EXTENSIONS;
    }

    @Override
    public String getDescription(Locale locale) {
        return Constants.DESCRIPTION;
    }

    @Override
    public SnapFileFilter getProductFileFilter() {
        return new SnapFileFilter(getFormatNames()[0], getDefaultFileExtensions(), getDescription(null));
    }
}
