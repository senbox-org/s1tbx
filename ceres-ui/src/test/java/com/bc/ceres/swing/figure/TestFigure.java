package com.bc.ceres.swing.figure;

import com.bc.ceres.swing.figure.support.DefaultFigureStyle;
import com.bc.ceres.swing.figure.support.DefaultShapeFigure;
import org.junit.Ignore;

import java.awt.geom.Ellipse2D;

@Ignore
public class TestFigure extends DefaultShapeFigure {
    public TestFigure() {
        this(false);
    }

    public TestFigure(boolean selectable) {
        super(new Ellipse2D.Double(0, 0, 10, 10), true, new DefaultFigureStyle());
        setSelectable(selectable);
    }
}
