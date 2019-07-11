/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.util.Debug;
import org.esa.snap.core.util.StopWatch;

import javax.imageio.stream.FileImageInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;


/**
 * Reading performance test
 *
 * @author Marco Peters
 */

public class TiffReadingPerformanceTest {

    public static void main(String[] args) throws IOException {

        final URL resource = TiffReadingPerformanceTest.class.getResource("20180623_185222_ssc4d2_0024_visual.tif");
        final String filePath = resource.getFile();
        File file = new File(filePath);
        final GeoTiffProductReader reader = new GeoTiffProductReader(new GeoTiffProductReaderPlugIn());
        Debug.setEnabled(true);
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        final Product product = reader.readGeoTIFFProduct(new FileImageInputStream(file), file);
        stopWatch.stopAndTrace("Opening product");
        assertNotNull(product);

        final Band band = product.getBandAt(0);
        assertNotNull(band);

        stopWatch.start();
        band.readRasterDataFully(ProgressMonitor.NULL);
        stopWatch.stopAndTrace("reading band_1");
        assertTrue(band.hasRasterData());

        ProductData rasterData = band.getRasterData();
        int width = band.getRasterWidth();
        assertEquals(113, rasterData.getElemIntAt(4 * width + 2995));
        assertEquals(98, rasterData.getElemIntAt(195 * width + 400));
        assertEquals(133, rasterData.getElemIntAt(1080 * width + 2647));
    }
}
