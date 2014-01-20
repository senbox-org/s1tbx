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

import javax.swing.*;
import java.awt.*;

/**

 */
public class BlockDrawer extends JPanel {

    int width = 10000;
    int height = 10000;
    Color[][] colors;

    public BlockDrawer(int w, int h) {
        super();
        this.width = w;
        this.height = h;

        setPreferredSize(new Dimension(width, height));
        colors = new Color[width / 100][height / 100];

        for (int i = 0; i < colors.length; i++) {
            for (int j = 0; j < colors[i].length; j++) {

                int r = (int) ((255) * Math.random());
                int g = (int) ((255) * Math.random());
                int b = (int) ((255) * Math.random());

                colors[i][j] = new Color(r, g, b, 150);
            }
        }
    }

   // tableComponent.setIcon(
   //         new ImageIcon(image.getScaledInstance(cellWidth, cellHeight, BufferedImage.SCALE_FAST)));

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        for (int i = 0; i < width; i += 100) {
            for (int j = 0; j < height; j += 100) {

                g.setColor(colors[i / 100][j / 100]);
                g.fillRect(i + 5, j + 5, 95, 95);
                g.setColor(Color.black);
                g.drawString(""+i+"x"+j, i+20, j+50);
            }
        }
    }
}
