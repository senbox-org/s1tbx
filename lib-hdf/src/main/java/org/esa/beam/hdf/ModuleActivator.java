package org.esa.beam.hdf;

import com.bc.ceres.core.runtime.Activator;
import com.bc.ceres.core.runtime.ModuleContext;
import com.bc.ceres.core.CoreException;


public class ModuleActivator implements Activator {

    public void start(ModuleContext moduleContext) throws CoreException {
        System.out.println("HDF Module started!!!");
    }

    public void stop(ModuleContext moduleContext) throws CoreException {
        System.out.println("HDF Module stopped!!!");
    }
}
