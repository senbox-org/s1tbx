package com.bc.ceres.grender.swing;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.MouseInputAdapter;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.geom.*;
import java.util.EventListener;

// todo - find better name
public class NavControl extends JComponent {
    private double rotationAngle;
    private double pannerShapeOffsetX;
    private double pannerShapeOffsetY;

    private static final int PREFERRED_SIZE = 100;
    private static final int MINIMUM_SIZE = 32;

    private Ellipse2D outerRotationCircle;
    private Ellipse2D innerRotationCircle;
    private Ellipse2D outerMoveCircle;
    private Shape pannerShape;
    private Shape[] moveArrowShapes;
    private Area[] rotationUnitShapes;

    public NavControl() {
        final MouseHandler mouseHandler = new MouseHandler();
        addMouseListener(mouseHandler);
        addMouseMotionListener(mouseHandler);
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
        this.rotationAngle = rotationAngle;
        firePropertyChange("rotationAngle", oldRotationAngle, rotationAngle);
    }

    @Override
    public Dimension getPreferredSize() {
        if (isPreferredSizeSet()) {
            return super.getPreferredSize();
        }
        return getSizePlusInsets(PREFERRED_SIZE);
    }

    @Override
    public Dimension getMinimumSize() {
        if (isMinimumSizeSet()) {
            return super.getMinimumSize();
        }
        return getSizePlusInsets(MINIMUM_SIZE);
    }

    private Dimension getSizePlusInsets(int s) {
        final Insets insets = getInsets();
        return new Dimension(s + (insets.left + insets.right),
                             s + (insets.top + insets.bottom));
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
        graphics2D.setStroke(new BasicStroke(0.5f));
        graphics2D.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        graphics2D.rotate(-Math.PI * 0.5 - Math.toRadians(getRotationAngle()),
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

        graphics2D.translate(pannerShapeOffsetX,
                             pannerShapeOffsetY);
        graphics2D.setColor(Color.WHITE);
        graphics2D.fill(pannerShape);
        graphics2D.setColor(Color.BLACK);
        graphics2D.draw(pannerShape);
        graphics2D.setTransform(oldTransform);
    }

    private void updateGeom() {
        final Insets insets = getInsets();
        int x = insets.left;
        int y = insets.top;
        int w = getWidth() - (insets.left + insets.right);
        int h = getHeight() - (insets.top + insets.bottom);
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
        pannerShape = createPanner();
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

    public static interface SelectionListener extends EventListener {
        void handleRotate(double rotationAngle);

        void handleMove(double moveDirX, double moveDirY);
    }

    private class MouseHandler extends MouseInputAdapter implements ActionListener {
        private Point point0;
        private double rotationAngle0;
        private double moveDirX;
        private double moveDirY;
        private double moveAcc;
        private Cursor cursor0;
        private int mode;
        private static final int TIMER_DELAY = 100;
        private final Timer dragTimer;

        private MouseHandler() {
            dragTimer = new Timer(TIMER_DELAY, this);
        }

        public void actionPerformed(ActionEvent e) {
            if (mode > 1) {
                fireMove(moveAcc * moveDirX, moveAcc * moveDirY);
                moveAcc *= 1.05;
                if (moveAcc > 2.5) {
                    moveAcc = 2.5;
                }
            }
        }

        @Override
        public void mousePressed(MouseEvent e) {
            cursor0 = getCursor();
            point0 = e.getPoint();
            mode = 0;
            moveAcc = 1;

            if (outerRotationCircle.contains(point0) && !innerRotationCircle.contains(point0)) {
                rotationAngle0 = getRotationAngle();
                mode = 1;
            } else if (pannerShape.contains(point0)) {
                mode = 2;
            } else {
                double[] dirXs = new double[]{0, -1, 0, 1};
                double[] dirYs = new double[]{1, 0, -1, 0};
                for (int i = 0; i < moveArrowShapes.length; i++) {
                    Shape moveArrowShape = moveArrowShapes[i];
                    if (moveArrowShape.contains(point0)) {
                        mode = 3;
                        moveDirX = dirXs[i];
                        moveDirY = dirYs[i];
                        dragTimer.start();
                        break;
                    }
                }
            }

            if (mode != 0) {
                setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            setCursor(cursor0);
            dragTimer.stop();
            cursor0 = null;
            point0 = null;
            moveDirX = 0;
            moveDirY = 0;
            mode = 0;
            pannerShapeOffsetX = 0;
            pannerShapeOffsetY = 0;
            moveAcc = 1;
            repaint();
        }

        @Override
        public void mouseDragged(MouseEvent e) {
            if (mode == 0) {
                return;
            }
            if (mode == 1) {
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
            } else if (mode == 2) {
                final Point point = e.getPoint();
                final double outerMoveRadius = 0.5 * outerMoveCircle.getWidth();
                double dx = point.x - outerMoveCircle.getCenterX();
                double dy = point.y - outerMoveCircle.getCenterY();
                final double r = Math.sqrt(dx * dx + dy * dy);
                if (r > outerMoveRadius) {
                    dx = outerMoveRadius * dx / r;
                    dy = outerMoveRadius * dy / r;
                }
                pannerShapeOffsetX = dx;
                pannerShapeOffsetY = dy;
                moveDirX = -dx / outerMoveRadius;
                moveDirY = -dy / outerMoveRadius;
                repaint();
                fireMove(moveDirX, moveDirY);
                moveAcc = 1;
                dragTimer.restart();
            } else if (mode == 3) {
                moveAcc = 1;
                dragTimer.restart();
            }
        }
    }


    public static void main(String[] args) {
        final JFrame frame = new JFrame("NavControl");
        final JPanel panel = new JPanel(new BorderLayout(3, 3));
        panel.setBackground(Color.GRAY);
        final JLabel label = new JLabel("Angle");
        final NavControl navControl = new NavControl();
        navControl.addSelectionListener(new SelectionListener() {
            public void handleRotate(double rotationAngle) {
                System.out.println("rotationAngle = " + rotationAngle);
            }

            public void handleMove(double moveDirX, double moveDirY) {
                System.out.println("moveDirX = " + moveDirX + ", moveDirY = " + moveDirY);
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
