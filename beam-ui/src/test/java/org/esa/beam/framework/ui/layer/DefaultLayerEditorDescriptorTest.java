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
