package org.esa.beam.dataio.envi;

import org.esa.beam.framework.dataop.maptransf.MapProjectionRegistry;
import org.esa.beam.framework.dataop.maptransf.MapTransform;
import org.esa.beam.framework.dataop.maptransf.MapTransformDescriptor;

import java.util.HashMap;

class EnviMapTransformFactory {

    static MapTransform create(int projectionNumber, double[] parameter) {
        if (projectionNameMap == null) {
            init();
        }

        final String beamName = projectionNameMap.get(projectionNumber);
        if (beamName == null) {
            throw new IllegalArgumentException("Projection not implemented: " + projectionNumber);
        }
        final MapTransformDescriptor descriptor = MapProjectionRegistry.getDescriptor(beamName);
        final ParameterMap map = parameterMaps.get(projectionNumber);
        if (map == null) {
            throw new IllegalStateException("Parameter map not found: " + projectionNumber);
        }
        return descriptor.createTransform(map.transform(parameter));
    }

    ///////////////////////////////////////////////////////////////////////////
    /////// END OF PUBLIC
    ///////////////////////////////////////////////////////////////////////////

    private static HashMap<Integer, String> projectionNameMap;
    private static HashMap<Integer, ParameterMap> parameterMaps;

    private static void init() {
        projectionNameMap = new HashMap<Integer, String>();
        parameterMaps = new HashMap<Integer, ParameterMap>();

        projectionNameMap.put(9, "Albers_Equal_Area_Conic");
        ParameterMap parameterMap = new ParameterMap(9, new int[]{0, 1, 2, 3, 7, 8, 4, 5});
        parameterMaps.put(9, parameterMap);
    }

}
