package org.esa.beam.visat.actions.session.dom;

import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerType;
import com.bc.ceres.glayer.LayerTypeRegistry;
import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.glayer.RoiLayerType;
import org.junit.After;
import org.junit.Before;

import java.awt.Color;
import java.awt.geom.AffineTransform;

public class RoiLayerConfigurationPersistencyTest extends AbstractLayerConfigurationPersistencyTest {

    private Product product;
    private RasterDataNode raster;

    public RoiLayerConfigurationPersistencyTest() {
        super(LayerTypeRegistry.getLayerType(RoiLayerType.class.getName()));
    }

    @Before
    public void setup() {
        product = createTestProduct("Test", "Test");
        raster = addVirtualBand(product, "virtualBand", ProductData.TYPE_INT32, "17");
        Mask mask = new Mask(raster.getName() + "_roi", product.getSceneRasterWidth(), product.getSceneRasterHeight(), new Mask.BandMathType());
        product.getMaskGroup().add(mask);
        Mask.BandMathType.setExpression(mask, "virtualBand == 17");

        getProductManager().addProduct(product);
    }

    @After
    public void tearDown() {
        getProductManager().removeProduct(product);
    }

    @Override
    protected Layer createLayer(LayerType layerType) throws Exception {
        final PropertySet configuration = layerType.createLayerConfig(null);
        configuration.setValue("raster", raster);
        configuration.setValue("color", new Color(17, 11, 67));
        configuration.setValue("transparency", 0.5);
        configuration.setValue("imageToModelTransform", new AffineTransform());
        return layerType.createLayer(null, configuration);
    }
}
