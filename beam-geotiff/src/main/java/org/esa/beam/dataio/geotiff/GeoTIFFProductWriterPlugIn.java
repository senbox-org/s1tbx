package org.esa.beam.dataio.geotiff;

import org.esa.beam.framework.dataio.ProductWriter;
import org.esa.beam.framework.dataio.ProductWriterPlugIn;
import org.esa.beam.util.io.BeamFileFilter;

import java.io.File;
import java.util.Locale;

/**
 * The <code>GeoTIFFProductWriterPlugIn</code> class is the plug-in entry-point for the GeoTIFF product writer.
 *
 * @author Marco Peters
 * @author Sabine Embacher
 * @version $Revision: $ $Date: $
 * @since BEAM 4.5
 */
public class GeoTIFFProductWriterPlugIn implements ProductWriterPlugIn{

    private static final String[] FORMAT_NAMES = new String[] {"GeoTIFF"};

    public Class[] getOutputTypes() {
        return new Class[]{String.class, File.class};
    }

    public ProductWriter createWriterInstance() {
        return new GeoTIFFProductWriter(this);
    }

    public String[] getFormatNames() {
        return FORMAT_NAMES;
    }

    public String[] getDefaultFileExtensions() {
        return new String[]{".tif", ".tiff"};
    }

    public String getDescription(Locale locale) {
        return "GeoTIFF data product.";
    }

    public BeamFileFilter getProductFileFilter() {
        return new BeamFileFilter(FORMAT_NAMES[0], getDefaultFileExtensions(), getDescription(Locale.getDefault()));
    }
}
