package org.esa.beam.visat.toolviews.layermanager;

import com.bc.ceres.glayer.LayerType;

/**
 * A descriptor for a layer editor.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @since BEAM 4.6
 */
public interface LayerEditorDescriptor {
    Class<? extends LayerType> getLayerTypeClass();

    Class<? extends LayerEditor> getLayerEditorClass();
}
