/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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

import org.esa.snap.core.dataio.EncodeQualification;
import org.esa.snap.core.datamodel.CrsGeoCoding;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.TiePointGeoCoding;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.core.util.io.SnapFileFilter;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static org.junit.Assert.*;

/**
 * todo - add API doc
 *
 * @author Marco Peters
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
public class GeoTiffProductWriterPlugInTest {

    private GeoTiffProductWriterPlugIn plugIn;

    @Before
    public void setup() {
        plugIn = new GeoTiffProductWriterPlugIn();
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
        final Class[] classes = plugIn.getOutputTypes();

        assertNotNull(classes);
        assertEquals(2, classes.length);
        final List<Class> list = Arrays.asList(classes);
        assertEquals(true, list.contains(File.class));
        assertEquals(true, list.contains(String.class));
    }

    @Test
    public void testProductFileFilter() {
        final SnapFileFilter snapFileFilter = plugIn.getProductFileFilter();

        assertNotNull(snapFileFilter);
        assertArrayEquals(plugIn.getDefaultFileExtensions(), snapFileFilter.getExtensions());
        assertEquals(plugIn.getFormatNames()[0], snapFileFilter.getFormatName());
        assertEquals(true, snapFileFilter.getDescription().contains(plugIn.getDescription(Locale.getDefault())));
    }

    @Test
    public void testEncodingQualification() throws Exception {
        Product product = new Product("N", "T", 2, 2);

        EncodeQualification encodeQualification = plugIn.getEncodeQualification(product);
        assertNotNull(encodeQualification);
        assertEquals(EncodeQualification.Preservation.PARTIAL, encodeQualification.getPreservation());
        assertNotNull(encodeQualification.getInfoString());

        TiePointGrid lat = new TiePointGrid("lat", 2, 2, 0, 0, 1, 1, new float[4]);
        TiePointGrid lon = new TiePointGrid("lon", 2, 2, 0, 0, 1, 1, new float[4]);
        product.addTiePointGrid(lat);
        product.addTiePointGrid(lon);
        product.setSceneGeoCoding(new TiePointGeoCoding(lat, lon));
        encodeQualification = plugIn.getEncodeQualification(product);
        assertEquals(EncodeQualification.Preservation.PARTIAL, encodeQualification.getPreservation());
        assertNotNull(encodeQualification.getInfoString());

        product.setSceneGeoCoding(new CrsGeoCoding(DefaultGeographicCRS.WGS84, 2, 2, 0, 0, 1, 1));
        encodeQualification = plugIn.getEncodeQualification(product);
        assertEquals(EncodeQualification.Preservation.FULL, encodeQualification.getPreservation());
    }
}
