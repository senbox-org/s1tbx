package com.bc.ceres.swing.figure.support;

import com.bc.ceres.swing.figure.AbstractHandle;
import com.bc.ceres.swing.figure.Figure;
import com.bc.ceres.swing.figure.support.FigureStyle;
import com.bc.ceres.swing.figure.support.StyleDefaults;

import java.awt.Cursor;
import java.awt.Shape;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;


public class ScaleHandle extends AbstractHandle {
    public final static int E = 0;
    public final static int NE = 1;
    public final static int N = 2;
    public final static int NW = 3;
    public final static int W = 4;
    public final static int SW = 5;
    public final static int S = 6;
    public final static int SE = 7;

    private final static int[] OPPONENT_INDEX = {
            W, // opponent of E
            SW, // opponent of NE
            S, // opponent of N
            SE, // opponent of NW
            E, // opponent of W
            NE, // opponent of SW
            N, // opponent of S
            NW, // opponent of SE
    };

    private final static int[] CURSOR_TYPES = {
            Cursor.E_RESIZE_CURSOR,
            Cursor.NE_RESIZE_CURSOR,
            Cursor.N_RESIZE_CURSOR,
            Cursor.NW_RESIZE_CURSOR,
            Cursor.W_RESIZE_CURSOR,
            Cursor.SW_RESIZE_CURSOR,
            Cursor.S_RESIZE_CURSOR,
            Cursor.SE_RESIZE_CURSOR,
    };
    private final static Point2D[] DIRECTIONS = {
            new Point2D.Double(+1.0, 0.0),
            new Point2D.Double(+1.0, +1.0),
            new Point2D.Double(0.0, +1.0),
            new Point2D.Double(-1.0, +1.0),
            new Point2D.Double(-1.0, 0.0),
            new Point2D.Double(-1.0, -1.0),
            new Point2D.Double(0.0, -1.0),
            new Point2D.Double(+1.0, -1.0),
    };

    private final static Point2D[] POSITIONS = {
            new Point2D.Double(1.0, 0.5),
            new Point2D.Double(1.0, 0.0),
            new Point2D.Double(0.5, 0.0),
            new Point2D.Double(0.0, 0.0),
            new Point2D.Double(0.0, 0.5),
            new Point2D.Double(0.0, 1.0),
            new Point2D.Double(0.5, 1.0),
            new Point2D.Double(1.0, 1.0),
    };

    private final int type;
    private final Point2D offset;

    public ScaleHandle(Figure figure,
                       int type, double dx, double dy,
                       FigureStyle style) {
        super(figure, style, style);
        this.type = type;
        this.offset = new Point2D.Double(dx, dy);
        setHandleShape();
    }

    @Override
    protected Shape createHandleShape() {
        final Point2D.Double pp = getRefPoint();
        return new Rectangle2D.Double(pp.getX() + offset.getX() - 0.5 * StyleDefaults.SCALE_HANDLE_SIZE,
                                      pp.getY() + offset.getY() - 0.5 * StyleDefaults.SCALE_HANDLE_SIZE,
                                      StyleDefaults.SCALE_HANDLE_SIZE,
                                      StyleDefaults.SCALE_HANDLE_SIZE);
    }

    @Override
    public void move(double dx, double dy) {
        dx *= DIRECTIONS[type].getX();
        dy *= -DIRECTIONS[type].getY();
        Rectangle2D bounds = getFigure().getBounds();
        double w = bounds.getWidth();
        double h = bounds.getHeight();
        double sx = (w + dx) / w;
        double sy = (h + dy) / h;
        getFigure().scale(getRefPoint(OPPONENT_INDEX[type]), sx, sy);
    }

    @Override
    public Cursor getCursor() {
        double rotation = 0.0; // todo - derive rotation angle
        if (rotation == 0.0) {
            return Cursor.getPredefinedCursor(CURSOR_TYPES[type]);
        } else {
            final Point2D direction = DIRECTIONS[type];
            // todo - rotate "direction" by "rotation"
            double a = Math.atan2(direction.getY(), direction.getX());
            if (a < 0.0) {
                a = 2.0 * Math.PI - a;
            }
            final double b = 0.5 * a / Math.PI;
            final double n = CURSOR_TYPES.length;
            final double c = n * b + 0.5 * (1.0 / n);
            int index = (int) c;
            if (index >= CURSOR_TYPES.length) {
                index = CURSOR_TYPES.length - 1;
            }
            return Cursor.getPredefinedCursor(CURSOR_TYPES[index]);
        }
    }

    private Point2D.Double getRefPoint() {
        return getRefPoint(type);
    }

    private Point2D.Double getRefPoint(int index) {
        final Rectangle2D bounds = getFigure().getBounds();
        final double x = bounds.getX();
        final double y = bounds.getY();
        final double w = bounds.getWidth();
        final double h = bounds.getHeight();
        return new Point2D.Double(x + POSITIONS[index].getX() * w,
                                  y + POSITIONS[index].getY() * h);
    }
}
