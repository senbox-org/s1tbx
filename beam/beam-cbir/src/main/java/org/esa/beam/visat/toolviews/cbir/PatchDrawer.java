package org.esa.beam.visat.toolviews.cbir;

import javax.swing.*;
import java.awt.*;

/**

 */
public class PatchDrawer extends JPanel {

    int width = 10000;
    int height = 1000;
    Color[][] colors;

    public PatchDrawer(int w, int h) {
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

    @Override
    public void paintComponent(Graphics g) {
        super.paintComponent(g);

        for (int i = 0; i < width; i += 100) {
            for (int j = 0; j < height; j += 100) {

                g.setColor(colors[i / 100][j / 100]);
                g.fillRect(i + 5, j + 5, 95, 95);
            }
        }
    }
}
