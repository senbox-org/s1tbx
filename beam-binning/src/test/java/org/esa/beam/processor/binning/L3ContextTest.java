package org.esa.beam.processor.binning;

import java.awt.geom.Rectangle2D;
import java.io.File;

import junit.framework.TestCase;

import org.esa.beam.framework.processor.ProcessorException;
import org.esa.beam.processor.binning.algorithm.AlgorithmFactory;

/**
 * Created by IntelliJ IDEA.
 * User: marcoz
 * Date: 12.07.2005
 * Time: 10:31:30
 * To change this template use File | Settings | File Templates.
 */
public class L3ContextTest extends TestCase {
    private File databaseDir;
    private L3Context context;

    @Override
    public void setUp() {
        databaseDir = new File("testDir");
        context = new L3Context();
        context.setAlgorithmCreator(new AlgorithmFactory());
    }

    public void testInit() throws ProcessorException {
        context.setMainParameter(databaseDir, L3Constants.RESAMPLING_TYPE_VALUE_BINNING, 10f);

        assertEquals("databaseDir", databaseDir, context.getDatabaseDir());
        assertEquals("resamplingType", L3Constants.RESAMPLING_TYPE_VALUE_BINNING, context.getResamplingType());
        assertEquals("grid cell size", 10f, context.getGridCellSize(), 0.00001f);

        assertEquals("bandNum", 0, context.getNumBands());

        context.addBandDefinition("radiance_1", "testMask", "Maximum Likelihood", "5");
        assertEquals("bandNum", 1, context.getNumBands());

        final L3Context.BandDefinition[] bandDefs = context.getBandDefinitions();
        assertEquals("number of band definitions", 1, bandDefs.length);

        assertEquals("algo", "Maximum Likelihood", bandDefs[0].getAlgorithm().getTypeString());
        assertEquals("bandName", "radiance_1", bandDefs[0].getBandName());
        assertEquals("bitmask", "testMask", bandDefs[0].getBitmaskExp());

        context.setBorder(40f, 50f, 20f, 80f);
        Rectangle2D rect = context.getBorder();
        assertEquals("latMin", 40f, rect.getMinY(), 0.000001f);
        assertEquals("latMax", 50f, rect.getMaxY(), 0.000001f);
        assertEquals("lonMin", 20f, rect.getMinX(), 0.000001f);
        assertEquals("lonMax", 80f, rect.getMaxX(), 0.000001f);
    }

    public void testInitMultiBand() throws ProcessorException {
        context.setMainParameter(databaseDir, L3Constants.RESAMPLING_TYPE_VALUE_BINNING, 10f);
        assertEquals("database", databaseDir, context.getDatabaseDir());
        assertEquals("grid cell size", 10f, context.getGridCellSize(), 0.00001f);

        assertEquals("bandNum", 0, context.getNumBands());

        context.addBandDefinition("radiance_1", "testMask", "Maximum Likelihood", "5");
        context.addBandDefinition("radiance_2", "testMask2", "Arithmetic Mean", "0.5");

        assertEquals("bandNum", 2, context.getNumBands());

        final L3Context.BandDefinition[] bandDefs = context.getBandDefinitions();
        assertEquals("number of band definitions", 2, bandDefs.length);

        assertEquals("algo_1", "Maximum Likelihood", bandDefs[0].getAlgorithm().getTypeString());
        assertEquals("bandName_1", "radiance_1", bandDefs[0].getBandName());
        assertEquals("bitmask_1", "testMask", bandDefs[0].getBitmaskExp());
        assertEquals("algo_2", "Arithmetic Mean", bandDefs[1].getAlgorithm().getTypeString());
        assertEquals("bandName_2", "radiance_2", bandDefs[1].getBandName());
        assertEquals("bitmask_2", "testMask2", bandDefs[1].getBitmaskExp());
    }
}
