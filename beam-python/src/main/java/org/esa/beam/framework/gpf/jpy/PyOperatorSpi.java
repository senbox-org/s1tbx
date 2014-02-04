package org.esa.beam.framework.gpf.jpy;

import com.bc.ceres.core.runtime.RuntimeContext;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.util.logging.BeamLogManager;

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;

/**
* @author Norman Fomferra
*/
public class PyOperatorSpi extends OperatorSpi {

    public static final String PY_OP_RESOURCE_NAME = "META-INF/services/beampy-operators";

    public PyOperatorSpi() {
        super(PyOperator.class);

        try {
            BeamLogManager.getSystemLogger().info("Scanning for Python extensions " + PY_OP_RESOURCE_NAME + "...");
            Enumeration<URL> resources = RuntimeContext.getResources(PY_OP_RESOURCE_NAME);
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                BeamLogManager.getSystemLogger().info("  >>> found " + url);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


}
