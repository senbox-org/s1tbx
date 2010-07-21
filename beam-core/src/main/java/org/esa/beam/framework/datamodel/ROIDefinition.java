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
package org.esa.beam.framework.datamodel;

import org.esa.beam.dataio.dimap.DimapProductConstants;
import org.esa.beam.framework.draw.Figure;
import org.esa.beam.framework.draw.ShapeFigure;
import org.esa.beam.util.Debug;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.StringUtils;
import org.esa.beam.util.XmlWriter;
import org.jdom.Element;

import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.Ellipse2D;
import java.awt.geom.GeneralPath;
import java.awt.geom.Line2D;
import java.awt.geom.PathIterator;
import java.awt.geom.Rectangle2D;
import java.util.ArrayList;
import java.util.List;

/**
 * This class contains information about the ROI associated with the a raster dataset.
 * @deprecated since BEAM 4.7, use {@link Mask} instead
 */
@Deprecated
public class ROIDefinition implements Cloneable {

    private boolean _shapeEnabled;
    private Figure _shapeFigure;

    private boolean _valueRangeEnabled;
    private float _valueRangeMin;
    private float _valueRangeMax;

    private boolean _bitmaskEnabled;
    private String _bitmaskExpr;

    private boolean _pinUseEnabled;

    private boolean _inverted;
    private boolean _orCombined;

    public ROIDefinition() {
        _valueRangeMin = 0.0F;
        _valueRangeMax = 1.0F;
        _bitmaskExpr = "";
    }

    public boolean isUsable() {
        return (isShapeEnabled() && getShapeFigure() != null)
               || (!StringUtils.isNullOrEmpty(getBitmaskExpr()) && isBitmaskEnabled())
               || isValueRangeEnabled()
               || isPinUseEnabled();
    }

    public boolean isBitmaskEnabled() {
        return _bitmaskEnabled;
    }

    public void setBitmaskEnabled(boolean bitmaskEnabled) {
        _bitmaskEnabled = bitmaskEnabled;
    }

    /**
     * Gets the bitmask expression.
     *
     * @return the expression, can be an empty string, never null
     */
    public String getBitmaskExpr() {
        return _bitmaskExpr;
    }

    /**
     * Sets the bitmask expression.
     *
     * @param bitmaskExpr the expression, can be an empty string, must not be null
     */
    public void setBitmaskExpr(String bitmaskExpr) {
        Guardian.assertNotNull("bitmaskExpr", bitmaskExpr);
        _bitmaskExpr = bitmaskExpr;
    }

    public boolean isShapeEnabled() {
        return _shapeEnabled;
    }

    public void setShapeEnabled(boolean shapeEnabled) {
        _shapeEnabled = shapeEnabled;
    }

    public Figure getShapeFigure() {
        return _shapeFigure;
    }

    public void setShapeFigure(Figure figure) {
        _shapeFigure = figure;
    }

    public boolean isValueRangeEnabled() {
        return _valueRangeEnabled;
    }

    public void setValueRangeEnabled(boolean valueRangeEnabled) {
        _valueRangeEnabled = valueRangeEnabled;
    }

    public float getValueRangeMax() {
        return _valueRangeMax;
    }

    public void setValueRangeMax(float valueRangeMax) {
        _valueRangeMax = valueRangeMax;
    }

    public float getValueRangeMin() {
        return _valueRangeMin;
    }

    public void setValueRangeMin(float valueRangeMin) {
        _valueRangeMin = valueRangeMin;
    }

    public boolean isPinUseEnabled() {
        return _pinUseEnabled;
    }

    public void setPinUseEnabled(boolean pinUseEnabled) {
        _pinUseEnabled = pinUseEnabled;
    }

    public boolean isOrCombined() {
        return _orCombined;
    }

    public void setOrCombined(boolean orCombined) {
        _orCombined = orCombined;
    }

    public boolean isInverted() {
        return _inverted;
    }

    public void setInverted(boolean inverted) {
        _inverted = inverted;
    }

    /**
     * Creates and returns a copy of this object.
     *
     * @return a copy of this object
     */
    public ROIDefinition createCopy() {
        ROIDefinition clone = new ROIDefinition();

        clone.setShapeEnabled(isShapeEnabled());
        final Figure shapeFigure = getShapeFigure();
        if (shapeFigure != null) {
            clone.setShapeFigure(shapeFigure.clone());
        }

        clone.setValueRangeEnabled(isValueRangeEnabled());
        clone.setValueRangeMin(getValueRangeMin());
        clone.setValueRangeMax(getValueRangeMax());

        clone.setBitmaskEnabled(isBitmaskEnabled());
        clone.setBitmaskExpr(getBitmaskExpr());

        clone.setPinUseEnabled(isPinUseEnabled());

        clone.setOrCombined(isOrCombined());
        clone.setInverted(isInverted());

        return clone;
    }

    // @todo 1 nf/se - recognize _pinUseEnabled
    public void writeXML(final XmlWriter writer, int indent) {
        Guardian.assertNotNull("writer", writer);
        Guardian.assertGreaterThan("indent", indent, -1);
        String[] roiTags = XmlWriter.createTags(indent, DimapProductConstants.TAG_ROI_DEFINITION);
        writer.println(roiTags[0]);
        writer.printLine(indent + 1, DimapProductConstants.TAG_BITMASK_EXPRESSION, getBitmaskExpr());
        writer.printLine(indent + 1, DimapProductConstants.TAG_VALUE_RANGE_MAX, getValueRangeMax());
        writer.printLine(indent + 1, DimapProductConstants.TAG_VALUE_RANGE_MIN, getValueRangeMin());
        writer.printLine(indent + 1, DimapProductConstants.TAG_BITMASK_ENABLED, isBitmaskEnabled());
        writer.printLine(indent + 1, DimapProductConstants.TAG_INVERTED, isInverted());
        writer.printLine(indent + 1, DimapProductConstants.TAG_OR_COMBINED, isOrCombined());
        writer.printLine(indent + 1, DimapProductConstants.TAG_SHAPE_ENABLED, isShapeEnabled());
        writer.printLine(indent + 1, DimapProductConstants.TAG_VALUE_RANGE_ENABLED, isValueRangeEnabled());
        writer.printLine(indent + 1, DimapProductConstants.TAG_PIN_USE_ENABLED, isPinUseEnabled());
        Shape shape = null;
        if (getShapeFigure() != null) {
            shape = getShapeFigure().getShape();
            writer.printLine(indent + 1, DimapProductConstants.TAG_ROI_ONE_DIMENSIONS,
                             getShapeFigure().isOneDimensional());
        }
        if (shape != null) {
            if (shape instanceof Line2D.Float) {
                Line2D.Float line = (Line2D.Float) shape;
                writer.printLine(indent + 1, DimapProductConstants.TAG_SHAPE_FIGURE,
                                 new String[][]{
                                         new String[]{DimapProductConstants.ATTRIB_TYPE, "Line2D"},
                                         new String[]{
                                                 DimapProductConstants.ATTRIB_VALUE, "" + line.getX1()
                                                                                     + "," + line.getY1()
                                                                                     + "," + line.getX2()
                                                                                     + "," + line.getY2()
                                         }
                                 }, "");
            } else if (shape instanceof Rectangle2D.Float) {
                final Rectangle2D.Float rectangle = (Rectangle2D.Float) shape;
                writer.printLine(indent + 1, DimapProductConstants.TAG_SHAPE_FIGURE,
                                 new String[][]{
                                         new String[]{DimapProductConstants.ATTRIB_TYPE, "Rectangle2D"},
                                         new String[]{
                                                 DimapProductConstants.ATTRIB_VALUE, "" + rectangle.getX()
                                                                                     + "," + rectangle.getY()
                                                                                     + "," + rectangle.getWidth()
                                                                                     + "," + rectangle.getHeight()
                                         }
                                 }, "");
            } else if (shape instanceof Ellipse2D.Float) {
                final Ellipse2D.Float ellipse = (Ellipse2D.Float) shape;
                writer.printLine(indent + 1, DimapProductConstants.TAG_SHAPE_FIGURE,
                                 new String[][]{
                                         new String[]{DimapProductConstants.ATTRIB_TYPE, "Ellipse2D"},
                                         new String[]{
                                                 DimapProductConstants.ATTRIB_VALUE, "" + ellipse.getX()
                                                                                     + "," + ellipse.getY()
                                                                                     + "," + ellipse.getWidth()
                                                                                     + "," + ellipse.getHeight()
                                         }
                                 }, "");
            } else {
                final String[][] atribs = new String[][]{new String[]{DimapProductConstants.ATTRIB_TYPE, "Path"}};
                String[] figureTags = XmlWriter.createTags(indent + 1, DimapProductConstants.TAG_SHAPE_FIGURE, atribs);
                writer.println(figureTags[0]);

                final PathIterator iterator = shape.getPathIterator(null);
                final float[] floats = new float[6];
                while (!iterator.isDone()) {
                    final int segType = iterator.currentSegment(floats);
                    switch (segType) {
                    case PathIterator.SEG_MOVETO:
                        writer.printLine(indent + 2, DimapProductConstants.TAG_PATH_SEG,
                                         new String[][]{
                                                 new String[]{DimapProductConstants.ATTRIB_TYPE, "moveTo"},
                                                 new String[]{
                                                         DimapProductConstants.ATTRIB_VALUE,
                                                         "" + floats[0] + "," + floats[1]
                                                 }
                                         }, "");
                        break;
                    case PathIterator.SEG_LINETO:
                        writer.printLine(indent + 2, DimapProductConstants.TAG_PATH_SEG,
                                         new String[][]{
                                                 new String[]{DimapProductConstants.ATTRIB_TYPE, "lineTo"},
                                                 new String[]{
                                                         DimapProductConstants.ATTRIB_VALUE,
                                                         "" + floats[0] + "," + floats[1]
                                                 }
                                         }, "");
                        break;
                    case PathIterator.SEG_QUADTO:
                        writer.printLine(indent + 2, DimapProductConstants.TAG_PATH_SEG,
                                         new String[][]{
                                                 new String[]{DimapProductConstants.ATTRIB_TYPE, "quadTo"},
                                                 new String[]{
                                                         DimapProductConstants.ATTRIB_VALUE,
                                                         "" + floats[0] + "," + floats[1] + "," + floats[2] + "," + floats[3]
                                                 }
                                         }, "");
                        break;
                    case PathIterator.SEG_CUBICTO:
                        writer.printLine(indent + 2, DimapProductConstants.TAG_PATH_SEG,
                                         new String[][]{
                                                 new String[]{DimapProductConstants.ATTRIB_TYPE, "cubicTo"},
                                                 new String[]{
                                                         DimapProductConstants.ATTRIB_VALUE,
                                                         "" + floats[0] + "," + floats[1] + "," + floats[2] + "," + floats[3] + "," + floats[4] + "," + floats[5]
                                                 }
                                         }, "");
                        break;
                    case PathIterator.SEG_CLOSE:
                        writer.printLine(indent + 2, DimapProductConstants.TAG_PATH_SEG,
                                         new String[][]{
                                                 new String[]{DimapProductConstants.ATTRIB_TYPE, "close"}
                                         }, "");
                    }
                    iterator.next();
                }
                writer.println(figureTags[1]);
            }
        }
        writer.print(roiTags[1]);
    }

    public void initFromJDOMElem(Element roiDefElem) {
        Guardian.assertNotNull("roiDefElem", roiDefElem);
        final Element exprElem = roiDefElem.getChild(DimapProductConstants.TAG_BITMASK_EXPRESSION);
        if (exprElem != null) {
            setBitmaskExpr(exprElem.getTextTrim());
        }

        final Element valRangeMaxElem = roiDefElem.getChild(DimapProductConstants.TAG_VALUE_RANGE_MAX);
        if (valRangeMaxElem != null) {
            try {
                final float max = Float.parseFloat(valRangeMaxElem.getTextTrim());
                setValueRangeMax(max);
            } catch (NumberFormatException e) {
                Debug.trace(e);
            }
        }

        final Element valRangeMinElem = roiDefElem.getChild(DimapProductConstants.TAG_VALUE_RANGE_MIN);
        if (valRangeMinElem != null) {
            try {
                final float min = Float.parseFloat(valRangeMinElem.getTextTrim());
                setValueRangeMin(min);
            } catch (NumberFormatException e) {
                Debug.trace(e);
            }
        }

        final Element bmskEnabledElem = roiDefElem.getChild(DimapProductConstants.TAG_BITMASK_ENABLED);
        if (bmskEnabledElem != null) {
            setBitmaskEnabled(Boolean.valueOf(bmskEnabledElem.getTextTrim()));
        }

        final Element invertedElem = roiDefElem.getChild(DimapProductConstants.TAG_INVERTED);
        if (invertedElem != null) {
            setInverted(Boolean.valueOf(invertedElem.getTextTrim()));
        }

        final Element orCombElem = roiDefElem.getChild(DimapProductConstants.TAG_OR_COMBINED);
        if (orCombElem != null) {
            setOrCombined(Boolean.valueOf(orCombElem.getTextTrim()));
        }

        final Element shapeEnabledElem = roiDefElem.getChild(DimapProductConstants.TAG_SHAPE_ENABLED);
        if (shapeEnabledElem != null) {
            setShapeEnabled(Boolean.valueOf(shapeEnabledElem.getTextTrim()));
        }

        final Element valRangeEnabledElem = roiDefElem.getChild(DimapProductConstants.TAG_VALUE_RANGE_ENABLED);
        if (valRangeEnabledElem != null) {
            setValueRangeEnabled(Boolean.valueOf(valRangeEnabledElem.getTextTrim()));
        }

        final Element pinUseEnabledElem = roiDefElem.getChild(DimapProductConstants.TAG_PIN_USE_ENABLED);
        if (pinUseEnabledElem != null) {
            setPinUseEnabled(Boolean.valueOf(pinUseEnabledElem.getTextTrim()));
        }

        final Element figureElement = roiDefElem.getChild(DimapProductConstants.TAG_SHAPE_FIGURE);
        if (figureElement != null) {
            final String type = figureElement.getAttributeValue("type");
            final String values = figureElement.getAttributeValue("value");
            String[] vals = null;
            if (values != null) {
                vals = StringUtils.csvToArray(values);
            }
            if ("Rectangle2D".equals(type)) {
                if (vals.length > 3) {
                    float x = Float.parseFloat(vals[0]);
                    float y = Float.parseFloat(vals[1]);
                    float w = Float.parseFloat(vals[2]);
                    float h = Float.parseFloat(vals[3]);
                    setShapeFigure(ShapeFigure.createRectangleArea(x, y, w, h, null));
                }
            } else if ("Line2D".equals(type)) {
                if (vals.length > 3) {
                    float x1 = Float.parseFloat(vals[0]);
                    float y1 = Float.parseFloat(vals[1]);
                    float x2 = Float.parseFloat(vals[2]);
                    float y2 = Float.parseFloat(vals[3]);
                    setShapeFigure(ShapeFigure.createLine(x1, y1, x2, y2, null));
                }
            } else if ("Ellipse2D".equals(type)) {
                if (vals.length > 3) {
                    float x = Float.parseFloat(vals[0]);
                    float y = Float.parseFloat(vals[1]);
                    float w = Float.parseFloat(vals[2]);
                    float h = Float.parseFloat(vals[3]);
                    setShapeFigure(ShapeFigure.createEllipseArea(x, y, w, h, null));
                }
            } else if ("Path".equals(type)) {
                @SuppressWarnings({"unchecked"})
                final List<Element> pathSegElems = figureElement.getChildren(DimapProductConstants.TAG_PATH_SEG);
                GeneralPath path = new GeneralPath();
                boolean pathOk = false;
                boolean isPathClosed = false;
                for (final Element pathSegElem : pathSegElems) {
                    final String segType = pathSegElem.getAttributeValue(DimapProductConstants.ATTRIB_TYPE);
                    if ("moveTo".equals(segType)) {
                        final String coords = pathSegElem.getAttributeValue(DimapProductConstants.ATTRIB_VALUE);
                        final String[] strings = StringUtils.csvToArray(coords);
                        path.moveTo(Float.parseFloat(strings[0]), Float.parseFloat(strings[1]));
                        pathOk = true;
                    } else if ("lineTo".equals(segType)) {
                        final String coords = pathSegElem.getAttributeValue(DimapProductConstants.ATTRIB_VALUE);
                        final String[] strings = StringUtils.csvToArray(coords);
                        path.lineTo(Float.parseFloat(strings[0]), Float.parseFloat(strings[1]));
                        pathOk = true;
                    } else if ("quadTo".equals(segType)) {
                        final String coords = pathSegElem.getAttributeValue(DimapProductConstants.ATTRIB_VALUE);
                        final String[] strings = StringUtils.csvToArray(coords);
                        path.quadTo(Float.parseFloat(strings[0]),
                                    Float.parseFloat(strings[1]),
                                    Float.parseFloat(strings[2]),
                                    Float.parseFloat(strings[3]));
                        pathOk = true;
                    } else if ("cubicTo".equals(segType)) {
                        final String coords = pathSegElem.getAttributeValue(DimapProductConstants.ATTRIB_VALUE);
                        final String[] strings = StringUtils.csvToArray(coords);
                        path.curveTo(Float.parseFloat(strings[0]),
                                     Float.parseFloat(strings[1]),
                                     Float.parseFloat(strings[2]),
                                     Float.parseFloat(strings[3]),
                                     Float.parseFloat(strings[4]),
                                     Float.parseFloat(strings[5]));
                        pathOk = true;
                    } else if ("close".equals(segType)) {
                        path.closePath();
                        isPathClosed = true;
                    }
                }
                if (pathOk) {
                    if (isPathClosed) {
                        setShapeFigure(ShapeFigure.createPolygonArea(path, null));
                    } else {
                        setShapeFigure(ShapeFigure.createPolyline(path, null));
                    }
                }
            }
        }
    }

    /**
     * Releases all of the resources used by this object instance and all of its owned children. Its primary use is to
     * allow the garbage collector to perform a vanilla job.
     * <p/>
     * <p>This method should be called only if it is for sure that this object instance will never be used again. The
     * results of referencing an instance of this class after a call to <code>dispose()</code> are undefined.
     * <p/>
     * <p>Overrides of this method should always call <code>super.dispose();</code> after disposing this instance.
     */
    public void dispose() {
        _shapeFigure = null;
        _bitmaskExpr = null;
    }

    static Mask toMask(ROIDefinition roiDefinition, RasterDataNode node) {
        final List<String> expParts = new ArrayList<String>();
        if (roiDefinition.isBitmaskEnabled()) {
            expParts.add(roiDefinition.getBitmaskExpr());
        }
        if (roiDefinition.isValueRangeEnabled()) {
            final StringBuilder rangeBuilder = new StringBuilder();
            rangeBuilder.append("(");
            rangeBuilder.append(node.getName());
            rangeBuilder.append(" >= ");
            rangeBuilder.append(roiDefinition.getValueRangeMin());
            rangeBuilder.append(" && ");
            rangeBuilder.append(node.getName());
            rangeBuilder.append(" <= ");
            rangeBuilder.append(roiDefinition.getValueRangeMax());
            rangeBuilder.append(")");
            expParts.add(rangeBuilder.toString());
        }
        if (roiDefinition.isPinUseEnabled()) {
            expParts.add("pins");
        }
        if (roiDefinition.isShapeEnabled()) {
            Shape shape = roiDefinition.getShapeFigure().getShape();
            if (shape != null) {
                // TODO added: mz , 2009-12-14
                // TODO create vector data node
                // TODO add it to the product
                // TODO use the corresponding mask here
            }
        }
        
        final StringBuilder expressionBuilder = new StringBuilder();
        for (int i = 0; i < expParts.size(); i++) {
            expressionBuilder.append(expParts.get(i));
            if (i < (expParts.size() - 1)) {
                if (roiDefinition.isOrCombined()) {
                    expressionBuilder.append(" || ");
                } else {
                    expressionBuilder.append(" && ");
                }   
            }
        }
        String expression = expressionBuilder.toString();
        if (roiDefinition.isInverted()) {
            expression = "!(" + expression + ")";
        }
        if (!expression.isEmpty()) {
            final String maskName = node.getName() + "_roi";
            final int w = node.getSceneRasterWidth();
            final int h = node.getSceneRasterHeight();
            
            return Mask.BandMathsType.create(maskName, null, w, h, expression, Color.RED, 0.5);
        } else {
            return null;
        }
    }
}
