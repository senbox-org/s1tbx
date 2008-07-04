/*
 * $Id: Pin.java,v 1.1.1.1 2006/09/11 08:16:45 norman Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.framework.datamodel;

import org.esa.beam.dataio.dimap.DimapProductConstants;
import org.esa.beam.dataio.dimap.DimapProductHelpers;
import org.esa.beam.framework.dataio.ProductSubsetDef;
import org.esa.beam.util.Debug;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.ObjectUtils;
import org.esa.beam.util.XmlWriter;
import org.jdom.Element;

import java.awt.Color;
import java.awt.Shape;

/**
 * This class represents a pin.
 * <p/>
 * Pins are displayed as symbols at the image's pixel position corresponding to their geographical position. The name is
 * displayed as label next to the symbol. If the user moves the mouse over a pin, the textual description property shall
 * appear as tool-tip text. Single pins can be selected either by mouse-click or by the ? Prev./Next Pin tool. Pins are
 * contained in the active product and stored in DIMAP format. To share pins between products, the pins of a product can
 * be imported and exported.
 *
 * @author Sabine Embacher
 * @version $Revision$ $Date$
 */
public class Pin extends ProductNode {

    public final static String PROPERTY_NAME_GEOPOS = "geoPos";
    public final static String PROPERTY_NAME_PIXELPOS = "pixelPos";
    public final static String PROPERTY_NAME_PINSYMBOL = "pinSymbol";
    private static final String PROPERTY_NAME_LABEL = "label";

    private String label;
    private PixelPos pixelPos;
    private GeoPos geoPos;
    private PinSymbol symbol;

    /**
     * @deprecated in 4.1, use {@link Pin#Pin(String, String, String, PixelPos, GeoPos, PinSymbol)}
     */
    @Deprecated
    public Pin(String name, String label, PixelPos pixelPos) {
        this(name, label, "", pixelPos, null, PinSymbol.createDefaultPinSymbol());
    }

    /**
     * @deprecated in 4.1, use {@link Pin#Pin(String, String, String, PixelPos, GeoPos, PinSymbol)}
     */
    @Deprecated
    public Pin(String name, String label, GeoPos geoPos) {
        this(name, label, "", null, geoPos, PinSymbol.createDefaultPinSymbol());
    }

    public Pin(String name,
               String label,
               String description,
               PixelPos pixelPos,
               GeoPos geoPos,
               PinSymbol pinSymbol) {
        super(name);
        if (pixelPos == null && geoPos == null) {
            throw new IllegalArgumentException("pixelPos == null && geoPos == null");
        }
        setLabel(label);
        setDescription(description);
        setPixelPos(pixelPos);
        setGeoPos(geoPos);
        setSymbol(pinSymbol);
    }

    /**
     * Gets this pin's label.
     *
     * @return the label, cannot be null
     */
    public String getLabel() {
        return label;
    }

    /**
     * Sets this pin's label.
     *
     * @param label the label, if null an empty label is assigned
     */
    public void setLabel(String label) {
        if (label == null) {
            label = "";
        }
        if (!label.equals(this.label)) {
            this.label = label;
            fireProductNodeChanged(PROPERTY_NAME_LABEL);
        }
    }

    /**
     * Gets an estimated, raw storage size in bytes of this product node.
     *
     * @param subsetDef if not <code>null</code> the subset may limit the size returned
     * @return the size in bytes.
     */
    @Override
    public long getRawStorageSize(ProductSubsetDef subsetDef) {
        return 256;
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
    }

    public PinSymbol getSymbol() {
        return symbol;
    }

    public void setSymbol(final PinSymbol symbol) {
        Guardian.assertNotNull("symbol", symbol);
        if (this.symbol != symbol) {
            this.symbol = symbol;
            fireProductNodeChanged(PROPERTY_NAME_PINSYMBOL);
        }
    }

    /**
     * @deprecated in 4.1, no replacement. Pin symbols are not in raster coordinates anymore
     */
    @Deprecated
    public boolean isPixelPosContainedInSymbolShape(float pixelX, float pixelY) {
        Shape shape = getSymbol().getShape();
        if (shape != null) {
            PixelPos pixelPos = getPixelPos();
            if (pixelPos != null) {
                double x = pixelX - pixelPos.getX();
                double y = pixelY - pixelPos.getY();
                PixelPos refPoint = symbol.getRefPoint();
                if (refPoint != null) {
                    x += refPoint.getX();
                    y += refPoint.getY();
                }
                return shape.contains(x, y);
            }
        }
        return false;
    }


    public GeoPos getGeoPos() {
        if (geoPos == null && canComputeGeoPos()) {
            geoPos = getProduct().getGeoCoding().getGeoPos(pixelPos, null);
        }
        if (geoPos == null) {
            return null;
        }
        return new GeoPos(geoPos.lat, geoPos.lon);
    }

    public void setPixelPos(PixelPos pixelPos) {
        if (ObjectUtils.equalObjects(this.pixelPos, pixelPos)) {
            return;
        }
        this.pixelPos = pixelPos;
        fireProductNodeChanged(PROPERTY_NAME_PIXELPOS);
    }

    /**
     * Gets the pixel position for this pin.
     *
     * @return the pixel position or null if the product to which this pin was added has no {@link
     *         <code>GeoCoding</code>} or the GeoCoding cannot provide the correct pixel position.
     * @see GeoCoding#canGetPixelPos()
     */
    public PixelPos getPixelPos() {
        if (pixelPos == null && canComputePixelPos()) {
            pixelPos = getProduct().getGeoCoding().getPixelPos(geoPos, null);
        }
        if (pixelPos == null) {
            return null;
        }
        return new PixelPos(pixelPos.x, pixelPos.y);
    }

    public void setGeoPos(GeoPos geoPos) {
        if (ObjectUtils.equalObjects(this.geoPos, geoPos)) {
            return;
        }
        this.geoPos = geoPos;
        fireProductNodeChanged(PROPERTY_NAME_GEOPOS);
    }

    public void writeXML(XmlWriter writer, int indent) {
        Guardian.assertNotNull("writer", writer);
        Guardian.assertGreaterThan("indent", indent, -1);
        String[] pinTags = XmlWriter.createTags(indent, DimapProductConstants.TAG_PLACEMARK,
                                                new String[][]{
                                                        new String[]{DimapProductConstants.ATTRIB_NAME, getName()}
                                                });
        writer.println(pinTags[0]);
        indent++;
        writer.printLine(indent, DimapProductConstants.TAG_PLACEMARK_LABEL, getLabel());
        writer.printLine(indent, DimapProductConstants.TAG_PLACEMARK_DESCRIPTION, getDescription());
        if (getGeoPos() != null) {
            writer.printLine(indent, DimapProductConstants.TAG_PLACEMARK_LATITUDE, getGeoPos().lat);
            writer.printLine(indent, DimapProductConstants.TAG_PLACEMARK_LONGITUDE, getGeoPos().lon);
        }
        if (getPixelPos() != null) {
            writer.printLine(indent, DimapProductConstants.TAG_PLACEMARK_PIXEL_X, getPixelPos().x);
            writer.printLine(indent, DimapProductConstants.TAG_PLACEMARK_PIXEL_Y, getPixelPos().y);
        }
        Color fillColor = (Color) symbol.getFillPaint();
        if (fillColor != null) {
            writeColor(DimapProductConstants.TAG_PLACEMARK_FILL_COLOR, indent, fillColor, writer);
        }
        Color outlineColor = symbol.getOutlineColor();
        if (outlineColor != null) {
            writeColor(DimapProductConstants.TAG_PLACEMARK_OUTLINE_COLOR, indent, outlineColor, writer);
        }
        writer.println(pinTags[1]);
    }

    // todo - move this method into a new DimapPersistable
    private void writeColor(final String tagName, final int indent, final Color color, final XmlWriter writer) {
        String[] colorTags = XmlWriter.createTags(indent, tagName);
        writer.println(colorTags[0]);
        DimapProductHelpers.printColorTag(indent + 1, color, writer);
        writer.println(colorTags[1]);
    }

    // todo - move this methods away from here

    public static Pin createGcp(Element element) {
        return createPlacemark(element, PinSymbol.createDefaultGcpSymbol());
    }

    public static Pin createPin(Element element) {
        return createPlacemark(element, PinSymbol.createDefaultPinSymbol());
    }

    /**
     * @throws NullPointerException     if element is null
     * @throws IllegalArgumentException if element is invalid
     */
    public static Pin createPlacemark(Element element, PinSymbol symbol) {
        if (!DimapProductConstants.TAG_PLACEMARK.equals(element.getName()) &&
                !DimapProductConstants.TAG_PIN.equals(element.getName())) {
            throw new IllegalArgumentException("Element '" + DimapProductConstants.TAG_PLACEMARK + "' or '" +
                    DimapProductConstants.TAG_PIN + "' expected.");
        }
        final String name = element.getAttributeValue(DimapProductConstants.ATTRIB_NAME);
        if (name == null) {
            throw new IllegalArgumentException("Missing attribute '" + DimapProductConstants.ATTRIB_NAME + "'.");
        }
        if (!Pin.isValidNodeName(name)) {
            throw new IllegalArgumentException("Invalid placemark name '" + name + "'.");
        }

        String label = element.getChildTextTrim(DimapProductConstants.TAG_PLACEMARK_LABEL);
        if (label == null) {
            label = name;
        }
        String description = element.getChildTextTrim(DimapProductConstants.TAG_PLACEMARK_DESCRIPTION);

        String latText = element.getChildTextTrim(DimapProductConstants.TAG_PLACEMARK_LATITUDE);
        String lonText = element.getChildTextTrim(DimapProductConstants.TAG_PLACEMARK_LONGITUDE);
        String pixelXText = element.getChildTextTrim(DimapProductConstants.TAG_PLACEMARK_PIXEL_X);
        String pixelYText = element.getChildTextTrim(DimapProductConstants.TAG_PLACEMARK_PIXEL_Y);

        GeoPos geoPos = null;
        if (latText != null && lonText != null) {
            try {
                float lat = Float.parseFloat(latText);
                float lon = Float.parseFloat(lonText);
                geoPos = new GeoPos(lat, lon);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid geo-position.");
            }
        }

        PixelPos pixelPos = null;
        if (pixelXText != null && pixelYText != null) {
            try {
                float pixelX = Float.parseFloat(pixelXText);
                float pixelY = Float.parseFloat(pixelYText);
                pixelPos = new PixelPos(pixelX, pixelY);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid pixel-position.");
            }
        }

        if (geoPos == null && pixelPos == null) {
            throw new IllegalArgumentException("Neither geo-position nor pixel-position given.");
        }

        Color fillColor = createColor(element.getChild(DimapProductConstants.TAG_PLACEMARK_FILL_COLOR));
        if (fillColor != null) {
            symbol.setFillPaint(fillColor);
        }
        Color outlineColor = createColor(element.getChild(DimapProductConstants.TAG_PLACEMARK_OUTLINE_COLOR));
        if (outlineColor != null) {
            symbol.setOutlineColor(outlineColor);
        }

        return new Pin(name, label, description, pixelPos, geoPos, symbol);
    }

    // todo - move this method into a new DimapPersistable
    private static Color createColor(Element elem) {
        if (elem != null) {
            Element colorElem = elem.getChild(DimapProductConstants.TAG_COLOR);
            if (colorElem != null) {
                try {
                    return DimapProductHelpers.createColor(colorElem);
                } catch (NumberFormatException e) {
                    Debug.trace(e);
                } catch (IllegalArgumentException e) {
                    Debug.trace(e);
                }
            }
        }
        return null;
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
        if (symbol != null) {
            symbol.dispose();
            symbol = null;
        }
        super.dispose();
    }


    private boolean canComputeGeoPos() {
        return (getProduct() != null
                && getProduct().getGeoCoding() != null
                && getProduct().getGeoCoding().canGetGeoPos());
    }

    private boolean canComputePixelPos() {
        return (getProduct() != null
                && getProduct().getGeoCoding() != null
                && getProduct().getGeoCoding().canGetPixelPos());
    }

    public void updatePixelPos(GeoCoding geoCoding) {
        if (getGeoPos() != null && geoCoding != null && geoCoding.canGetPixelPos()) {
            PixelPos pixelPos = geoCoding.getPixelPos(getGeoPos(), null);
            setPixelPos(pixelPos);
        }
    }
}
