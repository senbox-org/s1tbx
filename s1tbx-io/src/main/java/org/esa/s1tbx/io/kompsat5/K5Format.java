package org.esa.s1tbx.io.kompsat5;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.dataop.downloadable.XMLSupport;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.datamodel.metadata.AbstractMetadataIO;
import org.jdom2.Document;
import org.jdom2.Element;

import java.io.File;
import java.io.IOException;

/**
 * Created by luis on 12/08/2016.
 */
public interface K5Format {

    Product open(final File inputFile) throws IOException;

    void close() throws IOException;

    void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight,
                                          int sourceStepX, int sourceStepY, Band destBand, int destOffsetX,
                                          int destOffsetY, int destWidth, int destHeight, ProductData destBuffer,
                                          ProgressMonitor pm) throws IOException;

    default void addAuxXML(final Product product) {
        try {
            final File folder = product.getFileLocation().getParentFile();
            File dnFile = null;
            final File[] files = folder.listFiles();
            if (files != null) {
                for (File f : files) {
                    final String name = f.getName().toLowerCase();
                    if (name.endsWith("_aux.xml")) {
                        dnFile = f;
                        break;
                    }
                }
            }
            if (dnFile != null) {
                final Document xmlDoc = XMLSupport.LoadXML(dnFile.getAbsolutePath());
                final Element rootElement = xmlDoc.getRootElement();

                AbstractMetadataIO.AddXMLMetadata(rootElement, AbstractMetadata.getOriginalProductMetadata(product));
            }
        } catch (IOException e) {
            SystemUtils.LOG.warning("Unable to read aux file for " + product.getName());
        }
    }
}
