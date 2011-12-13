package org.esa.beam.dataio.landsat;

import com.bc.ceres.core.VirtualDir;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertFalse;
import static org.junit.Assert.*;

public class LandsatTMReaderPluginTest {

    @Test
    public void testGetDescription() {
        final LandsatTMReaderPlugIn plugIn = new LandsatTMReaderPlugIn();

        assertEquals("Landsat 5 TM Product Reader", plugIn.getDescription(Locale.getDefault()));
    }
}
