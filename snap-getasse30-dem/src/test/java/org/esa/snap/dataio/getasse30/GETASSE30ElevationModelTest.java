package org.esa.snap.dataio.getasse30;

import org.esa.snap.core.dataop.resamp.Resampling;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

/**
 * @author Marco Peters
 */
public class GETASSE30ElevationModelTest {

    @Test
    public void testFilenameCreation() throws IOException {
        final GETASSE30ElevationModel model = new GETASSE30ElevationModel(new GETASSE30ElevationModelDescriptor(),
                                                                                            Resampling.NEAREST_NEIGHBOUR);
        assertEquals("45S004W.GETASSE30", model.createTileFilename(-45, -4));
        assertEquals("45S004E.GETASSE30", model.createTileFilename(-45, +4));
        assertEquals("45N004W.GETASSE30", model.createTileFilename(+45, -4));
        assertEquals("45N004E.GETASSE30", model.createTileFilename(+45, +4));

        assertEquals("05S045W.GETASSE30", model.createTileFilename(-5, -45));
        assertEquals("05S045E.GETASSE30", model.createTileFilename(-5, +45));
        assertEquals("05N045W.GETASSE30", model.createTileFilename(+5, -45));
        assertEquals("05N045E.GETASSE30", model.createTileFilename(+5, +45));

        assertEquals("90S180W.GETASSE30", model.createTileFilename(-90, -180));
        assertEquals("90S180E.GETASSE30", model.createTileFilename(-90, +180));
        assertEquals("90N180W.GETASSE30", model.createTileFilename(+90, -180));
        assertEquals("90N180E.GETASSE30", model.createTileFilename(+90, +180));
    }

}