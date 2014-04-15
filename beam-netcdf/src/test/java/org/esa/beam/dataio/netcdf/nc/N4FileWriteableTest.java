package org.esa.beam.dataio.netcdf.nc;

import org.junit.Assert;
import org.junit.Test;

public class N4FileWriteableTest {

    @Test
    public void testConvertToValidName() throws Exception {

        Assert.assertEquals("thermal_infrared__tirs__1", N4FileWriteable.convertToValidName("thermal_infrared_(tirs)_1"));
        Assert.assertEquals("SPH:Map Projection:BAND_1__COEFFICIENTS_LATITUDE_0", N4FileWriteable.convertToValidName("SPH:Map Projection:BAND[1]_COEFFICIENTS_LATITUDE_0"));
        Assert.assertEquals("some_other_Name_With_Invalids", N4FileWriteable.convertToValidName("some/other%Name&With}Invalids"));
        Assert.assertEquals("_isTheFirstChar", N4FileWriteable.convertToValidName("2isTheFirstChar"));

    }
}
