package org.esa.beam.dataio.envi;

import org.geotools.referencing.CRS;
import org.junit.Test;
import org.opengis.parameter.GeneralParameterValue;
import org.opengis.parameter.ParameterValue;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.util.List;

import static org.junit.Assert.*;

public class EnviCrsFactoryTest {

    @Test(expected = IllegalArgumentException.class)
    public void testThrowsIllegalArgumentExceptionOnUnregisteredProjection() {
        EnviCrsFactory.createCrs(-11, new double[0], EnviConstants.DATUM_NAME_WGS84, "Meters");
    }

    @Test
    public void testCreateAlbersEqualAreaConic() {
        final double[] parameter = new double[]{
                1.0,    // a    semi major
                2.0,    // b    semi minor
                3.0,    // lat0 latitude of origin
                4.0,    // lon0 central meridian
                5.0,    // x0   false easting
                6.0,    // y0   false northing
                7.0,    // sp1  latitude of intersection 1
                8.0,    // sp2  latitude of intersection 2
        };
        final CoordinateReferenceSystem crs = EnviCrsFactory.createCrs(9, parameter, EnviConstants.DATUM_NAME_WGS84, "Meters");
        final ParameterValueGroup parameterValues = CRS.getMapProjection(crs).getParameterValues();
        final List<GeneralParameterValue> valueList = parameterValues.values();
        double[] actualValues = new double[8];
        for (int i = 0;i < valueList.size(); i++) {
            GeneralParameterValue gPValue = valueList.get(i);
            final ParameterValue<?> param = parameterValues.parameter(gPValue.getDescriptor().getName().getCode());
            actualValues[i] = param.doubleValue();
        }

        ParameterValue<?> param;
        param = parameterValues.parameter("semi_major");
        assertEquals(1.0, param.doubleValue(), 1.0e-6);
        param = parameterValues.parameter("semi_minor");
        assertEquals(2.0, param.doubleValue(), 1.0e-6);
        param = parameterValues.parameter("latitude_of_origin");
        assertEquals(3.0, param.doubleValue(), 1.0e-6);
        param = parameterValues.parameter("central_meridian");
        assertEquals(4.0, param.doubleValue(), 1.0e-6);
        param = parameterValues.parameter("false_easting");
        assertEquals(5.0, param.doubleValue(), 1.0e-6);
        param = parameterValues.parameter("false_northing");
        assertEquals(6.0, param.doubleValue(), 1.0e-6);
        param = parameterValues.parameter("standard_parallel_1");
        assertEquals(7.0, param.doubleValue(), 1.0e-6);
        param = parameterValues.parameter("standard_parallel_2");
        assertEquals(8.0, param.doubleValue(), 1.0e-6);
    }

//
//     public void testCreateAlbersEqualAreaConic() {
//        final double[] parameter = new double[]{
//                1.0,    // a    semi major
//                2.0,    // b    semi minor
//                3.0,    // lat0 latitude of origin
//                4.0,    // lon0 central meridian
//                5.0,    // x0   false easting
//                6.0,    // y0   false northing
//                7.0,    // sp1  latitude of intersection 1
//                8.0,    // sp2  latitude of intersection 2
//        };
//
//        final MapTransform transform = EnviMapTransformFactory.create(9, parameter);
//        assertEquals("Albers Equal Area Conic", transform.getDescriptor().getName());
//        final double[] parameterValues = transform.getParameterValues();
//        assertEquals(9, parameterValues.length);
//        assertEquals(1.0, parameterValues[0]);  // a    semi major
//        assertEquals(2.0, parameterValues[1]);  // b    semi minor
//        assertEquals(3.0, parameterValues[2]);  // lat0 latitude of origin
//        assertEquals(4.0, parameterValues[3]);  // lon0 central meridian
//        assertEquals(7.0, parameterValues[4]);  // sp1  latitude of intersection 1
//        assertEquals(8.0, parameterValues[5]);  // sp2  latitude of intersection 2
//        assertEquals(0.0, parameterValues[6]);  // scale factor - ignored
//        assertEquals(5.0, parameterValues[7]);  // x0   false easting
//        assertEquals(6.0, parameterValues[8]);  // y0   false northing
//    }
}
