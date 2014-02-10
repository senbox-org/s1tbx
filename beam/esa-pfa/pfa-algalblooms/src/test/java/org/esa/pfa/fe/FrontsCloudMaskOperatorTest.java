package org.esa.pfa.fe;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.CrsGeoCoding;
import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.util.ImageUtils;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.junit.Test;

import java.awt.image.RenderedImage;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;

/**
 * @author Norman Fomferra
 */
public class FrontsCloudMaskOperatorTest {
    @Test
    public void testIt() throws Exception {

        int w = 2;
        int h = 2;
        Product sourceProduct = new Product("A", "B", w, h);
        sourceProduct.setGeoCoding(new CrsGeoCoding(DefaultGeographicCRS.WGS84, w, h, 0, 0, 1, 1));
        addBand(sourceProduct, "reflec_1", new double[]{
                0.3819109,
                0.22538304,
                0.25125533,
                0.21675564,
        });
        addBand(sourceProduct, "reflec_3", new double[]{
                0.33981264,
                0.1669165,
                0.17584537,
                0.13200541,
        });
        addBand(sourceProduct, "reflec_4", new double[]{
                0.3239365,
                0.14908752,
                0.158101,
                0.11217712,
        });
        addBand(sourceProduct, "reflec_11", new double[]{
                0.077121526,
                0.011994329,
                0.025511047,
                0.011896654,
        });
        addBand(sourceProduct, "reflec_15", new double[]{
                0.2559383,
                0.016200244,
                0.06658918,
                0.015999883,
        });

        FrontsCloudMaskOperator operator = new FrontsCloudMaskOperator();
        operator.setSourceProduct(sourceProduct);
        operator.setRoiExpr("TRUE");
        operator.setThreshold(11);

        Product targetProduct = operator.getTargetProduct();
        assertNotNull(targetProduct.getGeoCoding());

        Band[] bands = targetProduct.getBands();
        assertEquals(2, bands.length);
        assertEquals("cloud_data_ori_or_flag", bands[0].getName());
        assertEquals("cloud_data_algo", bands[1].getName());

        Mask cloudMask = targetProduct.getMaskGroup().get("cloud_mask");
        assertNotNull(cloudMask);

        int[] cloudMaskData = new int[w * h];
        cloudMask.readPixels(0, 0, w, h, cloudMaskData);

        // Expected values:
        // [0] cloud
        // [1] bloom
        // [2] cloudy
        // [3] clear_water

        assertEquals(255, cloudMaskData[0]);
        assertEquals(0, cloudMaskData[1]);
        assertEquals(255, cloudMaskData[2]);
        assertEquals(0, cloudMaskData[3]);

        double[] cloud_data_ori_or_flag = new double[w * h];
        bands[0].readPixels(0, 0, w, h, cloud_data_ori_or_flag);
        assertEquals(24, cloud_data_ori_or_flag[0], 1e-5);
        assertEquals(0, cloud_data_ori_or_flag[1], 1e-5);
        assertEquals(24, cloud_data_ori_or_flag[2], 1e-5);
        assertEquals(0, cloud_data_ori_or_flag[3], 1e-5);

        double[] cloud_data_algo = new double[w * h];
        bands[1].readPixels(0, 0, w, h, cloud_data_algo);
        assertEquals(1.0221124701530002, cloud_data_algo[0], 1e-5);
        assertEquals(-0.024591700082000005, cloud_data_algo[1], 1e-5);
        assertEquals(0.21410025713449995, cloud_data_algo[2], 1e-5);
        assertEquals(-4.2551585499998185E-4, cloud_data_algo[3], 1e-5);

    }

    private void addBand(Product sourceProduct, String name, double[] data) {
        sourceProduct.addBand(name, ProductData.TYPE_FLOAT64).setSourceImage(createImage(sourceProduct.getSceneRasterWidth(),
                                                                                         sourceProduct.getSceneRasterHeight(),
                                                                                         data));
    }

    private RenderedImage createImage(int w, int h, double[] data) {
        return ImageUtils.createRenderedImage(w, h, ProductData.createInstance(data));
    }
}
