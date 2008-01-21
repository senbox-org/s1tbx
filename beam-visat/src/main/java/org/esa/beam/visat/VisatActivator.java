package org.esa.beam.visat;

import com.bc.ceres.core.CoreException;
import com.bc.ceres.core.ServiceRegistry;
import com.bc.ceres.core.ServiceRegistryFactory;
import com.bc.ceres.core.runtime.Activator;
import com.bc.ceres.core.runtime.ModuleContext;
import org.esa.beam.BeamCoreActivator;
import org.esa.beam.framework.ui.application.ToolViewDescriptor;
import org.esa.beam.framework.ui.application.ToolViewDescriptorRegistry;
import org.esa.beam.framework.ui.command.Command;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The activator for VISAT. This activator processes the extension point <code>plugins</code>.
 */
public class VisatActivator implements Activator, ToolViewDescriptorRegistry {

    private static VisatActivator instance;
    private ModuleContext moduleContext;
    private List<Command> commandRegistry;
    private Map<String, ToolViewDescriptor> toolViewDescriptorRegistry;
    private ServiceRegistry<VisatPlugIn> visatPluginRegistry;

    public VisatActivator() {
    }

    public ModuleContext getModuleContext() {
        return moduleContext;
    }

    public VisatPlugIn[] getPlugins() {
        Set<VisatPlugIn> visatPlugins = visatPluginRegistry.getServices();
        return visatPlugins.toArray(new VisatPlugIn[visatPlugins.size()]);
    }

    public Command[] getCommands() {
        return commandRegistry.toArray(new Command[commandRegistry.size()]);
    }

    public ToolViewDescriptor[] getToolViewDescriptors() {
        return toolViewDescriptorRegistry.values().toArray(new ToolViewDescriptor[toolViewDescriptorRegistry.values().size()]);
    }

    public ToolViewDescriptor getToolViewDescriptor(String viewDescriptorId) {
        return toolViewDescriptorRegistry.get(viewDescriptorId);
    }

    public static VisatActivator getInstance() {
        return instance;
    }

    public void start(ModuleContext moduleContext) throws CoreException {
        instance = this;
        this.moduleContext = moduleContext;

        ServiceRegistryFactory factory = ServiceRegistryFactory.getInstance();
        visatPluginRegistry = factory.getServiceRegistry(VisatPlugIn.class);

        List<ToolViewDescriptor> toolViewDescriptors = BeamCoreActivator.loadExecutableExtensions(moduleContext,
                                                                                                  "toolViews",
                                                                                                  "toolView",
                                                                                                  ToolViewDescriptor.class);
        toolViewDescriptorRegistry = new HashMap<String, ToolViewDescriptor>(2 * toolViewDescriptors.size());
        for (ToolViewDescriptor toolViewDescriptor : toolViewDescriptors) {
            toolViewDescriptorRegistry.put(toolViewDescriptor.getId(), toolViewDescriptor);
        }

        commandRegistry = BeamCoreActivator.loadExecutableExtensions(moduleContext,
                                                                     "actions",
                                                                     "action",
                                                                     Command.class);
    }


    public void stop(ModuleContext moduleContext) throws CoreException {
        this.moduleContext = null;
        this.commandRegistry.clear();
        this.toolViewDescriptorRegistry.clear();
    }
}
