package org.esa.beam.framework.ui.product;

import com.bc.ceres.glayer.Layer;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.layer.LayerEditor;

import javax.swing.JComponent;
import javax.swing.JLabel;

@Deprecated
public class VectorDataLayerEditor implements LayerEditor {

    @Override
    public JComponent createControl(AppContext appContext, Layer layer) {
        if (!(layer instanceof VectorDataLayer)) {
            return null;
        }

        return new JLabel("Figure style editor coming soon...");  // todo
    }

    @Override
    public void updateControl() {
        // todo
    }
}
