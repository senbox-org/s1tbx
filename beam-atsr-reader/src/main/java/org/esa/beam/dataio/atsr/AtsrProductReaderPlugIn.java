/*
 * $Id: AtsrProductReaderPlugIn.java,v 1.6 2007/01/25 10:26:57 marcop Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.dataio.atsr;

import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.util.io.BeamFileFilter;

import javax.imageio.stream.ImageInputStream;
import java.io.File;
import java.util.Locale;

/**
 * The plug-in providing the product reader for ERS ATSR products.
 *
 * @author Tom Block
 * @version $Revision$ $Date$
 */
public class AtsrProductReaderPlugIn implements ProductReaderPlugIn {

    /**
     * Constructs a new ATSR product reader plug-in instance.
     */
    public AtsrProductReaderPlugIn() {
    }

    /**
     * Checks whether the given object is an acceptable input for this product reader and if so, the method checks if it
     * is capable of decoding the input's content.
     */
    public DecodeQualification getDecodeQualification(Object input) {
        AtsrFileFactory factory = AtsrFileFactory.getInstance();
        if (input instanceof String) {
            return factory.getDecodeQualification((String) input);
        } else if (input instanceof File) {
            return factory.getDecodeQualification((File) input);
        } else if (input instanceof ImageInputStream) {
            return factory.getDecodeQualification((ImageInputStream) input);
        } else {
            return DecodeQualification.UNABLE;
        }
    }

    /**
     * Gets the names of the product formats handled by this reader or writer plug-in.
     */
    public String[] getFormatNames() {
        return new String[]{AtsrConstants.ATSR_FORMAT_NAME};
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
        return new String[]{""};
    }

    /**
     * Returns an array containing the classes that represent valid input types for this reader.
     * <p/>
     * <p> Instances of the classes returned in this array are valid objects for the <code>setInput</code> method of the
     * <code>ProductReader</code> interface (the method will not throw an <code>InvalidArgumentException</code> in this
     * case).
     *
     * @return an array containing valid input types, never <code>null</code>
     */
    public Class[] getInputTypes() {
        return new Class[]{String.class, File.class, ImageInputStream.class};
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
    public String getDescription(Locale locale) {
        return AtsrConstants.DESCRIPTION;
    }

    /**
     * Creates an instance of the actual product reader class. This method should never return <code>null</code>.
     *
     * @return a new reader instance, never <code>null</code>
     */
    public ProductReader createReaderInstance() {
        return new AtsrProductReader(this);
    }

    public BeamFileFilter getProductFileFilter() {
        return new BeamFileFilter(getFormatNames()[0], getDefaultFileExtensions(), getDescription(null));
    }
}
