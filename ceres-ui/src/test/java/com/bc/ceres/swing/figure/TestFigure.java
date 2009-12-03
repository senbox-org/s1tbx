package com.bc.ceres.swing.figure;

import com.bc.ceres.grender.Rendering;
import com.bc.ceres.swing.figure.support.DefaultShapeFigure;
import com.bc.ceres.swing.figure.support.DefaultFigureStyle;
import org.junit.Ignore;

import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.Point2D;
import java.awt.geom.Rectangle2D;

@Ignore
public class TestFigure extends DefaultShapeFigure {
    public TestFigure() {
                      this(false);
    }
    public TestFigure(boolean selected) {
        super(new Ellipse2D.Double(0, 0, 10, 10), true, new DefaultFigureStyle());
        setSelected(selected);
    }
}
