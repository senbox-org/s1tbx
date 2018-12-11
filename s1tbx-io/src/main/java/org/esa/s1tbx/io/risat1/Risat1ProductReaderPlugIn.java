
package org.esa.s1tbx.io.risat1;

import org.esa.snap.core.dataio.DecodeQualification;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.util.io.SnapFileFilter;
import org.esa.snap.engine_utilities.gpf.ReaderUtils;
import org.esa.snap.engine_utilities.util.ZipUtils;

import java.io.File;
import java.util.Locale;

/**
 * The ReaderPlugIn for RISAT-1 products.
 */
public class Risat1ProductReaderPlugIn implements ProductReaderPlugIn {

    /**
     * Checks whether the given object is an acceptable input for this product reader and if so, the method checks if it
     * is capable of decoding the input's content.
     *
     * @param input any input object
     * @return true if this product reader can decode the given input, otherwise false.
     */
    public DecodeQualification getDecodeQualification(final Object input) {
        final File file = ReaderUtils.getFileFromInput(input);
        if (file != null) {
            final File metadataFile = findMetadataFile(file);
            if (metadataFile != null) {
                return DecodeQualification.INTENDED;
            }
            final String filename = file.getName().toLowerCase();
            if (filename.endsWith(".zip") && filename.startsWith("rs2") &&
                    ZipUtils.findInZip(file, "", Risat1Constants.PRODUCT_HEADER_NAME)) {
                return DecodeQualification.INTENDED;
            }
        }
        //todo zip stream

        return DecodeQualification.UNABLE;
    }

    public static File findMetadataFile(final File folder) {
        if (folder.isDirectory()) {
            final File[] fileList = folder.listFiles();
            if (fileList != null) {
                for (File f : fileList) {
                    final String fileName = f.getName().toLowerCase();
                    if (fileName.equals(Risat1Constants.PRODUCT_HEADER_NAME)) {
                        return f;
                    }
                    if (f.isDirectory()) {
                        final File foundFile = findMetadataFile(f);
                        if (foundFile != null) {
                            return foundFile;
                        }
                    }
                }
            }
        } else {
            final String fileName = folder.getName().toLowerCase();
            if (fileName.equals(Risat1Constants.PRODUCT_HEADER_NAME)){
                return folder;
            }
        }
        return null;
    }

    /**
     * Returns an array containing the classes that represent valid input types for this reader.
     * <p>
     * <p> Intances of the classes returned in this array are valid objects for the <code>setInput</code> method of the
     * <code>ProductReader</code> interface (the method will not throw an <code>InvalidArgumentException</code> in this
     * case).
     *
     * @return an array containing valid input types, never <code>null</code>
     */
    public Class[] getInputTypes() {
        return Risat1Constants.VALID_INPUT_TYPES;
    }

    /**
     * Creates an instance of the actual product reader class. This method should never return <code>null</code>.
     *
     * @return a new reader instance, never <code>null</code>
     */
    public ProductReader createReaderInstance() {
        return new Risat1ProductReader(this);
    }

    public SnapFileFilter getProductFileFilter() {
        return new FileFilter();
    }

    /**
     * Gets the names of the product formats handled by this product I/O plug-in.
     *
     * @return the names of the product formats handled by this product I/O plug-in, never <code>null</code>
     */
    public String[] getFormatNames() {
        return Risat1Constants.getFormatNames();
    }

    /**
     * Gets the default file extensions associated with each of the format names returned by the <code>{@link
     * #getFormatNames}</code> method. <p>The string array returned shall always have the same length as the array
     * returned by the <code>{@link #getFormatNames}</code> method. <p>The extensions returned in the string array shall
     * always include a leading colon ('.') character, e.g. <code>".hdf"</code>
     *
     * @return the default file extensions for this product I/O plug-in, never <code>null</code>
     */
    public String[] getDefaultFileExtensions() {
        return Risat1Constants.getFormatFileExtensions();
    }

    /**
     * Gets a short description of this plug-in. If the given locale is set to <code>null</code> the default locale is
     * used.
     * <p>
     * <p> In a GUI, the description returned could be used as tool-tip text.
     *
     * @param locale the local for the given decription string, if <code>null</code> the default locale is used
     * @return a textual description of this product reader/writer
     */
    public String getDescription(final Locale locale) {
        return Risat1Constants.getPluginDescription();
    }

    public static class FileFilter extends SnapFileFilter {

        public FileFilter() {
            super();
            setFormatName(Risat1Constants.getFormatNames()[0]);
            setExtensions(Risat1Constants.getFormatFileExtensions());
            setDescription(Risat1Constants.getPluginDescription());
        }

        /**
         * Tests whether or not the given file is accepted by this filter. The default implementation returns
         * <code>true</code> if the given file is a directory or the path string ends with one of the registered extensions.
         * if no extension are defined, the method always returns <code>true</code>
         *
         * @param file the file to be or not be accepted.
         * @return <code>true</code> if given file is accepted by this filter
         */
        public boolean accept(final File file) {
            if (super.accept(file)) {
                final String name = file.getName().toUpperCase();
                if (file.isDirectory() ||
                        (name.startsWith(Risat1Constants.PRODUCT_HEADER_PREFIX) && name.endsWith(Risat1Constants.getIndicationKey())) ||
                        (name.startsWith("RS2") && name.endsWith(".ZIP"))) {
                    return true;
                }
            }
            return false;
        }
    }
}
