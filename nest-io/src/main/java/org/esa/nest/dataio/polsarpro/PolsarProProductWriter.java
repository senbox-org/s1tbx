
package org.esa.nest.dataio.polsarpro;

import org.esa.beam.dataio.dimap.EnviHeader;
import org.esa.beam.dataio.envi.EnviProductWriter;
import org.esa.beam.framework.dataio.ProductWriterPlugIn;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.util.io.FileUtils;
import org.esa.nest.dataio.FileImageOutputStreamExtImpl;
import org.esa.nest.datamodel.AbstractMetadata;
import org.esa.nest.gpf.PolBandUtils;

import javax.imageio.stream.ImageOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.ByteOrder;

/**
 * The product writer for PolSARPro products.
 *
 */
public class PolsarProProductWriter extends EnviProductWriter {

    private final static String BIN_EXTENSION = ".bin";

    /**
     * Construct a new instance of a product writer for the given ENVI product writer plug-in.
     *
     * @param writerPlugIn the given ENVI product writer plug-in, must not be <code>null</code>
     */
    public PolsarProProductWriter(final ProductWriterPlugIn writerPlugIn) {
        super(writerPlugIn);
    }

    /**
     * Writes the in-memory representation of a data product. This method was called by <code>writeProductNodes(product,
     * output)</code> of the AbstractProductWriter.
     *
     * @throws IllegalArgumentException if <code>output</code> type is not one of the supported output sources.
     * @throws java.io.IOException      if an I/O error occurs
     */
    @Override
    protected void writeProductNodesImpl() throws IOException {
        super.writeProductNodesImpl();

        writeConfigFile(getSourceProduct(), getOutputDir());

        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(getSourceProduct());
        AbstractMetadata.saveExternalMetadata(getSourceProduct(), absRoot, new File(getOutputDir(), "metadata.xml"));
    }

    protected void writeEnviHeader(Band band) throws IOException {
        EnviHeader.createPhysicalFile(getEnviHeaderFile(band),
                band,
                band.getRasterWidth(),
                band.getRasterHeight(), 0);
    }

    protected ImageOutputStream createImageOutputStream(Band band) throws IOException {
        final ImageOutputStream out = new FileImageOutputStreamExtImpl(getValidImageFile(band));
        out.setByteOrder(ByteOrder.LITTLE_ENDIAN);
        return out;
    }

    /**
     * Initializes all the internal file and directory elements from the given output file. This method only must be
     * called if the product writer should write the given data to raw data files without calling of writeProductNodes.
     * This may be at the time when a dimap product was opened and the data shold be continously changed in the same
     * product file without an previous call to the saveProductNodes to this product writer.
     *
     * @param outputFile the dimap header file location.
     */
    protected void initDirs(final File outputFile) {
        super.initDirs(outputFile);

        final PolBandUtils.MATRIX matrixType = PolBandUtils.getSourceProductType(getSourceProduct());
        String folder = "";
        if(matrixType.equals(PolBandUtils.MATRIX.C3)) {
            folder = "C3";
        } else if(matrixType.equals(PolBandUtils.MATRIX.T3)) {
            folder = "T3";
        } else if(matrixType.equals(PolBandUtils.MATRIX.C4)) {
            folder = "C4";
        } else if(matrixType.equals(PolBandUtils.MATRIX.T4)) {
            folder = "T4";
        }
        if(!folder.isEmpty()) {
            _outputDir = new File(_outputDir, folder);
            _outputDir.mkdirs();
            _outputFile = new File(_outputDir, outputFile.getName());
        }
    }

    protected void ensureNamingConvention() {
    }

    protected String createImageFilename(Band band) {
        return band.getName() + BIN_EXTENSION;
    }

    protected String createEnviHeaderFilename(Band band) {
        return band.getName() + BIN_EXTENSION + EnviHeader.FILE_EXTENSION;
    }

    private static void writeConfigFile(final Product srcProduct, final File folder) {
        PrintStream p = null;
        try {
            final File file = new File(folder, "config.txt");
            final FileOutputStream out = new FileOutputStream(file);
            p = new PrintStream(out);

            p.println("Nrow");
            p.println(srcProduct.getSceneRasterHeight());
            p.println("---------");

            p.println("Ncol");
            p.println(srcProduct.getSceneRasterWidth());
            p.println("---------");

            p.println("PolarCase");
            p.println(getPolarCase(srcProduct));
            p.println("---------");

            p.println("PolarType");
            p.println(PolBandUtils.getPolarType(srcProduct));
            p.println("---------");

        } catch(Exception e) {
            System.out.println("PolsarProWriter unable to write config.txt "+e.getMessage());
        } finally {
            if(p != null)
                p.close();
        }
    }

    private static String getPolarCase(final Product srcProduct) {
        final PolBandUtils.MATRIX matrixType = PolBandUtils.getSourceProductType(srcProduct);
        if(matrixType.equals(PolBandUtils.MATRIX.C4) || matrixType.equals(PolBandUtils.MATRIX.T4)) {
            return "bistatic";
        }
        return "monostatic";
    }

}