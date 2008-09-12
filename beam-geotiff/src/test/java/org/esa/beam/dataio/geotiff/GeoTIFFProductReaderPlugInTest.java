package org.esa.beam.dataio.geotiff;

import org.esa.beam.util.io.BeamFileFilter;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.MapGeoCoding;
import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.dataop.maptransf.MapInfo;
import org.esa.beam.framework.dataop.maptransf.UTM;
import org.esa.beam.framework.dataop.maptransf.Datum;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import com.sun.media.jai.codec.ByteArraySeekableStream;

/**
 * todo - add API doc
 *
 * @author Marco Peters
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
public class GeoTIFFProductReaderPlugInTest {

    private GeoTIFFProductReaderPlugIn plugIn;

    @Before
    public void setup() {
        plugIn = new GeoTIFFProductReaderPlugIn();
    }

    @Test
    public void testDecodeQualificationForTIFFWithoutGeoInformation() throws IOException {
        final Product product = new Product("p", "t", 20, 10);
        final Band band = product.addBand("band1", ProductData.TYPE_INT8);
        band.ensureRasterData();
        band.setSynthetic(true);
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        GeoTIFFProductWriter.writeGeoTIFFProduct(outputStream, product);
        final DecodeQualification decodeQualification = plugIn.getDecodeQualification(
                new ByteArraySeekableStream(outputStream.toByteArray()));

        assertEquals(DecodeQualification.SUITABLE, decodeQualification);
    }

    @Test
    public void testDecodeQualificationForTIFFWithGeoInformation() throws IOException {
        final Product product = new Product("p", "t", 20, 10);
        final Band band = product.addBand("band1", ProductData.TYPE_INT8);
        band.ensureRasterData();
        band.setSynthetic(true);
        final MapInfo mapInfo = new MapInfo(UTM.createProjection(26, true), 0, 0, 0, 0, 0, 0, Datum.WGS_84);
        mapInfo.setSceneWidth(product.getSceneRasterWidth());
        mapInfo.setSceneHeight(product.getSceneRasterHeight());
        product.setGeoCoding(new MapGeoCoding(mapInfo));
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        GeoTIFFProductWriter.writeGeoTIFFProduct(outputStream, product);
        final DecodeQualification decodeQualification = plugIn.getDecodeQualification(
                new ByteArraySeekableStream(outputStream.toByteArray()));

        assertEquals(DecodeQualification.INTENDED, decodeQualification);
    }

    @Test
    public void testFileExtensions() {
        final String[] fileExtensions = plugIn.getDefaultFileExtensions();

        assertNotNull(fileExtensions);
        final List<String> extensionList = Arrays.asList(fileExtensions);
        assertEquals(2, extensionList.size());
        assertEquals(true, extensionList.contains(".tif"));
        assertEquals(true, extensionList.contains(".tiff"));
    }

    @Test
    public void testFormatNames() {
        final String[] formatNames = plugIn.getFormatNames();

        assertNotNull(formatNames);
        assertEquals(1, formatNames.length);
        assertEquals("GeoTIFF", formatNames[0]);
    }

    @Test
    public void testOutputTypes() {
        final Class[] classes = plugIn.getInputTypes();

        assertNotNull(classes);
        assertEquals(2, classes.length);
        final List<Class> list = Arrays.asList(classes);
        assertEquals(true, list.contains(File.class));
        assertEquals(true, list.contains(String.class));
    }

    @Test
    public void testProductFileFilter() {
        final BeamFileFilter beamFileFilter = plugIn.getProductFileFilter();

        assertNotNull(beamFileFilter);
        assertArrayEquals(plugIn.getDefaultFileExtensions(), beamFileFilter.getExtensions());
        assertEquals(plugIn.getFormatNames()[0], beamFileFilter.getFormatName());
        assertEquals(true, beamFileFilter.getDescription().contains(plugIn.getDescription(Locale.getDefault())));
    }
}