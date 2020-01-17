package org.esa.s1tbx.io.geotiffxml;

import it.geosolutions.imageioimpl.plugins.tiff.TIFFImageReader;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.IOException;
import java.util.Iterator;

public class GeoTiffUtils {

    public static ImageReader getTiffIIOReader(final ImageInputStream stream) throws IOException {
        ImageReader reader = null;
        final Iterator<ImageReader> imageReaders = ImageIO.getImageReaders(stream);
        while (imageReaders.hasNext()) {
            final ImageReader iioReader = imageReaders.next();
            if (iioReader instanceof TIFFImageReader) {
                reader = iioReader;
                break;
            }
        }
        if (reader == null)
            throw new IOException("Unable to open " + stream.toString());
        reader.setInput(stream, true, true);
        return reader;
    }
}
