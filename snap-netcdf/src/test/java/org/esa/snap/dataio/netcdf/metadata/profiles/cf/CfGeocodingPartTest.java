package org.esa.snap.dataio.netcdf.metadata.profiles.cf;

import junit.framework.TestCase;
import ucar.ma2.Array;
import ucar.ma2.DataType;

/**
 * @author Olaf Danne
 */
public class CfGeocodingPartTest extends TestCase {

    public void testIsGlobalShifted180() {
        Array longitudeData = Array.makeArray(DataType.DOUBLE, 480, 0.0, 0.75);
        assertTrue(CfGeocodingPart.isGlobalShifted180(longitudeData));

        longitudeData = Array.makeArray(DataType.DOUBLE, 480, 0.75, 0.75);
        assertTrue(CfGeocodingPart.isGlobalShifted180(longitudeData));

        longitudeData = Array.makeArray(DataType.DOUBLE, 480, 0.375, 0.75);
        assertTrue(CfGeocodingPart.isGlobalShifted180(longitudeData));

        longitudeData = Array.makeArray(DataType.DOUBLE, 480, 1.0, 0.75);
        assertFalse(CfGeocodingPart.isGlobalShifted180(longitudeData));

        longitudeData = Array.makeArray(DataType.DOUBLE, 480, -0.375, 0.75);
        assertFalse(CfGeocodingPart.isGlobalShifted180(longitudeData));
    }
}
