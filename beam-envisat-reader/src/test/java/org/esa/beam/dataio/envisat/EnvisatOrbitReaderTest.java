package org.esa.beam.dataio.envisat;

import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;
import java.util.Date;

import org.esa.beam.framework.datamodel.ProductData;

/**
 * EnvisatAuxReader Tester.
 *
 * @author lveci
 */
public class EnvisatOrbitReaderTest extends TestCase {

    private final static String doris_por_orbit =
            "org/esa/beam/resources/testdata/DOR_POR_AXVF-P20080404_014700_20080401_215527_20080403_002327";
    private final static String doris_vor_orbit =
            "org/esa/beam/resources/testdata/DOR_VOR_AXVF-P20080331_075200_20080301_215527_20080303_002327";

    public EnvisatOrbitReaderTest(String name) {
        super(name);
    }

    @Override
    public void setUp() throws Exception {
        super.setUp();
    }

    @Override
    public void tearDown() throws Exception {
        super.tearDown();
    }

    public void testPOROrbitFiles() throws IOException {

        final File orbFile = EnvisatAuxReader.getFile(doris_por_orbit);
        if (!orbFile.exists())
            return;

        final EnvisatOrbitReader reader = new EnvisatOrbitReader();

        reader.readProduct(doris_por_orbit);

        final EnvisatOrbitReader.OrbitVector orb = getOrbitData(reader);

        ProductData.UTC utc = new ProductData.UTC(orb.utcTime);
        assertEquals("01-APR-2008 21:55:27.000000", utc.format());
        assertEquals(-3300453.451, orb.xPos);
        assertEquals(881817.654, orb.yPos);
        assertEquals(-6304026.222, orb.zPos);
        assertEquals(6673.625193, orb.xVel);
        assertEquals(880.089573, orb.yVel);
        assertEquals(-3372.728885, orb.zVel);
    }

    public void testVOROrbitFiles() throws IOException {

        final File orbFile = EnvisatAuxReader.getFile(doris_vor_orbit);
        if (!orbFile.exists())
            return;

        final EnvisatOrbitReader reader = new EnvisatOrbitReader();

        reader.readProduct(doris_vor_orbit);

        final EnvisatOrbitReader.OrbitVector orb = getOrbitData(reader);

        ProductData.UTC utc = new ProductData.UTC(orb.utcTime);
        assertEquals("01-MAR-2008 21:55:27.000000", utc.format());
        assertEquals(6494931.106, orb.xPos);
        assertEquals(578715.148, orb.yPos);
        assertEquals(-2977719.455, orb.zPos);
        assertEquals(3188.730641, orb.xVel);
        assertEquals(-1416.295158, orb.yVel);
        assertEquals(6692.698996, orb.zVel);
    }

    public void testCubicInterpolation() throws Exception {

        final File orbFile = EnvisatAuxReader.getFile(doris_vor_orbit);
        if (!orbFile.exists())
            return;

        final EnvisatOrbitReader reader = new EnvisatOrbitReader();

        reader.readProduct(doris_vor_orbit);
        reader.readOrbitData();
        
        final double utc1 = reader.getOrbitVector(1).utcTime;
        final double utc2 = reader.getOrbitVector(2).utcTime;
        final double utc = 0.3*utc1 + 0.7*utc2;
        EnvisatOrbitReader.OrbitVector orb = reader.getOrbitVector(utc);
        /*
        System.out.println("orb.xPos = " + orb.xPos);
        System.out.println("orb.yPos = " + orb.yPos);
        System.out.println("orb.zPos = " + orb.zPos);
        System.out.println("orb.xVel = " + orb.xVel);
        System.out.println("orb.yVel = " + orb.yVel);
        System.out.println("orb.zVel = " + orb.zVel);
        */
        assertEquals(6782118.314438924, orb.xPos);
        assertEquals(429045.3202724487, orb.yPos);
        assertEquals(-2279528.4549769447, orb.zPos);
        assertEquals(2436.6465715695913, orb.xVel);
        assertEquals(-1513.6427516392862, orb.yVel);
        assertEquals(6984.0148269792735, orb.zVel);
    }

    static EnvisatOrbitReader.OrbitVector getOrbitData(final EnvisatOrbitReader reader) throws IOException {

        // get the data
        reader.readOrbitData();

        final int numRecords = reader.getNumRecords();

        EnvisatOrbitReader.OrbitVector orb = reader.getOrbitVector(0);
        assertNotNull(orb);

        EnvisatOrbitReader.OrbitVector orb2 = reader.getOrbitVector(numRecords - 1);
        assertNotNull(orb2);

        return orb;
    }

}
