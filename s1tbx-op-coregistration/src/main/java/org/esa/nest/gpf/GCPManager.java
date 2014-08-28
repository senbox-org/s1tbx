package org.esa.nest.gpf;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Placemark;
import org.esa.beam.framework.datamodel.ProductNodeGroup;

import java.util.HashMap;
import java.util.Map;

/**
 * Temporary solution to removing band GCPs from product.
 */
public class GCPManager {

    private static GCPManager _instance = null;

    private final Map<Band, ProductNodeGroup<Placemark>> bandGCPGroup = new HashMap<>();

    private GCPManager() {

    }

    public static GCPManager instance() {
        if(_instance == null) {
            _instance = new GCPManager();
        }
        return _instance;
    }

    public ProductNodeGroup<Placemark> getGcpGroup(final Band band) {
        ProductNodeGroup<Placemark> gcpGroup = bandGCPGroup.get(band);
        if(gcpGroup == null) {
            gcpGroup = new ProductNodeGroup<Placemark>(band.getProduct(),
                    "ground_control_points", true);
            bandGCPGroup.put(band, gcpGroup);
        }
        return gcpGroup;
    }
}
