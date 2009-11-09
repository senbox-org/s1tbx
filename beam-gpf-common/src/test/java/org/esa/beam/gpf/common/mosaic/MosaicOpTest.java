package org.esa.beam.gpf.common.mosaic;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.CrsGeoCoding;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorSpiRegistry;
import org.geotools.referencing.CRS;
import org.junit.AfterClass;
import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.Test;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.operation.TransformException;

import javax.media.jai.operator.ConstantDescriptor;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.image.Raster;
import java.io.IOException;

/**
 * @author Marco Peters
 * @version $ Revision $ Date $
 * @since BEAM 4.7
 */
public class MosaicOpTest {

    private static final int WIDTH = 10;
    private static final int HEIGHT = 10;


    private static Product product1;
    private static Product product2;
    private static Product product3;

    @BeforeClass
    public static void setup() throws FactoryException, TransformException {
        product1 = createProduct("P1", 0, 0, 2.0f);
        product2 = createProduct("P2", 4, -4, 3.0f);
        product3 = createProduct("P3", -5, 5, 5.0f);
        // We have to load SPIs manually, otherwise SPI for Reproject is not available
        final OperatorSpiRegistry registry = GPF.getDefaultInstance().getOperatorSpiRegistry();
        registry.loadOperatorSpis();
    }

    private static Product createProduct(final String name, final int easting, final int northing,
                                         final float bandFillValue) throws FactoryException, TransformException {
        final Product product = new Product(name, "T", WIDTH, HEIGHT);
        product.addBand(createBand(bandFillValue));
        final AffineTransform transform = new AffineTransform();
        transform.translate(easting, northing);
        transform.scale(1, -1);
        transform.translate(-0.5, -0.5);
        product.setGeoCoding(
                new CrsGeoCoding(CRS.decode("EPSG:4326", true), new Rectangle(0, 0, WIDTH, HEIGHT), transform));
        return product;
    }

    private static Band createBand(float fillValue) {
        final Band band = new Band("b1", ProductData.TYPE_FLOAT32, WIDTH, HEIGHT);
        band.setSourceImage(ConstantDescriptor.create((float) WIDTH, (float) HEIGHT, new Float[]{fillValue}, null));
        return band;
    }

    @AfterClass
    public static void teardown() {
        product1.dispose();
        product2.dispose();
        product3.dispose();
    }

    @Test
    public void testXXXXX() throws IOException {
        final MosaicOp op = new MosaicOp();
        op.setSourceProducts(new Product[]{product1, product2, product3});
        op.outputVariables = new MosaicOp.Variable[]{
                new MosaicOp.Variable("b1", "b1"),

        };
        op.boundary = new MosaicOp.GeoBounds(-10, 10, 10, -10);
        op.pixelSizeX = 1;
        op.pixelSizeY = 1;

        final Product product = op.getTargetProduct();
        final GeoCoding geoCoding = product.getGeoCoding();
        PixelPos pp;

        final Band b1Band = product.getBand("b1");

        pp = geoCoding.getPixelPos(new GeoPos(8, -8), null);
        final Raster b1Raster = b1Band.getSourceImage().getData();
        assertEquals(0.0f, b1Raster.getSampleFloat((int) pp.x, (int) pp.y, 0), 1.0e-6);
        pp = geoCoding.getPixelPos(new GeoPos(4, -4), null);
        assertEquals(5.0f, b1Raster.getSampleFloat((int) pp.x, (int) pp.y, 0), 1.0e-6);
        pp = geoCoding.getPixelPos(new GeoPos(-1, 1), null);
        assertEquals(3.5f, b1Raster.getSampleFloat((int) pp.x, (int) pp.y, 0), 1.0e-6);
        pp = geoCoding.getPixelPos(new GeoPos(-4, 4), null);
        assertEquals(3.3333333f, b1Raster.getSampleFloat((int) pp.x, (int) pp.y, 0), 1.0e-6);
        pp = geoCoding.getPixelPos(new GeoPos(-8, 8), null);
        assertEquals(2.5f, b1Raster.getSampleFloat((int) pp.x, (int) pp.y, 0), 1.0e-6);

        final Band countBand = product.getBand("b1_count");

        pp = geoCoding.getPixelPos(new GeoPos(8, -8), null);
        final Raster countRaster = countBand.getSourceImage().getData();
        assertEquals(0, countRaster.getSample((int) pp.x, (int) pp.y, 0));
        pp = geoCoding.getPixelPos(new GeoPos(4, -4), null);
        assertEquals(1, countRaster.getSample((int) pp.x, (int) pp.y, 0));
        pp = geoCoding.getPixelPos(new GeoPos(-1, 1), null);
        assertEquals(2, countRaster.getSample((int) pp.x, (int) pp.y, 0));
        pp = geoCoding.getPixelPos(new GeoPos(-4, 4), null);
        assertEquals(3, countRaster.getSample((int) pp.x, (int) pp.y, 0));
        pp = geoCoding.getPixelPos(new GeoPos(-8, 8), null);
        assertEquals(2, countRaster.getSample((int) pp.x, (int) pp.y, 0));

    }
}