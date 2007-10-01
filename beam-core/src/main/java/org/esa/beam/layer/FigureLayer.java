/*
 * $Id: FigureLayer.java,v 1.1 2006/10/10 14:47:37 norman Exp $
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
 */
package org.esa.beam.layer;

import com.bc.view.ViewModel;
import org.esa.beam.framework.draw.Figure;
import org.esa.beam.util.Debug;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.PropertyMap;

import java.awt.AlphaComposite;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics2D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FigureLayer extends StyledLayer {

    public static final boolean DEFAULT_SHAPE_OUTLINED = true;
    public static final double DEFAULT_SHAPE_OUTL_TRANSPARENCY = 0.1;
    public static final Color DEFAULT_SHAPE_OUTL_COLOR = Color.yellow;
    public static final double DEFAULT_SHAPE_OUTL_WIDTH = 1.0;
    public static final boolean DEFAULT_SHAPE_FILLED = true;
    public static final double DEFAULT_SHAPE_FILL_TRANSPARENCY = 0.5;
    public static final Color DEFAULT_SHAPE_FILL_COLOR = Color.blue;

    private Map<String, Object> _figureAttributes;
    private List<Figure> _figures;

    public FigureLayer() {
        _figures = new ArrayList<Figure>();
        _figureAttributes = new HashMap<String, Object>();
    }


    @Override
    public String getPropertyNamePrefix() {
        return "shape";
    }

    @Override
    protected void setStylePropertiesImpl(final PropertyMap propertyMap) {
        super.setStylePropertiesImpl(propertyMap);

        final boolean outlined = propertyMap.getPropertyBool("shape.outlined", FigureLayer.DEFAULT_SHAPE_OUTLINED);
        final float outlTransp = (float) propertyMap.getPropertyDouble("shape.outl.transparency",
                                                                       FigureLayer.DEFAULT_SHAPE_OUTL_TRANSPARENCY);
        final Color outlColor = propertyMap.getPropertyColor("shape.outl.color", FigureLayer.DEFAULT_SHAPE_OUTL_COLOR);
        final float outlWidth = (float) propertyMap.getPropertyDouble("shape.outl.width",
                                                                      FigureLayer.DEFAULT_SHAPE_OUTL_WIDTH);

        final boolean filled = propertyMap.getPropertyBool("shape.filled", FigureLayer.DEFAULT_SHAPE_FILLED);
        final float fillTransp = (float) propertyMap.getPropertyDouble("shape.fill.transparency",
                                                                       FigureLayer.DEFAULT_SHAPE_OUTL_TRANSPARENCY);
        final Color fillColor = propertyMap.getPropertyColor("shape.fill.color", FigureLayer.DEFAULT_SHAPE_OUTL_COLOR);

        final AlphaComposite outlComp;
        if (outlTransp > 0.0f) {
            outlComp = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f - outlTransp);
        } else {
            outlComp = null;
        }

        final AlphaComposite fillComp;
        if (fillTransp > 0.0f) {
            fillComp = AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 1.0f - fillTransp);
        } else {
            fillComp = null;
        }

        _figureAttributes.put(Figure.OUTLINED_KEY, outlined ? Boolean.TRUE : Boolean.FALSE);
        _figureAttributes.put(Figure.OUTL_COMPOSITE_KEY, outlComp);
        _figureAttributes.put(Figure.OUTL_COLOR_KEY, outlColor);
        _figureAttributes.put(Figure.OUTL_STROKE_KEY, new BasicStroke(outlWidth));

        _figureAttributes.put(Figure.FILLED_KEY, filled ? Boolean.TRUE : Boolean.FALSE);
        _figureAttributes.put(Figure.FILL_COMPOSITE_KEY, fillComp);
        _figureAttributes.put(Figure.FILL_PAINT_KEY, fillColor);

        for (Object _figure : _figures) {
            final Figure figure = (Figure) _figure;
            figure.setAttributes(_figureAttributes);
        }
    }

    public void addFigure(final Figure figure) {
        Guardian.assertNotNull("figure", figure);
        figure.setAttributes(_figureAttributes);
        _figures.add(figure);
        fireLayerChanged();
    }

    public void removeFigure(final Figure figure) {
        Guardian.assertNotNull("figure", figure);
        _figures.remove(figure);
        fireLayerChanged();
    }

    public boolean contains(final Figure figure) {
        return _figures.contains(figure);
    }

    /**
     * @return the number of figures.
     */
    public int getNumFigures() {
        return _figures.size();
    }

    /**
     * Gets the figure at the specified index.
     *
     * @param index the figure index
     * @return the figure, never <code>null</code>
     */
    public Figure getFigureAt(final int index) {
        return _figures.get(index);
    }

    /**
     * Gets all figures.
     *
     * @return the figure array which is empty if no figures where found, never <code>null</code>
     */
    public Figure[] getAllFigures() {
        return _figures.toArray(new Figure[_figures.size()]);
    }

    /**
     * Gets all selected figures.
     *
     * @return the figure array which is empty if no figures where found, never <code>null</code>
     */
    public Figure[] getSelectedFigures() {
        Debug.traceMethodNotImplemented(getClass(), "getSelectedFigures");
        return new Figure[0]; // Selections currently not supported
    }

    /**
     * Gets all figures having an attribute with the given name.
     *
     * @param name the attribute name
     * @return the figure array which is empty if no figures where found, never <code>null</code>
     */
    public Figure[] getFiguresWithAttribute(final String name) {
        Guardian.assertNotNull("name", name);
        final List<Figure> list = new ArrayList<Figure>();
        for (int i = 0; i < getNumFigures(); i++) {
            final Figure figure = getFigureAt(i);
            if (figure.getAttribute(name) != null) {
                list.add(figure);
            }
        }
        return list.toArray(new Figure[list.size()]);
    }

    /**
     * Gets all figures having an attribute with the given name and value.
     *
     * @param name  the attribute name, must not be <code>null</code>
     * @param value the attribute value, must not be <code>null</code>
     * @return the figure array which is empty if no figures where found, never <code>null</code>
     */
    public Figure[] getFiguresWithAttribute(final String name, final Object value) {
        Guardian.assertNotNull("name", name);
        Guardian.assertNotNull("value", value);
        final List<Figure> list = new ArrayList<Figure>();
        for (int i = 0; i < getNumFigures(); i++) {
            final Figure figure = getFigureAt(i);
            if (value.equals(figure.getAttribute(name))) {
                list.add(figure);
            }
        }
        return list.toArray(new Figure[list.size()]);
    }

    /**
     * Gets the first figure which has an attribute with the given name and value.
     *
     * @param name  the attribute name, must not be <code>null</code>
     * @param value the attribute value, must not be <code>null</code>
     * @return the figure, null if no matching figure was found
     */
    public Figure getFigureWithAttribute(final String name, final Object value) {
        Guardian.assertNotNull("name", name);
        Guardian.assertNotNull("value", value);
        for (int i = 0; i < getNumFigures(); i++) {
            final Figure figure = getFigureAt(i);
            if (value.equals(figure.getAttribute(name))) {
                return figure;
            }
        }
        return null;
    }

    /**
     * Draws the layer using the given 2D graphics context.
     * The graphics context expects world coordinates.
     *
     * @param g2d       the 2D graphics context, never null
     * @param viewModel the view model
     */
    public void draw(final Graphics2D g2d, ViewModel viewModel) {
        for (final Figure figure : _figures) {
            figure.draw(g2d);
        }
    }
}
