package org.esa.beam.dataio.merisl3;

import junit.framework.TestCase;

/**
 * Created by IntelliJ IDEA.
 * User: Norman
 * Date: 26.06.2007
 * Time: 17:05:05
 * To change this template use File | Settings | File Templates.
 */
public class MerisL3FileFilterTest extends TestCase {

    public void testFilter() {
        assertTrue(MerisL3FileFilter.isMerisBinnedL3Name("L3_ENV_MER_ABSD_m__20050201_GLOB_SI_ACR_9277x9277_-90+90+-180+180_0000.nc"));
        assertFalse(MerisL3FileFilter.isMerisBinnedL3Name("L2_ENV_MER_ABSD_m__20050201_GLOB_SI_ACR_9277x9277_-90+90+-180+180_0000.nc"));
        assertFalse(MerisL3FileFilter.isMerisBinnedL3Name("L3_ENV_MER_ABSD_m__20050201_GLOB_SI_ACR_9277x9277_-90+90+-180+180_0000.hdf"));
        assertFalse(MerisL3FileFilter.isMerisBinnedL3Name("L2_ENV_MER_ABSD_m__20050201_XXXX_SI_ACR_9277x9277_-90+90+-180+180_0000.nc"));
    }
}
