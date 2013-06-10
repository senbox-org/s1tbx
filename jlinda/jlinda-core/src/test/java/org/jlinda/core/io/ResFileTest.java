package org.jlinda.core.io;


import org.apache.log4j.Logger;
import org.esa.beam.framework.datamodel.ProductData;
import org.junit.*;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class ResFileTest {

    static Logger logger = Logger.getLogger(ResFileTest.class.getName());

    private static File testFile;
    private static String resFileString;
    private static ResFile resFile;

    private static String processControlBlock;
    private static String readFilesBlock;

    private static double delta = Math.pow(10, -6);

    @BeforeClass
    public static void setupTestData() throws Exception {

        testFile = new File("test.res");

        processControlBlock = "Start_process_control\n" +
                "readfiles:	        1\n" +
                "precise_orbits:	1\n" +
                "crop:			    1\n" +
                "resample:		    1\n" +
                "filt_azi:		    0\n" +
                "filt_range:		0\n" +
                "NOT_USED:		    0\n" +
                "End_process_control";

        readFilesBlock = "*******************************************************************\n" +
                "*_Start_readfiles:\n" +
                "*******************************************************************\n" +
                "Logical volume generating facility:             \"UK-PAC\"\n" +
                "Logical volume creation date:                   dummy\n" +
                "Location and date/time of product creation:     \"08-JAN-2004 16:11:26.000000\"\n" +
                "Scene identification:                           ORBIT 6687\n" +
                "Scene location:                                 FRAME 17\n" +
                "Leader file: \"ASA_IMS_1PNUPA20030611_061252_000000162017_00120_06687_0098.N1\" 49\n" +
                "Scene_centre_latitude:                          29.1456\n" +
                "Scene_centre_longitude:                         58.5274\n" +
                "Radar_wavelength (m):                           0.0562356\n" +
                "First_pixel_azimuth_time (UTC):                 11-JUN-2003 06:12:52.090719\n" +
                "Pulse_Repetition_Frequency (computed, Hz):      1652.415649\n" +
                "Total_azimuth_band_width (Hz):                  1316.000000\n" +
                "*******************************************************************\n" +
                "* End_readfiles:_NORMAL\n" +
                "*******************************************************************";

        String preciseOrbitsBlock = "*******************************************************************\n" +
                "*_Start_precise_orbits:\n" +
                "*******************************************************************\n" +
                "	t(s)	X(m)	Y(m)	Z(m)\n" +
                "NUMBER_OF_DATAPOINTS: 			4\n" +
                "22371.000000	2980481.384	5510209.440	3468130.655\n" +
                "22373.000000	2986908.023	5514837.160	3455264.837\n" +
                "22375.000000	2993323.103	5519439.182	3442383.979\n" +
                "22377.000000	2999726.590	5524015.487	3429488.137\n" +
                "\n" +
                "*******************************************************************\n" +
                "* End_precise_orbits:_NORMAL\n" +
                "*******************************************************************\n";


        resFileString = processControlBlock + "\n\n" + readFilesBlock + "\n\n" + preciseOrbitsBlock;
        // org.apache.commons.io.FileUtils dependency
        // FileUtils.writeStringToFile(resFileContent, processControlBlock);
        try {
            BufferedWriter resFileOut = new BufferedWriter(new FileWriter(testFile));
            resFileOut.write(resFileString);
            resFileOut.close();
        } catch (IOException e) {
            logger.error("IO Exception: test file cannot be created.");
            logger.error(e.getMessage());
        }

    }

    @AfterClass
    public static void cleanTestData() {

        if (!testFile.exists())
            throw new IllegalArgumentException("Delete: no such file or directory: " + testFile.getName());

        boolean success = testFile.delete();

        if (!success)
            throw new IllegalArgumentException("Delete: deletion of file" + testFile.getName() + " failed");

        System.gc();
    }


    @Before
    public void setUp() throws Exception {
        resFile = new ResFile(testFile);
    }

    @Test
    public void testBufferIO() throws Exception {
        Assert.assertEquals(resFileString, resFile.getBuffer().toString());
    }

    @Test
    public void testBufferIO2() throws Exception {

        ResFile tempResFile = new ResFile();

        tempResFile.streamBuffer(testFile);
        tempResFile.setResFile(testFile);
        tempResFile.setStartIdx(0);
        tempResFile.setEndIdx(tempResFile.getBuffer().length());

        Assert.assertEquals(resFileString, tempResFile.getBuffer().toString());

    }

    @Test
    public void testSetSubBuffer() throws Exception {

        resFile.setSubBuffer("Start_process_control", "End_process_control");
        int idxStartExpected = 0;
        int idxEndExpected = processControlBlock.length();

        Assert.assertEquals(idxStartExpected, resFile.getStartIdx());
        Assert.assertEquals(idxEndExpected, resFile.getEndIdx());
        Assert.assertEquals(processControlBlock, resFile.getBuffer().substring(resFile.getStartIdx(), resFile.getEndIdx()));

    }

    @After
    public void tearDown() throws Exception {
        resFile.resetSubBuffer();
    }


    @Test
    public void testQueryKeyString() throws Exception {
        String expectedValue = "FRAME 17";
        String returnValue = resFile.parseStringValue("Scene location");

        Assert.assertEquals(expectedValue, returnValue);
    }

    @Test
    public void testQueryKeyInteger() throws Exception {
        int expectedValue = 1;
        int returnValue = resFile.parseIntegerValue("resample");

        Assert.assertEquals(expectedValue, returnValue, 0.0000);
    }

    @Test
    public void testQueryKeyDouble() throws Exception {
        double expectedValue = 29.1456;
        double returnValue = resFile.parseDoubleValue("Scene_centre_latitude");

        Assert.assertEquals(expectedValue, returnValue, delta);
    }

    @Test
    public void testQueryKeyDoubleSubBuffer() throws Exception {

        double expectedValue = 0.0562356;

        // define subbuffer
        resFile.setSubBuffer("_Start_readfiles", "End_readfiles");

        // query subbuffer
        double returnValue = resFile.parseDoubleValue("Radar_wavelength \\(m\\)");

        Assert.assertEquals(expectedValue, returnValue, delta);

    }

    @Test
    public void testParseTimeValue() throws Exception {

        ProductData.UTC expectedTime = ProductData.UTC.parse("11-JUN-2003 06:12:52.090719");
        ProductData.UTC returnedTime = resFile.parseDateTimeValue("First_pixel_azimuth_time \\(UTC\\)");
        Assert.assertEquals(expectedTime.toString(), returnedTime.toString());

    }

    @Test
    public void testParseOrbit() throws Exception {

        double[][] expectedOrbit = {{22371.000000,2980481.384,5510209.440,3468130.655},
                                    {22373.000000,2986908.023,5514837.160,3455264.837},
                                    {22375.000000,2993323.103,5519439.182,3442383.979},
                                    {22377.000000,2999726.590,5524015.487,3429488.137}};


        resFile.setSubBuffer("_Start_precise_orbits", "End_precise_orbits");
        double[][] returnedOrbit = resFile.parseOrbit();

        for (int i = 0; i < returnedOrbit.length; i++) {
            for (int j = 0; j < returnedOrbit[0].length; j++) {

                double expectedDouble = expectedOrbit[i][j];
                double returnedDouble = returnedOrbit[i][j];

                Assert.assertEquals(expectedDouble, returnedDouble, delta);

            }

        }

    }

}