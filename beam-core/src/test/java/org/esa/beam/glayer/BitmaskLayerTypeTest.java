package org.esa.beam.glayer;

import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.support.ImageLayer;
import org.esa.beam.framework.datamodel.BitmaskDef;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.VirtualBand;
import org.esa.beam.framework.datamodel.ProductData;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

import java.awt.geom.AffineTransform;
import java.awt.Color;

import static junit.framework.Assert.assertSame;

public class BitmaskLayerTypeTest extends LayerTypeTest {

    public BitmaskLayerTypeTest() {
        super(BitmaskLayerType.class);
    }

    @Test
    public void testConfigurationTemplate() {
        final ValueContainer template = getLayerType().getConfigurationTemplate();

        assertNotNull(template);
        ensurePropertyIsDeclaredButNotDefined(template, "bitmask.bitmaskDef", BitmaskDef.class);
        ensurePropertyIsDeclaredButNotDefined(template, "bitmask.product", Product.class);

        ensurePropertyIsDefined(template, "bitmask.i2mTransform", AffineTransform.class);
        ensurePropertyIsDefined(template, "border.shown", Boolean.class);
        ensurePropertyIsDefined(template, "border.width", Double.class);
        ensurePropertyIsDefined(template, "border.color", Color.class);
    }

    @Test
    public void testCreateLayer() throws ValidationException {
        final Product product = new Product("N", "T", 10, 10);
        final Band raster = new VirtualBand("A", ProductData.TYPE_INT32, 10, 10, "42");
        product.addBand(raster);
        final BitmaskDef bitmaskDef = new BitmaskDef("bitmask", "description", "A == 42", Color.BLUE, 0.4f);
        product.addBitmaskDef(bitmaskDef);

        final ValueContainer config = getLayerType().getConfigurationTemplate();
        config.setValue("bitmask.product", product);
        config.setValue("bitmask.i2mTransform", new AffineTransform());
        config.setValue("bitmask.bitmaskDef", bitmaskDef);

        final Layer layer = getLayerType().createLayer(null, config);
        assertNotNull(layer);
        assertSame(getLayerType(), layer.getLayerType());
        assertTrue(layer instanceof ImageLayer);
        ImageLayer imageLayer = (ImageLayer) layer;
        assertNotNull(imageLayer.getMultiLevelSource());
    }
}
