package org.esa.beam.framework.ui.product;

import com.bc.ceres.glayer.LayerType;
import com.bc.ceres.glayer.LayerTypeRegistry;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class VectorDataLayerTypeTest {

    @Test
    public void registry() {
        final VectorDataLayerType byClass = LayerTypeRegistry.getLayerType(VectorDataLayerType.class);
        final LayerType byName = LayerTypeRegistry.getLayerType(VectorDataLayerType.class.getName());
        assertTrue(byClass != null);
        assertSame(byClass, byName);
    }
}
