package org.esa.beam.dataio.spot;

import junit.framework.TestCase;

import javax.media.jai.BorderExtender;
import javax.media.jai.Interpolation;
import javax.media.jai.JAI;
import javax.media.jai.RenderedOp;
import javax.media.jai.operator.CropDescriptor;
import javax.media.jai.operator.ScaleDescriptor;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.geom.AffineTransform;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;


public class GridTest extends TestCase {
    private static final int MAX_USHORT = (2 << 15) - 1;
    private static final int SAMPLING = 100;

    public void testJaiImageScaling() throws Exception {
        int destW = 1024;
        int destH = 512;

        int srcW = destW / SAMPLING + 1;
        int srcH = destH / SAMPLING + 1;

        BufferedImage srcImg = createSrcImage(srcW, srcH);

        int tempW = srcW * SAMPLING + 1;
        int tempH = srcH * SAMPLING + 1;
        float xScale = (float) tempW / (float) srcW;
        float yScale = (float) tempH / (float) srcH;

        // dstMinX   = 0
        // dstMinY   = 0
        // dstWidth  = ceil(srcWidth  * scaleX - 0.5 + transX)
        // dstHeight = ceil(srcHeight * scaleY - 0.5 + transY)
        //
        RenderingHints renderingHints = new RenderingHints(JAI.KEY_BORDER_EXTENDER, BorderExtender.createInstance(BorderExtender.BORDER_COPY));
        RenderedOp tempImg = ScaleDescriptor.create(srcImg, xScale, yScale, -0.5f * SAMPLING + 1, -0.5f * SAMPLING + 1,
                                                    Interpolation.getInstance(Interpolation.INTERP_BILINEAR), renderingHints);

        assertEquals(tempW, tempImg.getWidth());
        assertEquals(tempH, tempImg.getHeight());

        RenderedOp dstImg = CropDescriptor.create(tempImg, 0f, 0f, (float) destW, (float) destH, null);

        assertEquals(1024, dstImg.getWidth());
        assertEquals(512, dstImg.getHeight());

        Raster data = dstImg.getData();
        assertEquals(MAX_USHORT, data.getSample(0, 0, 0));
        assertEquals(MAX_USHORT, data.getSample(destW - 1, 0, 0));
        assertEquals(MAX_USHORT, data.getSample(0, destH - 1, 0));
        assertEquals(MAX_USHORT, data.getSample(destW - 1, destH - 1, 0));
    }

    private static BufferedImage createSrcImage(int srcW, int srcH) {
        BufferedImage srcImg = new BufferedImage(srcW, srcH, BufferedImage.TYPE_USHORT_GRAY);
        for (int y = 0; y < srcH; y++) {
            for (int x = 0; x < srcW; x++) {
                srcImg.getRaster().setSample(x, y, 0, (int) (MAX_USHORT * Math.random()));
            }
        }
        srcImg.getRaster().setSample(0, 0, 0, MAX_USHORT);
        srcImg.getRaster().setSample(srcW - 1, 0, 0, MAX_USHORT);
        srcImg.getRaster().setSample(0, srcH - 1, 0, MAX_USHORT);
        srcImg.getRaster().setSample(srcW - 1, srcH - 1, 0, MAX_USHORT);
        return srcImg;
    }

    public static void main(String[] args) {
        int destW = 1024;
        int destH = 512;

        int srcW = destW / SAMPLING + 1;
        int srcH = destH / SAMPLING + 1;

        BufferedImage srcImg = createSrcImage(srcW, srcH);

        int tempW = srcW * SAMPLING + 1;
        int tempH = srcH * SAMPLING + 1;
        float xScale = (float) tempW / (float) srcW;
        float yScale = (float) tempH / (float) srcH;
        float xTrans = -SAMPLING * 0.5f;
        float yTrans = -SAMPLING * 0.5f;

        Interpolation[] interpolations = new Interpolation[]{
                Interpolation.getInstance(Interpolation.INTERP_NEAREST),
                Interpolation.getInstance(Interpolation.INTERP_BILINEAR),
                Interpolation.getInstance(Interpolation.INTERP_BICUBIC),
                Interpolation.getInstance(Interpolation.INTERP_BICUBIC_2),
        };
        for (int i = 0; i < interpolations.length; i++) {
            Interpolation interpolation = interpolations[i];
            RenderingHints renderingHints = new RenderingHints(JAI.KEY_BORDER_EXTENDER, BorderExtender.createInstance(BorderExtender.BORDER_COPY));
            RenderedOp image = ScaleDescriptor.create(srcImg, xScale, yScale, xTrans, yTrans, interpolation, renderingHints);
            System.out.println("==============================");
            System.out.println("x = " + image.getMinX());
            System.out.println("y = " + image.getMinY());
            System.out.println("w = " + image.getWidth());
            System.out.println("h = " + image.getHeight());

            showImage("[" + i + "] - " + interpolation, image);
        }
    }

    private static void showImage(String name, final RenderedImage image) {
        JFrame frame = new JFrame(name);
        frame.add(new JScrollPane(new JPanel() {
            @Override
            public Dimension getPreferredSize() {
                return new Dimension(image.getWidth(), image.getHeight());
            }

            @Override
            protected void paintComponent(Graphics g) {
                super.paintComponent(g);
                ((Graphics2D) g).drawRenderedImage(image, new AffineTransform());
                Color oldColor = g.getColor();
                g.setColor(Color.ORANGE);
                for (int x = image.getMinX(); x < image.getMinX() + image.getWidth(); x += SAMPLING) {
                    g.drawLine(x, image.getMinY(), x, image.getMinY() + image.getWidth());
                }
                for (int y = image.getMinY(); y < image.getMinY() + image.getHeight(); y += SAMPLING) {
                    g.drawLine(image.getMinX(), y, image.getMinX() + image.getWidth(), y);
                }
                g.setColor(oldColor);
            }
        }));
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }
}
