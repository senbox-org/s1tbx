package org.esa.beam.visat.toolviews.layermanager;

import com.bc.ceres.glayer.Layer;

import javax.swing.JLabel;
import javax.swing.JComponent;

import org.esa.beam.visat.VisatApp;
import org.esa.beam.framework.ui.AppContext;

/**
 * The form for an editor for a given layer.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
abstract class LayerEditorForm extends AbstractLayerForm {

    private LayerEditorForm(AppContext appContext) {
        super(appContext);
    }


    public static class Empty extends LayerEditorForm {
        private JLabel label;
        public Empty(AppContext appContext) {
            super(appContext);
            label = new JLabel("No layer selected.");
        }

        @Override
        public JComponent getFormControl() {
            return label;
        }

        @Override
        public void updateFormControl() {
        }
    }

    public static class NonEmpty extends LayerEditorForm {

        final Layer selectedLayer;
        private JLabel label;
        protected int numUpdates;

        public NonEmpty(AppContext appContext, Layer selectedLayer) {
            super(appContext);
            this.selectedLayer = selectedLayer;
            label = new JLabel();
            updateFormControl();
        }

        @Override
        public JComponent getFormControl() {
            return label;
        }

        @Override
        public void updateFormControl() {
            numUpdates++;
            label.setText("Editor " + selectedLayer.getClass().getSimpleName() + " (update " + numUpdates + ")");
        }

    }
}
