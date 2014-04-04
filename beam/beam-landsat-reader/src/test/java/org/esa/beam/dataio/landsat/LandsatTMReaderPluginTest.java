package org.esa.beam.dataio.landsat;

import org.junit.Test;

import java.util.Locale;

import static org.junit.Assert.*;


public class LandsatTMReaderPluginTest {

    @Test
    public void testGetDescription() {
        final LandsatTMReaderPlugIn plugIn = new LandsatTMReaderPlugIn();

        assertEquals("Landsat 5 TM Product Reader", plugIn.getDescription(Locale.getDefault()));
    }
}
