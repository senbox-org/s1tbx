package org.esa.beam.visat;

import com.bc.ceres.core.CoreException;
import com.bc.ceres.core.ServiceRegistry;
import com.bc.ceres.core.ServiceRegistryFactory;
import com.bc.ceres.core.runtime.Activator;
import com.bc.ceres.core.runtime.ModuleContext;
import org.esa.beam.BeamCoreActivator;
import org.esa.beam.BeamUiActivator;
import org.esa.beam.framework.ui.application.ToolViewDescriptor;
import org.esa.beam.framework.ui.application.ToolViewDescriptorRegistry;
import org.esa.beam.framework.ui.command.Command;
import org.esa.beam.visat.toolviews.layermanager.LayerEditorDescriptor;
import org.esa.beam.visat.toolviews.layermanager.LayerSourceDescriptor;

import java.util.HashMap;
import java.util.List;
import java.util.Set;

/**
 * The activator for VISAT. This activator processes the extension point <code>plugins</code>.
 */
public class VisatActivator implements Activator, ToolViewDescriptorRegistry {

    private static VisatActivator instance;
    private ModuleContext moduleContext;
    private ServiceRegistry<VisatPlugIn> visatPluginRegistry;
    private HashMap<String, LayerSourceDescriptor> layerSourcesRegistry;

    public VisatActivator() {
    }

    public static VisatActivator getInstance() {
        return instance;
    }

    public ModuleContext getModuleContext() {
        return moduleContext;
    }

    public VisatPlugIn[] getPlugins() {
        Set<VisatPlugIn> visatPlugins = visatPluginRegistry.getServices();
        return visatPlugins.toArray(new VisatPlugIn[visatPlugins.size()]);
    }

    public Command[] getCommands() {
        return BeamUiActivator.getInstance().getCommands();
    }

    public LayerSourceDescriptor[] getLayerSources() {
        return layerSourcesRegistry.values().toArray(
                new LayerSourceDescriptor[layerSourcesRegistry.values().size()]);
    }

    @Override
    public ToolViewDescriptor[] getToolViewDescriptors() {
        return BeamUiActivator.getInstance().getToolViewDescriptors();
    }

    @Override
    public ToolViewDescriptor getToolViewDescriptor(String viewDescriptorId) {
        return BeamUiActivator.getInstance().getToolViewDescriptor(viewDescriptorId);
    }

    @Override
    public void start(ModuleContext moduleContext) throws CoreException {
        instance = this;
        this.moduleContext = moduleContext;
        visatPluginRegistry = ServiceRegistryFactory.getInstance().getServiceRegistry(VisatPlugIn.class);
        registerLayerEditors(this.moduleContext);
        registerLayerSources(this.moduleContext);
    }

    @Override
    public void stop(ModuleContext moduleContext) throws CoreException {
        visatPluginRegistry = null;
        this.moduleContext = null;
        instance = null;
    }

    private void registerLayerEditors(ModuleContext moduleContext) {
        BeamCoreActivator.loadExecutableExtensions(moduleContext,
                                                   "layerEditors",
                                                   "layerEditor",
                                                   LayerEditorDescriptor.class);
    }

    private void registerLayerSources(ModuleContext moduleContext) {
        List<LayerSourceDescriptor> layerSourceListDescriptor =
                BeamCoreActivator.loadExecutableExtensions(moduleContext,
                                                           "layerSources",
                                                           "layerSource",
                                                           LayerSourceDescriptor.class);
        layerSourcesRegistry = new HashMap<String, LayerSourceDescriptor>(2 * layerSourceListDescriptor.size());
        for (LayerSourceDescriptor layerSourceDescriptor : layerSourceListDescriptor) {
            final String id = layerSourceDescriptor.getId();
            final LayerSourceDescriptor existingLayerSourceDescriptor = layerSourcesRegistry.get(id);
            if (existingLayerSourceDescriptor
                != null) {
                moduleContext.getLogger().info(String.format("Layer source [%s] has been redeclared!\n", id));
            }
            layerSourcesRegistry.put(id, layerSourceDescriptor);
        }
    }

}
