package org.esa.beam.dataio.bigtiff;


import it.geosolutions.imageioimpl.plugins.tiff.TIFFImageReader;
import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.util.io.BeamFileFilter;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Locale;

public class BigGeoTiffProductReaderPlugIn implements ProductReaderPlugIn {

    @Override
    public DecodeQualification getDecodeQualification(Object input) {
        try {
            final Object imageIOInput;
            if (input instanceof String) {
                imageIOInput = new File((String) input);
            } else if (input instanceof File || input instanceof InputStream) {
                imageIOInput = input;
            } else {
                return DecodeQualification.UNABLE;
            }

            try (ImageInputStream stream = ImageIO.createImageInputStream(imageIOInput)) {
                return getDecodeQualificationImpl(stream);
            }
        } catch (Exception ignore) {
            // nothing to do, return value is already UNABLE
        }

        return DecodeQualification.UNABLE;
    }

    @Override
    public ProductReader createReaderInstance() {
        return new BigGeoTiffProductReader(this);
    }

    public Class[] getInputTypes() {
        return new Class[]{String.class, File.class, InputStream.class,};
    }

    @Override
    public String[] getFormatNames() {
        return Constants.FORMAT_NAMES;
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
    public BeamFileFilter getProductFileFilter() {
        return new BeamFileFilter(Constants.FORMAT_NAMES[0], getDefaultFileExtensions(), getDescription(null));
    }

    // @todo 3 tb/tb write test following the original GeoTiff pattern 2015-01-08
    static DecodeQualification getDecodeQualificationImpl(ImageInputStream stream) {
        try {
            TIFFImageReader imageReader = getTiffImageReader(stream);
            if (imageReader == null) {
                return DecodeQualification.UNABLE;
            }
        } catch (Exception ignore) {
            return DecodeQualification.UNABLE;
        }
        return DecodeQualification.SUITABLE;
    }

    static TIFFImageReader getTiffImageReader(ImageInputStream stream) {
        final Iterator<ImageReader> imageReaders = ImageIO.getImageReaders(stream);
        TIFFImageReader imageReader = null;

        while (imageReaders.hasNext()) {
            final ImageReader reader = imageReaders.next();
            if (reader instanceof TIFFImageReader) {
                imageReader = (TIFFImageReader) reader;
                break;
            }
        }

        return imageReader;
    }
}
