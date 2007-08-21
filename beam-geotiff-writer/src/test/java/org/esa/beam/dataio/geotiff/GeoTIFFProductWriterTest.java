package org.esa.beam.dataio.geotiff;

/**
 * Created by IntelliJ IDEA.
 * User: marco
 * Date: 16.02.2005
 * Time: 12:18:19
 */

import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;

import com.bc.ceres.core.ProgressMonitor;

public class GeoTIFFProductWriterTest extends TestCase {

    private static final String FILENAME = "temp.tif";
    private GeoTIFFProductWriter _productWriter;
    private Product _product;

    protected void setUp() throws Exception {
        new File(FILENAME).delete();

        _productWriter = new GeoTIFFProductWriter(new GeoTIFFProductWriterPlugIn());

        _product = new Product("product", "type", 10, 20);
        _product.addBand("b1", ProductData.TYPE_UINT32);
        fillBandWithData(_product.getBand("b1"), 1);
    }

    protected void tearDown() throws Exception {
        _productWriter.close();
        new File(FILENAME).delete();
    }

    public void testGeoTIFFProductWriterCreation() {
        final GeoTIFFProductWriter productWriter = new GeoTIFFProductWriter(new GeoTIFFProductWriterPlugIn());

        assertNotNull(productWriter.getWriterPlugIn());
    }

    public void testThatStringIsAValidOutput() throws IOException {
        _productWriter.writeProductNodes(_product, FILENAME);
    }

    public void testThatFileIsAValidOutput() throws IOException {
        _productWriter.writeProductNodes(_product, new File(FILENAME));
    }

    public void testWriteProductNodesAreWritten() throws IOException {
        _productWriter.writeProductNodes(_product, FILENAME);
        _productWriter.close();

        assertTrue(new File(FILENAME).length() > 0);
    }

    public void testWriteProductNodes_CropFileSize() throws IOException {
        final long expectedSize = computeExpectedSize(_product);
        createBiggerFile(expectedSize);

        _productWriter.writeProductNodes(_product, FILENAME);
        writeAllBands(_product);
        _productWriter.close();

        assertEquals(expectedSize, new File(FILENAME).length());
    }

    private void writeAllBands(final Product product) throws IOException {
        final int width = product.getSceneRasterWidth();
        final int height = product.getSceneRasterHeight();
        final Band[] bands = product.getBands();
        for (Band band : bands) {
            _productWriter.writeBandRasterData(band, 0, 0, width, height, band.getData(), ProgressMonitor.NULL);
        }
    }

    private void createBiggerFile(final long expectedSize) throws IOException {
        final RandomAccessFile raf = new RandomAccessFile(FILENAME, "rw");
        raf.setLength(expectedSize + 10);
        raf.close();
    }

    private long computeExpectedSize(final Product product) {
        final TiffIFD tiffIFD = new TiffIFD(product);
        return tiffIFD.getRequiredEntireSize() + TiffHeader.FIRST_IFD_OFFSET.getValue();
    }

    private void fillBandWithData(final Band band, final int start) {
        final ProductData data = band.createCompatibleRasterData();
        for (int i = 0; i < band.getSceneRasterWidth() * band.getSceneRasterHeight(); i++) {
            data.setElemIntAt(i, start + i);
        }
        band.setData(data);
    }
}