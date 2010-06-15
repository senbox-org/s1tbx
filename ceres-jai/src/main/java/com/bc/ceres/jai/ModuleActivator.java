package com.bc.ceres.jai;

import com.bc.ceres.core.CoreException;
import com.bc.ceres.core.runtime.Activator;
import com.bc.ceres.core.runtime.ModuleContext;
import com.bc.ceres.jai.operator.ReinterpretDescriptor;
import com.bc.ceres.jai.opimage.ReinterpretRIF;

import javax.media.jai.JAI;
import javax.media.jai.OperationRegistry;
import javax.media.jai.registry.RenderedRegistryMode;
import java.util.logging.Level;

public class ModuleActivator implements Activator {

    private static final String META_INF_REGISTRY_FILE_JAI = "/META-INF/registryFile.jai";

    @Override
    public void start(ModuleContext moduleContext) throws CoreException {
        final JAI instance = JAI.getDefaultInstance();
        final OperationRegistry operationRegistry = instance.getOperationRegistry();
        operationRegistry.registerDescriptor(new ReinterpretDescriptor());
        operationRegistry.registerDescriptor(new ReinterpretDescriptor());
        operationRegistry.registerFactory(RenderedRegistryMode.MODE_NAME, "reinterpret", "com.bc.ceres.jai",
                                          new ReinterpretRIF());

        final String[] descriptorNames = operationRegistry.getDescriptorNames("rendered");
        if (moduleContext.getLogger().getLevel() == Level.FINE) {
            for (String descriptorName : descriptorNames) {
                moduleContext.getLogger().fine(descriptorName);
            }
        }
    }

    @Override
    public void stop(ModuleContext moduleContext) throws CoreException {
    }
}
