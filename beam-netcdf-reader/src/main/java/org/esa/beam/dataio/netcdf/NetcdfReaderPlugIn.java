package org.esa.beam.dataio.netcdf;

import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.util.io.BeamFileFilter;
import ucar.nc2.NetcdfFile;

import java.io.IOException;
import java.util.Locale;


/**
 * The plug-in class for the {@link NetcdfReader}.
 *
 * @author Norman Fomferra
 */
public class NetcdfReaderPlugIn implements ProductReaderPlugIn {

    public NetcdfReaderPlugIn() {
    }

    /**
     * Returns true only if all of the following conditions are true:
     * <ul>
     * <li>the given input object's string representation has the ".nc" extension,</li>
     * <li>the given input object's string representation points to a valid netCDF file,</li>
     * <li>the netCDF file has variables which can be interpreted as bands.</li>
     * </ul>
     */
    public DecodeQualification getDecodeQualification(final Object input) {

        String pathname = input.toString();
        if (!(pathname.endsWith(".nc") || pathname.endsWith(".NC"))) {
            return DecodeQualification.UNABLE;
        }

        final NetcdfFile netcdfFile;
        try {
            netcdfFile = NetcdfFile.open(pathname);
        } catch (IOException e) {
            return DecodeQualification.UNABLE;
        }

        try {
            final NcRasterDigest rasterDigest = NetcdfReaderUtils.createRasterDigest(netcdfFile.getRootGroup());
            if (rasterDigest == null) {
                return DecodeQualification.UNABLE;
            }
        } finally {
            try {
                netcdfFile.close();
            } catch (IOException e) {
                // OK, ignored
            }
        }

        return DecodeQualification.SUITABLE;
    }

    /**
     * Returns an array containing the classes that represent valid input types for this reader.
     * <p/>
     * <p> Intances of the classes returned in this array are valid objects for the <code>setInput</code> method of the
     * <code>ProductReader</code> interface (the method will not throw an <code>InvalidArgumentException</code> in this
     * case).
     *
     * @return an array containing valid input types, never <code>null</code>
     */
    public Class[] getInputTypes() {
        return NetcdfConstants.READER_INPUT_TYPES;
    }

    /**
     * Creates an instance of the actual product reader class. This method should never return <code>null</code>.
     *
     * @return a new reader instance, never <code>null</code>
     */
    public ProductReader createReaderInstance() {
        return new NetcdfReader(this);
    }

    public BeamFileFilter getProductFileFilter() {
        return new BeamFileFilter(getFormatNames()[0], getDefaultFileExtensions(), getDescription(null));
    }

    /**
     * Gets the names of the product formats handled by this product I/O plug-in.
     *
     * @return the names of the product formats handled by this product I/O plug-in, never <code>null</code>
     */
    public String[] getFormatNames() {
        return new String[]{
                NetcdfConstants.FORMAT_NAME
        };
    }

    /**
     * Gets the default file extensions associated with each of the format names returned by the <code>{@link
     * #getFormatNames}</code> method. <p>The string array returned shall always have the same lenhth as the array
     * returned by the <code>{@link #getFormatNames}</code> method. <p>The extensions returned in the string array shall
     * always include a leading colon ('.') character, e.g. <code>".hdf"</code>
     *
     * @return the default file extensions for this product I/O plug-in, never <code>null</code>
     */
    public String[] getDefaultFileExtensions() {
        return NetcdfConstants.FILE_EXTENSIONS;
    }

    /**
     * Gets a short description of this plug-in. If the given locale is set to <code>null</code> the default locale is
     * used.
     * <p/>
     * <p> In a GUI, the description returned could be used as tool-tip text.
     *
     * @param locale the local for the given decription string, if <code>null</code> the default locale is used
     *
     * @return a textual description of this product reader/writer
     */
    public String getDescription(final Locale locale) {
        return NetcdfConstants.FORMAT_DESCRIPTION;
    }
}
