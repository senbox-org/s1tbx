package org.esa.beam.dataio.chris;

import org.esa.beam.framework.datamodel.PointingFactoryRegistry;
import org.junit.Test;

import static org.junit.Assert.assertNotNull;

public class ChrisPointingFactoryLoadedAsServiceTest {

    @Test
    public void pointingFactoryIsRegistered() {
        assertNotNull(PointingFactoryRegistry.getInstance().getPointingFactory("CHRIS_M1_NR_AC_GC"));
        assertNotNull(PointingFactoryRegistry.getInstance().getPointingFactory("CHRIS_M2_NR_AC_GC"));
        assertNotNull(PointingFactoryRegistry.getInstance().getPointingFactory("CHRIS_M3_NR_AC_GC"));
        assertNotNull(PointingFactoryRegistry.getInstance().getPointingFactory("CHRIS_M4_NR_AC_GC"));
        assertNotNull(PointingFactoryRegistry.getInstance().getPointingFactory("CHRIS_M5_NR_AC_GC"));
    }

}
