package org.esa.beam.dataio.spot;

import junit.framework.TestCase;

import javax.media.jai.Interpolation;
import javax.media.jai.RenderedOp;
import javax.media.jai.operator.CropDescriptor;
import javax.media.jai.operator.ScaleDescriptor;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;


public class GridTest extends TestCase {
    private static final Interpolation INTERPOL = Interpolation.getInstance(Interpolation.INTERP_NEAREST);

    public void testJaiImageScaling() throws Exception {
        int destW = 1024;
        int destH = 512;

        int sampling = 100;
        int srcW = destW / sampling + 1;
        int srcH = destH / sampling + 1;

        BufferedImage srcImg = new BufferedImage(srcW, srcH, BufferedImage.TYPE_USHORT_GRAY);
        srcImg.getRaster().setSample(0, 0, 0, 16231);
        srcImg.getRaster().setSample(srcW - 1, 0, 0, 16232);
        srcImg.getRaster().setSample(0, srcH - 1, 0, 16233);
        srcImg.getRaster().setSample(srcW - 1, srcH - 1, 0, 16234);
        srcImg.getRaster().setSample(srcW / 2, srcH / 2, 0, 16235);

        int tempW = srcW * sampling + 1;
        int tempH = srcH * sampling + 1;
        float xScale = (float) tempW / (float) srcW;
        float yScale = (float) tempH / (float) srcH;
        RenderedOp tempImg = ScaleDescriptor.create(srcImg, xScale, yScale, 0f, 0f, INTERPOL, null);


        assertEquals(tempW, tempImg.getWidth());
        assertEquals(tempH, tempImg.getHeight());

        Raster data = tempImg.getData();
        assertEquals(16231, data.getSample(0, 0, 0));
        assertEquals(16232, data.getSample(tempW - 1, 0, 0));
        assertEquals(16233, data.getSample(0, tempH - 1, 0));
        assertEquals(16234, data.getSample(tempW - 1, tempH - 1, 0));
        assertEquals(16235, data.getSample(tempW / 2, tempH / 2, 0));


        RenderedOp dstImg = CropDescriptor.create(tempImg, 0f, 0f, (float) destW, (float) destH, null);

        assertEquals(1024, dstImg.getWidth());
        assertEquals(512, dstImg.getHeight());
    }

    public static void main(String[] args) {
        int destW = 1024;
        int destH = 512;

        int sampling = 100;
        int srcW = destW / sampling + 1;
        int srcH = destH / sampling + 1;

        BufferedImage srcImg = new BufferedImage(srcW, srcH, BufferedImage.TYPE_USHORT_GRAY);
        srcImg.getRaster().setSample(0, 0, 0, 16231);
        srcImg.getRaster().setSample(srcW - 1, 0, 0, 16232);
        srcImg.getRaster().setSample(0, srcH - 1, 0, 16233);
        srcImg.getRaster().setSample(srcW - 1, srcH - 1, 0, 16234);
        srcImg.getRaster().setSample(srcW / 2, srcH / 2, 0, 16235);

        int tempW = srcW * sampling + 1;
        int tempH = srcH * sampling + 1;
        float xScale = (float) tempW / (float) srcW;
        float yScale = (float) tempH / (float) srcH;
        final RenderedOp tempImg = ScaleDescriptor.create(srcImg, xScale, yScale, 0f, 0f, INTERPOL, null);
       final RenderedOp dstImg = CropDescriptor.create(tempImg, (float)tempImg.getMinX(), (float)tempImg.getMinY(), (float) destW, (float) destH, null);

        showImage("tempImg", tempImg);
        showImage("dstImg", dstImg);

    }

    private static void showImage(String name, final RenderedOp tempImg) {
        JFrame frame = new JFrame(name);
        frame.add(new JScrollPane(new JPanel() {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(tempImg.getWidth(), tempImg.getHeight());
            }

            @Override
            protected void paintComponent(Graphics g) {

                ((Graphics2D)g).drawRenderedImage(tempImg, new AffineTransform());
            }
        }));
        frame.pack();
        frame.setVisible(true);
    }
}
