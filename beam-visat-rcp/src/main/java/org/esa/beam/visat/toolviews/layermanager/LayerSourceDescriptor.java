package org.esa.beam.visat.toolviews.layermanager;

import com.bc.ceres.glayer.LayerType;

/**
 * The {@code LayerSourceDescriptor} provides metadata and
 * a factory method for a {@link LayerSource}.
 *
 * @author Marco Peters
 * @author Marco Zühlke
 * @version $ Revision $ $ Date $
 * @since BEAM 4.6
 */
public interface LayerSourceDescriptor {

    /**
     * A unique ID.
     *
     * @return The unique ID.
     */
    String getId();

    /**
     * A human readable name.
     *
     * @return The name.
     */
    String getName();

    /**
     * A text describing what the {@link LayerSource}, created by
     * this {@code LayerSourceDescriptor}, does.
     *
     * @return A description.
     */
    String getDescription();

    /**
     * Creates the {@link LayerSource} which is used in the graphical user interface to
     * add {@link com.bc.ceres.glayer.Layer} to a view.
     *
     * @return The {@link LayerSource}.
     */
    LayerSource createLayerSource();

    /**
     * The {@link LayerType}.
     *
     * @return the type of the layer which is added to a view, or {@code null} if
     *         multiple layers are added.
     */
    LayerType getLayerType();
}
