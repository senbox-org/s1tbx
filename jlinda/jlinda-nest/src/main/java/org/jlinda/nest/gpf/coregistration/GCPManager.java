package org.jlinda.nest.gpf.coregistration;

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

    private final Map<String, ProductNodeGroup<Placemark>> bandGCPGroup = new HashMap<>();

    private GCPManager() {

    }

    private String createKey(final Band band) {
        return band.getProduct().getName() +'_'+band.getName();
    }

    public static GCPManager instance() {
        if(_instance == null) {
            _instance = new GCPManager();
        }
        return _instance;
    }

    public ProductNodeGroup<Placemark> getGcpGroup(final Band band) {
        ProductNodeGroup<Placemark> gcpGroup = bandGCPGroup.get(createKey(band));
        if(gcpGroup == null) {
            gcpGroup = new ProductNodeGroup<>(band.getProduct(),
                    "ground_control_points", true);
            bandGCPGroup.put(createKey(band), gcpGroup);
        }
        return gcpGroup;
    }

    public void removeGcpGroup(final Band band) {
        bandGCPGroup.remove(createKey(band));
    }

    public void removeAllGcpGroups() {
        bandGCPGroup.clear();
    }
}
