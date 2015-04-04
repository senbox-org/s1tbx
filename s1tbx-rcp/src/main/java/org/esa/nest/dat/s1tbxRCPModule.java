package org.esa.nest.dat;

import org.esa.s1tbx.S1TBXSetup;
import org.openide.modules.OnStart;

/**
 * Handle OnStart for module
 */
public class s1tbxRCPModule {

    @OnStart
    public static class StartOp implements Runnable {

        @Override
        public void run() {
            S1TBXSetup.installColorPalettes(this.getClass());
        }
    }
}
