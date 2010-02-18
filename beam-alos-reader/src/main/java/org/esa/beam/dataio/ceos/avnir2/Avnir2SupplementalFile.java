package org.esa.beam.dataio.ceos.avnir2;

import org.esa.beam.dataio.ceos.CeosFileReader;
import org.esa.beam.framework.datamodel.MetadataElement;

import javax.imageio.stream.ImageInputStream;
import java.io.IOException;

/**
 * * This class represents a supplemental file of an Avnir-2 product.
 *
 * @author Marco Peters
 */
class Avnir2SupplementalFile {

    private static final String META_ELEMENT_NAME = "Supplemental";

    private CeosFileReader _ceosReader;

    public Avnir2SupplementalFile(final ImageInputStream supplementalStream) {
        _ceosReader = new CeosFileReader(supplementalStream);
    }

    public MetadataElement getAsMetadata() {
        final MetadataElement root = new MetadataElement(META_ELEMENT_NAME);
        addPCDData(root);
        return root;
    }

    private void addPCDData(final MetadataElement root) {
    }

    public void close() throws IOException {
        _ceosReader.close();
        _ceosReader = null;
    }
}
