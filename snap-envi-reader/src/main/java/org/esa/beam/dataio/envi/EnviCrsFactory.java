package org.esa.beam.dataio.envi;

import org.esa.beam.util.Debug;
import org.geotools.referencing.ReferencingFactoryFinder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.referencing.crs.DefaultProjectedCRS;
import org.geotools.referencing.cs.DefaultCartesianCS;
import org.geotools.referencing.cs.DefaultEllipsoidalCS;
import org.geotools.referencing.datum.DefaultEllipsoid;
import org.geotools.referencing.datum.DefaultGeodeticDatum;
import org.geotools.referencing.datum.DefaultPrimeMeridian;
import org.opengis.parameter.GeneralParameterDescriptor;
import org.opengis.parameter.ParameterValueGroup;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;
import org.opengis.referencing.operation.MathTransformFactory;

import javax.measure.unit.SI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EnviCrsFactory {

    private static final HashMap<Integer, int[]> axisMap;
    private static final HashMap<Integer, String> projectionMethodMap;
    private static final HashMap<Integer, Map<String, Integer>> projectionParameterMaps;

    static {
        axisMap = new HashMap<Integer, int[]>();
        projectionMethodMap = new HashMap<Integer, String>();
        projectionParameterMaps = new HashMap<Integer, Map<String, Integer>>();

        projectionMethodMap.put(9, "EPSG:9822");
        final HashMap<String, Integer> params9 = new HashMap<String, Integer>();
        params9.put("semi_major", 0);
        params9.put("semi_minor", 1);
        params9.put("latitude_of_origin", 2);
        params9.put("central_meridian", 3);
        params9.put("standard_parallel_1", 6);
        params9.put("standard_parallel_2", 7);
        params9.put("false_easting", 4);
        params9.put("false_northing", 5);
        projectionParameterMaps.put(9, params9);
        axisMap.put(9, new int[]{0,1});

        projectionMethodMap.put(16, "OGC:Sinusoidal");
        final HashMap<String, Integer> params16 = new HashMap<String, Integer>();
        params16.put("semi_major", 0);
        params16.put("semi_minor", 0);
        params16.put("central_meridian", 1);
        params16.put("false_easting", 2);
        params16.put("false_northing", 3);
        projectionParameterMaps.put(16, params16);
        axisMap.put(16, new int[]{0,0});
    }

    private EnviCrsFactory() {
    }


    public static CoordinateReferenceSystem createCrs(int enviProjectionNumber, double[] parameter, String datumString, String unitString) {
        if (projectionMethodMap.containsKey(enviProjectionNumber)) {
            String method = projectionMethodMap.get(enviProjectionNumber);
            try {
                final MathTransformFactory transformFactory = ReferencingFactoryFinder.getMathTransformFactory(null);
                final ParameterValueGroup parameters = transformFactory.getDefaultParameters(method);
                final Map<String, Integer> map = projectionParameterMaps.get(enviProjectionNumber);
                final List<GeneralParameterDescriptor> parameterDescriptors = parameters.getDescriptor().descriptors();
                for (GeneralParameterDescriptor parameterDescriptor : parameterDescriptors) {
                    final String paramName = parameterDescriptor.getName().getCode();
                    if (map.containsKey(paramName)) {
                        final Integer index = map.get(paramName);
                        parameters.parameter(paramName).setValue(parameter[index]);
                    }
                }
                MathTransform mathTransform = transformFactory.createParameterizedTransform(parameters);
                DefaultGeographicCRS base;
                if (EnviConstants.DATUM_NAME_WGS84.equals(datumString)) {
                    base = DefaultGeographicCRS.WGS84;
                } else {
                    int[] indices = axisMap.get(enviProjectionNumber);
                    if (indices != null) {
                        double semiMajorAxis = parameter[indices[0]];
                        double semiMinorAxis = parameter[indices[1]];
                        DefaultEllipsoid ellipsoid = DefaultEllipsoid.createEllipsoid("SPHERE", semiMajorAxis, semiMinorAxis, SI.METER);
                        DefaultGeodeticDatum datum = new DefaultGeodeticDatum(datumString, ellipsoid, DefaultPrimeMeridian.GREENWICH);
                        base = new DefaultGeographicCRS(datum, DefaultEllipsoidalCS.GEODETIC_2D);
                    } else {
                        base = DefaultGeographicCRS.WGS84;
                    }
                }

                return new DefaultProjectedCRS(parameters.getDescriptor().getName().getCode() + " / " + datumString,
                                               base, mathTransform, DefaultCartesianCS.PROJECTED);
            } catch (FactoryException fe) {
                Debug.trace(fe);
            }
        }
        throw new IllegalArgumentException(String.format("Unknown ENVI projection number: %d", enviProjectionNumber));
    }

}
