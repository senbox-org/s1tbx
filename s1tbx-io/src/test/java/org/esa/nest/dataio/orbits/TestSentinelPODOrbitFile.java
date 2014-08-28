package org.esa.nest.dataio.orbits;

import org.esa.beam.framework.datamodel.ProductData;
import org.esa.snap.datamodel.Orbits;
import org.junit.Before;
import org.junit.Test;

/**
 * To test SentinelPODOrbitFile
 */
public class TestSentinelPODOrbitFile {

    @Before
    public void setUp() throws Exception {

        System.out.println("TestSentinelPODOrbitFile.setup: do nothing");
    }

    @Test
    public void testSentinelPODOrbitFile() throws Throwable {

        System.out.println("testSentinelPODOrbitFile...");
        SentinelPODOrbitFile podOrbitFile = new SentinelPODOrbitFile("P:\\s1tbx\\s1tbx\\Data\\Orbit\\unzipped\\S1A_OPER_AUX_RESORB_OPOD_20140611T152302_V20140525T151921_20140525T183641.EOF");

        // First OSV (exact match)
        String utcStr1 = "UTC=2014-05-25T15:19:21.698661";
        double utc1 = ProductData.UTC.parse(SentinelPODOrbitFile.convertUTC(utcStr1)).getMJD();
        Orbits.OrbitData orbitData = podOrbitFile.getOrbitData(utc1);
        assert(orbitData.xPos == 5385157.178934F);
        assert(orbitData.yPos == 4581079.075900F);
        assert(orbitData.zPos == -98597.029370F);
        assert(orbitData.xVel == 1097.015759F);
        assert(orbitData.yVel == -1143.130525F);
        assert(orbitData.zVel == 7433.180927F);

        // Last OSV (exact match)
        utcStr1 = "UTC=2014-05-25T18:36:41.698661";
        utc1 = ProductData.UTC.parse(SentinelPODOrbitFile.convertUTC(utcStr1)).getMJD();
        orbitData = podOrbitFile.getOrbitData(utc1);
        assert(orbitData.xPos == 6983513.585397F);
        assert(orbitData.yPos == -1106276.317976F);
        assert(orbitData.zPos == -49616.303393F);
        assert(orbitData.xVel == -204.781377F);
        assert(orbitData.yVel == -1569.053554F);
        assert(orbitData.zVel == 7433.664983F);

        // 500th OSV (exact match)
        utcStr1 = "UTC=2014-05-25T16:42:31.698661";
        utc1 = ProductData.UTC.parse(SentinelPODOrbitFile.convertUTC(utcStr1)).getMJD();
        orbitData = podOrbitFile.getOrbitData(utc1);
        assert(orbitData.xPos == 3337932.006259F);
        assert(orbitData.yPos == 2076857.815226F);
        assert(orbitData.zPos == -5882905.084557F);
        assert(orbitData.xVel == 6280.633665F);
        assert(orbitData.yVel == 1311.969574F);
        assert(orbitData.zVel == 4028.979288F);

        // between 450th and 451th OSV (closer to 450th)
        utcStr1 = "UTC=2014-05-25T16:34:14.698661";
        utc1 = ProductData.UTC.parse(SentinelPODOrbitFile.convertUTC(utcStr1)).getMJD();
        orbitData = podOrbitFile.getOrbitData(utc1);
        assert(orbitData.xPos == -80628.326729F);
        assert(orbitData.yPos == 1048544.403175F);
        assert(orbitData.zPos == -6997393.487976F);
        assert(orbitData.xVel == 7056.916004F);
        assert(orbitData.yVel == 2714.487896F);
        assert(orbitData.zVel == 325.454442F);

        // between 450th and 451th OSV (closer to 451th)
        utcStr1 = "UTC=2014-05-25T16:34:18.698661";
        utc1 = ProductData.UTC.parse(SentinelPODOrbitFile.convertUTC(utcStr1)).getMJD();
        orbitData = podOrbitFile.getOrbitData(utc1);
        assert(orbitData.xPos == -10036.237767F);
        assert(orbitData.yPos == 1075578.908285F);
        assert(orbitData.zPos == -6993746.360154F);
        assert(orbitData.xVel == 7061.365380F);
        assert(orbitData.yVel == 2692.361802F);
        assert(orbitData.zVel == 403.964409F);

        // OSV earlier than all
        utcStr1 = "UTC=2014-05-25T15:10:21.698661";
        utc1 = ProductData.UTC.parse(SentinelPODOrbitFile.convertUTC(utcStr1)).getMJD();
        orbitData = podOrbitFile.getOrbitData(utc1);
        assert(orbitData.xPos == 5385157.178934F);
        assert(orbitData.yPos == 4581079.075900F);
        assert(orbitData.zPos == -98597.029370F);
        assert(orbitData.xVel == 1097.015759F);
        assert(orbitData.yVel == -1143.130525F);
        assert(orbitData.zVel == 7433.180927F);

        // OSV later than all
        utcStr1 = "UTC=2014-05-25T18:36:41.699662";
        utc1 = ProductData.UTC.parse(SentinelPODOrbitFile.convertUTC(utcStr1)).getMJD();
        orbitData = podOrbitFile.getOrbitData(utc1);
        assert(orbitData.xPos == 6983513.585397F);
        assert(orbitData.yPos == -1106276.317976F);
        assert(orbitData.zPos == -49616.303393F);
        assert(orbitData.xVel == -204.781377F);
        assert(orbitData.yVel == -1569.053554F);
        assert(orbitData.zVel == 7433.664983F);


        String str = podOrbitFile.getMissionFromHeader();
        System.out.println("Mission from Header = " + str);
        assert(str.equals("Sentinel-1A"));

        str = podOrbitFile.getFileTypeFromHeader();
        System.out.println("File_Type from Header = " + str);
        assert(str.equals("AUX_RESORB"));

        str = podOrbitFile.getValidityStartFromHeader();
        System.out.println("Validity_Start from Header = " + str);
        assert(str.equals("UTC=2014-05-25T15:19:21"));

        str = podOrbitFile.getValidityStopFromHeader();
        System.out.println("Validity_Stop from Header = " + str);
        assert(str.equals("UTC=2014-05-25T18:36:41"));

        final String missionID = SentinelPODOrbitFile.getMissionIDFromFilename("S1A_OPER_AUX_POEORB_OPOD_20140526T151322_V20140509T225944_20140511T005944.EOF");
        System.out.println("mission ID from filename = " + missionID);
        assert(missionID.equals("S1A"));

        final String fileType = SentinelPODOrbitFile.getFileTypeFromFilename("S1A_OPER_AUX_POEORB_OPOD_20140526T151322_V20140509T225944_20140511T005944.EOF");
        System.out.println("file type from filename = " + fileType);
        assert(fileType.equals("AUX_POEORB"));

        final String vStart = SentinelPODOrbitFile.getValidityStartFromFilename("S1A_OPER_AUX_POEORB_OPOD_20140526T151322_V20140509T225944_20140511T005944.EOF");
        System.out.println("validity start from filename = " + vStart );
        assert(vStart.equals("UTC=2014-05-09T22:59:44"));

        final String vStop = SentinelPODOrbitFile.getValidityStopFromFilename("S1A_OPER_AUX_POEORB_OPOD_20140526T151322_V20140509T225944_20140511T005944.EOF");
        System.out.println("validity stop from filename = " + vStop);
        assert(vStop.equals("UTC=2014-05-11T00:59:44"));
    }
}
