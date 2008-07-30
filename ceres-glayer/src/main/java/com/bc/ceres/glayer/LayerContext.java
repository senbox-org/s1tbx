package com.bc.ceres.glayer;

/**
 * TODO - Apidoc
 *
 * @author Norman Fomferra
 * @version $revision$ $date$
 */
public class LayerContext {

    Layer layer;
    Object coordinateSystem;

    public LayerContext(Layer layer, Object coordinateSystem) {
        this.layer = layer;
        this.coordinateSystem = coordinateSystem;
    }

    public Layer getLayer() {
        return layer;
    }

    public Object getCoordinateSystem() {
        return coordinateSystem;
    }

}
