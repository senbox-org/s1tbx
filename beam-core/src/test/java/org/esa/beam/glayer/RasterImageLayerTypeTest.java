package org.esa.beam.glayer;

import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.glayer.LayerType;
import com.bc.ceres.glayer.support.ImageLayer;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.VirtualBand;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;

import java.awt.Color;

public class RasterImageLayerTypeTest extends LayerTypeTest {

    public RasterImageLayerTypeTest() {
        super(RasterImageLayerType.class);
    }

    @Test
    public void testDefaultConfiguration() {
        final LayerType layerType = getLayerType();

        final PropertySet template = layerType.createLayerConfig(null);
        assertNotNull(template);

        ensurePropertyIsDeclaredButNotDefined(template, "raster", RasterDataNode.class);
        ensurePropertyIsDefined(template, "borderShown", Boolean.class);
        ensurePropertyIsDefined(template, "borderWidth", Double.class);
        ensurePropertyIsDefined(template, "borderColor", Color.class);
    }

    @Test
    public void testCreateLayerWithSingleRaster() {
        final RasterImageLayerType layerType = (RasterImageLayerType) getLayerType();

        final Product product = new Product("N", "T", 10, 10);
        final Band raster = new VirtualBand("A", ProductData.TYPE_INT32, 10, 10, "42");
        product.addBand(raster);

        final ImageLayer imageLayer = (ImageLayer) layerType.createLayer(raster, null);
        assertNotNull(imageLayer.getMultiLevelSource());
    }

}
