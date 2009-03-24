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
    JComponent createControl();

    void updateControl(Layer selectedLayer);
}
