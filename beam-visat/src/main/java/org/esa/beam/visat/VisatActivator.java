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
import org.esa.beam.util.Debug;
import org.esa.beam.util.SystemUtils;

import java.util.ArrayList;
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
    private List<VisatPlugIn> pluginRegistry;
    private List<Command> commandRegistry;
    private Map<String, ToolViewDescriptor> toolViewDescriptorRegistry;

    public VisatActivator() {
    }

    public ModuleContext getModuleContext() {
        return moduleContext;
    }

    public VisatPlugIn[] getPlugins() {
        return pluginRegistry.toArray(new VisatPlugIn[0]);
    }

    public Command[] getCommands() {
        return commandRegistry.toArray(new Command[commandRegistry.size()]);
    }

    public ToolViewDescriptor[] getToolViewDescriptors() {
        return toolViewDescriptorRegistry.values().toArray(new ToolViewDescriptor[0]);
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
        ServiceRegistry<VisatPlugIn> visatPluginRegistry = factory.getServiceRegistry(VisatPlugIn.class);
        Iterable<VisatPlugIn> iterable = SystemUtils.loadServices(VisatPlugIn.class, getClass().getClassLoader());
        for (VisatPlugIn service : iterable) {
            visatPluginRegistry.addService(service);
        }
        Set<VisatPlugIn> visatPlugins = visatPluginRegistry.getServices();
        pluginRegistry = new ArrayList<VisatPlugIn>();
        Debug.trace("registering visat plugins...");
        for (VisatPlugIn plugin : visatPlugins) {
            pluginRegistry.add(plugin);
            Debug.trace("visat plugin registered: " + plugin.getClass().getName());
        }


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
        this.pluginRegistry.clear();
    }
}
