package org.esa.nest.dataio.orbits;

import org.esa.beam.framework.datamodel.ProductData;
import org.esa.snap.datamodel.Orbits;
import org.esa.snap.util.TestData;
import org.esa.snap.util.TestUtils;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertEquals;

/**
 * To test SentinelPODOrbitFile
 */
public class TestSentinelPODOrbitFile {

    private final static File orbitFile = new File(TestData.inputSAR+"Orbits"+TestData.sep+
            "S1A_OPER_AUX_RESORB_OPOD_20140611T152302_V20140525T151921_20140525T183641.EOF");

    @Test
    public void testSentinelPODOrbitFile() throws Exception {
        if (!orbitFile.exists()) {
            TestUtils.skipTest(this, orbitFile + " not found");
            return;
        }
        TestUtils.log.info("testSentinelPODOrbitFile...");
        final SentinelPODOrbitFile podOrbitFile = new SentinelPODOrbitFile(orbitFile);

        // First OSV (exact match)
        String utcStr1 = "UTC=2014-05-25T15:19:21.698661";
        double utc1 = SentinelPODOrbitFile.toUTC(utcStr1).getMJD();
        Orbits.OrbitData orbitData = podOrbitFile.getOrbitData(utc1);
        assert(orbitData.xPos == 5385157.178934F);
        assert(orbitData.yPos == 4581079.075900F);
        assert(orbitData.zPos == -98597.029370F);
        assert(orbitData.xVel == 1097.015759F);
        assert(orbitData.yVel == -1143.130525F);
        assert(orbitData.zVel == 7433.180927F);

        // Last OSV (exact match)
        utcStr1 = "UTC=2014-05-25T18:36:41.698661";
        utc1 = SentinelPODOrbitFile.toUTC(utcStr1).getMJD();
        orbitData = podOrbitFile.getOrbitData(utc1);
        assert(orbitData.xPos == 6983513.585397F);
        assert(orbitData.yPos == -1106276.317976F);
        assert(orbitData.zPos == -49616.303393F);
        assert(orbitData.xVel == -204.781377F);
        assert(orbitData.yVel == -1569.053554F);
        assert(orbitData.zVel == 7433.664983F);

        // 500th OSV (exact match)
        utcStr1 = "UTC=2014-05-25T16:42:31.698661";
        utc1 = SentinelPODOrbitFile.toUTC(utcStr1).getMJD();
        orbitData = podOrbitFile.getOrbitData(utc1);
        assert(orbitData.xPos == 3337932.006259F);
        assert(orbitData.yPos == 2076857.815226F);
        assert(orbitData.zPos == -5882905.084557F);
        assert(orbitData.xVel == 6280.633665F);
        assert(orbitData.yVel == 1311.969574F);
        assert(orbitData.zVel == 4028.979288F);

        // between 450th and 451th OSV (closer to 450th)
        utcStr1 = "UTC=2014-05-25T16:34:14.698661";
        utc1 = SentinelPODOrbitFile.toUTC(utcStr1).getMJD();
        orbitData = podOrbitFile.getOrbitData(utc1);
        assert(orbitData.xPos == -80628.326729F);
        assert(orbitData.yPos == 1048544.403175F);
        assert(orbitData.zPos == -6997393.487976F);
        assert(orbitData.xVel == 7056.916004F);
        assert(orbitData.yVel == 2714.487896F);
        assert(orbitData.zVel == 325.454442F);

        // between 450th and 451th OSV (closer to 451th)
        utcStr1 = "UTC=2014-05-25T16:34:18.698661";
        utc1 = SentinelPODOrbitFile.toUTC(utcStr1).getMJD();
        orbitData = podOrbitFile.getOrbitData(utc1);
        assert(orbitData.xPos == -10036.237767F);
        assert(orbitData.yPos == 1075578.908285F);
        assert(orbitData.zPos == -6993746.360154F);
        assert(orbitData.xVel == 7061.365380F);
        assert(orbitData.yVel == 2692.361802F);
        assert(orbitData.zVel == 403.964409F);

        // OSV earlier than all
        utcStr1 = "UTC=2014-05-25T15:10:21.698661";
        utc1 = SentinelPODOrbitFile.toUTC(utcStr1).getMJD();
        orbitData = podOrbitFile.getOrbitData(utc1);
        assert(orbitData.xPos == 5385157.178934F);
        assert(orbitData.yPos == 4581079.075900F);
        assert(orbitData.zPos == -98597.029370F);
        assert(orbitData.xVel == 1097.015759F);
        assert(orbitData.yVel == -1143.130525F);
        assert(orbitData.zVel == 7433.180927F);

        // OSV later than all
        utcStr1 = "UTC=2014-05-25T18:36:41.699662";
        utc1 = SentinelPODOrbitFile.toUTC(utcStr1).getMJD();
        orbitData = podOrbitFile.getOrbitData(utc1);
        assert(orbitData.xPos == 6983513.585397F);
        assert(orbitData.yPos == -1106276.317976F);
        assert(orbitData.zPos == -49616.303393F);
        assert(orbitData.xVel == -204.781377F);
        assert(orbitData.yVel == -1569.053554F);
        assert(orbitData.zVel == 7433.664983F);


        String str = podOrbitFile.getMissionFromHeader();
        TestUtils.log.info("Mission from Header = " + str);
        assert(str.startsWith("Sentinel-1"));

        str = podOrbitFile.getFileTypeFromHeader();
        TestUtils.log.info("File_Type from Header = " + str);
        assert(str.equals("AUX_RESORB"));

        str = podOrbitFile.getValidityStartFromHeader();
        TestUtils.log.info("Validity_Start from Header = " + str);
        assert(str.equals("UTC=2014-05-25T15:19:21"));

        str = podOrbitFile.getValidityStopFromHeader();
        TestUtils.log.info("Validity_Stop from Header = " + str);
        assert(str.equals("UTC=2014-05-25T18:36:41"));

        final String missionID = SentinelPODOrbitFile.getMissionIDFromFilename("S1A_OPER_AUX_POEORB_OPOD_20140526T151322_V20140509T225944_20140511T005944.EOF");
        TestUtils.log.info("mission ID from filename = " + missionID);
        assert(missionID.equals("S1A"));

        final String fileType = SentinelPODOrbitFile.getFileTypeFromFilename("S1A_OPER_AUX_POEORB_OPOD_20140526T151322_V20140509T225944_20140511T005944.EOF");
        TestUtils.log.info("file type from filename = " + fileType);
        assert(fileType.equals("AUX_POEORB"));

        final String vStart = SentinelPODOrbitFile.getValidityStartFromFilename("S1A_OPER_AUX_POEORB_OPOD_20140526T151322_V20140509T225944_20140511T005944.EOF");
        TestUtils.log.info("validity start from filename = " + vStart );
        assert(vStart.equals("UTC=2014-05-09T22:59:44"));

        final String vStop = SentinelPODOrbitFile.getValidityStopFromFilename("S1A_OPER_AUX_POEORB_OPOD_20140526T151322_V20140509T225944_20140511T005944.EOF");
        TestUtils.log.info("validity stop from filename = " + vStop);
        assert(vStop.equals("UTC=2014-05-11T00:59:44"));
    }

    @Test
    public void testSentinelPODOrbitFileOperations() throws Exception {
        if (!orbitFile.exists()) {
            TestUtils.skipTest(this, orbitFile + " not found");
            return;
        }

        String name = orbitFile.getName();
        final ProductData.UTC utcStart = SentinelPODOrbitFile.getValidityStartFromFilenameUTC(name);
        assertEquals(5258.6384375, utcStart.getMJD(), 0.00001);

        final ProductData.UTC utcEnd = SentinelPODOrbitFile.getValidityStopFromFilenameUTC(name);
        assertEquals(5258.775474537037, utcEnd.getMJD(), 0.00001);
    }
}
