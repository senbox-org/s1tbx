package org.esa.snap.core.gpf.operators.tooladapter;

import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.OperatorSpiRegistry;
import org.esa.snap.runtime.Activator;

import java.util.Collection;

/**
 * Registers the installed tool adapters.
 *
 * @author Cosmin Cara
 */
public class ToolAdapterActivator implements Activator {

    @Override
    public void start() {
        OperatorSpiRegistry spiRegistry = GPF.getDefaultInstance().getOperatorSpiRegistry();
        if (spiRegistry != null) {
            Collection<OperatorSpi> operatorSpis = spiRegistry.getOperatorSpis();
            if (operatorSpis != null) {
                final Collection<ToolAdapterOpSpi> toolAdapterOpSpis = ToolAdapterIO.searchAndRegisterAdapters();
                operatorSpis.addAll(toolAdapterOpSpis);
            }
        }
    }

    @Override
    public void stop() {

    }
}
