package org.esa.beam.visat.toolviews.mask;

import com.jidesoft.utils.Lm;
import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.GcpDescriptor;
import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.PinDescriptor;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Placemark;
import org.esa.beam.framework.datamodel.PlacemarkGroup;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.VectorDataNode;

import javax.swing.table.TableModel;
import java.awt.Color;
import java.awt.image.BufferedImage;

public class MaskFormTest extends TestCase {
    private Product product;
    private MaskManagerForm maskManagerForm;
    private MaskViewerForm maskViewerForm;

    static {
        Lm.verifyLicense("Brockmann Consult", "BEAM", "lCzfhklpZ9ryjomwWxfdupxIcuIoCxg2");
    }

    @Override
    public void setUp() {
        product = createTestProduct();

        maskManagerForm = new MaskManagerForm(null, null);
        maskManagerForm.reconfigureMaskTable(product, null);

        maskViewerForm = new MaskViewerForm(null);
        maskViewerForm.reconfigureMaskTable(product, null);
    }

    public void testMaskManagerForm() {
        assertSame(product, maskManagerForm.getProduct());
        assertNotNull(maskManagerForm.getHelpButton());
        assertEquals("helpButton", maskManagerForm.getHelpButton().getName());
        assertNotNull(maskManagerForm.createContentPanel());
        assertEquals(12, maskManagerForm.getRowCount());
        final TableModel tableModel = maskManagerForm.getMaskTable().getModel();
        assertEquals("M_3", tableModel.getValueAt(0, 0));
        tableModel.setValueAt("test_3", 0, 0);
        assertEquals("test_3", tableModel.getValueAt(0, 0));
        assertEquals("M_4", tableModel.getValueAt(1, 0));

        final PlacemarkGroup gcpGroup = product.getGcpGroup();
        final PlacemarkGroup pinGroup = product.getPinGroup();

        gcpGroup.add(new Placemark("G", "L", "D", new PixelPos(10, 10), null, GcpDescriptor.INSTANCE, null));
        assertEquals(13, maskManagerForm.getRowCount());

        assertEquals(Product.GCP_MASK_NAME, tableModel.getValueAt(0, 0));
        tableModel.setValueAt("test_gcp", 0, 0);
        assertEquals("test_gcp", tableModel.getValueAt(0, 0));

        assertEquals("test_3", tableModel.getValueAt(1, 0));
        tableModel.setValueAt("M_3", 1, 0);
        assertEquals("M_3", tableModel.getValueAt(1, 0));

        pinGroup.add(new Placemark("P", "L", "D", new PixelPos(10, 10), null, PinDescriptor.INSTANCE, null));
        assertEquals(14, maskManagerForm.getRowCount());

        assertEquals(Product.PIN_MASK_NAME, tableModel.getValueAt(0, 0));
        tableModel.setValueAt("test_pin", 0, 0);
        assertEquals("test_pin", tableModel.getValueAt(0, 0));

        assertEquals("test_gcp", tableModel.getValueAt(1, 0));
        tableModel.setValueAt(Product.GCP_MASK_NAME, 1, 0);
        assertEquals(Product.GCP_MASK_NAME, tableModel.getValueAt(1, 0));

        assertEquals("M_3", tableModel.getValueAt(2, 0));
        tableModel.setValueAt("test_3", 2, 0);
        assertEquals("test_3", tableModel.getValueAt(2, 0));

        gcpGroup.removeAll();
        assertEquals(13, maskManagerForm.getRowCount());
        assertEquals("test_pin", tableModel.getValueAt(0, 0));
        assertEquals("test_3", tableModel.getValueAt(1, 0));

    }

    public void testMaskViewerForm() {
        assertSame(product, maskViewerForm.getProduct());
        assertNull(maskViewerForm.getHelpButton());
        assertNotNull(maskViewerForm.createContentPanel());
        assertEquals(12, maskViewerForm.getRowCount());
    }

    static Product createTestProduct() {
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
            String name = "M_" + (product.getMaskGroup().getNodeCount() + 1);
            Mask mask = Mask.BandMathsType.create(name, expression, product.getSceneRasterWidth(), product.getSceneRasterHeight(),
                                                 expression, colors[i], 1.0 - 1.0 / (1 + (i % 4)));
            product.getMaskGroup().add(mask);
        }

        for (int i = 0; i < product.getVectorDataGroup().getNodeCount(); i++) {
            VectorDataNode vectorDataNode = product.getVectorDataGroup().get(i);
            Mask mask = new Mask(vectorDataNode.getName(),
                                 product.getSceneRasterWidth(),
                                 product.getSceneRasterHeight(),
                                 Mask.VectorDataType.INSTANCE);
            mask.getImageConfig().setValue("color", colors[i % colors.length].brighter());
            mask.getImageConfig().setValue("transparency", 0.1);
            mask.getImageConfig().setValue("vectorData", vectorDataNode);
            product.getMaskGroup().add(mask);
        }
        return product;
    }
}
