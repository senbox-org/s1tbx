package com.bc.ceres.swing.figure.support;

import com.bc.ceres.swing.figure.FigureStyle;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Paint;
import java.awt.Stroke;
import static java.lang.Math.max;

public class StyleDefaults {

    public static final Stroke FIRST_OF_MULTI_SELECTION_STROKE = new BasicStroke(1.0f);

    public static final Paint MULTI_SELECTION_STROKE_PAINT = Color.BLUE.brighter();
    public static final Stroke MULTI_SELECTION_STROKE = new BasicStroke(1.0f);

    public static final Paint SELECTION_STROKE_PAINT = Color.ORANGE;
    public static final Stroke SELECTION_STROKE = new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0.0f, new float[]{4.0f, 4.0f}, 0.0f);

    public static final Paint SELECTION_RECT_FILL_PAINT = new Color(127, 127, 255, 100);
    public static final Paint SELECTION_RECT_STROKE_PAINT = new Color(50, 50, 255, 100);

    public static final Paint SELECTED_SHAPE_STROKE_PAINT = new Color(255, 255, 0, 150);

    public static final Color HANDLE_FILL_PAINT = Color.WHITE;
    public static final Color HANDLE_STROKE_PAINT = Color.DARK_GRAY;
    public static final FigureStyle HANDLE_STYLE = DefaultFigureStyle.createPolygonStyle(HANDLE_FILL_PAINT,
                                                                                         HANDLE_STROKE_PAINT,
                                                                                         new BasicStroke(1.0f));

    public static final Color SELECTED_HANDLE_FILL_PAINT = Color.ORANGE;
    public static final Color SELECTED_HANDLE_STROKE_PAINT = Color.BLUE;
    public static final FigureStyle SELECTED_HANDLE_STYLE = DefaultFigureStyle.createPolygonStyle(SELECTED_HANDLE_FILL_PAINT,
                                                                                                  SELECTED_HANDLE_STROKE_PAINT,
                                                                                                  new BasicStroke(1.0f));

    public static final double VERTEX_HANDLE_SIZE = 8;
    public static final double SCALE_HANDLE_SIZE = 8;
    public static final double ROTATE_HANDLE_SIZE = 8;
    public static final double ROTATE_ANCHOR_SIZE = 6;
    public static final double SELECTION_EXTEND_SIZE = max(VERTEX_HANDLE_SIZE, max(SCALE_HANDLE_SIZE, max(ROTATE_HANDLE_SIZE, ROTATE_ANCHOR_SIZE)));

    public static final FigureStyle INSERT_STYLE = DefaultFigureStyle.createPolygonStyle(Color.RED,
                                                                                         Color.BLACK,
                                                                                         new BasicStroke(1.0f));
}
