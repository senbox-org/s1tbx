package org.esa.beam.dataio.netcdf.metadata.profiles.hdfeos;

import org.junit.Test;

import static org.junit.Assert.*;

public class HdfEosGridInfoTest {

    private static final double[] PROJ_PARAMS = new double[]{
            12.3, 4.0, 56.8332, 1234.660, 0.0071,
            223, 12.009, 12.735, 211.3, 10,
            1234546.03, 84.9, 234.222224
    };

    @Test
    public void testGridInfosAreEqual() throws Exception {
        HdfEosGridInfo gridInfo1 = new HdfEosGridInfo("testName", -120, 50, -100, 40, "Sinusoidal");
        HdfEosGridInfo gridInfo2 = new HdfEosGridInfo("testName", -120, 50, -100, 40, "Sinusoidal");
        assertTrue(gridInfo1.equalProjections(gridInfo2));
    }

    @Test
    public void testGridInfosWithProjParamsAreEqual() throws Exception {
        HdfEosGridInfo gridInfo1 = new HdfEosGridInfo("testName", -120, 50, -100, 40, "Sinusoidal");
        gridInfo1.setProjectionParameter(PROJ_PARAMS);
        HdfEosGridInfo gridInfo2 = new HdfEosGridInfo("testName", -120, 50, -100, 40, "Sinusoidal");
        gridInfo2.setProjectionParameter(PROJ_PARAMS);
        assertTrue(gridInfo1.equalProjections(gridInfo2));
    }

    @Test
    public void testGridInfosAreNotEqual() throws Exception {
        HdfEosGridInfo gridInfo1 = new HdfEosGridInfo("testName", -120, 50.1, -100, 40, "Sinusoidal");
        HdfEosGridInfo gridInfo2 = new HdfEosGridInfo("testName", -120, 50, -100, 40, "Sinusoidal");
        assertFalse(gridInfo1.equalProjections(gridInfo2));
    }

    @Test
    public void testGridInfosWithProjParamsAreNotEqual() throws Exception {

        HdfEosGridInfo gridInfo1 = new HdfEosGridInfo("testName", -120, 50, -100, 40, "Sinusoidal");
        gridInfo1.setProjectionParameter(PROJ_PARAMS);
        HdfEosGridInfo gridInfo2 = new HdfEosGridInfo("testName", -120, 50, -100, 40, "Sinusoidal");
        double[] projParams2 = PROJ_PARAMS.clone();
        projParams2[4] = 42.42;
        gridInfo2.setProjectionParameter(projParams2);
        assertFalse(gridInfo1.equalProjections(gridInfo2));
    }

    @Test
    public void testEqualProjectionsWithProjParamsNullParamsAreNotEqual() throws Exception {

        HdfEosGridInfo gridInfo1 = new HdfEosGridInfo("testName", -120, 50, -100, 40, "Sinusoidal");
        gridInfo1.setProjectionParameter(PROJ_PARAMS);
        HdfEosGridInfo gridInfo2 = new HdfEosGridInfo("testName", -120, 50, -100, 40, "Sinusoidal");
        gridInfo2.setProjectionParameter(null);
        assertFalse(gridInfo1.equalProjections(gridInfo2));
    }
}