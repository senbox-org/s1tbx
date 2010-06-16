package com.bc.ceres.jai;

import com.bc.ceres.core.CoreException;
import com.bc.ceres.core.runtime.Activator;
import com.bc.ceres.core.runtime.ModuleContext;
import com.bc.ceres.jai.operator.DFTConvolveDescriptor;
import com.bc.ceres.jai.operator.ExpressionDescriptor;
import com.bc.ceres.jai.operator.GeneralFilterDescriptor;
import com.bc.ceres.jai.operator.ReinterpretDescriptor;
import com.bc.ceres.jai.operator.XmlDescriptor;
import com.bc.ceres.jai.opimage.DFTConvolveRIF;
import com.bc.ceres.jai.opimage.ExpressionCRIF;
import com.bc.ceres.jai.opimage.GeneralFilterRIF;
import com.bc.ceres.jai.opimage.ReinterpretRIF;
import com.bc.ceres.jai.opimage.XmlRIF;

import javax.media.jai.JAI;
import javax.media.jai.OperationRegistry;
import javax.media.jai.RegistryElementDescriptor;
import javax.media.jai.registry.RenderedRegistryMode;
import java.awt.image.renderable.RenderedImageFactory;

public class ModuleActivator implements Activator {

    @Override
    public void start(ModuleContext moduleContext) throws CoreException {
        register("xml", new XmlDescriptor(), new XmlRIF());
        register("reinterpret", new ReinterpretDescriptor(), new ReinterpretRIF());
        register("generalfilter", new GeneralFilterDescriptor(), new GeneralFilterRIF());
        register("expression", new ExpressionDescriptor(), new ExpressionCRIF());
        register("dftconvolve", new DFTConvolveDescriptor(), new DFTConvolveRIF());
    }

    private void register(String descriptorName, RegistryElementDescriptor descriptor, RenderedImageFactory rif) {
        final OperationRegistry registry = JAI.getDefaultInstance().getOperationRegistry();
        registry.registerDescriptor(descriptor);
        registry.registerFactory(RenderedRegistryMode.MODE_NAME, descriptorName, "com.bc.ceres.jai", rif);
    }

    @Override
    public void stop(ModuleContext moduleContext) throws CoreException {
    }
}
