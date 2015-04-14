package org.esa.snap.dataio.netcdf;

import org.junit.Test;

import java.io.File;

import static junit.framework.Assert.*;

public class DefaultNetCdfReaderTest {

    @Test
    public void testExtractProductName() {
        final File ncFile = new File("MER_FRS_CCL2R_20120225_085936_000001973112_00079_52248_0001.nc");
        assertEquals("MER_FRS_CCL2R_20120225_085936_000001973112_00079_52248_0001", DefaultNetCdfReader.extractProductName(ncFile));

        final File ncGzFile = new File("MER_FSG_CCL2W_20060528_074246_000002952048_00078_22176_0001.nc.gz");
        assertEquals("MER_FSG_CCL2W_20060528_074246_000002952048_00078_22176_0001", DefaultNetCdfReader.extractProductName(ncGzFile));

        final File ncAtsFile = new File("ATS_NR__2PNPDK20101103_130629_000060673096_00096_45369_7055.nc");
        assertEquals("ATS_NR__2PNPDK20101103_130629_000060673096_00096_45369_7055", DefaultNetCdfReader.extractProductName(ncAtsFile));

        final File ncHdfFile = new File("MCD43C1.A2010041.005.2010064142011.hdf");
        assertEquals("MCD43C1.A2010041.005.2010064142011", DefaultNetCdfReader.extractProductName(ncHdfFile));

        final File ncOifNc3File = new File("oif_sample.nc3");
        assertEquals("oif_sample", DefaultNetCdfReader.extractProductName(ncOifNc3File));

        final File ncOifNc4File = new File("oif_sample.nc4");
        assertEquals("oif_sample", DefaultNetCdfReader.extractProductName(ncOifNc4File));

        final File ncOifNc4cFile = new File("oif_sample.nc4c");
        assertEquals("oif_sample", DefaultNetCdfReader.extractProductName(ncOifNc4cFile));
    }
}
