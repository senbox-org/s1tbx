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

import com.bc.jexp.Term;
import org.esa.beam.dataio.dimap.DimapProductConstants;
import org.esa.beam.dataio.dimap.DimapProductHelpers;
import org.esa.beam.framework.dataio.ProductSubsetDef;
import org.esa.beam.util.*;
import org.jdom.Element;

import java.awt.Color;

/**
 * Represents a bitmask definition comprising the bitmask properties name, description, flag expression color and
 * transparancy.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @deprecated since BEAM 4.7, use {@code Mask} with {@code Mask.BandMathType} instead.
 */
@Deprecated
public class BitmaskDef extends ProductNode {

    public final static String PROPERTY_NAME_COLOR = "color";
    public final static String PROPERTY_NAME_EXPR = "expr";
    public final static String PROPERTY_NAME_TRANSPARENCY = "transparency";

    private final static Color _DEFAULT_COLOR = Color.yellow;

    private String _expr;
    private Color _color;
    private float _transparency;

    public BitmaskDef(String name, String description, String expr, Color color, float transparency) {
        super(name, description);
        setExpr(expr);
        setColor(color);
        setTransparency(transparency);
    }

    /**
     * Gets the bitmask's boolean expression code.
     * To create a {@link Term} use {@link Product#parseExpression(String) createTerm(getExpr())}.
     *
     * @return the bitmask's boolean expression code.
     */
    public String getExpr() {
        return _expr;
    }

    /**
     * Sets the boolean flag expression code of this <code>BitmaskDef</code> instance. Also sets the <code>term</code>
     * property if the given string representation of the expression can be compiled.
     *
     * @param expr the string representation of the expression
     * @throws IllegalArgumentException if the expression is invalid (and not parsable)
     */
    public void setExpr(String expr) {
        if (!ObjectUtils.equalObjects(expr, _expr)) {
            _expr = (expr != null) ? expr : "";
            fireProductNodeChanged(PROPERTY_NAME_EXPR);
            setModified(true);
        }
    }

    /**
     * Replaces in the expression that this class contains
     * all occurences of the oldExternalName with the given newExternalName.
     *
     * @param oldExternalName
     * @param newExternalName
     */
    @Override
    public void updateExpression(final String oldExternalName, final String newExternalName) {
        if (_expr == null) {
            return;
        }
        final String expression = StringUtils.replaceWord(_expr, oldExternalName, newExternalName);
        if (!_expr.equals(expression)) {
            _expr = expression;
            setModified(true);
        }
        super.updateExpression(oldExternalName, newExternalName);
    }

    /**
     * Returns the color of the bitmask.
     *
     * @return the color
     */
    public Color getColor() {
        return _color;
    }

    /**
     * Sets the color of this bitmask definition. A bitmask definition must allways have a color. If the given color is
     * <code>null</code> the default color is set to this bitmask definition.
     *
     * @param color the color to be set
     */
    public void setColor(Color color) {
        if (!ObjectUtils.equalObjects(_color, color)) {
            if (color != null) {
                _color = color;
            } else {
                _color = _DEFAULT_COLOR;
            }
            fireProductNodeChanged(PROPERTY_NAME_COLOR);
            setModified(true);
        }
    }

    /**
     * Gets the alpha value of the bitmask, which is <code>1 - getTransparency()</code>.
     *
     * @return the alpha value
     */
    public float getAlpha() {
        return 1.0F - getTransparency();
    }

    /**
     * Gets the transparency of the bitmask.
     *
     * @return the transparency
     */
    public float getTransparency() {
        return _transparency;
    }

    /**
     * Sets the transparency value. The valid value range is <code>0.0F</code> to <code>1.0F</code>.
     *
     * @param transparency the transparency value
     * @throws IllegalArgumentException if the given transparency value is out of range.
     */
    public void setTransparency(float transparency) {
        if (transparency < 0.0F || transparency > 1.0F) {
            throw new IllegalArgumentException("transparency must be >= 0 and <= 1");
        }
        if (_transparency != transparency) {
            _transparency = transparency;
            fireProductNodeChanged(PROPERTY_NAME_TRANSPARENCY);
            setModified(true);
        }
    }

    /**
     * Accepts the given visitor. This method implements the well known 'Visitor' design pattern of the gang-of-four.
     * The visitor pattern allows to define new operations on the product data model without the need to add more code
     * to it. The new operation is implemented by the visitor.
     *
     * @param visitor the visitor
     */
    @Override
    public void acceptVisitor(ProductVisitor visitor) {
        Guardian.assertNotNull("visitor", visitor);
        visitor.visit(this);
    }

    /**
     * Returns whether or not this product node equals another object.
     */
    @Override
    public boolean equals(Object object) {
        if (!super.equals(object)) {
            return false;
        }
        final BitmaskDef def = (BitmaskDef) object;
        return ObjectUtils.equalObjects(getExpr(), def.getExpr())
                && ObjectUtils.equalObjects(getColor(), def.getColor())
                && getTransparency() == def.getTransparency();
    }

    /**
     * Creates and returns a copy of this object.
     *
     * @return a copy of this object
     */
    public BitmaskDef createCopy() {
        return new BitmaskDef(getName(),
                              getDescription(),
                              getExpr(),
                              getColor(),
                              getTransparency());
    }


    /**
     * Gets the size in bytes of this product node.
     *
     * @param subsetDef if not <code>null</code> the subset may limit the size returned
     * @return the size in bytes.
     */
    @Override
    public long getRawStorageSize(ProductSubsetDef subsetDef) {
        return 64 + _expr.length();
    }

    public void writeXML(XmlWriter writer, int indent) {
        Guardian.assertNotNull("writer", writer);
        Guardian.assertGreaterThan("indent", indent, -1);

        String[][] attributes = new String[1][];

        attributes[0] = new String[]{DimapProductConstants.ATTRIB_NAME, getName()};
        String[] bdTags = XmlWriter.createTags(indent, DimapProductConstants.TAG_BITMASK_DEFINITION, attributes);
        writer.println(bdTags[0]);

        String description = getDescription();
        if (description == null) {
            description = "";
        }
        attributes[0] = new String[]{DimapProductConstants.ATTRIB_VALUE, description};
        writer.printLine(indent + 1, DimapProductConstants.TAG_BITMASK_DESCRIPTION, attributes, null);

        String expr = getExpr();
        if (expr == null) {
            expr = "";
        }
        attributes[0] = new String[]{DimapProductConstants.ATTRIB_VALUE, expr};
        writer.printLine(indent + 1, DimapProductConstants.TAG_BITMASK_EXPRESSION, attributes, null);

        DimapProductHelpers.printColorTag(indent + 1, getColor(), writer);

        attributes[0] = new String[]{DimapProductConstants.ATTRIB_VALUE, String.valueOf(getTransparency())};
        writer.printLine(indent + 1, DimapProductConstants.TAG_BITMASK_TRANSPARENCY, attributes, null);

        writer.println(bdTags[1]);
    }

    public static BitmaskDef createBitmaskDef(Element element) {
        //@todo replace BitmaskDef constructor by multiple param constuctor
        final String name = element.getAttributeValue(DimapProductConstants.ATTRIB_NAME);
        String description = null;
        Element descElem = element.getChild(DimapProductConstants.TAG_BITMASK_DESCRIPTION);
        if (descElem != null) {
            description = descElem.getAttributeValue(DimapProductConstants.ATTRIB_VALUE).trim();
        }
        final String expression = element.getChild(DimapProductConstants.TAG_BITMASK_EXPRESSION).getAttributeValue(
                DimapProductConstants.ATTRIB_VALUE).trim();
        final Color color = DimapProductHelpers.createColor(element.getChild(DimapProductConstants.TAG_BITMASK_COLOR));
        final String value = element.getChild(DimapProductConstants.TAG_BITMASK_TRANSPARENCY).getAttributeValue(
                DimapProductConstants.ATTRIB_VALUE);
        float transparency = 0.5F;
        try {
            transparency = Float.parseFloat(value);
        } catch (NumberFormatException e) {
            Debug.trace(e);
        }
        return new BitmaskDef(name, description, expression, color, transparency);
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
    @Override
    public void dispose() {
        _expr = null;
        _color = null;
        super.dispose();
    }
    
    public Mask createMask(int width, int height) {
        return Mask.BandMathsType.create(getName(), getDescription(), width, height,
                                             getExpr(), getColor(), getTransparency());
    }
}
