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

package org.esa.snap.binning.operator;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import org.esa.snap.binning.BinManager;
import org.esa.snap.binning.CompositingType;
import org.esa.snap.binning.TemporalBin;
import org.esa.snap.binning.aggregators.AggregatorMinMax;
import org.esa.snap.binning.support.BinningContextImpl;
import org.esa.snap.binning.support.SEAGrid;
import org.esa.snap.binning.support.VariableContextImpl;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.dataio.netcdf.util.NetcdfFileOpener;
import org.geotools.geometry.jts.JTS;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import ucar.ma2.Array;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;

import java.awt.geom.Rectangle2D;
import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.logging.Logger;

import static org.junit.Assert.*;

/**
 * @author Marco Peters
 */
public class SeaDASLevel3BinWriterTest {

    private int numRows;
    private BinWriter binWriter;
    private File tempFile;

    @Before
    public void setUp() throws Exception {
        numRows = 216;
        tempFile = File.createTempFile("SeaDASLevel3BinWriterTest-temp", ".nc");
        binWriter = createBinWriter(tempFile, numRows);
    }

    @After
    public void tearDown() throws Exception {
        if (tempFile != null) {
            if (!tempFile.delete()) {
                tempFile.deleteOnExit();
            }
        }
        if (binWriter != null) {
            File binTempFile = new File(binWriter.getTargetFilePath());
            if (!binTempFile.delete()) {
                binTempFile.deleteOnExit();
            }
        }
    }

    @Test
    public void testWriting() throws Exception {
        final HashMap<String, String> metadataProperties = createMetadataProperties();
        final ArrayList<TemporalBin> temporalBins = createTemporalBins();

        binWriter.write(metadataProperties, temporalBins);

        final NetcdfFile netcdfFile = NetcdfFileOpener.open(binWriter.getTargetFilePath());
        assertNotNull(netcdfFile.findGlobalAttribute("title"));
        assertNotNull(netcdfFile.findGlobalAttribute("super_sampling"));
        assertEquals(numRows * 2, netcdfFile.findGlobalAttribute("SEAGrid_bins").getNumericValue());

        assertEquals(numRows, netcdfFile.findDimension("bin_index").getLength());
        assertEquals(temporalBins.size(), netcdfFile.findDimension("bin_list").getLength());

        assertEquals(numRows, netcdfFile.findVariable("bi_row_num").getDimension(0).getLength());
        Array bi_row_num = netcdfFile.findVariable("bi_row_num").read();
        assertEquals(0, bi_row_num.getInt(0));
        assertEquals(200, bi_row_num.getInt(200));

        assertEquals(numRows, netcdfFile.findVariable("bi_vsize").getDimension(0).getLength());
        Array bi_vsize = netcdfFile.findVariable("bi_vsize").read();
        assertEquals(180.0 / numRows, bi_vsize.getDouble(numRows / 2), 1.0e-6); // equator
        assertEquals(180.0 / numRows, bi_vsize.getDouble(0), 1.0e-6); // north-pole
        assertEquals(180.0 / numRows, bi_vsize.getDouble(215), 1.0e-6); // south-pole

        assertEquals(numRows, netcdfFile.findVariable("bi_hsize").getDimension(0).getLength());
        Array bi_hsize = netcdfFile.findVariable("bi_hsize").read();
        assertEquals(360.0 / (numRows * 2.0), bi_hsize.getDouble(numRows / 2), 1.0e-6); // equator
        assertEquals(120.0, bi_hsize.getDouble(215), 1.0e-6); // south-pole

        assertEquals(numRows, netcdfFile.findVariable("bi_start_num").getDimension(0).getLength());
        Array bi_start_num = netcdfFile.findVariable("bi_start_num").read();
        assertEquals(1, bi_start_num.getInt(215));
        assertEquals(4, bi_start_num.getInt(214));
        assertEquals(12315, bi_start_num.getInt(150));
        assertEquals(59406, bi_start_num.getInt(0));

        // here start the data dependent variables
        assertNotNull(netcdfFile.findVariable("bi_begin_offset"));
        assertEquals(numRows, netcdfFile.findVariable("bi_begin_offset").getDimension(0).getLength());
        Array bi_begin_offset = netcdfFile.findVariable("bi_begin_offset").read();
        assertEquals(0, bi_begin_offset.getInt(150));
        assertEquals(-1, bi_begin_offset.getInt(214));
        assertEquals(-1, bi_begin_offset.getInt(0));

        assertEquals(numRows, netcdfFile.findVariable("bi_begin").getDimension(0).getLength());
        Array bi_begin = netcdfFile.findVariable("bi_begin").read();
        assertEquals(0, bi_begin.getInt(10));
        assertEquals(46774, bi_begin.getInt(150));
        assertEquals(0, bi_begin.getInt(215));

        assertEquals(numRows, netcdfFile.findVariable("bi_extent").getDimension(0).getLength());
        Array bi_extent = netcdfFile.findVariable("bi_extent").read();
        assertEquals(0, bi_extent.getInt(10));
        assertEquals(2, bi_extent.getInt(150));
        assertEquals(0, bi_extent.getInt(215));

        assertEquals(numRows, netcdfFile.findVariable("bi_max").getDimension(0).getLength());
        Array bi_max = netcdfFile.findVariable("bi_max").read();
        assertEquals(3, bi_max.getInt(0));
        assertEquals(numRows * 2, bi_max.getInt(numRows / 2));
        assertEquals(3, bi_max.getInt(215));

        assertEquals(temporalBins.size(), netcdfFile.findVariable("bl_bin_num").getDimension(0).getLength());
        Array bl_bin_num = netcdfFile.findVariable("bl_bin_num").read();
        assertEquals(46774, bl_bin_num.getInt(0));
        assertEquals(46775, bl_bin_num.getInt(1));

        assertEquals(temporalBins.size(), netcdfFile.findVariable("bl_nobs").getDimension(0).getLength());
        Array bl_nobs = netcdfFile.findVariable("bl_nobs").read();
        assertEquals(1, bl_nobs.getInt(0));
        assertEquals(2, bl_nobs.getInt(1));

        assertEquals(temporalBins.size(), netcdfFile.findVariable("bl_nscenes").getDimension(0).getLength());
        Array bl_nscenes = netcdfFile.findVariable("bl_nscenes").read();
        assertEquals(1, bl_nscenes.getInt(0));
        assertEquals(2, bl_nscenes.getInt(1));

        assertEquals(temporalBins.size(), netcdfFile.findVariable("bl_test_min").getDimension(0).getLength());
        Array bl_test_min = netcdfFile.findVariable("bl_test_min").read();
        assertEquals(0.004f, bl_test_min.getFloat(0), 1.0e-6);
        assertEquals(0.398f, bl_test_min.getFloat(1), 1.0e-6);

        assertEquals(temporalBins.size(), netcdfFile.findVariable("bl_test_max").getDimension(0).getLength());
        Array bl_test_max = netcdfFile.findVariable("bl_test_max").read();
        assertEquals(0.14f, bl_test_max.getFloat(0), 1.0e-6);
        assertEquals(0.89f, bl_test_max.getFloat(1), 1.0e-6);
    }

    @Test
    public void testWriting_startAndStopTimeMetadata() throws Exception {
        final HashMap<String, String> metadataProperties = createMetadataProperties();
        final ArrayList<TemporalBin> temporalBins = createTemporalBins();

        final Date startTime = new Date(500000000000L);
        final Date stopTime = new Date(510000000000L);
        final BinWriter binWriterWithUtc = createBinWriter(tempFile, numRows,
                ProductData.UTC.create(startTime, 0),
                ProductData.UTC.create(stopTime, 0));
        binWriterWithUtc.write(metadataProperties, temporalBins);

        final NetcdfFile netcdfFile = NetcdfFileOpener.open(binWriterWithUtc.getTargetFilePath());
        final Attribute startTimeAttribute = netcdfFile.findGlobalAttribute("start_time");
        assertNotNull(startTimeAttribute);
        assertEquals("1985-11-05T00:53:20.000", startTimeAttribute.getStringValue());

        final Attribute stopTimeAttribute = netcdfFile.findGlobalAttribute("stop_time");
        assertNotNull(stopTimeAttribute);
        assertEquals("1986-02-28T18:40:00.000", stopTimeAttribute.getStringValue());

        final Attribute startCoverageAttribute = netcdfFile.findGlobalAttribute("time_coverage_start");
        assertNotNull(startCoverageAttribute);
        assertEquals("1985-11-05T00:53:20.000", startCoverageAttribute.getStringValue());

        final Attribute endCoverageAttribute = netcdfFile.findGlobalAttribute("time_coverage_end");
        assertNotNull(endCoverageAttribute);
        assertEquals("1986-02-28T18:40:00.000", endCoverageAttribute.getStringValue());
    }

    @Test
    public void testToDateString() {
        final Date dateTime = new Date(520000000000L);
        final ProductData.UTC utc = ProductData.UTC.create(dateTime, 0);

        final String utcString = SeaDASLevel3BinWriter.toDateString(utc);
        assertEquals("1986-06-24T12:26:40.000", utcString);
    }

    @Test
    public void testToDateString_nullInput() {
        final String utcString = SeaDASLevel3BinWriter.toDateString(null);
        assertEquals("", utcString);
    }

    private ArrayList<TemporalBin> createTemporalBins() {
        ArrayList<TemporalBin> temporalBins = new ArrayList<TemporalBin>();
        temporalBins.add(new TemporalBin(12345, 2));
        temporalBins.add(new TemporalBin(12346, 2));
        float[][] data = new float[][]{{0.004f, 0.14f}, {0.398f, 0.89f}};
        for (int i = 0; i < temporalBins.size(); i++) {
            TemporalBin temporalBin = temporalBins.get(i);
            temporalBin.getFeatureValues()[0] = data[i][0];
            temporalBin.getFeatureValues()[1] = data[i][1];
            temporalBin.setNumObs(i + 1);
            temporalBin.setNumPasses(i + 1);
        }
        return temporalBins;
    }

    private HashMap<String, String> createMetadataProperties() {
        HashMap<String, String> metadataProperties = new HashMap<String, String>();
        metadataProperties.put("test", "Spongebob");
        return metadataProperties;
    }

    private BinWriter createBinWriter(File tempFile, int numRows) {
        final ProductData.UTC startTime = null;
        final ProductData.UTC stopTime = null;

        return createBinWriter(tempFile, numRows, startTime, stopTime);
    }

    private BinWriter createBinWriter(File tempFile, int numRows, ProductData.UTC startTime, ProductData.UTC stopTime) {
        final SEAGrid seaGrid = new SEAGrid(numRows);
        final VariableContextImpl variableContext = new VariableContextImpl();
        variableContext.defineVariable("test", "blah");

        final BinManager binManager = new BinManager(variableContext, new AggregatorMinMax(variableContext, "test", "test"));
        final BinningContextImpl binningContext = new BinningContextImpl(seaGrid, binManager, CompositingType.BINNING, 1, -1, null, null);
        final Geometry region = JTS.shapeToGeometry(new Rectangle2D.Double(-180, -90, 360, 180), new GeometryFactory());

        final SeaDASLevel3BinWriter binWriter = new SeaDASLevel3BinWriter(region, startTime, stopTime);
        binWriter.setBinningContext(binningContext);
        binWriter.setTargetFileTemplatePath(tempFile.getAbsolutePath());
        binWriter.setLogger(Logger.getLogger("SeaDASLevel3BinWriterTest"));
        return binWriter;
    }
}
