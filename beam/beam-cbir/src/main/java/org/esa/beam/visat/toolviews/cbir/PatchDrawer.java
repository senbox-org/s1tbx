/*
 * Copyright (C) 2013 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.beam.visat.toolviews.cbir;

import org.esa.beam.search.PatchImage;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

/**

 */
public class PatchDrawer extends JPanel {

    private final static int width = 100;
    private final static int height = 100;
    private final static Font font = new Font("Ariel", Font.BOLD, 18);

    public PatchDrawer(final PatchImage[] imageList) {
        super();

        update(imageList);
    }

    public void update(final PatchImage[] imageList) {
        this.removeAll();

        for(PatchImage img : imageList) {
            final Patch label = new Patch(img);
            this.add(label);
        }
        updateUI();
    }

    private static class Patch extends JLabel {
        private final PatchImage img;

        public Patch(final PatchImage img) {
            super();
            this.img = img;

            setIcon(new ImageIcon(img.getImage().getScaledInstance(width, height, BufferedImage.SCALE_FAST)));
        }

        @Override
        public void paintComponent(Graphics g) {
            super.paintComponent(g);

            g.setColor(Color.WHITE);
            g.fillRect(30, 30, 30, 30);
            g.setColor(Color.RED);
            g.setFont(font);
            g.drawString(Integer.toString(img.getID()), 35, 50);
        }
    }
}
