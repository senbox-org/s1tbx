package com.bc.ceres.glayer;

public interface LayerContext {
    /**
     * The coordinate reference system (CRS) used by all the layers in this context.
     * May be used by a {@link com.bc.ceres.glayer.LayerType} in order to decide whether
     * the source can provide a new layer instance for this context.
     * @return The CRS. May be {@code null}.
     */
    Object getCoordinateReferenceSystem();

    /**
     * @return The root layer.
     */
    Layer getRootLayer();
}
