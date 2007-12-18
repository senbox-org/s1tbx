package org.esa.beam.visat.toolviews.stat;

import junit.framework.TestCase;

import java.awt.Rectangle;
import java.awt.image.BufferedImage;

public class XYImagePlotTest extends TestCase {

    public void testDefaultValues() {
        final XYImagePlot imagePlot = new XYImagePlot();

        assertNull(imagePlot.getImage());
        assertNull(imagePlot.getImageDataBounds());

        assertNull(imagePlot.getDataset());
        assertNotNull(imagePlot.getDomainAxis());
        assertNotNull(imagePlot.getRangeAxis());

        assertTrue(imagePlot.isDomainGridlinesVisible());
        assertTrue(imagePlot.isRangeGridlinesVisible());
    }

    public void testImage() {
        final XYImagePlot imagePlot = new XYImagePlot();

        final BufferedImage image = new BufferedImage(16, 9, BufferedImage.TYPE_INT_ARGB);
        imagePlot.setImage(image);
        assertSame(image, imagePlot.getImage());
        assertEquals(new Rectangle(0, 0, 16, 9), imagePlot.getImageDataBounds());

        final BufferedImage otherImage = new BufferedImage(4, 3, BufferedImage.TYPE_INT_ARGB);
        imagePlot.setImage(otherImage);
        assertSame(otherImage, imagePlot.getImage());
        assertEquals(new Rectangle(0, 0, 16, 9), imagePlot.getImageDataBounds());
    }

    public void testImageDataBounds() {
        final XYImagePlot imagePlot = new XYImagePlot();
        assertNull(imagePlot.getDataset());
        final Rectangle bounds = new Rectangle(0, 2, 20, 40);
        imagePlot.setImageDataBounds(bounds);
        assertNotSame(bounds, imagePlot.getImageDataBounds());
        assertEquals(bounds, imagePlot.getImageDataBounds());
        assertEquals(0.0, imagePlot.getDomainAxis().getLowerBound(), 1e-10);
        assertEquals(20.0, imagePlot.getDomainAxis().getUpperBound(), 1e-10);
        assertEquals(2.0, imagePlot.getRangeAxis().getLowerBound(), 1e-10);
        assertEquals(42.0, imagePlot.getRangeAxis().getUpperBound(), 1e-10);
        assertNotNull(imagePlot.getDataset());
    }

}