package org.esa.beam.framework.ui.product;

import com.bc.ceres.glayer.LayerType;
import com.bc.ceres.glayer.LayerTypeRegistry;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class VectorDataCollectionLayerTypeTest {

    @Test
    public void registry() {
        final VectorDataCollectionLayerType byClass = LayerTypeRegistry.getLayerType(
                VectorDataCollectionLayerType.class);
        final LayerType byName = LayerTypeRegistry.getLayerType(VectorDataCollectionLayerType.class.getName());
        assertTrue(byClass != null);
        assertSame(byClass, byName);
    }
}
