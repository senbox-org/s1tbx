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
package org.esa.beam.framework.ui;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;

import javax.swing.*;

/**
 * A component representing an image display with a draggable slider box in it.
 */
public class SliderBoxImageDisplay extends JComponent {

    private static final int HANDLE_SIZE = 6;

    private BufferedImage image;
    private final int imageWidth;
    private final int imageHeight;
    private final SliderBoxChangeListener sliderBoxChangeListener;
    private final JComponent sliderBox;
    private int sliderSectionX;
    private int sliderSectionY;
    private Rectangle sliderRectOld;
    private Point clickPos;
    private boolean imageWidthFixed;
    private boolean imageHeightFixed;


    public SliderBoxImageDisplay(int imageWidth, int imageHeight, SliderBoxChangeListener sliderBoxChangeListener) {
        this.imageWidth = imageWidth;
        this.imageHeight = imageHeight;
        this.sliderBoxChangeListener = sliderBoxChangeListener;

        sliderBox = new JLabel();
        sliderBox.setBounds(0, 0, 1, 1);
        sliderBox.setOpaque(false);
        sliderBox.setBorder(UIDefaults.SLIDER_BOX_BORDER);

        setLayout(null);
        setPreferredSize(new Dimension(imageWidth, imageHeight));
        add(sliderBox);
        clearSliderSections();

        addMouseListener(new MouseAdapter() {

            @Override
            public void mousePressed(MouseEvent e) {
                sliderRectOld = new Rectangle(sliderBox.getBounds());
                clickPos = new Point(e.getPoint());
                computeSliderSections(e);
            }

            @Override
            public void mouseReleased(MouseEvent e) {
                sliderRectOld = null;
                clickPos = null;
                clearSliderSections();
            }
        });

        addMouseMotionListener(new MouseMotionAdapter() {

            @Override
            public void mouseDragged(MouseEvent e) {
                if (sliderRectOld == null || clickPos == null) {
                    return;
                }
                modifySliderBox(e);
            }
        });
    }

    public void setImage(BufferedImage image) {
        this.image = image;
        setPreferredSize(new Dimension(image.getWidth(), image.getHeight()));
        repaint();
    }

    @Override
    protected void paintComponent(Graphics graphics) {
        graphics.setColor(getBackground());
        if (image != null) {
            graphics.drawImage(image, 0, 0, null);
        }
    }

    public boolean isImageWidthFixed() {
        return imageWidthFixed;
    }

    public void setImageWidthFixed(boolean imageWidthFixed) {
        if (this.imageWidthFixed == imageWidthFixed) {
            return;
        }
        this.imageWidthFixed = imageWidthFixed;
        if (this.imageWidthFixed) {
            setSliderBoxBounds(0, sliderBox.getY(), imageWidth, sliderBox.getHeight(), true);
        }
    }

    public boolean isImageHeightFixed() {
        return imageHeightFixed;
    }

    public void setImageHeightFixed(boolean imageHeightFixed) {
        if (this.imageHeightFixed == imageHeightFixed) {
            return;
        }
        this.imageHeightFixed = imageHeightFixed;
        if (this.imageHeightFixed) {
            setSliderBoxBounds(sliderBox.getX(), 0, sliderBox.getWidth(), imageHeight, true);
        }
    }

    public SliderBoxChangeListener getSliderBoxChangeListener() {
        return sliderBoxChangeListener;
    }

    public Rectangle getSliderBoxBounds() {
        return sliderBox.getBounds();
    }

    public void setSliderBoxBounds(Rectangle rectangle) {
        setSliderBoxBounds(rectangle, false);
    }

    public void setSliderBoxBounds(Rectangle rectangle, boolean fireEvent) {
        setSliderBoxBounds(rectangle.x, rectangle.y, rectangle.width, rectangle.height, fireEvent);
    }

    public void setSliderBoxBounds(int x, int y, int width, int height) {
        setSliderBoxBounds(x, y, width, height, false);
    }

    public void setSliderBoxBounds(int x, int y, int width, int height, boolean fireEvent) {
        if (isImageWidthFixed()) {
            x = 0;
            width = imageWidth;
        }
        if (isImageHeightFixed()) {
            y = 0;
            height = imageHeight;
        }
        if (sliderBox.getX() == x
            && sliderBox.getY() == y
            && sliderBox.getWidth() == width
            && sliderBox.getHeight() == height) {
            return;
        }
        sliderBox.setBounds(x, y, width, height); // also repaints!
        if (sliderBoxChangeListener != null && fireEvent) {
            sliderBoxChangeListener.sliderBoxChanged(sliderBox.getBounds());
        }
    }


    private void clearSliderSections() {
        sliderSectionX = -1;
        sliderSectionY = -1;
    }

    private void computeSliderSections(MouseEvent e) {

        int x = e.getX();
        int y = e.getY();
        int x1 = sliderBox.getX();
        int y1 = sliderBox.getY();
        int x2 = sliderBox.getX() + sliderBox.getWidth();
        int y2 = sliderBox.getY() + sliderBox.getHeight();
        int dx1 = Math.abs(x1 - x);
        int dy1 = Math.abs(y1 - y);
        int dx2 = Math.abs(x2 - x);
        int dy2 = Math.abs(y2 - y);

        sliderSectionX = -1;
        if (dx1 <= HANDLE_SIZE) {
            sliderSectionX = 0;   // left slider handle selected
        } else if (dx2 <= HANDLE_SIZE) {
            sliderSectionX = 2;   // right slider handle selected
        } else if (x >= x1 && x < x2) {
            sliderSectionX = 1;   // center slioder handle selected
        }

        sliderSectionY = -1;
        if (dy1 <= HANDLE_SIZE) {
            sliderSectionY = 0; // upper slider handle selected
        } else if (dy2 <= HANDLE_SIZE) {
            sliderSectionY = 2; // lower slider handle selected
        } else if (y > y1 && y < y2) {
            sliderSectionY = 1; // center slider handle selected
        }
    }

    private void modifySliderBox(MouseEvent e) {
        int x = e.getX();
        int y = e.getY();
        int dx = x - clickPos.x;
        int dy = y - clickPos.y;

        int sbx = 0;
        int sby = 0;
        int sbw = 0;
        int sbh = 0;
        boolean validMode = false;

        if (sliderSectionX == 0 && sliderSectionY == 0) {
            sbx = sliderRectOld.x + dx;
            sby = sliderRectOld.y + dy;
            sbw = sliderRectOld.width - dx;
            sbh = sliderRectOld.height - dy;
            validMode = true;
        } else if (sliderSectionX == 1 && sliderSectionY == 0) {
            sbx = sliderRectOld.x;
            sby = sliderRectOld.y + dy;
            sbw = sliderRectOld.width;
            sbh = sliderRectOld.height - dy;
            validMode = true;
        } else if (sliderSectionX == 2 && sliderSectionY == 0) {
            sbx = sliderRectOld.x;
            sby = sliderRectOld.y + dy;
            sbw = sliderRectOld.width + dx;
            sbh = sliderRectOld.height - dy;
            validMode = true;
        } else if (sliderSectionX == 0 && sliderSectionY == 1) {
            sbx = sliderRectOld.x + dx;
            sby = sliderRectOld.y;
            sbw = sliderRectOld.width - dx;
            sbh = sliderRectOld.height;
            validMode = true;
        } else if (sliderSectionX == 1 && sliderSectionY == 1) {
            sbx = sliderRectOld.x + dx;
            sby = sliderRectOld.y + dy;
            sbw = sliderRectOld.width;
            sbh = sliderRectOld.height;
            validMode = true;
        } else if (sliderSectionX == 2 && sliderSectionY == 1) {
            sbx = sliderRectOld.x;
            sby = sliderRectOld.y;
            sbw = sliderRectOld.width + dx;
            sbh = sliderRectOld.height;
            validMode = true;
        } else if (sliderSectionX == 0 && sliderSectionY == 2) {
            sbx = sliderRectOld.x + dx;
            sby = sliderRectOld.y;
            sbw = sliderRectOld.width - dx;
            sbh = sliderRectOld.height + dy;
            validMode = true;
        } else if (sliderSectionX == 1 && sliderSectionY == 2) {
            sbx = sliderRectOld.x;
            sby = sliderRectOld.y;
            sbw = sliderRectOld.width;
            sbh = sliderRectOld.height + dy;
            validMode = true;
        } else if (sliderSectionX == 2 && sliderSectionY == 2) {
            sbx = sliderRectOld.x;
            sby = sliderRectOld.y;
            sbw = sliderRectOld.width + dx;
            sbh = sliderRectOld.height + dy;
            validMode = true;
        }

        if (validMode && sbw > 2 && sbh > 2) {
            setSliderBoxBounds(sbx, sby, sbw, sbh, true);
        }
    }

    public static interface SliderBoxChangeListener {

        void sliderBoxChanged(Rectangle sliderBoxBounds);
    }
}
