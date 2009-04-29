package org.esa.beam.glayer;

import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerType;
import com.bc.ceres.glayer.support.AbstractLayerListener;
import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.Pin;
import org.esa.beam.framework.datamodel.PinDescriptor;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.PlacemarkSymbol;
import org.esa.beam.framework.datamodel.Product;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

public class PlacemarkLayerTest extends TestCase {

    public void testConstruction() throws ValidationException {
        final Product product = new Product("N", "T", 16, 16);
        final PinDescriptor pmd = PinDescriptor.INSTANCE;
        final AffineTransform i2m = new AffineTransform();

        final LayerType type = LayerType.getLayerType(PlacemarkLayerType.class.getName());
        final ValueContainer template = type.getConfigurationTemplate();
        template.setValue("product", product);
        template.setValue("placemarkDescriptor", pmd);
        template.setValue("imageToModelTransform", i2m);
        final PlacemarkLayer placemarkLayer = (PlacemarkLayer) type.createLayer(null, template);
        assertEquals(product, placemarkLayer.getProduct());
        assertEquals(pmd, placemarkLayer.getPlacemarkDescriptor());
        assertEquals(i2m, placemarkLayer.getImageToModelTransform());

        assertTrue(type instanceof PlacemarkLayerType);
        assertNotNull(type.getName());

        final ValueContainer configuration = type.getConfigurationCopy(null, placemarkLayer);
        assertNotNull(configuration);
        assertEquals(product, configuration.getValue("product"));
        assertEquals(pmd, configuration.getValue("placemarkDescriptor"));
        assertEquals(i2m, configuration.getValue("imageToModelTransform"));

        final Layer layerCopy = type.createLayer(null, configuration);
        assertTrue(layerCopy instanceof PlacemarkLayer);
        final PlacemarkLayer placemarkLayerCopy = (PlacemarkLayer) layerCopy;
        assertEquals(product, placemarkLayerCopy.getProduct());
        assertEquals(pmd, placemarkLayerCopy.getPlacemarkDescriptor());
        assertEquals(i2m, placemarkLayerCopy.getImageToModelTransform());
    }

    public void testLayerDataChanges() {
        final Product product = new Product("N", "T", 16, 16);
        final PinDescriptor pmd = PinDescriptor.INSTANCE;
        final AffineTransform i2m = new AffineTransform();

        final PlacemarkLayer placemarkLayer1 = new PlacemarkLayer(product, pmd, i2m);
        final PlacemarkLayer placemarkLayer2 = new PlacemarkLayer(product, pmd, i2m);

        placemarkLayer1.setName("L1");
        placemarkLayer2.setName("L2");

        final MyLayerListener layerListener = new MyLayerListener();
        placemarkLayer1.addListener(layerListener);
        placemarkLayer2.addListener(layerListener);
        assertEquals("", layerListener.trace);

        product.getPinGroup().add(createPin("P1"));
        assertEquals("L1;L2;", layerListener.trace);

        product.getPinGroup().add(createPin("P2"));
        assertEquals("L1;L2;L1;L2;", layerListener.trace);

        placemarkLayer1.dispose();
        layerListener.trace = "";

        product.getPinGroup().add(createPin("P3"));
        assertEquals("L2;", layerListener.trace);

        placemarkLayer2.dispose();
        layerListener.trace = "";

        product.getPinGroup().add(createPin("P4"));
        assertEquals("", layerListener.trace);
    }

    private Pin createPin(String s) {
        return new Pin(s, "L", "D", new PixelPos(), new GeoPos(), new PlacemarkSymbol("S", new Rectangle(0, 0, 1, 1)));
    }

    private static class MyLayerListener extends AbstractLayerListener {

        String trace = "";

        @Override
        public void handleLayerDataChanged(Layer layer, Rectangle2D modelRegion) {
            trace += layer.getName() + ";";
        }
    }
}
