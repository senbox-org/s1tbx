package org.esa.snap.raster.gpf.masks;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.util.DummyProductBuilder;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * @author Marco Peters
 */
public class CreateLandMaskOpTest {

    @Test
    public void testWithPixelGeoCodingProduct() throws Exception {
        final Product source = new DummyProductBuilder()
                .size(DummyProductBuilder.Size.SMALL)
                .gc(DummyProductBuilder.GC.PER_PIXEL)
                .gp(DummyProductBuilder.GP.NULL_MERIDIAN)
                .sizeOcc(DummyProductBuilder.SizeOcc.SINGLE)
                .gcOcc(DummyProductBuilder.GCOcc.UNIQUE)
                .create();

        Map<String, Object> params = new HashMap<>();
        Product target = GPF.createProduct("Land-Sea-Mask", params, source);
        assertNotNull(target);
        List<String> bandNames = Arrays.asList(target.getBandNames());
        assertTrue(bandNames.containsAll(Arrays.asList("band_a", "band_b", "band_c", "latitude", "longitude")));
        assertNotNull(target.getSceneGeoCoding());
    }
}