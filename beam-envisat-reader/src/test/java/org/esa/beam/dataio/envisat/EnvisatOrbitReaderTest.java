package org.esa.beam.dataio.envisat;

import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;
import java.util.Date;

/**
 * EnvisatAuxReader Tester.
 *
 * @author lveci
 */
public class EnvisatOrbitReaderTest extends TestCase {

    String doris_por_orbit = "P:\\nest\\nest\\ESA Data\\Orbits\\Doris\\por\\200804\\DOR_POR_AXVF-P20080427_015500_20080424_215527_20080426_002327";
    String doris_vor_orbit = "P:\\nest\\nest\\ESA Data\\Orbits\\Doris\\vor\\200803\\DOR_VOR_AXVF-P20080428_114800_20080330_215527_20080401_002327";

    public EnvisatOrbitReaderTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public void testPOROrbitFiles() {

        EnvisatOrbitReader reader = new EnvisatOrbitReader();

        try {

            File orbFile1 = new File(doris_por_orbit);
            if (orbFile1.exists()) {
                reader.readProduct(doris_por_orbit);

                getOrbitData(reader);
            }
        } catch (IOException e) {

        }
    }

    public void testVOROrbitFiles() {

        EnvisatOrbitReader reader = new EnvisatOrbitReader();

        try {

            File orbFile1 = new File(doris_vor_orbit);
            if (orbFile1.exists()) {
                reader.readProduct(doris_vor_orbit);

                getOrbitData(reader);
            }
        } catch (IOException e) {

        }
    }

    static void getOrbitData(EnvisatOrbitReader reader) throws IOException {

        Date productDate = new Date(12345678);           // from the product

        Date startDate = reader.getSensingStart();
        Date stopDate = reader.getSensingStop();
        //if (productDate.after(startDate) && productDate.before(stopDate)) {
            // we found the right orbit file

            // get the data
            reader.readOrbitData();

            EnvisatOrbitReader.OrbitVector orb = reader.getOrbitVector(startDate);
            assertNotNull(orb);

            EnvisatOrbitReader.OrbitVector orb2 = reader.getOrbitVector(stopDate);
            assertNotNull(orb2);

            //print out some values
            System.out.print("UTC " + orb.utcTime.toString());
            System.out.print(" xpos " + orb.xPos);
            System.out.print(" ypos " + orb.yPos);
            System.out.print(" zpos " + orb.zPos);
            System.out.print(" xVel " + orb.xVel);
            System.out.print(" yVel " + orb.yVel);
            System.out.print(" zVel " + orb.zVel);
            System.out.println();

        //}

    }


}
