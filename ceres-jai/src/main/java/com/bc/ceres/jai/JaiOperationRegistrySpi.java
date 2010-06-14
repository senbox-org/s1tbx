package com.bc.ceres.jai;

import javax.media.jai.OperationRegistry;
import javax.media.jai.OperationRegistrySpi;
import java.io.IOException;

public class JaiOperationRegistrySpi implements OperationRegistrySpi {

    public JaiOperationRegistrySpi() {
    }

    @Override
    public void updateRegistry(OperationRegistry operationRegistry) {
        try {
            operationRegistry.updateFromStream(getClass().getResourceAsStream("/META-INF/registryFile.jai"));
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
