package org.esa.beam.glayer;

import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerTypeRegistry;
import com.bc.ceres.glayer.support.ImageLayer;
import static junit.framework.Assert.assertSame;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.BitmaskDef;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.VirtualBand;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

import java.awt.Color;
import java.awt.geom.AffineTransform;

public class BitmaskLayerTypeTest extends LayerTypeTest {

    public BitmaskLayerTypeTest() {
        super(BitmaskLayerType.class);
    }

    @Test
    public void testConfigurationTemplate() {
        final PropertySet template = getLayerType().createLayerConfig(null);

        assertNotNull(template);
        ensurePropertyIsDeclaredButNotDefined(template, "bitmaskDef", BitmaskDef.class);
        ensurePropertyIsDeclaredButNotDefined(template, "product", Product.class);
        ensurePropertyIsDeclaredButNotDefined(template, "imageToModelTransform", AffineTransform.class);

        ensurePropertyIsDefined(template, "borderShown", Boolean.class);
        ensurePropertyIsDefined(template, "borderWidth", Double.class);
        ensurePropertyIsDefined(template, "borderColor", Color.class);
    }

    @Test
    public void testCreateLayer() {
        final Product product = new Product("N", "T", 10, 10);
        final Band raster = new VirtualBand("A", ProductData.TYPE_INT32, 10, 10, "42");
        product.addBand(raster);
        final BitmaskDef bitmaskDef = new BitmaskDef("bitmask", "description", "A == 42", Color.BLUE, 0.4f);
        product.addBitmaskDef(bitmaskDef);

        final PropertySet config = getLayerType().createLayerConfig(null);
        config.setValue("product", product);
        config.setValue("imageToModelTransform", new AffineTransform());
        config.setValue("bitmaskDef", bitmaskDef);

        final Layer layer = getLayerType().createLayer(null, config);
        assertNotNull(layer);
        
        MaskLayerType maskLayerType = LayerTypeRegistry.getLayerType(MaskLayerType.class);
        assertSame(maskLayerType, layer.getLayerType());
        assertTrue(layer instanceof ImageLayer);
        ImageLayer imageLayer = (ImageLayer) layer;
        assertNotNull(imageLayer.getMultiLevelSource());
    }
}
