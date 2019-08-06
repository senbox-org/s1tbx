
package org.csa.rstb.soilmoisture;

import org.esa.snap.engine_utilities.util.ResourceUtils;
import org.openide.modules.OnStart;

/**
 * Handle OnStart for module
 */
public class rstbSoilMoistureModule {

    @OnStart
    public static class StartOp implements Runnable {

        @Override
        public void run() {
            ResourceUtils.installGraphs(this.getClass(), "org/csa/rstb/soilmoisture/graphs/");
        }
    }
}
