package com.bc.ceres;

import com.bc.ceres.core.CoreException;
import com.bc.ceres.core.runtime.Activator;
import com.bc.ceres.core.runtime.ModuleContext;

import javax.media.jai.JAI;
import java.io.IOException;


public class CeresJaiActivator implements Activator {

    @Override
    public void start(ModuleContext moduleContext) throws CoreException {
        try {
            JAI.getDefaultInstance().getOperationRegistry().updateFromStream(
                    CeresJaiActivator.class.getResourceAsStream("/META-INF/registryFile.jai"));
        } catch (IOException e) {
            throw new CoreException(e);
        }
    }

    @Override
    public void stop(ModuleContext moduleContext) throws CoreException {
    }
}
