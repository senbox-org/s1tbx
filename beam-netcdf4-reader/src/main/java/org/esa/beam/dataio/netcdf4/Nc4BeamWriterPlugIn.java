package org.esa.beam.dataio.netcdf4;

import org.esa.beam.framework.dataio.ProductWriter;
import org.esa.beam.framework.dataio.ProductWriterPlugIn;
import org.esa.beam.util.io.BeamFileFilter;

import java.io.File;
import java.util.Locale;


/**
 * The plug-in class for the {@link org.esa.beam.dataio.netcdf4.Nc4Reader}.
 *
 * @author Norman Fomferra
 */
public class Nc4BeamWriterPlugIn implements ProductWriterPlugIn {

    /**
     * Returns an array containing the classes that represent valid output types for this writer.
     * <p/>
     * <p> Intances of the classes returned in this array are valid objects for the <code>setOutput</code> method of the
     * <code>ProductWriter</code> interface (the method will not throw an <code>InvalidArgumentException</code> in this
     * case).
     *
     * @return an array containing valid output types, never <code>null</code>
     *
     * @see org.esa.beam.framework.dataio.ProductWriter#writeProductNodes
     */
    @Override
    public Class[] getOutputTypes() {
        return new Class[]{String.class, File.class};
    }

    /**
     * Creates an instance of the actual product writer class. This method should never return <code>null</code>.
     *
     * @return a new writer instance, never <code>null</code>
     */
    @Override
    public ProductWriter createWriterInstance() {
        return new Nc4BeamWriter(this);
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
        return new String[]{Nc4Constants.FORMAT_NAME_BEAM};
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
        return new String[]{Nc4Constants.FILE_EXTENSION_NC};
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
        return Nc4Constants.FORMAT_DESCRIPTION_BEAM;
    }
}