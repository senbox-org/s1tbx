package org.esa.s1tbx.gpf;

import org.esa.s1tbx.calibration.gpf.CalibrationOp;
import org.esa.s1tbx.commons.test.S1TBXTests;
import org.esa.s1tbx.commons.test.TestData;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.engine_utilities.gpf.TestProcessor;
import org.esa.snap.engine_utilities.util.TestUtils;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assume.assumeTrue;

public class TestCalibration {

    static {
        TestUtils.initTestEnvironment();
    }

    private final static OperatorSpi spi = new CalibrationOp.Spi();
    private TestProcessor testProcessor;

    private String[] productTypeExemptions = {"_BP", "XCA", "RAW", "WVW", "WVI", "WVS", "WSS", "OCN", "DOR", "GeoTIFF", "SCS_U"};
    private String[] exceptionExemptions = {"not supported", "numbands is zero",
            "calibration has already been applied",
            "The product has already been calibrated",
            "Cannot apply calibration to coregistered product",
            "WV is not a valid acquisition mode from: IW,EW,SM"
    };

    @Before
    public void setUp() {
        testProcessor = S1TBXTests.createS1TBXTestProcessor();

        // If any of the file does not exist: the test will be ignored
        assumeTrue(TestData.inputASAR_WSM + "not found", TestData.inputASAR_WSM.exists());
    }

    @Test
    public void testProcessAllIceye() throws Exception {
        testProcessor.testProcessAllInPath(spi, S1TBXTests.rootPathsIceye, "IceyeProduct", productTypeExemptions, exceptionExemptions);
    }
}
