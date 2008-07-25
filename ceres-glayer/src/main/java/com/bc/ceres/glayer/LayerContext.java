package com.bc.ceres.glayer;

/**
 * TODO - Apidoc
 *
 * @author Norman Fomferra
 * @version $revision$ $date$
 */
public class LayerContext {
    CollectionLayer layer;
    Object coordinateSystem;

    public LayerContext(CollectionLayer layer, Object coordinateSystem) {
        this.layer = layer;
        this.coordinateSystem = coordinateSystem;
    }

    public CollectionLayer getLayer() {
        return layer;
    }

    public Object getCoordinateSystem() {
        return coordinateSystem;
    }

}
