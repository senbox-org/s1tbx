
package org.esa.s1tbx.io.paz;

import org.esa.s1tbx.io.terrasarx.TerraSarXProductDirectory;
import org.esa.s1tbx.io.terrasarx.TerraSarXProductReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;

import java.io.File;

/**
 * The product reader for TerraSarX products.
 */
public class PazProductReader extends TerraSarXProductReader {

    /**
     * Constructs a new abstract product reader.
     *
     * @param readerPlugIn the reader plug-in which created this reader, can be <code>null</code> for internal reader
     *                     implementations
     */
    public PazProductReader(final ProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
    }


    @Override
    protected TerraSarXProductDirectory createProductDirectory(final File fileFromInput) {
        return new PazProductDirectory(fileFromInput);
    }
}
