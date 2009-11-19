package com.bc.ceres.figure.support;

import com.bc.ceres.figure.support.DefaultFigureStyle;
import com.bc.ceres.figure.support.FigureStyle;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Stroke;
import static java.lang.Math.*;

public class UIDefaults {
    public static final Stroke FIRST_OF_MULTI_SELECTION_STROKE = new BasicStroke(1.2f);
    public static final Stroke MULTI_SELECTION_STROKE = new BasicStroke(1.0f);
    public static final Color MULTI_SELECTION_COLOR = Color.BLUE.brighter();
    public static final Color SELECTION_DRAW_PAINT = Color.ORANGE;
    public static final Color SELECTION_FILL_PAINT = new Color(127, 127, 255, 100);
    public static final Stroke SELECTION_STROKE = new BasicStroke(1.0f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND, 0.0f, new float[]{4.0f, 4.0f}, 0.0f);

    public static final Stroke HANDLE_DRAW_STROKE = new BasicStroke(1.0f);
    public static final Color HANDLE_DRAW_PAINT = Color.DARK_GRAY;
    public static final Color HANDLE_FILL_PAINT = Color.ORANGE;
    public static final FigureStyle HANDLE_STYLE = new DefaultFigureStyle(HANDLE_DRAW_STROKE,
                                                                          HANDLE_DRAW_PAINT,
                                                                          HANDLE_FILL_PAINT);

    public static final Stroke SELECTED_HANDLE_DRAW_STROKE = new BasicStroke(1.5f);
    public static final Color SELECTED_HANDLE_DRAW_PAINT = Color.BLUE;
    public static final Color SELECTED_HANDLE_FILL_PAINT = Color.ORANGE;
    public static final FigureStyle SELECTED_HANDLE_STYLE = new DefaultFigureStyle(SELECTED_HANDLE_DRAW_STROKE,
                                                                                   SELECTED_HANDLE_DRAW_PAINT,
                                                                                   SELECTED_HANDLE_FILL_PAINT);

    public static final double VERTEX_HANDLE_SIZE = 8;
    public static final double SCALE_HANDLE_SIZE = 6;
    public static final double ROTATE_HANDLE_SIZE = 8;
    public static final double ROTATE_ANCHOR_SIZE = 5;
    public static final double SELECTION_EXTEND_SIZE = max(VERTEX_HANDLE_SIZE, max(SCALE_HANDLE_SIZE, max(ROTATE_HANDLE_SIZE, ROTATE_ANCHOR_SIZE)));
}
