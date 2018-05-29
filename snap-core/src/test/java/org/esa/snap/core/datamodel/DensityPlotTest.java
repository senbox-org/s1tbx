package org.esa.snap.core.datamodel;

import com.bc.ceres.core.ProgressMonitor;
import org.junit.Test;

import javax.media.jai.ImageLayout;
import javax.media.jai.JAI;
import javax.media.jai.operator.ConstantDescriptor;
import java.awt.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;


public class DensityPlotTest {


    @Test
    public void testThatMaskImageUsesSameTilingAsInputRasters() {
        int width = 5000;
        int height = 1000;

        final ImageLayout imageLayout = new ImageLayout();
        imageLayout.setTileWidth(width);
        imageLayout.setTileHeight(10);
        RenderingHints renderingHints = new RenderingHints(JAI.KEY_IMAGE_LAYOUT, imageLayout);

        Band b1 = new Band("b1", ProductData.TYPE_FLOAT32, width, height);
        b1.setSourceImage(ConstantDescriptor.create((float) width, (float) height, new Float[]{23.42f}, renderingHints));
        b1.setValidPixelExpression("b1>1");
        assertEquals(width, b1.getSourceImage().getTileWidth());
        assertEquals(10, b1.getSourceImage().getTileHeight());
        Band b2 = new Band("b2", ProductData.TYPE_INT8, width, height);
        b2.setSourceImage(ConstantDescriptor.create((float) width, (float) height, new Byte[]{64}, renderingHints));
        b2.setValidPixelExpression("b2>1");
        assertEquals(width, b2.getSourceImage().getTileWidth());
        assertEquals(10, b2.getSourceImage().getTileHeight());

        Product product = new Product("P", "T", width, height);
        product.addBand(b1);
        product.addBand(b2);

        byte[] pixelValues = new byte[width * height];
        try {
            DensityPlot.accumulate(b1, 20, 25,
                    b2, 50, 100,
                    null, width, height, pixelValues, ProgressMonitor.NULL);
        } catch (IllegalArgumentException e) {
            if(e.getMessage().contains("specified Rectangle is not completely contained")){
                e.printStackTrace();
                fail("Should have been fixed by applying the same tiling to the maskImage as for the input rasters");
            }
        }
    }
}