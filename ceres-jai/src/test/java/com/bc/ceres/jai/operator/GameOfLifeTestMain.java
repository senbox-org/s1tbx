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

package com.bc.ceres.jai.operator;

import com.bc.ceres.jai.GeneralFilterFunction;

import javax.media.jai.BorderExtender;
import javax.media.jai.JAI;
import javax.media.jai.RenderedOp;
import javax.swing.*;
import java.awt.*;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.awt.image.DataBufferByte;
import java.util.Timer;
import java.util.TimerTask;

public class GameOfLifeTestMain {

    private static final int IMAGE_UPDATE_PERIOD = 30; // ms

    private JLabel label;
    private BufferedImage image;
    private JFrame frame;

    public static void main(String[] args) {
        new GameOfLifeTestMain().run();
    }

    public void run() {
        image = new BufferedImage(512, 512, BufferedImage.TYPE_BYTE_GRAY);
        final byte[] bytes = ((DataBufferByte) image.getRaster().getDataBuffer()).getData();
        for (int i = 0; i < bytes.length; i++) {
            bytes[i] = (byte) (Math.random() < 0.5 ? 0 : 255);
        }

        SwingUtilities.invokeLater(new Runnable() {
            public void run() {
                label = new JLabel(new ImageIcon(image));

                frame = new JFrame("GoL");
                frame.getContentPane().add(label);
                frame.pack();
                frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                frame.addWindowListener(new WindowAdapter() {
                    @Override
                    public void windowOpened(WindowEvent e) {
                        final Timer timer = new Timer();
                        timer.schedule(new MyTimerTask(), 1000, IMAGE_UPDATE_PERIOD);
                    }
                });
                frame.setVisible(true);

            }
        });
    }

    /*
       For a space that is 'populated':
           Each cell with one or no neighbors dies, as if by loneliness.
           Each cell with four or more neighbors dies, as if by overpopulation.
           Each cell with two or three neighbors survives.
       For a space that is 'empty' or 'unpopulated'
           Each cell with three neighbors becomes populated.
    */
    private static class GoLFilterFunction extends GeneralFilterFunction {

        private GoLFilterFunction() {
            super(3, 3, 1, 1, null);
        }

        public float filter(float[] fdata) {
            int n = 0;
            for (int i = 0; i < 9; i++) {
                if (i != 4 && fdata[i] != 0) {
                    n++;
                }
            }
            if (fdata[4] > 0.0) {
                return n == 2 || n == 3 ? 255 : 0;
            } else {
                return n == 3 ? 255 : 0;
            }
        }
    }

    private class MyTimerTask extends TimerTask {
        public void run() {
            final RenderedOp op = GeneralFilterDescriptor.create(image,
                                                                 new GoLFilterFunction(),
                                                                 new RenderingHints(JAI.KEY_BORDER_EXTENDER, BorderExtender.createInstance(BorderExtender.BORDER_WRAP)));
            image = op.getAsBufferedImage();
            SwingUtilities.invokeLater(new Runnable() {
                public void run() {
                    label.setIcon(new ImageIcon(image));
                }
            });
        }
    }
}
