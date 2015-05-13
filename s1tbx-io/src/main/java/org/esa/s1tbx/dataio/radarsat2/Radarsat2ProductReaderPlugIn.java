/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.s1tbx.dataio.radarsat2;

import org.esa.snap.framework.dataio.DecodeQualification;
import org.esa.snap.framework.dataio.ProductReader;
import org.esa.snap.framework.dataio.ProductReaderPlugIn;
import org.esa.snap.framework.datamodel.RGBImageProfile;
import org.esa.snap.framework.datamodel.RGBImageProfileManager;
import org.esa.snap.gpf.ReaderUtils;
import org.esa.snap.util.io.SnapFileFilter;

import java.io.File;
import java.util.Locale;
import java.util.Optional;
import java.util.zip.ZipFile;

/**
 * The ReaderPlugIn for Radarsat2 products.
 */
public class Radarsat2ProductReaderPlugIn implements ProductReaderPlugIn {

    static{
        registerRGBProfiles();
    }

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
            final String filename = file.getName().toLowerCase();
            if (filename.equals(Radarsat2Constants.PRODUCT_HEADER_NAME) ||
                    filename.equalsIgnoreCase(Radarsat2Constants.RSM_SIM_PRODUCT_HEADER_NAME)) {

                final File[] files = file.getParentFile().listFiles();
                if(files != null) {
                    for (File f : files) {
                        if (f.getName().toLowerCase().endsWith("ntf")) {
                            return DecodeQualification.SUITABLE;
                        }
                    }
                }
                return DecodeQualification.INTENDED;
            }
            if (filename.endsWith(".zip") && isZippedRS2(file)) {
                return DecodeQualification.INTENDED;
            }
        }
        //todo zip stream

        return DecodeQualification.UNABLE;
    }

    static boolean isZippedRS2(final File file) {
        try {
            final ZipFile productZip = new ZipFile(file, ZipFile.OPEN_READ);

            final Optional result = productZip.stream()
                    .filter(ze -> !ze.isDirectory())
                    .filter(ze -> ze.getName().toLowerCase().endsWith(Radarsat2Constants.PRODUCT_HEADER_NAME))
                    .findFirst();
            return result.isPresent();
        } catch (Exception e) {
            //e.printStackTrace();
        }
        return false;
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
        return Radarsat2Constants.VALID_INPUT_TYPES;
    }

    /**
     * Creates an instance of the actual product reader class. This method should never return <code>null</code>.
     *
     * @return a new reader instance, never <code>null</code>
     */
    public ProductReader createReaderInstance() {
        return new Radarsat2ProductReader(this);
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
        return Radarsat2Constants.getFormatNames();
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
        return Radarsat2Constants.getForamtFileExtensions();
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
        return Radarsat2Constants.getPluginDescription();
    }

    public static class FileFilter extends SnapFileFilter {

        public FileFilter() {
            super();
            setFormatName(Radarsat2Constants.getFormatNames()[0]);
            setExtensions(Radarsat2Constants.getForamtFileExtensions());
            setDescription(Radarsat2Constants.getPluginDescription());
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
                        (name.startsWith(Radarsat2Constants.PRODUCT_HEADER_PREFIX) && name.endsWith(Radarsat2Constants.getIndicationKey())) ||
                        (name.startsWith("RS2") && name.endsWith(".ZIP"))) {
                    return true;
                }
            }
            return false;
        }
    }

    private static void registerRGBProfiles() {
        RGBImageProfileManager manager = RGBImageProfileManager.getInstance();
        manager.addProfile(new RGBImageProfile("Pauli",
                                               new String[]{
                                                       "((i_HH-i_VV)*(i_HH-i_VV)+(q_HH-q_VV)*(q_HH-q_VV))/2",
                                                       "((i_HV+i_VH)*(i_HV+i_VH)+(q_HV+q_VH)*(q_HV+q_VH))/2",
                                                       "((i_HH+i_VV)*(i_HH+i_VV)+(q_HH+q_VV)*(q_HH+q_VV))/2"
                                               }
        ));
        manager.addProfile(new RGBImageProfile("Sinclair",
                                               new String[]{
                                                       "i_VV*i_VV+q_VV*q_VV",
                                                       "((i_HV+i_VH)*(i_HV+i_VH)+(q_HV+q_VH)*(q_HV+q_VH))/4",
                                                       "i_HH*i_HH+q_HH*q_HH"
                                               }
        ));
        manager.addProfile(new RGBImageProfile("Dual Pol HH+HV",
                                               new String[]{
                                                       "Intensity_HH",
                                                       "Intensity_HV",
                                                       "Intensity_HH/Intensity_HV"
                                               }
        ));
        manager.addProfile(new RGBImageProfile("Dual Pol VV+VH",
                                               new String[]{
                                                       "Intensity_VV",
                                                       "Intensity_VH",
                                                       "Intensity_VV/Intensity_VH"
                                               }
        ));
        manager.addProfile(new RGBImageProfile("Dual Pol HH+VV",
                                               new String[]{
                                                       "Intensity_HH",
                                                       "Intensity_VV",
                                                       "Intensity_HH/Intensity_VV"
                                               }
        ));
    }
}
