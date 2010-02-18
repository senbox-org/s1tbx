package org.esa.beam.framework.ui.layer;

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
 * <p/>
 * <i>Note: This API is not public yet and may significantly change in the future. Use it at your own risk.</i>
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @since BEAM 4.6
 */
@SuppressWarnings({"UnusedDeclaration"})
public class DefaultLayerEditorDescriptor implements LayerEditorDescriptor, ConfigurableExtension {
    private Class<? extends LayerType> layerTypeClass;
    private Class<? extends LayerEditor> layerEditorClass;

    /**
     * Constructor used by Ceres runtime for creating a dedicated {@link ConfigurationElement}s for this
     * {@code LayerEditorDescriptor}.
     */
    public DefaultLayerEditorDescriptor() {
    }

    /**
     * Used for unit testing only.
     *
     * @param layerTypeClass   The layer type.
     * @param layerEditorClass The layer editor.
     */
    DefaultLayerEditorDescriptor(Class<? extends LayerType> layerTypeClass, Class<? extends LayerEditor> layerEditorClass) {
        Assert.notNull(layerTypeClass, "layerTypeClass");
        Assert.notNull(layerEditorClass, "layerEditorClass");
        this.layerTypeClass = layerTypeClass;
        this.layerEditorClass = layerEditorClass;
    }

    @Override
    public Class<? extends LayerType> getLayerTypeClass() {
        return layerTypeClass;
    }

    @Override
    public Class<? extends LayerEditor> getLayerEditorClass() {
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
