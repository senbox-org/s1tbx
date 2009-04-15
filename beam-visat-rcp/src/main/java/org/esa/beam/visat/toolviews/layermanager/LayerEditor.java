package org.esa.beam.visat.toolviews.layermanager;

import com.bc.ceres.glayer.Layer;

import javax.swing.JComponent;

/**
 * An editor for a specific layer type.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @since BEAM 4.6
 */
public interface LayerEditor {

    /**
     * Creates the control for the user interface which is displayed
     * in the Layer Editor Toolview.
     *
     * @param layer The layer to create the control for.
     *
     * @return The control.
     */
    JComponent createControl(Layer layer);

    /**
     * It is called whenever the control must be updated.
     */
    void updateControl();
}
