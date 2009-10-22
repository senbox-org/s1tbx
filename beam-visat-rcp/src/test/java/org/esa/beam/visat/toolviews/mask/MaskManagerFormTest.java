package org.esa.beam.visat.toolviews.mask;

import com.jidesoft.utils.Lm;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.ProductNodeEvent;
import org.esa.beam.framework.datamodel.ProductNodeListenerAdapter;
import org.esa.beam.framework.datamodel.VectorData;
import org.junit.Assert;
import org.junit.Test;

import javax.swing.JFrame;
import javax.swing.UIManager;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.util.Locale;

public class MaskManagerFormTest {
    static {
        Lm.verifyLicense("Brockmann Consult", "BEAM", "lCzfhklpZ9ryjomwWxfdupxIcuIoCxg2");
    }

    @Test
    public void testForm() {
        MaskManagerForm form = createTestForm();
        Assert.assertNull(form.getSceneView());
        Assert.assertNotNull(form.getHelpButton());
        Assert.assertEquals("helpButton", form.getHelpButton().getName());
        Assert.assertNotNull(form.createContentPanel());
    }

    public static void main(String[] args) {
        Locale.setDefault(Locale.UK);
        try {
            UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
        } catch (Exception ignored) {
            // ignore
        }


        MaskManagerForm form = createTestForm();

        JFrame frame = new JFrame(MaskManagerFormTest.class.getName());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(form.createContentPanel());
        frame.pack();
        frame.setVisible(true);
    }

    private static MaskManagerForm createTestForm() {
        Product product = createTestProduct();

        MaskManagerForm form = new MaskManagerForm();
        form.reconfigureMasks(product, product.getBand("B"));
        return form;
    }

    private static Product createTestProduct() {
        Color[] colors = {
                Color.WHITE,
                Color.BLACK,
                Color.GREEN,
                Color.BLUE,
                Color.CYAN,
                Color.MAGENTA,
                Color.PINK,
                Color.YELLOW,
                Color.ORANGE,
                Color.RED,
        };
        Product product = new Product("P", "T", 256, 256);
        Band a = product.addBand("A", ProductData.TYPE_UINT8);
        Band b = product.addBand("B", ProductData.TYPE_UINT8);
        Band c = product.addBand("C", ProductData.TYPE_UINT8);
        a.setScalingFactor(1.0 / 255.0);
        b.setScalingFactor(1.0 / 255.0);
        c.setScalingFactor(1.0 / 255.0);
        a.setSourceImage(new BufferedImage(256, 256, BufferedImage.TYPE_BYTE_GRAY));
        b.setSourceImage(new BufferedImage(256, 256, BufferedImage.TYPE_BYTE_GRAY));
        c.setSourceImage(new BufferedImage(256, 256, BufferedImage.TYPE_BYTE_GRAY));
        for (int i = 0; i < colors.length; i++) {
            String expression = "B > " + (i / (colors.length - 1.0));
            Mask mask = new Mask("M_" + (product.getMaskGroup().getNodeCount() + 1),
                                 product.getSceneRasterWidth(),
                                 product.getSceneRasterHeight(),
                                 new Mask.BandMathType());
            mask.getImageConfig().setValue("color", colors[i]);
            mask.getImageConfig().setValue("transparency", 1.0f - 1.0f / (1 + (i % 4)));
            mask.getImageConfig().setValue("expression", expression);
            mask.setDescription(expression);
            product.getMaskGroup().add(mask);
        }

        for (int i = 0; i < product.getVectorDataGroup().getNodeCount(); i++) {
            VectorData vectorData = product.getVectorDataGroup().get(i);
            Mask mask = new Mask(vectorData.getName(),
                                 product.getSceneRasterWidth(),
                                 product.getSceneRasterHeight(),
                                 new Mask.GeometryType());
            mask.getImageConfig().setValue("color", colors[i % colors.length].brighter());
            mask.getImageConfig().setValue("transparency", 0.1f);
            mask.getImageConfig().setValue("vectorData", vectorData);
            product.getMaskGroup().add(mask);
        }

        product.addProductNodeListener(new ProductNodeListenerAdapter() {
            @Override
            public void nodeChanged(ProductNodeEvent event) {
                System.out.println("event = " + event);
            }

            @Override
            public void nodeDataChanged(ProductNodeEvent event) {
                System.out.println("event = " + event);
            }

            @Override
            public void nodeAdded(ProductNodeEvent event) {
                System.out.println("event = " + event);
            }

            @Override
            public void nodeRemoved(ProductNodeEvent event) {
                System.out.println("event = " + event);
            }
        });
        return product;
    }
}
