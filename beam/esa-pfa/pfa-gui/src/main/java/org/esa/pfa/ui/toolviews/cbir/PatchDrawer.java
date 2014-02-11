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
package org.esa.pfa.ui.toolviews.cbir;

import org.esa.pfa.fe.op.Patch;

import javax.swing.*;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.image.BufferedImage;

/**

 */
public class PatchDrawer extends JPanel implements MouseListener {

    private final static int width = 100;
    private final static int height = 100;
    private final static int margin = 4;

    private static final boolean DEBUG = true;
    private static final Font font = new Font("Ariel", Font.BOLD, 18);

    private static final java.net.URL imageURL = PatchDrawer.class.getClassLoader().getResource("images/check_ball.png");
    private static final ImageIcon icon = new ImageIcon(imageURL);

    private static enum SelectionMode { CHECK, RECT }
    private SelectionMode mode = SelectionMode.CHECK;

    private PatchDrawing selection = null;

    public PatchDrawer(final Patch[] imageList) {
        super();

        addMouseListener(this);
        update(imageList);
    }

    public void update(final Patch[] imageList) {
        this.removeAll();

        for(Patch patch : imageList) {
            final PatchDrawing label = new PatchDrawing(patch);
            this.add(label);
        }
        updateUI();
    }

    /**
     * Invoked when the mouse button has been clicked (pressed
     * and released) on a component.
     */
    public void mouseClicked(MouseEvent e){
        final Point p = e.getPoint();
        int offset = 0;
        int x = (p.x / (width + margin+1)) + offset;
        int y = (p.y / (height +  margin+1)) + offset;
        int numColumns = this.getWidth() / (width + margin);
        selection = (PatchDrawing)this.getComponent((y*numColumns) + x);

        repaint();
    }

    /**
     * Invoked when a mouse button has been pressed on a component.
     */
    public void mousePressed(MouseEvent e){
    }

    /**
     * Invoked when a mouse button has been released on a component.
     */
    public void mouseReleased(MouseEvent e){
        System.out.println(e.getX()+", "+e.getY());
    }

    /**
     * Invoked when the mouse enters a component.
     */
    public void mouseEntered(MouseEvent e){
    }

    /**
     * Invoked when the mouse exits a component.
     */
    public void mouseExited(MouseEvent e) {
    }


    private class PatchDrawing extends JLabel {
        private final Patch patch;

        public PatchDrawing(final Patch patch) {
            super();
            this.patch = patch;

            setIcon(new ImageIcon(patch.getImage().getScaledInstance(width, height, BufferedImage.SCALE_FAST)));
        }

        @Override
        public void paintComponent(Graphics graphics) {
            super.paintComponent(graphics);
            final Graphics2D g = (Graphics2D) graphics;

            if(DEBUG) {
                g.setColor(Color.WHITE);
                g.fillRect(30, 30, 40, 30);
                g.setColor(Color.RED);
                g.setFont(font);
                g.drawString(Integer.toString(patch.getID()), 35, 50);
            }

            if(this.equals(selection)) {
                if(mode.equals(SelectionMode.CHECK)) {
                    g.drawImage(icon.getImage(), 0, 0, null);
                } else {
                    g.setColor(Color.CYAN);
                    g.setStroke(new BasicStroke(5));
                    g.drawRoundRect(0, 0, width, height-5, 25, 25);
                }
            }
        }
    }
}
