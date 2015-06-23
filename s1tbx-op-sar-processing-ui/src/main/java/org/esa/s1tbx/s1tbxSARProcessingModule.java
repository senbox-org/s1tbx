package org.esa.s1tbx;

import org.openide.modules.OnStart;

/**
 * Handle OnStart for module
 */
public class s1tbxSARProcessingModule {

    @OnStart
    public static class StartOp implements Runnable {

        @Override
        public void run() {
            S1TBXSetup.installGraphs(this.getClass(), "org/esa/s1tbx/sar/graphs/");
        }
    }
}
