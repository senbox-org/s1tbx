/*
 * $Id: ErsProductReaderPlugIn.java,v 1.2 2007/02/09 09:54:49 marcop Exp $
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
package org.esa.beam.dataio.ers;

import org.esa.beam.dataio.envisat.EnvisatProductReader;
import org.esa.beam.dataio.envisat.EnvisatProductReaderPlugIn;
import org.esa.beam.dataio.envisat.ProductFile;
import org.esa.beam.framework.dataio.ProductReader;

import javax.imageio.stream.ImageInputStream;
import java.io.File;
import java.util.Locale;

/**
 * The <code>ErsProductReaderPlugIn</code> class is an implementation of the <code>ProductReaderPlugIn</code>
 * interface exclusively for ERS1/2 data products having the standard ESA/ENVISAT raw format.
 * <p/>
 *
 * @author Norman Fomferra
 * @version $Revision: 1.2 $ $Date: 2007/02/09 09:54:49 $
 */
public class ErsProductReaderPlugIn extends EnvisatProductReaderPlugIn {


    /**
     * Constructs a new ERS1/2 product reader plug-in instance.
     */
    public ErsProductReaderPlugIn() {
    }

    /**
     * Returns a string array containing the single entry <code>&quot;ERS1/2&quot;</code>.
     */
    @Override
    public String[] getFormatNames() {
        return new String[]{"ERS1/2"};
    }

    /**
     * Gets the default file extensions associated with each of the format names returned by the <code>{@link
     * #getFormatNames}</code> method. <p>The string array returned shall always have the same lenhth as the array
     * returned by the <code>{@link #getFormatNames}</code> method. <p>The extensions returned in the string array shall
     * always include a leading colon ('.') character, e.g. <code>".hdf"</code>
     *
     * @return the default file extensions for this product I/O plug-in, never <code>null</code>
     */
    @Override
    public String[] getDefaultFileExtensions() {
        return new String[]{".E1", ".E2"};
    }

    /**
     * Gets a short description of this plug-in. If the given locale is set to <code>null</code> the default locale is
     * used.
     * <p/>
     * <p> In a GUI, the description returned could be used as tool-tip text.
     *
     * @param name the local for the given decription string, if <code>null</code> the default locale is used
     *
     * @return a textual description of this product reader/writer
     */
    @Override
    public String getDescription(Locale name) {
        return "ERS1/2 AATSR and SAR products";
    }

    /**
     * Checks whether the given object is an acceptable input for this product reader and if so, the method checks if
     * it's content has the ENVISAT format by checking if the first bytes in the file equals the ENVISAT magic file
     * string <code>PRODUCT=&quot;</code>.
     * <p/>
     *
     * @param object the input object
     *
     * @return <code>true</code> if the given input is an object referencing a physical ERS1/2 data source.
     */
    @Override
    public boolean canDecodeInput(Object object) {
        if (object instanceof String) {
            return ProductFile.getProductType(new File((String) object)) != null;
        } else if (object instanceof File) {
            return ProductFile.getProductType((File) object) != null;
        } else if (object instanceof ImageInputStream) {
            return ProductFile.getProductType((ImageInputStream) object) != null;
        } else {
            return false;
        }
    }

    /**
     * Returns an array containing the classes that represent valid input types for an ERS1/2 product reader.
     * <p/>
     * <p> Instances of the classes returned in this array are valid objects for the <code>readProductNodes</code>
     * method of the <code>AbstractProductReader</code> class (the method will not throw an
     * <code>InvalidArgumentException</code> in this case).
     *
     * @return an array containing valid input types, never <code>null</code>
     *
     * @see org.esa.beam.framework.dataio.AbstractProductReader#readProductNodes
     */
    @Override
    public Class[] getInputTypes() {
        return new Class[]{String.class, File.class, ImageInputStream.class};
    }

    /**
     * Creates an instance of the actual ENVISAT product reader class.
     *
     * @return a new instance of the <code>EnvisatProductReader</code> class
     */
    @Override
    public ProductReader createReaderInstance() {
        return new EnvisatProductReader(this);
    }
}
