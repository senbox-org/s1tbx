
package org.esa.snap.dataio.envi;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.dataio.AbstractProductWriter;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.dataio.ProductWriterPlugIn;
import org.esa.snap.core.dataio.dimap.DimapProductConstants;
import org.esa.snap.core.dataio.dimap.DimapProductReader;
import org.esa.snap.core.dataio.dimap.EnviHeader;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.FilterBand;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.ProductNode;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.datamodel.VirtualBand;
import org.esa.snap.core.util.Debug;
import org.esa.snap.core.util.Guardian;
import org.esa.snap.core.util.io.FileUtils;

import javax.imageio.stream.FileImageOutputStream;
import javax.imageio.stream.ImageOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Map;

/**
 * The product writer for ENVI products.
 *
 */
public class EnviProductWriter extends AbstractProductWriter {

    protected File _outputDir;
    protected File _outputFile;
    private Map _bandOutputStreams;
    private boolean _incremental = true;

    /**
     * Construct a new instance of a product writer for the given ENVI product writer plug-in.
     *
     * @param writerPlugIn the given ENVI product writer plug-in, must not be <code>null</code>
     */
    public EnviProductWriter(ProductWriterPlugIn writerPlugIn) {
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
        final Object output = getOutput();

        File outputFile = null;
        if (output instanceof String) {
            outputFile = new File((String) output);
        } else if (output instanceof File) {
            outputFile = (File) output;
        }
        Debug.assertNotNull(outputFile); // super.writeProductNodes should have checked this already
        initDirs(outputFile);

        ensureNamingConvention();
        getSourceProduct().setProductWriter(this);
        getSourceProduct().setFileLocation(_outputDir);
        deleteRemovedNodes();
    }

    /**
     * Initializes all the internal file and directory elements from the given output file. This method only must be
     * called if the product writer should write the given data to raw data files without calling of writeProductNodes.
     * This may be at the time when a dimap product was opened and the data should be continuously changed in the same
     * product file without an previous call to the saveProductNodes to this product writer.
     *
     * @param outputFile the dimap header file location.
     */
    protected void initDirs(final File outputFile) {
        final String name = FileUtils.getFilenameWithoutExtension(outputFile);          
        _outputDir = outputFile.getParentFile();
        if (_outputDir == null) {
            _outputDir = new File(".");
        }
        _outputDir = new File(_outputDir, name);
        _outputDir.mkdirs();
        _outputFile = new File(_outputDir, outputFile.getName());
    }

    protected void ensureNamingConvention() {
        if (_outputFile != null) {
            getSourceProduct().setName(FileUtils.getFilenameWithoutExtension(_outputFile));
        }
    }

    /**
     * {@inheritDoc}
     */
    public void writeBandRasterData(Band sourceBand,
                                    int sourceOffsetX, int sourceOffsetY,
                                    int sourceWidth, int sourceHeight,
                                    ProductData sourceBuffer,
                                    ProgressMonitor pm) throws IOException {
        Guardian.assertNotNull("sourceBand", sourceBand);
        Guardian.assertNotNull("sourceBuffer", sourceBuffer);
        checkBufferSize(sourceWidth, sourceHeight, sourceBuffer);
        final int sourceBandWidth = sourceBand.getRasterWidth();
        final int sourceBandHeight = sourceBand.getRasterHeight();
        checkSourceRegionInsideBandRegion(sourceWidth, sourceBandWidth, sourceHeight, sourceBandHeight, sourceOffsetX,
                                          sourceOffsetY);
        final ImageOutputStream outputStream = getOrCreateImageOutputStream(sourceBand);
        long outputPos = (long) sourceOffsetY * (long) sourceBandWidth + sourceOffsetX;
        pm.beginTask("Writing band '" + sourceBand.getName() + "'...", 1);//sourceHeight);
        try {
            final long max = sourceHeight * sourceWidth;
            for (int sourcePos = 0; sourcePos < max; sourcePos += sourceWidth) {
                sourceBuffer.writeTo(sourcePos, sourceWidth, outputStream, outputPos);
                outputPos += sourceBandWidth;
            }
            pm.worked(1);
        } finally {
            pm.done();
        }
    }

    /**
     * Deletes the physically representation of the product from the hard disk.
     */
    public void deleteOutput() throws IOException {
        flush();
        close();
        if (_outputFile != null && _outputFile.exists() && _outputFile.isFile()) {
            _outputFile.delete();
        }
    }

    private static void checkSourceRegionInsideBandRegion(int sourceWidth, final int sourceBandWidth, int sourceHeight,
                                                          final int sourceBandHeight, int sourceOffsetX,
                                                          int sourceOffsetY) {
        Guardian.assertWithinRange("sourceWidth", sourceWidth, 1, sourceBandWidth);
        Guardian.assertWithinRange("sourceHeight", sourceHeight, 1, sourceBandHeight);
        Guardian.assertWithinRange("sourceOffsetX", sourceOffsetX, 0, sourceBandWidth - sourceWidth);
        Guardian.assertWithinRange("sourceOffsetY", sourceOffsetY, 0, sourceBandHeight - sourceHeight);
    }

    private static void checkBufferSize(int sourceWidth, int sourceHeight, ProductData sourceBuffer) {
        final long expectedBufferSize = (long) sourceWidth * (long) sourceHeight;
        final long actualBufferSize = sourceBuffer.getNumElems();
        Guardian.assertEquals("sourceWidth * sourceHeight", actualBufferSize, expectedBufferSize);  /*I18N*/
    }

    /**
     * Writes all data in memory to disk. After a flush operation, the writer can be closed safely
     *
     * @throws java.io.IOException on failure
     */
    public void flush() throws IOException {
        if (_bandOutputStreams == null) {
            return;
        }
        for (Object o : _bandOutputStreams.values()) {
            ((ImageOutputStream) o).flush();
        }
    }

    /**
     * Closes all output streams currently open.
     *
     * @throws java.io.IOException on failure
     */
    public void close() throws IOException {
        if (_bandOutputStreams == null) {
            return;
        }
        for (Object o : _bandOutputStreams.values()) {
            ((ImageOutputStream) o).close();
        }
        _bandOutputStreams.clear();
        _bandOutputStreams = null;
    }

    /**
     * Returns the data output stream associated with the given <code>Band</code>. If no stream exists, one is created
     * and fed into the hash map
     */
    private ImageOutputStream getOrCreateImageOutputStream(Band band) throws IOException {
        ImageOutputStream outputStream = getImageOutputStream(band);
        if (outputStream == null) {
            outputStream = createImageOutputStream(band);
            if (_bandOutputStreams == null) {
                _bandOutputStreams = new HashMap();
            }
            _bandOutputStreams.put(band, outputStream);
        }
        return outputStream;
    }

    private ImageOutputStream getImageOutputStream(Band band) {
        if (_bandOutputStreams != null) {
            return (ImageOutputStream) _bandOutputStreams.get(band);
        }
        return null;
    }

    /**
     * Returns a file associated with the given <code>Band</code>. The method ensures that the file exists and have the
     * right size. Also ensures a recreate if the file not exists or the file have a different file size. A new envi
     * header file was written every call.
     */
    protected File getValidImageFile(Band band) throws IOException {
        writeEnviHeader(band); // always (re-)write ENVI header
        final File file = getImageFile(band);
        if (file.exists()) {
            if (file.length() != getImageFileSize(band)) {
                createPhysicalImageFile(band, file);
            }
        } else {
            createPhysicalImageFile(band, file);
        }
        return file;
    }

    private static void createPhysicalImageFile(Band band, File file) throws IOException {
        createPhysicalFile(file, getImageFileSize(band));
    }

    protected void writeEnviHeader(Band band) throws IOException {
        EnviHeader.createPhysicalFile(getEnviHeaderFile(band),
                                      band,
                                      band.getRasterWidth(),
                                      band.getRasterHeight());
    }

    protected ImageOutputStream createImageOutputStream(Band band) throws IOException {
        return new FileImageOutputStream(getValidImageFile(band));
    }

    private static long getImageFileSize(RasterDataNode band) {
        return (long) ProductData.getElemSize(band.getDataType()) *
                (long) band.getRasterWidth() *
                (long) band.getRasterHeight();
    }

    protected File getEnviHeaderFile(Band band) {
        return new File(_outputDir, createEnviHeaderFilename(band));
    }

    protected String createEnviHeaderFilename(Band band) {
        return band.getName() + EnviHeader.FILE_EXTENSION;
    }

    private File getImageFile(Band band) {
        return new File(_outputDir, createImageFilename(band));
    }

    protected String createImageFilename(Band band) {
        return band.getName() + DimapProductConstants.IMAGE_FILE_EXTENSION;
    }

    private static void createPhysicalFile(File file, long fileSize) throws IOException {
        final File parentDir = file.getParentFile();
        if (parentDir != null) {
            parentDir.mkdirs();
        }
        final RandomAccessFile randomAccessFile = new RandomAccessFile(file, "rw");
        randomAccessFile.setLength(fileSize);
        randomAccessFile.close();
    }

    @Override
    public boolean shouldWrite(ProductNode node) {
        if (node instanceof VirtualBand) {
            return false;
        }
        if (node instanceof FilterBand) {
            return false;
        }
        if (node.isModified()) {
            return true;
        }
        if (!isIncrementalMode()) {
            return true;
        }
        if (!(node instanceof Band)) {
            return true;
        }
        final File imageFile = getImageFile((Band) node);
        return !(imageFile != null && imageFile.exists());
    }

    /**
     * Enables resp. disables incremental writing of this product writer. By default, a reader should enable progress
     * listening.
     *
     * @param enabled enables or disables progress listening.
     */
    @Override
    public void setIncrementalMode(boolean enabled) {
        _incremental = enabled;
    }

    /**
     * Returns whether this product writer writes only modified product nodes.
     *
     * @return <code>true</code> if so
     */
    @Override
    public boolean isIncrementalMode() {
        return _incremental;
    }

    /**
     * Entfernt alle zu l�schenden dateifragmente aus dem Product bevor der header und die b�nder geschrieben werden.
     * Das ist nur notwendig, f�r den fall da� der Benutzer ein DIMAP-Product ge�ffnet hat, darin eine oderer mehrere
     * product nodes gel�scht und anschlie�end nodes mit den gleichen namen erzeugt hat. Sind die gel�schten und
     * wiedererstellten nodes zum Beispiel B�nder, so w�rde der writer diese neu erzeugten Banddaten nicht schreiben,
     * wenn diese zuvor nicht von der Festplatte gel�scht worden sind. Bevor banddaten von der Festplatte gel�scht
     * werden k�nnen ist es notwendig den reader zu schlie�en (reader.close()) damit dieser die Dateien zum l�schen frei
     * gibt.
     *
     * @throws java.io.IOException if an IOException occurs.
     */
    private void deleteRemovedNodes() throws IOException {
        final Product product = getSourceProduct();
        final ProductReader productReader = product.getProductReader();
        if (productReader instanceof DimapProductReader) {
            final ProductNode[] removedNodes = product.getRemovedChildNodes();
            if (removedNodes.length > 0) {
                productReader.close();
                for (ProductNode removedNode : removedNodes) {
                    removedNode.removeFromFile(this);
                }
            }
        }
    }

    @Override
    public void removeBand(Band band) {
        if (band != null) {
            final String headerFilename = createEnviHeaderFilename(band);
            final String imageFilename = createImageFilename(band);
            File[] files = null;
            if (_outputDir != null && _outputDir.exists()) {
                files = _outputDir.listFiles();
            }
            if (files == null) {
                return;
            }
            String name;
            for (File file : files) {
                name = file.getName();
                if (file.isFile() && (name.equals(headerFilename) || name.equals(imageFilename))) {
                    file.delete();
                }
            }
        }
    }

    protected File getOutputDir() {
        return _outputDir;
    }
}
