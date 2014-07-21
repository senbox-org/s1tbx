
package org.esa.beam.dataio.envi;

import org.esa.beam.framework.dataio.ProductWriter;
import org.esa.beam.framework.dataio.ProductWriterPlugIn;
import org.esa.beam.util.io.BeamFileFilter;

import java.io.File;
import java.util.Locale;

/**
 * The Envi writer
 */
public class EnviProductWriterPlugIn implements ProductWriterPlugIn {

    public final static String FORMAT_NAME = EnviConstants.FORMAT_NAME;
    private final BeamFileFilter fileFilter = new BeamFileFilter(getFormatNames()[0], getDefaultFileExtensions(), getDescription(null));

    /**
     * Constructs a new ENVI product writer plug-in instance.
     */
    public EnviProductWriterPlugIn() {
    }

    /**
     * Returns a string array containing the single entry.
     */
    public String[] getFormatNames() {
        return new String[]{FORMAT_NAME};
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
        return EnviConstants.VALID_EXTENSIONS;
    }

    /**
     * Returns an array containing the classes that represent valid output types for this product writer.
     * <p/>
     * <p> Intances of the classes returned in this array are valid objects for the <code>writeProductNodes</code>
     * method of the <code>AbstractProductWriter</code> interface (the method will not throw an
     * <code>InvalidArgumentException</code> in this case).
     *
     * @return an array containing valid output types, never <code>null</code>
     *
     * @see org.esa.beam.framework.dataio.AbstractProductWriter#writeProductNodes
     */
    public Class[] getOutputTypes() {
        return new Class[]{String.class, File.class};
    }

    /**
     * Gets a short description of this plug-in. If the given locale is set to <code>null</code> the default locale is
     * used.
     * <p/>
     * <p> In a GUI, the description returned could be used as tool-tip text.
     *
     * @param locale the locale name for the given decription string, if <code>null</code> the default locale is used
     *
     * @return a textual description of this product reader/writer
     */
    public String getDescription(Locale locale) {
        return "ENVI product writer";
    }

    /**
     * Creates an instance of the actual ENVI product writer class.
     *
     * @return a new instance of the <code>EnviProductWriter</code> class
     */
    public ProductWriter createWriterInstance() {
        return new EnviProductWriter(this);
    }

    public BeamFileFilter getProductFileFilter() {
        return fileFilter;
    }
}