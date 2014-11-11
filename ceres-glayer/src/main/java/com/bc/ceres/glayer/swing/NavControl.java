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

/**
 * A navigation control which appears as a screen overlay.
 * It can fire rotation, translation and scale events.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 */
public class NavControl extends JComponent {
    private static final Dimension PREFERRED_SIZE = new Dimension(100, 120);
    private static final int TIMER_DELAY = 50;

    private final NavControlModel model;
    private double pannerHandleOffsetX;
    private double pannerHandleOffsetY;
    private double scaleHandleOffsetX;
    private double scaleHandleOffsetY;

    private Ellipse2D outerRotationCircle;
    private Ellipse2D innerRotationCircle;
    private Ellipse2D outerMoveCircle;
    private Shape pannerHandle;
    private Shape[] moveArrowShapes;
    private Area[] rotationUnitShapes;
    private RectangularShape scaleHandle;
    private RectangularShape scaleBar;

    public NavControl(NavControlModel model) {
        this.model = model;
        final MouseHandler mouseHandler = new MouseHandler();
        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
        setBounds(0, 0, PREFERRED_SIZE.width, PREFERRED_SIZE.height);
    }

    @Override
    public Dimension getPreferredSize() {
        if (isPreferredSizeSet()) {
            return super.getPreferredSize();
        }
        return PREFERRED_SIZE;
    }

    @Override
    public void setBounds(int x, int y, int width, int height) {
        super.setBounds(x, y, width, height);
        updateGeom();
    }

    @Override
    protected void paintComponent(Graphics g) {
        final Graphics2D graphics2D = (Graphics2D) g;
        final AffineTransform oldTransform = graphics2D.getTransform();
        graphics2D.setStroke(new BasicStroke(0.6f));
        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics2D.rotate(-Math.PI * 0.5 - Math.toRadians(model.getCurrentAngle()),
                          outerRotationCircle.getCenterX(),
                          outerRotationCircle.getCenterY());
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

    /**
     * Gives the UI delegate an opportunity to define the precise
     * shape of this component for the sake of mouse processing.
     *
     * @return true if this component logically contains x,y
     * @see java.awt.Component#contains(int, int)
     * @see javax.swing.plaf.ComponentUI
     */
    @Override
    public boolean contains(int x, int y) {
        return getAction(x, y) != ACTION_NONE;
    }

    private void updateGeom() {
        final double scaleHandleW = Math.max(4, 0.025 * getWidth());
        final double scaleHandleH = 4 * scaleHandleW;
        final double gap = Math.max(4, 0.05 * getHeight());

        final Insets insets = getInsets();
        double x = insets.left;
        double y = insets.top;
        double w = getWidth() - (insets.left + insets.right) - 2;
        double h = getHeight() - (insets.top + insets.bottom + gap + scaleHandleH) - 2;
        final double outerRotationDiameter;
        if (w > h) {
            x += (w - h) / 2;
            outerRotationDiameter = h;
        } else {
            y += (h - w) / 2;
            outerRotationDiameter = w;
        }
        final double innerRotationDiameter = 0.8 * outerRotationDiameter;
        final double outerMoveDiameter = 0.4 * outerRotationDiameter;

        outerRotationCircle = new Ellipse2D.Double(x,
                                                   y,
                                                   outerRotationDiameter,
                                                   outerRotationDiameter);
        innerRotationCircle = new Ellipse2D.Double(outerRotationCircle.getCenterX() - 0.5 * innerRotationDiameter,
                                                   outerRotationCircle.getCenterY() - 0.5 * innerRotationDiameter,
                                                   innerRotationDiameter,
                                                   innerRotationDiameter);
        outerMoveCircle = new Ellipse2D.Double(innerRotationCircle.getCenterX() - 0.5 * outerMoveDiameter,
                                               innerRotationCircle.getCenterY() - 0.5 * outerMoveDiameter,
                                               outerMoveDiameter,
                                               outerMoveDiameter);

        rotationUnitShapes = createRotationUnitShapes();
        moveArrowShapes = createMoveArrows();
        pannerHandle = createPanner();

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
    }

    private Shape createPanner() {
        final double innerRadius = 0.5 * innerRotationCircle.getWidth();
        final double s = 0.25 * innerRadius;
        final Rectangle2D r1 = new Rectangle2D.Double(-0.5 * s, -0.5 * s, s, s);
        final Shape r2 = AffineTransform.getRotateInstance(0.25 * Math.PI).createTransformedShape(r1);
        Area area = new Area(r1);
        area.add(new Area(r2));
        return AffineTransform.getTranslateInstance(innerRotationCircle.getCenterX(), innerRotationCircle.getCenterY()).createTransformedShape(area);
    }

    private Shape[] createMoveArrows() {
        final double innerRadius = 0.5 * innerRotationCircle.getWidth();
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
            at.rotate(i * 0.5 * Math.PI, innerRotationCircle.getCenterX(), innerRotationCircle.getCenterY());
            at.translate(innerRotationCircle.getCenterX(), innerRotationCircle.getCenterY() - 0.95 * innerRadius);
            at.scale(0.2 * innerRadius, 0.3 * innerRadius);
            final Area area = new Area(at.createTransformedShape(path));
            area.subtract(new Area(outerMoveCircle));
            arrows[i] = area;
        }
        return arrows;
    }

    private Area[] createRotationUnitShapes() {
        final Area[] areas = new Area[36];
        final Area rhs = new Area(innerRotationCircle);
        for (int i = 0; i < 36; i++) {
            final Arc2D.Double arc = new Arc2D.Double(outerRotationCircle.getX(),
                                                      outerRotationCircle.getY(),
                                                      outerRotationCircle.getWidth(),
                                                      outerRotationCircle.getHeight(),
                                                      i * 10 - 0.5 * 7,
                                                      7,
                                                      Arc2D.PIE);
            final Area area = new Area(arc);
            area.subtract(rhs);
            areas[i] = area;
        }
        return areas;
    }

    private double getAngle(Point point) {
        final double a = Math.atan2(-(point.y - innerRotationCircle.getCenterY()),
                                    point.x - innerRotationCircle.getCenterX());
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
            if (outerRotationCircle.contains(x, y) && !innerRotationCircle.contains(x, y)) {
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

    public static interface NavControlModel {
        double getCurrentAngle();
        
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
                rotationAngle0 = model.getCurrentAngle();
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
            actionTrigger.restart();
        }

        private void stopTriggeringActions() {
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
            model.handleRotate(a);
            repaint();
        }

        private void fireAcceleratedScale() {
            model.handleScale(scaleAcc * scaleDir / 4.0);
            scaleAcc *= 1.1;
            if (scaleAcc > 4.0) {
                scaleAcc = 4.0;
            }
        }

        private void fireAcceleratedMove() {
            model.handleMove(moveAcc * moveDirX, moveAcc * moveDirY);
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
        final NavControl navControl = new NavControl(new NavControlModel() {
            double angle;
            @Override
            public double getCurrentAngle() {
                return angle;
            }
            
            public void handleRotate(double rotationAngle) {
                angle = rotationAngle;
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
}
