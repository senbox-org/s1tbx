package org.esa.beam.visat.toolviews.layermanager.editors;

import com.bc.ceres.core.ExtensionFactory;
import org.esa.beam.framework.ui.layer.LayerEditor;
import org.esa.beam.framework.ui.product.VectorDataLayer;

/**
 * Experimental code: A factory that creates a specific {@link LayerEditor} for a given {@link VectorDataLayer}.
 *
 * @author Norman Fomferra
 * @since BEAM 4.10
 */
public class VectorDataLayerEditorFactory implements ExtensionFactory {
    @Override
    public LayerEditor getExtension(Object object, Class<?> extensionType) {
        if (object instanceof VectorDataLayer) {
            // todo - special TrackPointEditor: work on this after 4.10 (nf)
            /*
            VectorDataLayer vectorDataLayer = (VectorDataLayer) object;
            String featureTypeName = vectorDataLayer.getVectorDataNode().getFeatureType().getTypeName();
            if (TrackLayerType.isTrackPointNode(vectorDataLayer.getVectorDataNode())) {
                return new TrackLayerEditor();
            } else {
                return new VectorDataLayerEditor();
            }
            */
            return new VectorDataLayerEditor();
        }
        return null;
    }

    @Override
    public Class<?>[] getExtensionTypes() {
        return new Class<?>[]{LayerEditor.class};
    }

}
