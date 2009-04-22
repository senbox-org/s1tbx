package org.esa.beam.visat.toolviews.layermanager;

import com.bc.ceres.core.ExtensionFactory;
import com.bc.ceres.glayer.LayerType;
import junit.framework.TestCase;
import org.esa.beam.glayer.GraticuleLayerType;
import org.esa.beam.visat.toolviews.layermanager.editors.GraticuleLayerEditor;

public class DefaultLayerEditorDescriptorTest extends TestCase {

    public void testTypesAndFactory() {
        Class<GraticuleLayerType> layerTypeClass = GraticuleLayerType.class;
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
