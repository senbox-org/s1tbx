package com.bc.ceres.glayer;

/**
 * The context in which layers are managed, created and/or rendered, e.g. the view used to
 * display multiple layers.
 * <p/>
 * By default, the context is composed of the root layer and a common coordinate reference system (CRS)
 * shared by all layers, this is the root layer and all of its child layers and so forth.
 * <p/>
 * Instances of this interface are passed to the several methods of {@link LayerType}
 * in order to provide special layer type implementations with access to application specific services.
 * Therefore this interface is intended to be implemented by clients.
 * Since implementations of this interface are application-specific, there is no default implementation.
 *
 * @author Norman Fomferra
 */
public interface LayerContext {
    /**
     * The coordinate reference system (CRS) used by all the layers in this context.
     * The CRS defines the model coordinate system and may be used by a
     * {@link com.bc.ceres.glayer.LayerType} in order to decide whether
     * it is can create new layer instance for this context.
     *
     * @return The CRS. May be {@code null}, if not used.
     */
    Object getCoordinateReferenceSystem();

    /**
     * @return The root layer.
     */
    Layer getRootLayer();
}
