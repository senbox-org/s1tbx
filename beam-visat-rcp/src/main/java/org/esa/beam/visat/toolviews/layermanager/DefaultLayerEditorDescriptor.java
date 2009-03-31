package org.esa.beam.visat.toolviews.layermanager;

import com.bc.ceres.core.Assert;
import com.bc.ceres.core.CoreException;
import com.bc.ceres.core.ExtensionFactory;
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
    private Class<LayerType> layerTypeClass;
    private Class<LayerEditor> editorClass;

    public DefaultLayerEditorDescriptor(Class<LayerType> layerTypeClass, Class<LayerEditor> editorClass) {
        Assert.notNull(layerTypeClass, "layerTypeClass");
        Assert.notNull(editorClass, "editorClass");
        this.layerTypeClass = layerTypeClass;
        this.editorClass = editorClass;
    }

    @Override
    public void configure(ConfigurationElement config) throws CoreException {
        ExtensionManager.getInstance().register(layerTypeClass, createExtensionFactory());
    }

    ExtensionFactory createExtensionFactory() {
        return new SingleTypeExtensionFactory<LayerType, LayerEditor>(LayerEditor.class, editorClass);
    }

}
