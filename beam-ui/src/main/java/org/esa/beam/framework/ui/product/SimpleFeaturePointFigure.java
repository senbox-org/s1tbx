package org.esa.beam.framework.ui.product;

import com.bc.ceres.grender.Rendering;
import com.bc.ceres.swing.figure.AbstractPointFigure;
import com.bc.ceres.swing.figure.FigureStyle;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import org.opengis.feature.simple.SimpleFeature;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;


public class SimpleFeaturePointFigure extends AbstractPointFigure implements SimpleFeatureFigure {

    private final SimpleFeature simpleFeature;
    private Point geometry;
    private double radius; // in model coordinates

    public SimpleFeaturePointFigure(SimpleFeature simpleFeature, FigureStyle style) {
        this.simpleFeature = simpleFeature;
        Object o = simpleFeature.getDefaultGeometry();
        if (!(o instanceof Point)) {
            throw new IllegalArgumentException("simpleFeature");
        }
        geometry = (Point) o;
        radius = 5.0;
        setSelectable(true);
    }

    @Override
    public SimpleFeature getSimpleFeature() {
        return simpleFeature;
    }

    @Override
    public Geometry getGeometry() {
        return geometry;
    }

    @Override
    public double getX() {
        return geometry.getX();
    }

    @Override
    public double getY() {
        return geometry.getY();
    }

    @Override
    public void setLocation(double x, double y) {
        Coordinate coordinate = geometry.getCoordinate();
        coordinate.x = x;
        coordinate.y = y;
        fireFigureChanged();
    }

    @Override
    public Rectangle2D getBounds() {
        return new Rectangle2D.Double(getX() - radius, getY() - radius, 2 * radius, 2 * radius);
    }

    @Override
    public boolean isCloseTo(Point2D point, AffineTransform m2v) {
        double dx = point.getX() - getX();
        double dy = point.getY() - getY();
        return dx * dx + dy * dy < radius * radius;
    }

    @Override
    protected void drawPointSymbol(Rendering rendering) {
        double determinant = rendering.getViewport().getModelToViewTransform().getDeterminant();
        double scale = Math.sqrt(Math.abs(determinant));
        rendering.getGraphics().setPaint(Color.BLUE);
        rendering.getGraphics().setStroke(new BasicStroke(1.0f));
        drawCross(rendering, scale);
        if (isSelected()) {
            rendering.getGraphics().setPaint(new Color(255, 255, 0, 200));
            rendering.getGraphics().setStroke(new BasicStroke(3.0f));
            drawCross(rendering, scale);
        }
    }

    private void drawCross(Rendering rendering, double scale) {
        rendering.getGraphics().draw(
                new Line2D.Double(-scale * radius, -scale * radius,
                                  +scale * radius, +scale * radius));
        rendering.getGraphics().draw(
                new Line2D.Double(+scale * radius, -scale * radius,
                                  -scale * radius, +scale * radius));
    }


}