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
    private Class<LayerEditor> layerEditorClass;

    public DefaultLayerEditorDescriptor(Class<LayerType> layerTypeClass, Class<LayerEditor> layerEditorClass) {
        Assert.notNull(layerTypeClass, "layerTypeClass");
        Assert.notNull(layerEditorClass, "layerEditorClass");
        this.layerTypeClass = layerTypeClass;
        this.layerEditorClass = layerEditorClass;
    }

    @Override
    public Class<LayerType> getLayerTypeClass() {
        return layerTypeClass;
    }

    @Override
    public Class<LayerEditor> getLayerEditorClass() {
        return layerEditorClass;
    }

    @Override
    public void configure(ConfigurationElement config) throws CoreException {
        ExtensionManager.getInstance().register(layerTypeClass, createExtensionFactory());
    }

    ExtensionFactory createExtensionFactory() {
        return new SingleTypeExtensionFactory<LayerType, LayerEditor>(LayerEditor.class, layerEditorClass);
    }

}
