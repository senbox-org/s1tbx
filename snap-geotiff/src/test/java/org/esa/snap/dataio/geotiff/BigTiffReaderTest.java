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
import org.junit.Test;

import javax.imageio.stream.FileCacheImageInputStream;
import java.io.File;
import java.io.IOException;
import java.net.URL;

import static org.junit.Assert.*;


/**
 * BigTiff reading
 *
 * @author Serge Stankovic
 */

public class BigTiffReaderTest {

    @Test
    public void testReadImageFile() throws IOException {
        final URL resource = getClass().getResource("tiger-minisblack-strip-16.tif");
        final String filePath = resource.getFile();
        final GeoTiffProductReader reader = new GeoTiffProductReader(new GeoTiffProductReaderPlugIn());
        final Product product = reader.readGeoTIFFProduct(new FileCacheImageInputStream(resource.openStream(), null), new File(filePath));
        assertNotNull(product);

        final Band band = product.getBandAt(0);
        assertNotNull(band);

        final int[] pixels = new int[band.getRasterWidth() * band.getRasterHeight()];
        band.readPixels(0, 0, band.getRasterWidth(), band.getRasterHeight(), pixels, ProgressMonitor.NULL);

        assertEquals(52428, pixels[20]);
        assertEquals(18295, pixels[40]);
        assertEquals(52418, pixels[60]);
    }
}
