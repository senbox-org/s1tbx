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
package com.bc.ceres.glayer.swing;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.MouseInputAdapter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.util.EventListener;

/**
 * A navigation control which appears as a screen overlay.
 * It can fire rotation, translation and scale events.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 */
public class NavControl2 extends JComponent {
    private static final int W = 100;
    private static final int H = 120;
    private static final Dimension SIZE = new Dimension(W, H);

    private final double pannerHandleW = 10;
    private final double pannerHandleH = 10;

    private final double scaleHandleW = 5;
    private final double scaleHandleH = 16;
    private final double gap = 4;

    private static final int TIMER_DELAY = 50;

    private double rotationAngle;
    private double pannerHandleOffsetX;
    private double pannerHandleOffsetY;
    private double scaleHandleOffsetX;
    private double scaleHandleOffsetY;

    private Ellipse2D outerWheelCircle;
    private Ellipse2D innerWheelCircle;
    private Ellipse2D outerMoveCircle;
    private Shape pannerHandle;
    private Shape[] moveArrowShapes;
    private Area[] rotationUnitShapes;
    private RectangularShape scaleHandle;
    private RectangularShape scaleBar;

    private BufferedImage backgroundImage;
    private BufferedImage rotationWheelImage;
    private BufferedImage pannerHandleImage;
    private BufferedImage scaleHandleImage;


    public NavControl2() {
        final MouseHandler mouseHandler = new MouseHandler();
        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
        setBounds(0, 0, W, H);
    }

    /**
     * Gets the model's current rotation angle in degrees.
     *
     * @return the model's current rotation angle in degrees.
     */
    public double getRotationAngle() {
        return rotationAngle;
    }

    /**
     * Sets the current rotation angle in degrees.
     *
     * @param rotationAngle the model's current rotation angle in degrees.
     */
    public void setRotationAngle(double rotationAngle) {
        double oldRotationAngle = this.rotationAngle;
        if (oldRotationAngle != rotationAngle) {
            this.rotationAngle = rotationAngle;
            firePropertyChange("rotationAngle", oldRotationAngle, rotationAngle);
        }
    }

    @Override
    public Dimension getPreferredSize() {
        return SIZE;
    }

    @Override
    public Dimension getMinimumSize() {
        return SIZE;
    }

    @Override
    public Dimension getMaximumSize() {
        return SIZE;
    }

    @Override
    protected void paintComponent(Graphics g) {

        final Graphics2D graphics2D = (Graphics2D) g;
        final AffineTransform oldTransform = graphics2D.getTransform();
        graphics2D.setStroke(new BasicStroke(0.5f));
        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics2D.rotate(-Math.PI * 0.5 - Math.toRadians(getRotationAngle()),
                          outerWheelCircle.getCenterX(),
                          outerWheelCircle.getCenterY());
        for (int i = 0; i < rotationUnitShapes.length; i++) {
            graphics2D.setColor(i == 0 ? Color.ORANGE : Color.WHITE);
            graphics2D.fill(rotationUnitShapes[i]);
            graphics2D.setColor(Color.BLACK);
            graphics2D.draw(rotationUnitShapes[i]);
        }
        graphics2D.setTransform(oldTransform);

        for (Shape arrow : moveArrowShapes) {
            graphics2D.setColor(Color.WHITE);
            graphics2D.fill(arrow);
            graphics2D.setColor(Color.BLACK);
            graphics2D.draw(arrow);
        }

        graphics2D.translate(pannerHandleOffsetX, pannerHandleOffsetY);
        graphics2D.setColor(Color.WHITE);
        graphics2D.fill(pannerHandle);
        graphics2D.setColor(Color.BLACK);
        graphics2D.draw(pannerHandle);
        graphics2D.setTransform(oldTransform);

        graphics2D.setColor(Color.WHITE);
        graphics2D.fill(scaleBar);
        graphics2D.setColor(Color.BLACK);
        graphics2D.draw(scaleBar);

        graphics2D.translate(scaleHandleOffsetX, scaleHandleOffsetY);
        graphics2D.setColor(Color.WHITE);
        graphics2D.fill(scaleHandle);
        graphics2D.setColor(Color.BLACK);
        graphics2D.draw(scaleHandle);
        graphics2D.setTransform(oldTransform);
    }

    @Override
    public boolean contains(int x, int y) {
        return getAction(x, y) != ACTION_NONE;
    }

    private void initGeom() {

        final Insets insets = getInsets();
        double x = 1;
        double y = 1;
        final double outerRotationDiameter = W - 2;

        final double innerRotationDiameter = 0.8 * outerRotationDiameter;
        final double outerMoveDiameter = 0.4 * outerRotationDiameter;

        outerWheelCircle = new Ellipse2D.Double(x,
                                                y,
                                                outerRotationDiameter,
                                                outerRotationDiameter);
        innerWheelCircle = new Ellipse2D.Double(outerWheelCircle.getCenterX() - 0.5 * innerRotationDiameter,
                                                outerWheelCircle.getCenterY() - 0.5 * innerRotationDiameter,
                                                innerRotationDiameter,
                                                innerRotationDiameter);
        outerMoveCircle = new Ellipse2D.Double(innerWheelCircle.getCenterX() - 0.5 * outerMoveDiameter,
                                               innerWheelCircle.getCenterY() - 0.5 * outerMoveDiameter,
                                               outerMoveDiameter,
                                               outerMoveDiameter);

        rotationUnitShapes = createRotationUnitShapes();
        moveArrowShapes = createMoveArrows();
        pannerHandle = createPanHandle();

        /////////////////////////////////////////////////////////

        final double scaleBarW = outerRotationDiameter;
        final double scaleBarH = scaleHandleW;

        final double scaleBarX = x;
        final double scaleHandleY = y + outerRotationDiameter + gap;

        scaleBar = new Rectangle2D.Double(scaleBarX,
                                          scaleHandleY + 0.5 * (scaleHandleH - scaleBarH),
                                          scaleBarW,
                                          scaleBarH);

        scaleHandle = new Rectangle2D.Double(scaleBarX + 0.5 * (scaleBarW - scaleHandleW),
                                             scaleHandleY,
                                             scaleHandleW,
                                             scaleHandleH);

        //////////////////////////////////////////////////////////////////////

        scaleHandleImage = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);

        //////////////////////////////////////////////////////////////////////
        // backgroundImage
        backgroundImage = new BufferedImage(W, H, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g1 = backgroundImage.createGraphics();
        g1.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g1.setStroke(new BasicStroke(1f));
        for (Shape arrow : moveArrowShapes) {
            g1.setColor(Color.WHITE);
            g1.fill(arrow);
            g1.setColor(Color.BLACK);
            g1.draw(arrow);
        }
        g1.setColor(Color.WHITE);
        g1.fill(scaleBar);
        g1.setColor(Color.BLACK);
        g1.draw(scaleBar);
        g1.dispose();
        // backgroundImage
        //////////////////////////////////////////////////////////////////////

        //////////////////////////////////////////////////////////////////////
        // rotationWheelImage
        rotationWheelImage = new BufferedImage(W, W, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g2 = backgroundImage.createGraphics();
        final AffineTransform oldTransform = g2.getTransform();
        g2.setStroke(new BasicStroke(1f));
        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.rotate(-Math.PI * 0.5 - Math.toRadians(getRotationAngle()),
                          outerWheelCircle.getCenterX(),
                          outerWheelCircle.getCenterY());
        for (int i = 0; i < rotationUnitShapes.length; i++) {
            g2.setColor(i == 0 ? Color.ORANGE : Color.WHITE);
            g2.fill(rotationUnitShapes[i]);
            g2.setColor(Color.BLACK);
            g2.draw(rotationUnitShapes[i]);
        }
        g2.setTransform(oldTransform);
        g2.dispose();
        // backgroundImage
        //////////////////////////////////////////////////////////////////////


        //////////////////////////////////////////////////////////////////////
        // pannerHandleImage
        pannerHandleImage = new BufferedImage(W, W, BufferedImage.TYPE_INT_ARGB);
        final Graphics2D g3 = backgroundImage.createGraphics();
        g3.setColor(Color.WHITE);
        g3.fill(pannerHandle);
        g3.setColor(Color.BLACK);
        g3.draw(pannerHandle);
        g3.dispose();

        g2.setColor(Color.WHITE);
        g2.fill(scaleBar);
        g2.setColor(Color.BLACK);
        g2.draw(scaleBar);

        g2.translate(scaleHandleOffsetX, scaleHandleOffsetY);
        g2.setColor(Color.WHITE);
        g2.fill(scaleHandle);
        g2.setColor(Color.BLACK);
        g2.draw(scaleHandle);
        g2.setTransform(oldTransform);

    }

    private Shape createPanHandle() {
        final Rectangle2D r1 = new Rectangle2D.Double(-0.5 * pannerHandleW, -0.5 * pannerHandleH, pannerHandleW, pannerHandleH);
        final Shape r2 = AffineTransform.getRotateInstance(0.25 * Math.PI).createTransformedShape(r1);
        Area area = new Area(r1);
        area.add(new Area(r2));
        return AffineTransform.getTranslateInstance(innerWheelCircle.getCenterX(), innerWheelCircle.getCenterY()).createTransformedShape(area);
    }

    private Shape[] createMoveArrows() {
        final double innerRadius = 0.5 * innerWheelCircle.getWidth();
        final GeneralPath path = new GeneralPath();
        path.moveTo(0, 0);
        path.lineTo(2, 1);
        path.lineTo(1, 1);
        path.lineTo(1, 2);
        path.lineTo(-1, 2);
        path.lineTo(-1, 1);
        path.lineTo(-2, 1);
        path.closePath();
        Shape[] arrows = new Shape[4];
        for (int i = 0; i < 4; i++) {
            final AffineTransform at = new AffineTransform();
            at.rotate(i * 0.5 * Math.PI, innerWheelCircle.getCenterX(), innerWheelCircle.getCenterY());
            at.translate(innerWheelCircle.getCenterX(), innerWheelCircle.getCenterY() - 0.95 * innerRadius);
            at.scale(0.2 * innerRadius, 0.3 * innerRadius);
            final Area area = new Area(at.createTransformedShape(path));
            area.subtract(new Area(outerMoveCircle));
            arrows[i] = area;
        }
        return arrows;
    }

    private Area[] createRotationUnitShapes() {
        final Area[] areas = new Area[36];
        final Area rhs = new Area(innerWheelCircle);
        for (int i = 0; i < 36; i++) {
            final Arc2D.Double arc = new Arc2D.Double(outerWheelCircle.getX(),
                                                      outerWheelCircle.getY(),
                                                      outerWheelCircle.getWidth(),
                                                      outerWheelCircle.getHeight(),
                                                      i * 10 - 0.5 * 7,
                                                      7,
                                                      Arc2D.PIE);
            final Area area = new Area(arc);
            area.subtract(rhs);
            areas[i] = area;
        }
        return areas;
    }

    public void addSelectionListener(SelectionListener l) {
        listenerList.add(SelectionListener.class, l);
    }

    public void removeSelectionListener(SelectionListener l) {
        listenerList.remove(SelectionListener.class, l);
    }

    public SelectionListener[] getSelectionListeners() {
        return listenerList.getListeners(SelectionListener.class);
    }

    protected void fireRotate(double rotationAngle) {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == SelectionListener.class) {
                ((SelectionListener) listeners[i + 1]).handleRotate(rotationAngle);
            }
        }
    }

    protected void fireMove(double moveDirX, double moveDirY) {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == SelectionListener.class) {
                ((SelectionListener) listeners[i + 1]).handleMove(moveDirX, moveDirY);
            }
        }
    }

    protected void fireScale(double scaleDir) {
        Object[] listeners = listenerList.getListenerList();
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == SelectionListener.class) {
                ((SelectionListener) listeners[i + 1]).handleScale(scaleDir);
            }
        }
    }

    private double getAngle(Point point) {
        final double a = Math.atan2(-(point.y - innerWheelCircle.getCenterY()),
                                    point.x - innerWheelCircle.getCenterX());
        return normaliseAngle(a - 0.5 * Math.PI);
    }

    private static double normaliseAngle(double a) {
        while (a < 0.0) {
            a += 2.0 * Math.PI;
        }
        a %= 2.0 * Math.PI;
        if (a > Math.PI) {
            a += -2.0 * Math.PI;
        }
        return a;
    }

    int getAction(int x, int y) {
        if (super.contains(x, y)) {
            if (outerWheelCircle.contains(x, y) && !innerWheelCircle.contains(x, y)) {
                return ACTION_ROT;
            } else if (pannerHandle.contains(x, y)) {
                return ACTION_PAN;
            } else if (scaleHandle.contains(x, y) || scaleBar.contains(x, y)) {
                return ACTION_SCALE;
            } else {
                for (int i = 0; i < moveArrowShapes.length; i++) {
                    Shape moveArrowShape = moveArrowShapes[i];
                    if (moveArrowShape.contains(x, y)) {
                        return ACTION_MOVE_DIRS[i];
                    }
                }
            }
        }
        return ACTION_NONE;
    }

    public static interface SelectionListener extends EventListener {
        void handleRotate(double rotationAngle);

        void handleMove(double moveDirX, double moveDirY);

        void handleScale(double scaleDir);
    }

    private final static int ACTION_NONE = 0;
    private final static int ACTION_ROT = 1;
    private final static int ACTION_SCALE = 2;
    private final static int ACTION_PAN = 3;
    private final static int ACTION_MOVE_N = 4;
    private final static int ACTION_MOVE_S = 5;
    private final static int ACTION_MOVE_W = 6;
    private final static int ACTION_MOVE_E = 7;

    private final static int[] ACTION_MOVE_DIRS = {ACTION_MOVE_N, ACTION_MOVE_S, ACTION_MOVE_W, ACTION_MOVE_E};
    private static final double[] X_DIRS = new double[]{0, -1, 0, 1};
    private static final double[] Y_DIRS = new double[]{1, 0, -1, 0};

    private class MouseHandler extends MouseInputAdapter implements ActionListener {
        private Point point0;
        private double rotationAngle0;
        private double moveDirX;
        private double moveDirY;
        private double moveAcc;
        private double scaleDir;
        private double scaleAcc;
        private Cursor cursor0;
        private final Timer actionTrigger;
        private int action;  // see MODE_XXX values

        private MouseHandler() {
            actionTrigger = new Timer(TIMER_DELAY, this);
            action = ACTION_NONE;
        }

        public void actionPerformed(ActionEvent e) {
            if (action == ACTION_PAN
                    || action == ACTION_MOVE_N
                    || action == ACTION_MOVE_S
                    || action == ACTION_MOVE_W
                    || action == ACTION_MOVE_E) {
                fireAcceleratedMove();
            } else if (action == ACTION_SCALE) {
                fireAcceleratedScale();
            }
        }

        @Override
        public void mousePressed(MouseEvent e) {
            cursor0 = getCursor();
            point0 = e.getPoint();
            moveAcc = 1.0;
            scaleAcc = 1.0;
            action = getAction(e.getX(), e.getY());

            if (action == ACTION_ROT) {
                rotationAngle0 = getRotationAngle();
            } else if (action == ACTION_SCALE) {
                doScale(e);
            } else if (action == ACTION_MOVE_N) {
                startMove(0);
            } else if (action == ACTION_MOVE_S) {
                startMove(1);
            } else if (action == ACTION_MOVE_W) {
                startMove(2);
            } else if (action == ACTION_MOVE_E) {
                startMove(3);
            }
            if (action != ACTION_NONE) {
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if (action == ACTION_ROT) {
                doRotate(e);
            } else if (action == ACTION_PAN) {
                doPan(e);
            } else if (action == ACTION_SCALE) {
                doScale(e);
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            stopAction();
        }

        private void doScale(MouseEvent e) {
            final Point point = e.getPoint();
            double dx = point.x - scaleBar.getCenterX();
            double a = 0.5 * (scaleBar.getWidth() - scaleHandle.getWidth());
            if (dx < -a) {
                dx = -a;
            }
            if (dx > +a) {
                dx = +a;
            }
            scaleHandleOffsetX = dx;
            scaleHandleOffsetY = 0;
            scaleDir = dx / a;
            scaleAcc = 1.0;
            fireAcceleratedScale();
            startTriggeringActions();
        }

        private void startTriggeringActions() {
            System.out.println("NavControl.startTriggeringActions() >>>");
            actionTrigger.restart();
        }

        private void stopTriggeringActions() {
            System.out.println("NavControl.stopTriggeringActions() <<<");
            actionTrigger.stop();
        }

        private void doPan(MouseEvent e) {
            final Point point = e.getPoint();
            final double outerMoveRadius = 0.5 * outerMoveCircle.getWidth();
            double dx = point.x - outerMoveCircle.getCenterX();
            double dy = point.y - outerMoveCircle.getCenterY();
            final double r = Math.sqrt(dx * dx + dy * dy);
            if (r > outerMoveRadius) {
                dx = outerMoveRadius * dx / r;
                dy = outerMoveRadius * dy / r;
            }
            pannerHandleOffsetX = dx;
            pannerHandleOffsetY = dy;
            moveDirX = -dx / outerMoveRadius;
            moveDirY = -dy / outerMoveRadius;
            moveAcc = 1.0;
            fireAcceleratedMove();
            startTriggeringActions();
        }

        void startMove(int dir) {
            moveDirX = X_DIRS[dir];
            moveDirY = Y_DIRS[dir];
            doMove();
        }

        private void doMove() {
            moveAcc = 1.0;
            startTriggeringActions();
        }

        private void doRotate(MouseEvent e) {
            double a1 = getAngle(point0);
            double a2 = getAngle(e.getPoint());
            double a = Math.toDegrees(normaliseAngle(Math.toRadians(rotationAngle0) + (a2 - a1)));
            if (e.isControlDown()) {
                double t = 0.5 * 45.0;
                a = t * Math.floor(a / t);
            }
            setRotationAngle(a);
            repaint();
            fireRotate(a);
        }

        private void fireAcceleratedScale() {
            fireScale(scaleAcc * scaleDir / 4.0);
            scaleAcc *= 1.1;
            if (scaleAcc > 4.0) {
                scaleAcc = 4.0;
            }
        }

        private void fireAcceleratedMove() {
            fireMove(moveAcc * moveDirX, moveAcc * moveDirY);
            moveAcc *= 1.05;
            if (moveAcc > 4.0) {
                moveAcc = 4.0;
            }
        }

        private void stopAction() {
            setCursor(cursor0);
            stopTriggeringActions();
            cursor0 = null;
            point0 = null;
            action = ACTION_NONE;

            moveDirX = 0;
            moveDirY = 0;
            moveAcc = 1.0;
            pannerHandleOffsetX = 0;
            pannerHandleOffsetY = 0;

            scaleDir = 0;
            scaleAcc = 1.0;
            scaleHandleOffsetX = 0;
            scaleHandleOffsetY = 0;

            repaint();
        }

    }

    public static void main(String[] args) {
        final JFrame frame = new JFrame("NavControl");
        final JPanel panel = new JPanel(new BorderLayout(3, 3));
        panel.setBackground(Color.GRAY);
        final JLabel label = new JLabel("Angle: ");
        final NavControl2 navControl = new NavControl2();
        navControl.addSelectionListener(new SelectionListener() {
            public void handleRotate(double rotationAngle) {
                label.setText("Angle: " + rotationAngle);
                System.out.println("NavControl: rotationAngle = " + rotationAngle);
            }

            public void handleMove(double moveDirX, double moveDirY) {
                System.out.println("NavControl: moveDirX = " + moveDirX + ", moveDirY = " + moveDirY);
            }

            public void handleScale(double scaleDir) {
                System.out.println("NavControl: scaleDir = " + scaleDir);
            }
        });

        navControl.setBorder(new EmptyBorder(4, 4, 4, 4));
        panel.add(label, BorderLayout.SOUTH);
        panel.add(navControl, BorderLayout.CENTER);
        frame.add(panel);
        frame.pack();
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setVisible(true);
    }

    private class I {
        int x0;
        int y0;
        BufferedImage image;
        Shape shape;
        Shape testShape;

        private I(int x0, int y0, BufferedImage image, Shape shape) {
            this.x0 = x0;
            this.y0 = y0;
            this.image = image;
            this.shape = shape;
        }

        private void draw(Graphics2D g ) {
            g.drawImage(image, null, x0, y0);

        }

        private boolean contains(int x, int y) {
             return testShape.contains(x - x0 - 0.5* image.getWidth(),
                                       y - y0 - 0.5* image.getHeight());
        }
    }

}