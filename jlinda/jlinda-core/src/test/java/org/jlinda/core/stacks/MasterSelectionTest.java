package org.jlinda.core.stacks;

import com.bc.ceres.core.ProgressMonitor;
import org.jlinda.core.Orbit;
import org.jlinda.core.SLCImage;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static junit.framework.Assert.assertEquals;

/**
 * User: pmar@ppolabs.com
 * Date: 1/20/12
 * Time: 3:40 PM
 */
public class MasterSelectionTest {

    final static int ORBIT_DEGREE = 2;
    private SLCImage[] slcImages;
    private Orbit[] orbits;
    private float modeledCoherence_EXPECTED = (float)0.245;
    private long optimalMaster_EXPECTED = 57892;


    @Before
    public void setUp() throws Exception {

        final String testDirectory = "/d2/test.processing/testStacksData/";
        final long[] listOfOrbits;

        listOfOrbits = new long[]{147872, 148373, 148874, 150878, 151379, 151880,
                                  152381, 156389, 156890, 157892, 160898, 161399,
                                  162401, 162902, 163403, 163904,};

        // initialize slcimage container array
        slcImages = new SLCImage[listOfOrbits.length];
        orbits = new Orbit[listOfOrbits.length];

        for (int i = 0; i < listOfOrbits.length; i++) {

            File resFile = new File(testDirectory + listOfOrbits[i] + ".res");

//            System.out.println("resFile = " + resFile);

            // setup slc containers
            slcImages[i] = new SLCImage();
            slcImages[i].parseResFile(resFile);

            // setup orbits
            orbits[i] = new Orbit();
            orbits[i].parseOrbit(resFile);
            orbits[i].computeCoefficients(ORBIT_DEGREE);
        }
    }


    @Test
    public void testFindOptimalMaster() throws Exception {

        final MasterSelection dataStack = new MasterSelection();
        dataStack.setInput(slcImages, orbits);
        dataStack.estimateOptimalMaster(ProgressMonitor.NULL);

        float modeledCoherence_ACTUAL = dataStack.getModeledCoherence();
        long optimalMaster_ACTUAL = dataStack.getOrbitNumber();

        assertEquals(modeledCoherence_EXPECTED, modeledCoherence_ACTUAL , 1e-3);
        assertEquals(optimalMaster_EXPECTED, optimalMaster_EXPECTED);

    }

    @After
    public void tearDown() throws Exception {

    }
}
