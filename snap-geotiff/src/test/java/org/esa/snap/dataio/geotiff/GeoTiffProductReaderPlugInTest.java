/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.snap.dataio.geotiff;

import com.bc.ceres.core.ProgressMonitor;
import com.sun.media.jai.codec.ByteArraySeekableStream;
import org.esa.snap.core.dataio.DecodeQualification;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.MapGeoCoding;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.dataop.maptransf.Datum;
import org.esa.snap.core.dataop.maptransf.MapInfo;
import org.esa.snap.core.dataop.maptransf.UTM;
import org.esa.snap.core.util.io.SnapFileFilter;
import org.junit.Before;
import org.junit.Test;

import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import javax.imageio.stream.MemoryCacheImageOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static org.junit.Assert.*;

public class GeoTiffProductReaderPlugInTest {

    private GeoTiffProductReaderPlugIn plugIn;

    @Before
    public void setup() {
        plugIn = new GeoTiffProductReaderPlugIn();
    }

    @Test
    public void testDecodeQualificationForTIFFWithoutGeoInformation() throws IOException {
        final Product product = new Product("p", "t", 20, 10);
        final Band band = product.addBand("band1", ProductData.TYPE_INT8);
        band.ensureRasterData();
        final ImageInputStream inputStream = writeToInputStream(product);
        final DecodeQualification decodeQualification = GeoTiffProductReaderPlugIn.getDecodeQualificationImpl(
                inputStream);

        assertEquals(DecodeQualification.SUITABLE, decodeQualification);
    }

    @Test
    public void testDecodeQualificationForTIFFWithGeoInformation() throws IOException {
        final Product product = new Product("p", "t", 20, 10);
        final Band band = product.addBand("band1", ProductData.TYPE_INT8);
        band.ensureRasterData();
        final MapInfo mapInfo = new MapInfo(UTM.createProjection(26, true), 0, 0, 0, 0, 1, 1, Datum.WGS_84);
        mapInfo.setSceneWidth(product.getSceneRasterWidth());
        mapInfo.setSceneHeight(product.getSceneRasterHeight());
        product.setSceneGeoCoding(new MapGeoCoding(mapInfo));
        final ImageInputStream inputStream = writeToInputStream(product);
        final DecodeQualification decodeQualification = GeoTiffProductReaderPlugIn.getDecodeQualificationImpl(inputStream);
        assertEquals(DecodeQualification.SUITABLE, decodeQualification);
    }

    @Test
    public void testDecodeQualification() throws URISyntaxException {
        assertEquals(DecodeQualification.UNABLE, plugIn.getDecodeQualification("file.zip"));
        assertEquals(DecodeQualification.UNABLE, plugIn.getDecodeQualification("file"));
        assertEquals(DecodeQualification.INTENDED, plugIn.getDecodeQualification("file.tif"));
        assertEquals(DecodeQualification.INTENDED, plugIn.getDecodeQualification("file.tiff"));
        assertEquals(DecodeQualification.INTENDED, plugIn.getDecodeQualification("file.btf"));

        final File zippedgeoTiff = new File(getClass().getResource("nearGreenwichMeridian.zip").toURI());
        if(zippedgeoTiff.exists()) {
            assertEquals(DecodeQualification.INTENDED, plugIn.getDecodeQualification(zippedgeoTiff));
        }

        final File tifInDirInZip = new File(getClass().getResource("tifInDirInZip.zip").toURI());
        if(tifInDirInZip.exists()) {
            assertEquals(DecodeQualification.UNABLE, plugIn.getDecodeQualification(tifInDirInZip));
        }

    }

    @Test
    public void testFileExtensions() {
        final String[] fileExtensions = plugIn.getDefaultFileExtensions();

        assertNotNull(fileExtensions);
        final List<String> extensionList = Arrays.asList(fileExtensions);
        assertEquals(4, extensionList.size());
        assertEquals(true, extensionList.contains(".tif"));
        assertEquals(true, extensionList.contains(".tiff"));
        assertEquals(true, extensionList.contains(".zip"));
    }

    @Test
    public void testFormatNames() {
        final String[] formatNames = plugIn.getFormatNames();

        assertNotNull(formatNames);
        assertEquals(1, formatNames.length);
        assertEquals("GeoTIFF", formatNames[0]);
    }

    @Test
    public void testInputTypes() {
        final Class[] classes = plugIn.getInputTypes();

        assertNotNull(classes);
        assertEquals(3, classes.length);
        final List<Class> list = Arrays.asList(classes);
        assertEquals(true, list.contains(File.class));
        assertEquals(true, list.contains(String.class));
        assertEquals(true, list.contains(InputStream.class));
    }

    @Test
    public void testProductFileFilter() {
        final SnapFileFilter snapFileFilter = plugIn.getProductFileFilter();

        assertNotNull(snapFileFilter);
        assertArrayEquals(plugIn.getDefaultFileExtensions(), snapFileFilter.getExtensions());
        assertEquals(plugIn.getFormatNames()[0], snapFileFilter.getFormatName());
        assertEquals(true, snapFileFilter.getDescription().contains(plugIn.getDescription(Locale.getDefault())));
    }

    private static ImageInputStream writeToInputStream(Product product) throws IOException {
        final ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
        final ImageOutputStream outputStream = new MemoryCacheImageOutputStream(byteStream);
        final GeoTiffProductWriter writer = (GeoTiffProductWriter) new GeoTiffProductWriterPlugIn().createWriterInstance();
        product.setProductWriter(writer);
        writer.writeGeoTIFFProduct(outputStream, product);
        final Band[] bands = product.getBands();
        for (Band band : bands) {
            band.writeRasterDataFully(ProgressMonitor.NULL);
        }
        outputStream.flush();
        return new MemoryCacheImageInputStream(new ByteArraySeekableStream(byteStream.toByteArray()));
    }

}
