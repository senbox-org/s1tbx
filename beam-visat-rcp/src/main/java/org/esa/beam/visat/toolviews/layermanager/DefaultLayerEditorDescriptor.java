package org.esa.beam.visat.toolviews.layermanager;

import com.bc.ceres.core.CoreException;
import com.bc.ceres.core.ExtensionFactory;
import com.bc.ceres.core.ExtensionManager;
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
        ExtensionManager.getInstance().register(layerTypeClass, new MyExtensionFactory());
    }

    private class MyExtensionFactory implements ExtensionFactory<LayerType> {
        @Override
        public <E> E getExtension(LayerType layerType, Class<E> extensionType) {
            if (LayerEditor.class.isAssignableFrom(extensionType)) {
                //noinspection EmptyCatchBlock
                try {
                    //noinspection unchecked
                    return (E) editorClass.newInstance();
                } catch (Throwable e) {
                }
            }
            return null;
        }

        @Override
        public Class<?>[] getExtensionTypes() {
            return new Class<?>[]{LayerEditor.class};
        }
    }
}
