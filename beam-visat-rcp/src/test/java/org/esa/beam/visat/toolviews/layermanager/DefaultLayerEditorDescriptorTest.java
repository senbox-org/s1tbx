package org.esa.beam.visat.toolviews.layermanager;

import junit.framework.TestCase;
import org.esa.beam.glayer.GraticuleLayer;
import org.esa.beam.visat.toolviews.layermanager.editors.GraticuleLayerEditor;
import com.bc.ceres.core.ExtensionFactory;
import com.bc.ceres.glayer.LayerType;

public class DefaultLayerEditorDescriptorTest extends TestCase {
    public void testTypesAndFactory() {
        Class<GraticuleLayer.Type> layerTypeClass = GraticuleLayer.Type.class;
        Class<GraticuleLayerEditor> layerEditorClass = GraticuleLayerEditor.class;

        DefaultLayerEditorDescriptor descriptor = new DefaultLayerEditorDescriptor(layerTypeClass, layerEditorClass);
        assertSame(layerTypeClass, descriptor.getLayerTypeClass());
        assertSame(layerEditorClass, descriptor.getLayerEditorClass());

        ExtensionFactory factory = descriptor.createExtensionFactory();
        assertNotNull(factory);

        LayerType layerType = LayerType.getLayerType(layerTypeClass.getName());
        assertNotNull(layerType);

        Object extension = factory.getExtension(layerType, LayerEditor.class);
        assertNotNull(extension);
        assertTrue(extension instanceof LayerEditor);
        assertTrue(extension instanceof GraticuleLayerEditor);
    }
}
