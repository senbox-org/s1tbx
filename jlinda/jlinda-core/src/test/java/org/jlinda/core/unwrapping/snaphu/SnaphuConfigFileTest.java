package org.jlinda.core.unwrapping.snaphu;

import org.jlinda.core.Orbit;
import org.jlinda.core.SLCImage;
import org.jlinda.core.Window;
import org.junit.Before;
import org.junit.Test;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

public class SnaphuConfigFileTest {

    static SnaphuConfigFile snaphuConfigFile;

    @Before
    public void setUp() throws Exception {

        String unitTestDir = "/d2/unit_test_data/bam/";
        String masterRes = "9192.res";
        String slaveRes = "6687.res";

        /// master
        File masterResFile = new File(unitTestDir + masterRes);

        SLCImage masterMetadata = new SLCImage();
        masterMetadata.parseResFile(masterResFile);

        Orbit masterOrbit = new Orbit();
        masterOrbit.parseOrbit(masterResFile);
        masterOrbit.computeCoefficients(3);

        /// slave
        File slaveResFile = new File(unitTestDir + slaveRes);

        SLCImage slaveMetadata = new SLCImage();
        slaveMetadata.parseResFile(slaveResFile);

        Orbit slaveOrbit = new Orbit();
        slaveOrbit.parseOrbit(slaveResFile);
        slaveOrbit.computeCoefficients(3);

        /// data extend
        Window dataWindow = new Window(1, 26672, 1, 5142);

        /// snaphu parameters
        SnaphuParameters parameters = new SnaphuParameters();
        parameters.setOutFileName("snaphu.unw");
        parameters.setUnwrapMode("defo");
        parameters.setLogFileName("snaphu.log");
        parameters.setCoherenceFileName("snaphu.coh");
        parameters.setSnaphuInit("mst");
        parameters.setVerbosityFlag("true");

        /// initiate snaphuconfig
        snaphuConfigFile = new SnaphuConfigFile(masterMetadata, slaveMetadata, masterOrbit, slaveOrbit, dataWindow, parameters);

    }

    @Test
    public void testBuildConfFile() throws Exception {

        snaphuConfigFile.buildConfFile();

        // write buffer for testing
        try {
            BufferedWriter out = new BufferedWriter(new FileWriter("/d2/snaphu_test.conf"));
            out.write(snaphuConfigFile.getConfigFileBuffer().toString());
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // show buffer only on screen
        System.out.println(snaphuConfigFile.getConfigFileBuffer());

    }
}
