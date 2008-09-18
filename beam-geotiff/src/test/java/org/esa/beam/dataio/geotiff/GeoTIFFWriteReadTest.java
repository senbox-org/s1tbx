package org.esa.beam.dataio.geotiff;

import com.sun.media.jai.codec.ByteArraySeekableStream;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.dataop.maptransf.*;
import org.esa.beam.jai.ImageManager;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import javax.imageio.stream.MemoryCacheImageInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;

public class GeoTIFFWriteReadTest {

    private Product outProduct;
    private ByteArrayOutputStream outputStream;
    private GeoTIFFProductReader reader;
    private File location;

    @Before
    public void setup() {
        reader = (GeoTIFFProductReader) new GeoTIFFProductReaderPlugIn().createReaderInstance();
        outputStream = new ByteArrayOutputStream();
        location = new File("memory.tif");
        final int width = 14;
        final int height = 14;
        outProduct = new Product("P", "T", width, height);
        final Band bandInt16 = outProduct.addBand("int16", ProductData.TYPE_INT16);
        bandInt16.setDataElems(createProductShorts(getProductSize(), 23));
        bandInt16.setSynthetic(true);
        ImageManager.getInstance().getBandImage(bandInt16, 0);
    }

    @Test
    public void testWriteReadBeamMetadata() throws IOException {
        final Band expectedBand = outProduct.getBand("int16");
        expectedBand.setDescription("Danger");
        expectedBand.setUnit("Voltage");
        expectedBand.setScalingFactor(0.7);
        expectedBand.setScalingOffset(100);
        expectedBand.setLog10Scaled(true);
        expectedBand.setNoDataValue(12.5);
        expectedBand.setNoDataValueUsed(true);

        final Product inProduct = writeReadProduct();

        assertEquals(outProduct.getName(), inProduct.getName());
        assertEquals(outProduct.getProductType(), inProduct.getProductType());
        assertEquals(outProduct.getNumBands(), inProduct.getNumBands());

        final Band actualBand = inProduct.getBandAt(0);
        assertEquals(expectedBand.getName(), actualBand.getName());
        assertEquals(expectedBand.getDescription(), actualBand.getDescription());
        assertEquals(expectedBand.getUnit(), actualBand.getUnit());
        assertEquals(expectedBand.getDataType(), actualBand.getDataType());
        assertEquals(expectedBand.getScalingFactor(), actualBand.getScalingFactor(), 1.0e-6);
        assertEquals(expectedBand.getScalingOffset(), actualBand.getScalingOffset(), 1.0e-6);
        assertEquals(expectedBand.isLog10Scaled(), actualBand.isLog10Scaled());
        assertEquals(expectedBand.getNoDataValue(), actualBand.getNoDataValue(), 1.0e-6);
        assertEquals(expectedBand.isNoDataValueUsed(), actualBand.isNoDataValueUsed());
    }

    @Test
    public void testWriteReadVirtualBandIsExcluded() throws IOException {
        final VirtualBand virtualBand = new VirtualBand("VB", ProductData.TYPE_FLOAT32,
                                                        outProduct.getSceneRasterWidth(),
                                                        outProduct.getSceneRasterHeight(), "X * Y");
        outProduct.addBand(virtualBand);
        final Product inProduct = writeReadProduct();

        assertEquals(outProduct.getNumBands()-1, inProduct.getNumBands());
        assertEquals(true, inProduct.getBand("VB") == null);
    }  

    @Test
    public void testWriteReadUTMProjectProduct() throws IOException {
        setUTMGeoCoding(outProduct);
        final Product inProduct = writeReadProduct();

        assertEquals(outProduct.getName(), inProduct.getName());
        assertEquals(outProduct.getProductType(), inProduct.getProductType());
        assertEquals(outProduct.getNumBands(), inProduct.getNumBands());
        assertEquals(outProduct.getBandAt(0).getName(), inProduct.getBandAt(0).getName());
        assertEquals(outProduct.getBandAt(0).getDataType(), inProduct.getBandAt(0).getDataType());
        assertEquals(outProduct.getBandAt(0).getScalingFactor(), inProduct.getBandAt(0).getScalingFactor(), 1.0e-6);
        assertEquals(outProduct.getBandAt(0).getScalingOffset(), inProduct.getBandAt(0).getScalingOffset(), 1.0e-6);
        assertEquals(location, inProduct.getFileLocation());
        assertNotNull(inProduct.getGeoCoding());
        assertEquality(outProduct.getGeoCoding(), inProduct.getGeoCoding());
    }

    @Test
    public void testWriteReadProductWithLatLonGeocoding() throws IOException {
        setIdentityTransformGeoCoding(outProduct);
        final Product inProduct = writeReadProduct();

        assertEquals(outProduct.getName(), inProduct.getName());
        assertEquals(outProduct.getProductType(), inProduct.getProductType());
        assertEquals(outProduct.getNumBands(), inProduct.getNumBands());
        assertEquals(outProduct.getBandAt(0).getName(), inProduct.getBandAt(0).getName());
        assertEquals(outProduct.getBandAt(0).getDataType(), inProduct.getBandAt(0).getDataType());
        assertEquals(outProduct.getBandAt(0).getScalingFactor(), inProduct.getBandAt(0).getScalingFactor(), 1.0e-6);
        assertEquals(outProduct.getBandAt(0).getScalingOffset(), inProduct.getBandAt(0).getScalingOffset(), 1.0e-6);
        assertEquals(location, inProduct.getFileLocation());
        assertNotNull(inProduct.getGeoCoding());
        assertEquality(outProduct.getGeoCoding(), inProduct.getGeoCoding());
    }

    @Test
    public void testWriteReadTiePointProduct() throws IOException {
        setTiePointGeoCoding(outProduct);
        final Band bandfloat32 = outProduct.addBand("float32", ProductData.TYPE_FLOAT32);
        bandfloat32.setDataElems(createFloats(getProductSize(), 2.343f));
        bandfloat32.setSynthetic(true);

        final Product inProduct = writeReadProduct();

        assertEquals(outProduct.getName(), inProduct.getName());
        assertEquals(outProduct.getProductType(), inProduct.getProductType());
        assertEquals(outProduct.getNumBands(), inProduct.getNumBands());
        for (int i = 0; i < outProduct.getNumBands(); i++) {
            assertEquality(outProduct.getBandAt(i), inProduct.getBandAt(i), ProductData.TYPE_FLOAT32);
        }
        assertEquals(location, inProduct.getFileLocation());
        assertNotNull(inProduct.getGeoCoding());
        assertEquality(outProduct.getGeoCoding(), inProduct.getGeoCoding());
    }

    @Test
    public void testWriteReadTransverseMercatorProduct() throws IOException {
        setTransverseMercatorGeoCoding(outProduct);

        final Product inProduct = writeReadProduct();

        assertEquals(outProduct.getName(), inProduct.getName());
        assertEquals(outProduct.getProductType(), inProduct.getProductType());
        assertEquals(outProduct.getNumBands(), inProduct.getNumBands());
        for (int i = 0; i < outProduct.getNumBands(); i++) {
            assertEquality(outProduct.getBandAt(i), inProduct.getBandAt(i), ProductData.TYPE_INT16);
        }
        assertEquals(location, inProduct.getFileLocation());
        assertNotNull(inProduct.getGeoCoding());
        assertEquality(outProduct.getGeoCoding(), inProduct.getGeoCoding());
    }

    @Test
    public void testWriteReadLambertConformalConicProduct() throws IOException {
        setLambertConformalConicGeoCoding(outProduct);

        final Product inProduct = writeReadProduct();

        assertEquals(outProduct.getName(), inProduct.getName());
        assertEquals(outProduct.getProductType(), inProduct.getProductType());
        assertEquals(outProduct.getNumBands(), inProduct.getNumBands());
        for (int i = 0; i < outProduct.getNumBands(); i++) {
            assertEquality(outProduct.getBandAt(i), inProduct.getBandAt(i), ProductData.TYPE_INT16);
        }
        assertEquals(location, inProduct.getFileLocation());
        assertNotNull(inProduct.getGeoCoding());
        assertEquality(outProduct.getGeoCoding(), inProduct.getGeoCoding());
    }

    @Test
    public void testWriteReadStereographicProduct() throws IOException {
        setStereographicGeoCoding(outProduct);

        final Product inProduct = writeReadProduct();

        assertEquals(outProduct.getName(), inProduct.getName());
        assertEquals(outProduct.getProductType(), inProduct.getProductType());
        assertEquals(outProduct.getNumBands(), inProduct.getNumBands());
        for (int i = 0; i < outProduct.getNumBands(); i++) {
            assertEquality(outProduct.getBandAt(i), inProduct.getBandAt(i), ProductData.TYPE_INT16);
        }
        assertEquals(location, inProduct.getFileLocation());
        assertNotNull(inProduct.getGeoCoding());
        assertEquality(outProduct.getGeoCoding(), inProduct.getGeoCoding());
    }

    @Test
    public void testWriteReadAlbersEqualAreaProduct() throws IOException {
        setAlbersEqualAreaGeoCoding(outProduct);

        final Product inProduct = writeReadProduct();

        assertEquals(outProduct.getName(), inProduct.getName());
        assertEquals(outProduct.getProductType(), inProduct.getProductType());
        assertEquals(outProduct.getNumBands(), inProduct.getNumBands());
        for (int i = 0; i < outProduct.getNumBands(); i++) {
            assertEquality(outProduct.getBandAt(i), inProduct.getBandAt(i), ProductData.TYPE_INT16);
        }
        assertEquals(location, inProduct.getFileLocation());
        assertNotNull(inProduct.getGeoCoding());
        assertEquality(outProduct.getGeoCoding(), inProduct.getGeoCoding());
    }

    private int getProductSize() {
        final int w = outProduct.getSceneRasterWidth();
        final int h = outProduct.getSceneRasterHeight();
        final int size = w * h;
        return size;
    }

    private static void assertEquality(Band band1, Band band2, int productDataType) throws IOException {
        assertEquals(band1.getName(), band2.getName());
        assertEquals(productDataType, band2.getDataType());
        assertEquals(band1.getScalingFactor(), band2.getScalingFactor(), 1.0e-6);
        assertEquals(band1.getScalingOffset(), band2.getScalingOffset(), 1.0e-6);
        final int width = band1.getRasterWidth();
        final int height = band1.getRasterHeight();
        band2.readRasterDataFully();
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                assertEquals(band1.getPixelDouble(i, j), band2.getPixelDouble(i, j), 1.0e-13);
            }
        }
    }

    private void assertEquality(final GeoCoding gc1, final GeoCoding gc2) {
        assertNotNull(gc2);
        assertEquals(gc1.canGetGeoPos(), gc2.canGetGeoPos());
        assertEquals(gc1.canGetPixelPos(), gc2.canGetPixelPos());
        assertEquals(gc1.isCrossingMeridianAt180(), gc2.isCrossingMeridianAt180());
        assertEquality(gc1.getDatum(), gc2.getDatum());

        if (gc1 instanceof MapGeoCoding) {
            assertEquals(MapGeoCoding.class, gc2.getClass());
            final MapGeoCoding mgc1 = (MapGeoCoding) gc1;
            final MapGeoCoding mgc2 = (MapGeoCoding) gc2;
            assertEquality(mgc1.getMapInfo(), mgc2.getMapInfo());
        } else if (gc1 instanceof TiePointGeoCoding) {
            assertEquals(TiePointGeoCoding.class, gc2.getClass());
        }

        final int width = outProduct.getSceneRasterWidth();
        final int height = outProduct.getSceneRasterHeight();
        GeoPos geoPos1 = null;
        GeoPos geoPos2 = null;
        final String msgPattern = "%s at [%d,%d] is not equal";
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                final PixelPos pixelPos = new PixelPos(i, j);
                geoPos1 = gc1.getGeoPos(pixelPos, geoPos1);
                geoPos2 = gc2.getGeoPos(pixelPos, geoPos2);
                assertEquals(String.format(msgPattern, "Latitude", i, j), geoPos1.lat, geoPos2.lat, 1.0e-5f);
                assertEquals(String.format(msgPattern, "Longitude", i, j), geoPos1.lon, geoPos2.lon, 1.0e-5f);
            }
        }
    }

    private static void assertEquality(MapInfo mi1, MapInfo mi2) {
        assertNotNull(mi2);
        assertEquals(mi1.toString(), mi2.toString());
        assertEquality(mi1.getMapProjection(), mi2.getMapProjection());
    }

    private static void assertEquality(MapProjection mp1, MapProjection mp2) {
        assertEquals(mp1.getName(), mp2.getName());
        assertEquals(mp1.getMapUnit(), mp2.getMapUnit());
        assertEquality(mp1.getMapTransform(), mp2.getMapTransform());
    }

    private static void assertEquality(MapTransform mt1, MapTransform mt2) {
        assertEquals(mt1.getClass(), mt2.getClass());
        assertTrue(Arrays.equals(mt1.getParameterValues(), mt2.getParameterValues()));
    }

    private static void assertEquality(Datum datum1, Datum datum2) {
        assertNotNull(datum2);
        assertEquals(datum1.getName(), datum2.getName());
        assertTrue(datum1.getDX() == datum2.getDX());
        assertTrue(datum1.getDY() == datum2.getDY());
        assertTrue(datum1.getDZ() == datum2.getDZ());
        assertEquality(datum1.getEllipsoid(), datum2.getEllipsoid());
    }

    private static void assertEquality(Ellipsoid e1, Ellipsoid e2) {
        assertNotNull(e2);
        assertEquals(e1.getName(), e2.getName());
        assertEquals(e1.getName(), e2.getName());
        assertTrue(e1.getSemiMajor() == e2.getSemiMajor());
        assertTrue(e1.getSemiMinor() == e2.getSemiMinor());
    }

    private static short[] createProductShorts(final int size, final int offset) {
        final short[] shorts = new short[size];
        for (int i = 0; i < shorts.length; i++) {
            shorts[i] = (short) (i + offset);
        }
        return shorts;
    }

    private static float[] createFloats(final int size, final float offset) {
        final float[] floats = new float[size];
        for (int i = 0; i < floats.length; i++) {
            floats[i] = i * 6.3243f + offset;
        }
        return floats;
    }

    private static void setUTMGeoCoding(final Product product) {
        final MapInfo mapInfo = new MapInfo(UTMProjection.create(28, true),
                                            0.5f, 0.5f,
                                            0.0f, 0.0f,
                                            1.0f, 1.0f,
                                            Datum.WGS_84);
        mapInfo.setSceneWidth(product.getSceneRasterWidth());
        mapInfo.setSceneHeight(product.getSceneRasterHeight());
        product.setGeoCoding(new MapGeoCoding(mapInfo));
    }

    private static void setTransverseMercatorGeoCoding(final Product product) {
        final MapTransformDescriptor descriptor = MapProjectionRegistry.getDescriptor(
                    TransverseMercatorDescriptor.TYPE_ID);
        final double[] values = descriptor.getParameterDefaultValues();
        for (int i = 0; i < values.length; i++) {
            values[i] = values[i] + 0.001;
        }
        final MapTransform transform = descriptor.createTransform(values);
        final MapProjection mapProjection = new MapProjection(descriptor.getTypeID(), transform);
        final MapInfo mapInfo = new MapInfo(mapProjection, .5f, .6f, .7f, .8f, .09f, .08f, Datum.WGS_72);
        mapInfo.setSceneWidth(product.getSceneRasterWidth());
        mapInfo.setSceneHeight(product.getSceneRasterHeight());
        product.setGeoCoding(new MapGeoCoding(mapInfo));
    }

    private static void setStereographicGeoCoding(final Product product) {
        final MapTransformDescriptor descriptor = MapProjectionRegistry.getDescriptor(
                    StereographicDescriptor.TYPE_ID);
        final double[] values = descriptor.getParameterDefaultValues();
        for (int i = 0; i < values.length; i++) {
            values[i] = values[i] + 0.001;
        }
        final MapTransform transform = descriptor.createTransform(values);
        final MapProjection mapProjection = new MapProjection(descriptor.getTypeID(), transform);
        final MapInfo mapInfo = new MapInfo(mapProjection, .5f, .6f, .7f, .8f, .09f, .08f, Datum.WGS_84);
        mapInfo.setSceneWidth(product.getSceneRasterWidth());
        mapInfo.setSceneHeight(product.getSceneRasterHeight());
        product.setGeoCoding(new MapGeoCoding(mapInfo));
    }

    private static void setLambertConformalConicGeoCoding(final Product product) {
        final MapTransformDescriptor descriptor = MapProjectionRegistry.getDescriptor(
                    LambertConformalConicDescriptor.TYPE_ID);
        final double[] values = descriptor.getParameterDefaultValues();
        for (int i = 0; i < values.length; i++) {
            values[i] = values[i] + 0.001;
        }
        final MapTransform transform = descriptor.createTransform(values);
        final MapProjection mapProjection = new MapProjection(descriptor.getTypeID(), transform);
        final MapInfo mapInfo = new MapInfo(mapProjection, .5f, .6f, .7f, .8f, .09f, .08f, Datum.WGS_84);
        mapInfo.setSceneWidth(product.getSceneRasterWidth());
        mapInfo.setSceneHeight(product.getSceneRasterHeight());
        product.setGeoCoding(new MapGeoCoding(mapInfo));
    }

    private static void setAlbersEqualAreaGeoCoding(final Product product) {
        final MapTransformDescriptor descriptor = MapProjectionRegistry.getDescriptor(
                    AlbersEqualAreaConicDescriptor.TYPE_ID);
        final double[] values = descriptor.getParameterDefaultValues();
        for (int i = 0; i < values.length; i++) {
            values[i] = values[i] + 0.001;
        }
        final MapTransform transform = descriptor.createTransform(values);
        final MapProjection mapProjection = new MapProjection(descriptor.getTypeID(), transform);
        final MapInfo mapInfo = new MapInfo(mapProjection, .5f, .6f, .7f, .8f, .09f, .08f, Datum.WGS_84);
        mapInfo.setSceneWidth(product.getSceneRasterWidth());
        mapInfo.setSceneHeight(product.getSceneRasterHeight());
        product.setGeoCoding(new MapGeoCoding(mapInfo));
    }

    private static void setIdentityTransformGeoCoding(final Product product) {
        final MapProjection projection = MapProjectionRegistry.getProjection(IdentityTransformDescriptor.NAME);

        final MapInfo mapInfo = new MapInfo(projection,
                                            0.5f, 0.5f,
                                            12.3f, 14.6f,
                                            1.43f, 0.023f,
                                            Datum.WGS_84);
        mapInfo.setSceneWidth(product.getSceneRasterWidth());
        mapInfo.setSceneHeight(product.getSceneRasterHeight());
        product.setGeoCoding(new MapGeoCoding(mapInfo));
    }

    private static void setTiePointGeoCoding(final Product product) {
        final TiePointGrid latGrid = new TiePointGrid("lat", 3, 3, 0, 0, 5, 5, new float[]{
                    85, 84, 83,
                    75, 74, 73,
                    65, 64, 63
        });
        final TiePointGrid lonGrid = new TiePointGrid("lon", 3, 3, 0, 0, 5, 5, new float[]{
                    -15, -5, 5,
                    -16, -6, 4,
                    -17, -7, 3
        });
        product.addTiePointGrid(latGrid);
        product.addTiePointGrid(lonGrid);
        product.setGeoCoding(new TiePointGeoCoding(latGrid, lonGrid, Datum.WGS_84));
    }

    private Product writeReadProduct() throws IOException {
        final GeoTIFFProductWriter writer = (GeoTIFFProductWriter) new GeoTIFFProductWriterPlugIn().createWriterInstance();
        writer.writeGeoTIFFProduct(outputStream, outProduct);
        ByteArraySeekableStream inputStream = new ByteArraySeekableStream(outputStream.toByteArray());
        return reader.readGeoTIFFProduct(new MemoryCacheImageInputStream(inputStream), location);
    }

}
