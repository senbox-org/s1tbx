package org.esa.beam.binning.operator;

import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import org.esa.beam.binning.BinManager;
import org.esa.beam.binning.CompositingType;
import org.esa.beam.binning.TemporalBin;
import org.esa.beam.binning.aggregators.AggregatorMinMax;
import org.esa.beam.binning.support.BinningContextImpl;
import org.esa.beam.binning.support.SEAGrid;
import org.esa.beam.binning.support.VariableContextImpl;
import org.geotools.geometry.jts.JTS;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import ucar.ma2.Array;
import ucar.nc2.NetcdfFile;

import java.awt.geom.Rectangle2D;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.logging.Logger;

import static org.junit.Assert.*;

/**
 * @author Marco Peters
 */
public class BinWriterTest {

    private File tempFile;

    @Before
    public void setUp() throws Exception {
        tempFile = File.createTempFile("BinWriterTest-temp", ".nc");
    }

    @After
    public void tearDown() throws Exception {
        if (!tempFile.delete()) {
            tempFile.deleteOnExit();
        }
    }

    @Test
    public void testWriting() throws Exception {
        int numRows = 216;
        BinWriter binWriter = createBinWriter(numRows);
        HashMap<String, String> metadataProperties = new HashMap<String, String>();
        metadataProperties.put("test", "Spongebob");
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

        binWriter.write(tempFile, metadataProperties, temporalBins);

        NetcdfFile netcdfFile = NetcdfFile.open(tempFile.getPath());
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

    private BinWriter createBinWriter(int numRows) {
        SEAGrid seaGrid = new SEAGrid(numRows);
        VariableContextImpl variableContext = new VariableContextImpl();
        variableContext.defineVariable("test", "blah");
        BinManager binManager = new BinManager(variableContext, new AggregatorMinMax(variableContext, "test", -1));
        BinningContextImpl binningContext = new BinningContextImpl(seaGrid, binManager, CompositingType.BINNING, 1);
        Geometry region = JTS.shapeToGeometry(new Rectangle2D.Double(-180, -90, 360, 180), new GeometryFactory());
        return new BinWriter(binningContext, Logger.getLogger("BinWriterTest"), region, null, null);
    }

}
