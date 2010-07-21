/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

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
