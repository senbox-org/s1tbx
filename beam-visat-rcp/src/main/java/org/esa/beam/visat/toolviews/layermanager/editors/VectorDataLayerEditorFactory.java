package org.esa.beam.visat.toolviews.layermanager.editors;

import com.bc.ceres.core.ExtensionFactory;
import org.esa.beam.framework.ui.layer.AbstractLayerEditor;
import org.esa.beam.framework.ui.layer.LayerEditor;
import org.esa.beam.framework.ui.product.VectorDataLayer;
import sun.plugin.javascript.navig4.Layer;

import javax.swing.JComponent;
import javax.swing.JLabel;

/**
 * todo - add api doc
 *
 * @author Norman Fomferra
 */
public class VectorDataLayerEditorFactory implements ExtensionFactory {
    @Override
    public Object getExtension(Object object, Class<?> extensionType) {
        if (object instanceof VectorDataLayer) {
            VectorDataLayer vectorDataLayer = (VectorDataLayer) object;
            String featureTypeName = vectorDataLayer.getVectorDataNode().getFeatureType().getTypeName();
            if (featureTypeName.equals("Placemark")) {
                return new AbstractLayerEditor() {
                    @Override
                    protected JComponent createControl() {
                        return new JLabel("I am an editor for features of type Placemark");
                    }
                };
            } else {
                return new VectorDataLayerEditor();
            }
        }
        return null;
    }

    @Override
    public Class<?>[] getExtensionTypes() {
        return new Class<?>[ ] {LayerEditor.class};
    }
}
