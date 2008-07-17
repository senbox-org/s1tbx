package org.esa.beam.gpf;

import junit.framework.TestCase;

import javax.media.jai.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.renderable.ParameterBlock;
import java.util.ArrayList;
import java.util.Collection;

public class CollectionImageTest extends TestCase {
    public void testCollectionImage() {
        JAI.getDefaultInstance().getOperationRegistry().registerDescriptor(new MyRED());
        JAI.getDefaultInstance().getOperationRegistry().registerFactory("collection",
                                                                        "Bibo",
                                                                        "beam-ngpf",
                                                                        new MyCIF());

        final ParameterBlockJAI parameterBlockJAI = new ParameterBlockJAI("Bibo");
        final Collection collection = JAI.createCollection("Bibo", parameterBlockJAI);

        assertNotNull(collection);
        assertTrue(collection instanceof CollectionOp);

        final CollectionOp op = (CollectionOp) collection;
        final Collection collection1 = op.getCollection();
        assertNotNull(collection1);
        assertTrue(collection1 instanceof AttributedImageCollection);
    }

    private static class MyRED extends OperationDescriptorImpl {
        /**
         * The resource strings that provide the general documentation
         * and specify the parameter list for this operation.
         */
        private static final String[][] resources = {
                {"GlobalName", "Bibo"},
                {"LocalName", "Bibo"},
                {"Vendor", "org.esa.beam"},
                {"Description", "Blablabla"},
                {"DocURL", "http://java.sun.com/products/java-media/jai/forDevelopers/jai-apidocs/javax/media/jai/operator/AndConstDescriptor.html"},
                {"Version", "0.5"},
                {"arg0Desc", "Blablabla"},
                {"arg1Desc", "Blablabla"},
        };

        private MyRED() {
            super(resources,
                  new String[]{"collection"},
                  0,
                  new String[]{"a", "b"},
                  new Class[]{Double.class, Double.class},
                  new Object[]{0.0, 0.0},
                  new Object[]{null, null});
        }
    }

    private static class MyCIF implements CollectionImageFactory {

        public CollectionImage create(ParameterBlock parameterBlock,
                                      RenderingHints renderingHints) {
            ArrayList<AttributedImage> list = new ArrayList<AttributedImage>(3);
            list.add(new AttributedImage(new TiledImage(new BufferedImage(16, 16, BufferedImage.TYPE_3BYTE_BGR), false), "A"));
            list.add(new AttributedImage(new TiledImage(new BufferedImage(16, 16, BufferedImage.TYPE_3BYTE_BGR), false), "B"));
            list.add(new AttributedImage(new TiledImage(new BufferedImage(16, 16, BufferedImage.TYPE_3BYTE_BGR), false), "C"));
            return new AttributedImageCollection(list) {
            };
        }

        public CollectionImage update(ParameterBlock newParameterBlock,
                                      RenderingHints newRenderingHints,
                                      ParameterBlock oldPrameterBlock,
                                      RenderingHints oldRenderingHints,
                                      CollectionImage collectionImage,
                                      CollectionOp collectionOp) {
            return null;
        }
    }
}
