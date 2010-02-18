package org.esa.beam.framework.ui.layer;

import com.bc.ceres.glayer.LayerType;

/**
 * A descriptor for a layer editor.
 * <p/>
 * <i>Note: This API is not public yet and may significantly change in the future. Use it at your own risk.</i>
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @since BEAM 4.6
 */
public interface LayerEditorDescriptor {

    /**
     * Gets the {@link LayerType} class for which the {@link LayerEditor} is intended.
     * The corresponding {@code LayerEditor} class can be retrieved by a call to
     * {@link #getLayerEditorClass()}.
     *
     * @return The {@link LayerType} class.
     */
    Class<? extends LayerType> getLayerTypeClass();

    /**
     * Gets the {@link LayerEditor} class, which is intended for the
     * {@link LayerType} class returned by {@link #getLayerTypeClass()}
     *
     * @return The {@link LayerEditor} class
     */
    Class<? extends LayerEditor> getLayerEditorClass();
}
