package org.esa.beam.visat.toolviews.layermanager;

import com.bc.ceres.core.CoreException;
import com.bc.ceres.core.ExtensionManager;
import com.bc.ceres.core.SingleTypeExtensionFactory;
import com.bc.ceres.core.runtime.ConfigurableExtension;
import com.bc.ceres.core.runtime.ConfigurationElement;
import com.bc.ceres.glayer.LayerType;

/**
 * The default descriptor for a layer editor.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @since BEAM 4.6
 */
@SuppressWarnings({"UnusedDeclaration"})
public class DefaultLayerEditorDescriptor implements LayerEditorDescriptor, ConfigurableExtension {

    private Class<LayerEditor> editorClass;
    private Class<LayerType> layerTypeClass;

    @Override
    public void configure(ConfigurationElement config) throws CoreException {
        ExtensionManager.getInstance().register(layerTypeClass, new LayerEditorFactory());
    }

    private class LayerEditorFactory extends SingleTypeExtensionFactory<LayerType> {

        private LayerEditorFactory() {
            super(LayerEditor.class, editorClass);
        }

        @Override
        protected Object getExtensionImpl(LayerType layerType, Class<?> extensionType) throws Throwable {
            return editorClass.newInstance();
        }
    }
}
