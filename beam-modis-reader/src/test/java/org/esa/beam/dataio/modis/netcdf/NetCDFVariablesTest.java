package org.esa.beam.dataio.modis.netcdf;

import junit.framework.TestCase;

public class NetCDFVariablesTest extends TestCase {

    public void testExtractBandName() {
        assertEquals("band_name", NetCDFVariables.extractBandName("band_name"));
        assertEquals("EV_250_Aggr500_RefSB", NetCDFVariables.extractBandName("MODIS_SWATH_Type_L1B/Data Fields/EV_250_Aggr500_RefSB"));
        assertEquals("EV_500_RefSB_Uncert_Indexes", NetCDFVariables.extractBandName("MODIS_SWATH_Type_L1B/Data Fields/EV_500_RefSB_Uncert_Indexes"));
    }
}
