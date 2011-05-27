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

import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.core.ExtensionFactory;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerContext;
import com.bc.ceres.glayer.LayerType;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import javax.swing.JComponent;

import static org.junit.Assert.*;

public class DefaultLayerEditorDescriptorTest {

    private DefaultLayerEditorDescriptor descriptor;

    @Before
    public void setUp() throws Exception {
        descriptor = new DefaultLayerEditorDescriptor();
    }

    @Test
    public void testWithLayerTypeAndEditor() {

        descriptor.setLayerTypeClass(TestLayerType.class);
        descriptor.setLayerEditorClass(TestLayerEditor1.class);

        ExtensionFactory factory = descriptor.createExtensionFactory();
        assertNotNull(factory);
        Object extension = factory.getExtension(new TestLayerType(), LayerEditor.class);
        assertNotNull(extension);
        assertTrue(extension instanceof TestLayerEditor1);
    }

    @Test
    public void testWithLayerAndEditor() {

        descriptor.setLayerClass(TestLayer.class);
        descriptor.setLayerEditorClass(TestLayerEditor1.class);

        ExtensionFactory factory = descriptor.createExtensionFactory();
        assertNotNull(factory);
        Object extension = factory.getExtension(new TestLayer(), LayerEditor.class);
        assertNotNull(extension);
        assertTrue(extension instanceof TestLayerEditor1);
    }

    @Test
    public void testWithLayerTypeAndEditorFactory() {

        descriptor.setLayerTypeClass(TestLayerType.class);
        descriptor.setLayerEditorFactoryClass(TestLayerEditorFactory.class);

        ExtensionFactory factory = descriptor.createExtensionFactory();
        assertNotNull(factory);
        Object extension = factory.getExtension(new TestLayerType(), LayerEditor.class);
        assertNotNull(extension);
        assertTrue(extension instanceof TestLayerEditor2);
    }

    @Test
    public void testWithLayerAndEditorFactory() {

        descriptor.setLayerClass(TestLayer.class);
        descriptor.setLayerEditorFactoryClass(TestLayerEditorFactory.class);

        ExtensionFactory factory = descriptor.createExtensionFactory();
        assertNotNull(factory);
        Object extension = factory.getExtension(new TestLayer(), LayerEditor.class);
        assertNotNull(extension);
        assertTrue(extension instanceof TestLayerEditor3);
    }

    @Test
    public void testWithLayerTypeAndLayerAndEditorFactory() {

        descriptor.setLayerClass(TestLayer.class);
        descriptor.setLayerTypeClass(TestLayerType.class);
        descriptor.setLayerEditorFactoryClass(TestLayerEditorFactory.class);

        ExtensionFactory factory = descriptor.createExtensionFactory();
        assertNotNull(factory);
        Object extension = factory.getExtension(new TestLayerType(), LayerEditor.class);
        assertNotNull(extension);
        assertTrue(extension instanceof TestLayerEditor2);

        extension = factory.getExtension(new TestLayer(), LayerEditor.class);
        assertNotNull(extension);
        assertTrue(extension instanceof TestLayerEditor3);
    }

    public static class TestLayerType extends LayerType {
        @Override
        public Layer createLayer(LayerContext ctx, PropertySet layerConfig) {
            return new TestLayer();
        }

        @Override
        public boolean isValidFor(LayerContext ctx) {
            return false;
        }

        @Override
        public PropertySet createLayerConfig(LayerContext ctx) {
            return new PropertyContainer();
        }
    }

    public static class TestLayer extends Layer {
        public TestLayer() {
            super(new TestLayerType());
        }
    }

    public static class TestLayerEditor1 extends AbstractLayerEditor {
        @Override
        protected JComponent createControl() {
            return null;
        }
    }

    public static class TestLayerEditor2 extends AbstractLayerEditor {
        @Override
        protected JComponent createControl() {
            return null;
        }
    }

    public static class TestLayerEditor3 extends AbstractLayerEditor {
        @Override
        protected JComponent createControl() {
            return null;
        }
    }


    public static class TestLayerEditorFactory implements ExtensionFactory {
        @Override
        public Object getExtension(Object object, Class<?> extensionType) {
            if (object instanceof TestLayerType) {
                return new TestLayerEditor2();
            }
            if (object instanceof TestLayer) {
                return new TestLayerEditor3();
            }
            return null;
        }

        @Override
        public Class<?>[] getExtensionTypes() {
            return new Class<?>[]{TestLayerType.class, TestLayer.class};
        }
    }
}
