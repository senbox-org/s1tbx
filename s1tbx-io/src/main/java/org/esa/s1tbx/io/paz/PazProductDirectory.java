
package org.esa.s1tbx.io.paz;

import org.esa.s1tbx.io.terrasarx.TerraSarXProductDirectory;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.datamodel.metadata.AbstractMetadataIO;
import org.jdom2.Element;

import java.io.*;

/**
 * This class represents a product directory.
 */
public class PazProductDirectory extends TerraSarXProductDirectory {


    public PazProductDirectory(final File inputFile) {
        super(inputFile);
    }

    @Override
    protected MetadataElement addMetaData() throws IOException {
        final MetadataElement root = new MetadataElement(Product.METADATA_ROOT_NAME);
        final Element rootElement = xmlDoc.getRootElement();
        AbstractMetadataIO.AddXMLMetadata(rootElement, AbstractMetadata.addOriginalProductMetadata(root));

        addAbstractedMetadataHeader(root);

        return root;
    }

    @Override
    protected void findImages(final MetadataElement newRoot) throws IOException {
        final String parentPath = getRelativePathToImageFolder();
        findImages(parentPath, newRoot);
    }

    @Override
    protected String getMission() {
        return "PAZ";
    }
}
