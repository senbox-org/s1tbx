package org.esa.beam.visat;

import com.bc.ceres.core.CoreException;
import com.bc.ceres.core.ServiceRegistry;
import com.bc.ceres.core.ServiceRegistryManager;
import com.bc.ceres.core.runtime.Activator;
import com.bc.ceres.core.runtime.ModuleContext;
import org.esa.beam.BeamUiActivator;
import org.esa.beam.framework.ui.application.ToolViewDescriptor;
import org.esa.beam.framework.ui.application.ToolViewDescriptorRegistry;
import org.esa.beam.framework.ui.command.Command;

import java.util.Set;

/**
 * The activator for VISAT. This activator processes the extension point <code>plugins</code>.
 */
public class VisatActivator implements Activator, ToolViewDescriptorRegistry {

    private static VisatActivator instance;
    private ModuleContext moduleContext;
    private ServiceRegistry<VisatPlugIn> visatPluginRegistry;

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
        visatPluginRegistry = ServiceRegistryManager.getInstance().getServiceRegistry(VisatPlugIn.class);
    }

    @Override
    public void stop(ModuleContext moduleContext) throws CoreException {
        visatPluginRegistry = null;
        this.moduleContext = null;
        instance = null;
    }

}
