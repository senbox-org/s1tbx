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

import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.event.MouseInputAdapter;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
import java.awt.Image;
import java.awt.Insets;
import java.awt.LinearGradientPaint;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.Shape;
import java.awt.event.ActionEvent;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.List;

/**
 * A navigation control which appears as a screen overlay.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 */
class ButtonOverlayControl extends JComponent {

    private static final int BUTTON_SPACING = 3;
    private static final int BUTTON_AREA_INSET = 5;
    private static final int ARC_SIZE = BUTTON_AREA_INSET * 2;

    private final Dimension buttonDimension;

    private Rectangle2D.Double buttonArea;
    private List<ButtonDef> buttonDefList;
    private int numCols;

    ButtonOverlayControl(Action... actions) {
        this(2, actions);
    }

    ButtonOverlayControl(int numCols, Action... actions) {
        this.numCols = numCols;
        buttonDimension = new Dimension(24, 24);
        buttonDefList = new ArrayList<ButtonDef>();
        for (Action action : actions) {
            buttonDefList.add(new ButtonDef(action, buttonDimension, numCols));
        }

        Dimension preferredSize = computePreferredSize();
        setPreferredSize(preferredSize);
        setBounds(0, 0, preferredSize.width, preferredSize.height);

        updateGeom(getPaintArea());

        final MouseHandler mouseHandler = new MouseHandler();
        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
    }

    @Override
    public Dimension getPreferredSize() {
        if (isPreferredSizeSet()) {
            return super.getPreferredSize();
        }
        return computePreferredSize();
    }

    @Override
    public final void setBounds(int x, int y, int width, int height) {
        super.setBounds(x, y, width, height);
        updateGeom(getPaintArea());
    }


    @Override
    protected void paintComponent(Graphics g) {
        final Rectangle bounds = getPaintArea();
        if (bounds.isEmpty()) {
            return;
        }
        final Graphics2D graphics2D = (Graphics2D) g;
        graphics2D.setStroke(new BasicStroke(0.6f));
        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);

        drawBackground(bounds, graphics2D);

        for (int i = 0; i < buttonDefList.size(); i++) {
            drawButton(graphics2D, i);
        }

    }

    @Override
    public void setEnabled(boolean enabled) {
        super.setEnabled(enabled);
        for (ButtonDef buttonDef : buttonDefList) {
            buttonDef.getAction().setEnabled(enabled);
        }
    }

    private void drawBackground(Rectangle bounds, Graphics2D graphics2D) {
        final Shape backgroundShape = new RoundRectangle2D.Double(bounds.x, bounds.y,
                                                                  bounds.width, bounds.height,
                                                                  ARC_SIZE, ARC_SIZE);
        graphics2D.setColor(Color.BLACK);
        graphics2D.draw(backgroundShape);
        graphics2D.setColor(new Color(255, 255, 255, 64));
        graphics2D.fill(backgroundShape);
    }

    private void drawButton(Graphics2D graphics2D, int buttonIndex) {
        ButtonDef buttonDef = buttonDefList.get(buttonIndex);

        Shape shape = buttonDef.getShape();
        drawGradientShape(graphics2D, shape, buttonDef.isHighlighted());

        Image image = buttonDef.getImage();
        Point imageOffset = buttonDef.getImageOffset();
        graphics2D.drawImage(image, imageOffset.x, imageOffset.y, null);
    }

    private void drawGradientShape(Graphics2D graphics2D, Shape shape, boolean highlighted) {
        graphics2D.setColor(Color.BLACK);
        graphics2D.draw(shape);

        final Point startPos = shape.getBounds().getLocation();
        final Point endPos = (Point) startPos.clone();
        endPos.y = startPos.y + shape.getBounds().height;
        final LinearGradientPaint paint;
        if (highlighted) {
            paint = new LinearGradientPaint(startPos, endPos,
                                            new float[]{0.0f, 0.5f, 0.6f, 0.8f, 1.0f},
                                            new Color[]{
                                                    new Color(255, 255, 255, 64),
                                                    new Color(255, 255, 255, 255),
                                                    new Color(255, 255, 255, 255),
                                                    new Color(255, 255, 255, 160),
                                                    new Color(0, 0, 0, 160)
                                            });
        } else {
            paint = new LinearGradientPaint(startPos, endPos,
                                            new float[]{0.0f, 0.5f, 0.6f, 0.8f, 1.0f},
                                            new Color[]{
                                                    new Color(255, 255, 255, 0),
                                                    new Color(255, 255, 255, 64),
                                                    new Color(255, 255, 255, 64),
                                                    new Color(255, 255, 255, 30),
                                                    new Color(0, 0, 0, 40)
                                            });
        }
        graphics2D.setPaint(paint);
        graphics2D.fill(shape);
    }

    private Point computeButtonLocation(int buttonIndex) {
        final int xIndex = buttonIndex % numCols;
        final int yIndex = buttonIndex / numCols;
        int xLocation = (int) buttonArea.x + xIndex * (buttonDimension.width + BUTTON_SPACING);
        int yLocation = (int) buttonArea.y + yIndex * (buttonDimension.height + BUTTON_SPACING);
        return new Point(xLocation, yLocation);
    }

    private Dimension computePreferredSize() {
        int numButtons = buttonDefList.size();
        int buttonCols = numButtons <= 1 ? 1 : numCols;
        int buttonRows = numButtons <= 1 ? 1 : (numButtons + 1) / buttonCols;

        int width = BUTTON_AREA_INSET;
        int height = BUTTON_AREA_INSET;
        width += buttonCols * (buttonDimension.width);
        height += buttonRows * (buttonDimension.height);
        width += (buttonCols - 1) * (BUTTON_SPACING);
        height += (buttonRows - 1) * (BUTTON_SPACING);
        width += BUTTON_AREA_INSET;
        height += BUTTON_AREA_INSET;
        return new Dimension(width, height);
    }

    private void updateGeom(Rectangle bounds) {
        buttonArea = new Rectangle2D.Double(bounds.x + BUTTON_AREA_INSET - 1,
                                            bounds.y + BUTTON_AREA_INSET - 1,
                                            bounds.width - BUTTON_AREA_INSET * 2 + 2,
                                            bounds.height - BUTTON_AREA_INSET * 2 + 2);
        for (int i = 0; i < buttonDefList.size(); i++) {
            ButtonDef buttonDef = buttonDefList.get(i);
            buttonDef.setShapeLocation(computeButtonLocation(i));
        }
    }

    private Rectangle getPaintArea() {
        final Insets insets = getInsets();
        int x = insets.left + 1;
        int y = insets.top + 1;
        int w = getWidth() - (insets.left + insets.right) - 2;
        int h = getHeight() - (insets.top + insets.bottom) - 2;
        return new Rectangle(x, y, w, h);
    }


    private ButtonDef getButtonDef(int x, int y) {
        for (ButtonDef buttonDef : buttonDefList) {
            if (buttonDef.getShape().contains(x, y)) {
                return buttonDef;
            }
        }
        return null;
    }

    private class MouseHandler extends MouseInputAdapter {

        @Override
        public void mouseClicked(MouseEvent e) {
            ButtonDef buttonDef = getButtonDef(e.getX(), e.getY());
            if (buttonDef != null) {
                final Action action = buttonDef.getAction();
                action.actionPerformed(new ActionEvent(e.getSource(), e.getID(), e.paramString()));
                e.consume();
            }
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            for (ButtonDef buttonDef : buttonDefList) {
                if (buttonDef.getShape().contains(e.getX(), e.getY())) {
                    buttonDef.setHighlighted(true);
                } else {
                    buttonDef.setHighlighted(false);
                }
            }
            repaint();
        }
    }

    private static class ButtonDef {

        private final Action action;
        private final int numCols;
        private final Image image;
        private RoundRectangle2D.Double shape;
        private boolean highlighted;

        private ButtonDef(Action action, Dimension buttonDimension, int numCols) {
            this.action = action;
            this.numCols = numCols;
            Image rawImage = iconToImage((Icon) this.action.getValue(Action.LARGE_ICON_KEY));
            image = rawImage.getScaledInstance(buttonDimension.width,
                                               buttonDimension.height,
                                               Image.SCALE_SMOOTH);
            shape = new RoundRectangle2D.Double();
            shape.arcwidth = 4;
            shape.archeight = 4;
            shape.setFrame(new Point(), buttonDimension);
        }

        public Action getAction() {
            return action;
        }

        public Image getImage() {
            return image;
        }

        public Point getImageOffset() {
            final Rectangle bounds = shape.getBounds();
            final int xOffset = bounds.x + (bounds.width - image.getWidth(null)) / numCols;
            final int yOffset = bounds.y + (bounds.height - image.getHeight(null)) / numCols;
            return new Point(xOffset, yOffset);
        }

        public Shape getShape() {
            return (Shape) shape.clone();
        }

        public void setShapeLocation(Point point) {
            shape.setFrame(point, shape.getBounds().getSize());
        }

        public void setShapeDimension(Dimension dimension) {
            shape.setFrame(shape.getBounds().getLocation(), dimension);
        }

        private static Image iconToImage(Icon icon) {
            if (icon instanceof ImageIcon) {
                return ((ImageIcon) icon).getImage();
            } else {
                int w = icon.getIconWidth();
                int h = icon.getIconHeight();
                GraphicsEnvironment ge =
                        GraphicsEnvironment.getLocalGraphicsEnvironment();
                GraphicsDevice gd = ge.getDefaultScreenDevice();
                GraphicsConfiguration gc = gd.getDefaultConfiguration();
                BufferedImage image = gc.createCompatibleImage(w, h);
                Graphics2D g = image.createGraphics();
                icon.paintIcon(null, g, 0, 0);
                g.dispose();
                return image;
            }
        }

        public void setHighlighted(boolean highlighted) {
            this.highlighted = highlighted;
        }

        public boolean isHighlighted() {
            return highlighted;
        }
    }
}
