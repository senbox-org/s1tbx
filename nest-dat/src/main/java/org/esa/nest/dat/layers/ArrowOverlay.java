package org.esa.nest.dat.layers;

import java.awt.*;
import java.awt.geom.Line2D;

/**
   Draws an arrow overlay
 */
public class ArrowOverlay {

    private final double[] ipts = new double[10];
    private final double[] vpts = new double[10];
    private Point headPoint;
    private Point tailPoint;
    private Point textPoint;
    private String text = null;
    private final static Color outlineColour = Color.BLACK;
    private final static Color bodyColour = Color.YELLOW;

    private final static BasicStroke thickStroke = new BasicStroke(6);
    private final static BasicStroke thinStroke = new BasicStroke(2);

    public ArrowOverlay(final int x, final int y, final int x2, final int y2) {
        createArrow(x, y, x2, y2);
    }

    public void setText(final String text) {
        this.text = text;
    }

    private void createArrow(final int x, final int y, final int xx, final int yy)
    {
        ipts[0] = x;
        ipts[1] = y;
        ipts[2] = xx;
        ipts[3] = yy;
        final double d = xx - x;
        final double d1 = -(yy - y);
        double d2 = Math.sqrt(d * d + d1 * d1);
        final double d3;
        final double size = 2.0;
        //final int headSize = Math.max(5, (int)((xx-x)*0.1));
        final int headSize = Math.max(3, (int)size);
        if(d2 > (3.0 * headSize))
            d3 = headSize;
        else
            d3 = d2 / 3.0;
        if(d2 < 1.0)
            d2 = 1.0;

        final double d4 = (d3 * d) / d2;
        final double d5 = -((d3 * d1) / d2);
        final double d6 = (double)xx - size * d4;
        final double d7 = (double)yy - size * d5;
        ipts[4] = (int)(d6 - d5);
        ipts[5] = (int)(d7 + d4);
        ipts[6] = (int)(d6 + d5);
        ipts[7] = (int)(d7 - d4);

        // text point
        ipts[8] = xx + (d/10.0);
        ipts[9] = yy - (d1/10.0);
    }

    public void drawArrow(final Graphics2D g, final ScreenPixelConverter screenPixel) {

        screenPixel.pixelToScreen(ipts, vpts);
        headPoint = new Point((int)vpts[0], (int)vpts[1]);
        tailPoint = new Point((int)vpts[2], (int)vpts[3]);
        textPoint = new Point((int)vpts[8], (int)vpts[9]);

        g.setColor(outlineColour);
        g.setStroke(thickStroke);
        paintTriangleArrowHead(g);
        paintStraightBody(g);

        g.setColor(bodyColour);
        g.setStroke(thinStroke);
        paintTriangleArrowHead(g);
        paintStraightBody(g);

        if(text != null) {
            paintText(g);
        }
    }

    /*
        simple arrowhead
     */
    private void paintTriangleArrowHead(final Graphics2D g) {

        g.draw(new Line2D.Double(vpts[4], vpts[5], vpts[2], vpts[3]));
        g.draw(new Line2D.Double(vpts[6], vpts[7], vpts[2], vpts[3]));
        final Polygon head = new Polygon();
        head.addPoint((int)vpts[4], (int)vpts[5]);
        head.addPoint((int)vpts[2], (int)vpts[3]);
        head.addPoint((int)vpts[6], (int)vpts[7]);
        head.addPoint((int)vpts[4], (int)vpts[5]);
        g.fill(head);
    }

    /*
        simple body
     */
    private void paintStraightBody(final Graphics2D g) {
        g.draw(new Line2D.Double(vpts[0], vpts[1], vpts[2], vpts[3]));
    }

    private void paintText(final Graphics2D g) {
        GraphicsUtils.outlineText(g, bodyColour, text, textPoint.x, textPoint.y);
    }

    public Point getHead() {
        return headPoint;
    }

    public Point getTail() {
        return tailPoint;
    }
}
