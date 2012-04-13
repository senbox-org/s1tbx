package org.esa.beam.framework.ui.product;

import com.bc.ceres.core.ExtensionFactory;
import com.bc.ceres.glayer.LayerTypeRegistry;
import org.esa.beam.framework.datamodel.VectorDataNode;

/**
 * The {@link ExtensionFactory} that adapts {@link VectorDataNode}s using the {@code FeatureType} "org.esa.beam.TrackPoint"
 * to the special {@link TrackLayerType}.
 * <p/>
 * <i>Note: this is experimental code.</i>
 *
 * @author Norman Fomferra
 * @since BEAM 4.10
 */
public class TrackLayerTypeFactory implements ExtensionFactory {
    @Override
    public Object getExtension(Object object, Class<?> extensionType) {
        VectorDataNode node = (VectorDataNode) object;
        if (TrackLayerType.isTrackPointNode(node)) {
            return LayerTypeRegistry.getLayerType(TrackLayerType.class);
        }
        return null;
    }

    @Override
    public Class<?>[] getExtensionTypes() {
        return new Class<?>[]{VectorDataLayerType.class};
    }
}
