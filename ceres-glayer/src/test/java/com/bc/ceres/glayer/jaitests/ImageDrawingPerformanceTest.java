/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package com.bc.ceres.glayer.jaitests;

import org.junit.Ignore;

import javax.imageio.ImageIO;
import javax.swing.JComponent;
import javax.swing.JFrame;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

@Ignore
public class ImageDrawingPerformanceTest {
    private static final String[] BUFFERED_IMAGE_TYPE_NAMES = new String[]{
            "TYPE_3BYTE_BGR",
            "TYPE_4BYTE_ABGR",
            "TYPE_4BYTE_ABGR_PRE",
            "TYPE_BYTE_BINARY",
            "TYPE_BYTE_GRAY",
            "TYPE_BYTE_INDEXED",
            "TYPE_INT_ARGB",
            "TYPE_INT_ARGB_PRE",
            "TYPE_INT_BGR",
            "TYPE_INT_RGB",
            "TYPE_USHORT_555_RGB",
            "TYPE_USHORT_565_RGB",
            "TYPE_USHORT_GRAY",
    };

    public static void main(String[] args) throws IOException {
        final Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        final JFrame frame = new JFrame("ImageDrawingPerformanceTest");
        frame.getContentPane().add(new TestCanvas());
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(screenSize);
        frame.setVisible(true);
    }

    private static class TestCanvas extends JComponent {
        private BufferedImage image;

        private TestCanvas() throws IOException {
            image = ImageIO.read(new File("src/test/resources/images/image-with-alpha.png"));

        }

        @Override
        protected void paintComponent(Graphics g) {
            System.out.printf("==> paintComponent: clip=%s\n", g.getClip());
            System.out.println("name\ttype\ttime(ms)\tFPS");
            final Graphics2D graphics2D = (Graphics2D) g;
            for (String typeName : BUFFERED_IMAGE_TYPE_NAMES) {
                test(graphics2D, new BufferedImage(getWidth(), getHeight(), getType(typeName)), typeName);
            }
            test(graphics2D, graphics2D.getDeviceConfiguration().createCompatibleImage(getWidth(), getHeight(), image.getTransparency()), "COMPATIBLE");
        }

        private void test(Graphics2D graphics2D, BufferedImage bufferedImage, String typeName) {
            int type = bufferedImage.getType();
            final Graphics2D imageG = bufferedImage.createGraphics();
            imageG.drawImage(image, null, 0, 0);
            imageG.setFont(getFont().deriveFont(32.0f));
            imageG.drawString(String.format("type = %d (%s)", type, typeName), 100, 100);
            imageG.dispose();

            graphics2D.drawRenderedImage(bufferedImage, null);

            final long t0 = System.nanoTime();
            final int n = 10;
            for (int j = 0; j < n; j++) {
                graphics2D.drawRenderedImage(bufferedImage, null);
            }
            final double time = (System.nanoTime() - t0) / (1024.0 * 1024.0) / n;
            final double fps = 1000.0 / time;
            System.out.println(typeName + "\t" + type + "\t" + time + "\t" + fps);
        }

        private static int getType(String typeName) {
            int type = 0;
            try {
                type = BufferedImage.class.getField(typeName).getInt(null);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
            return type;
        }
    }
}
