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

package org.esa.beam.framework.ui.layer;

import com.bc.ceres.core.ExtensionFactory;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerType;
import com.bc.ceres.glayer.LayerTypeRegistry;
import junit.framework.TestCase;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.glayer.GraticuleLayerType;

import javax.swing.JComponent;

public class DefaultLayerEditorDescriptorTest extends TestCase {

    public void testTypesAndFactory() {
        Class<GraticuleLayerType> layerTypeClass = GraticuleLayerType.class;
        Class<GraticuleLayerEditor> layerEditorClass = GraticuleLayerEditor.class;

        DefaultLayerEditorDescriptor descriptor = new DefaultLayerEditorDescriptor(layerTypeClass, layerEditorClass);
        assertSame(layerTypeClass, descriptor.getLayerTypeClass());
        assertSame(layerEditorClass, descriptor.getLayerEditorClass());

        ExtensionFactory factory = descriptor.createExtensionFactory();
        assertNotNull(factory);

        LayerType layerType = LayerTypeRegistry.getLayerType(layerTypeClass.getName());
        assertNotNull(layerType);

        Object extension = factory.getExtension(layerType, LayerEditor.class);
        assertNotNull(extension);
        assertTrue(extension instanceof LayerEditor);
        assertTrue(extension instanceof GraticuleLayerEditor);
    }

    public static class GraticuleLayerEditor implements LayerEditor {

        @Override
        public JComponent createControl(AppContext appContext, Layer layer) {
            return null;
        }

        @Override
        public void updateControl() {
        }
    }
}
