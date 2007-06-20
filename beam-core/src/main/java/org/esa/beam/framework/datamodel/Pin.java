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
import org.esa.beam.framework.dataio.ProductSubsetDef;
import org.esa.beam.util.Debug;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.SystemUtils;
import org.esa.beam.util.XmlHelper;
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
 * @version $Revision: 1.1.1.1 $ $Date: 2006/09/11 08:16:45 $
 */
public class Pin extends ProductNode {

    public final static String PROPERTY_NAME_PINSYMBOL = "pinSymbol";
    public final static String PROPERTY_NAME_LATITUDE = "latitude";
    public final static String PROPERTY_NAME_LONGITUDE = "longitude";
    public final static String PROPERTY_NAME_SELECTED = "selected";

    private String label;
    private float _longitude;
    private float _latitude;
    private boolean _selected;
    private PinSymbol _symbol;


    /**
     * Constructs a new product node with the given name.
     *
     * @param name the node name, must not be <code>null</code>
     *
     * @throws java.lang.IllegalArgumentException
     *          if the given name is not a valid node identifier
     */
    public Pin(String name) {
        super(name);
        setLabel(generateLabel(name));
        setSymbol(PinSymbol.createDefaultPinSymbol());
    }

    public Pin(final String name, final String label, final String description, final float lat,
               final float lon, final PinSymbol pinSymbol) {
        this(name);
        if (label != null) {
            setLabel(label);
        }
        setDescription(description);
        setLatitude(lat);
        setLongitude(lon);
        if (pinSymbol != null) {
            setSymbol(pinSymbol);
        }
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
        this.label = (label != null) ? label : "";
    }

    /**
     * Gets an estimated, raw storage size in bytes of this product node.
     *
     * @param subsetDef if not <code>null</code> the subset may limit the size returned
     *
     * @return the size in bytes.
     */
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
    public void acceptVisitor(ProductVisitor visitor) {
    }

    public PinSymbol getSymbol() {
        return _symbol;
    }

    public void setSymbol(final PinSymbol symbol) {
        Guardian.assertNotNull("symbol", symbol);
        if (_symbol != symbol) {
            _symbol = symbol;
            fireProductNodeChanged(PROPERTY_NAME_PINSYMBOL);
        }
    }

    public float getLongitude() {
        return _longitude;
    }

    public void setLongitude(final float longitude) {
        if (_longitude != longitude) {
            _longitude = longitude;
            fireProductNodeChanged(PROPERTY_NAME_LONGITUDE);
        }
    }

    public float getLatitude() {
        return _latitude;
    }

    public void setLatitude(final float latitude) {
        if (_latitude != latitude) {
            _latitude = latitude;
            fireProductNodeChanged(PROPERTY_NAME_LATITUDE);
        }
    }

    public boolean isSelected() {
        return _selected;
    }

    public void setSelected(boolean selected) {
        if (_selected != selected) {
            _selected = selected;
            fireProductNodeChanged(PROPERTY_NAME_SELECTED);
        }
    }

    public boolean containsPixelPos(float pixelX, float pixelY, GeoCoding geoCoding) {
        Shape shape = getSymbol().getShape();
        if (shape != null) {
            PixelPos pinPixelPos = getPixelPos(geoCoding);
            double x = pixelX - pinPixelPos.getX();
            double y = pixelY - pinPixelPos.getY();
            PixelPos refPoint = _symbol.getRefPoint();
            if (refPoint != null) {
                x += refPoint.getX();
                y += refPoint.getY();
            }
            return shape.contains(x, y);
        }
        return false;
    }

    private PixelPos getPixelPos(GeoCoding geoCoding) {
        if (geoCoding != null) {
            final GeoPos geoPos = new GeoPos(getLatitude(), getLongitude());
            return geoCoding.getPixelPos(geoPos, null);
        }
        return null;
    }

    /**
     * Gets the pixel position for this pin.
     *
     * @return the pixel position or null if the product to which this pin was added has no {@link
     *         <code>GeoCoding</code>} or the GeoCoding cannot provide the correct pixel position.
     *
     * @see GeoCoding#canGetPixelPos()
     */
    public PixelPos getPixelPos() {
        PixelPos pos = null;
        final Product product = getProduct();
        if (product != null) {
            final GeoCoding geoCoding = product.getGeoCoding();
            if (geoCoding != null) {
                pos = getPixelPos(geoCoding);
            }
        }
        return pos;
    }

    public void writeXML(XmlWriter writer, int indent) {
        Guardian.assertNotNull("writer", writer);
        Guardian.assertGreaterThan("indent", indent, -1);
        String[] pinTags = XmlWriter.createTags(indent, DimapProductConstants.TAG_PIN,
                new String[][]{
                    new String[]{DimapProductConstants.ATTRIB_NAME, getName()}
                });
        writer.println(pinTags[0]);
        indent++;
        writer.printLine(indent, DimapProductConstants.TAG_PIN_LABEL, getLabel());
        writer.printLine(indent, DimapProductConstants.TAG_PIN_DESCRIPTION, getDescription());
        writer.printLine(indent, DimapProductConstants.TAG_PIN_LATITUDE, _latitude);
        writer.printLine(indent, DimapProductConstants.TAG_PIN_LONGITUDE, _longitude);
        writeColor(DimapProductConstants.TAG_PIN_FILL_COLOR, indent, (Color) _symbol.getFillPaint(), writer);
        writeColor(DimapProductConstants.TAG_PIN_OUTLINE_COLOR, indent, (Color) _symbol.getOutlinePaint(), writer);
        writer.println(pinTags[1]);
    }

    private void writeColor(final String tagName, final int indent, final Color color, final XmlWriter writer) {
        String[] colorTags = XmlWriter.createTags(indent, tagName);
        writer.println(colorTags[0]);
        XmlHelper.printColorTag(indent + 1, color, writer);
        writer.println(colorTags[1]);
    }

    public static Pin createPin(Element element) {
        //@todo 1 he/he - exception werfen und dort wo die methode verwendet wird auswerten.
        // Z.B. im ProdcutReader einen Warnings Vector einbauen der diese
        // fehlermeldung sammelt. Anschließend können diese Fehlermeldungen ausgewertet
        // und geloggt werden.
        final String message = "failed to create pin: " + SystemUtils.LS;
        if (element == null) {
            Debug.trace(message + "element is null.");
            return null;
        }
        if (!DimapProductConstants.TAG_PIN.equals(element.getName())) {
            Debug.trace(message + "element is not a pin element.");
            return null;
        }
        final String name = element.getAttributeValue(DimapProductConstants.ATTRIB_NAME);
        final String label = element.getChildTextTrim(DimapProductConstants.TAG_PIN_LABEL);
        final String description = element.getChildTextTrim(DimapProductConstants.TAG_PIN_DESCRIPTION);
        final String latitude = element.getChildTextTrim(DimapProductConstants.TAG_PIN_LATITUDE);
        final String longitude = element.getChildTextTrim(DimapProductConstants.TAG_PIN_LONGITUDE);

// TODO 1 i get a null pointer exception here - so, check for null pointers and then set create to false
// TODO 1 Sabine -> checlk if this is correct behaviour

        if ((name == null) || (latitude == null) || (longitude == null)) {
            Debug.trace("Element does not contain all required parameter!");
            return null;
        }

        boolean create = true;
        if (!Pin.isValidNodeName(name)) {
            Debug.trace(message + "'" + name + "' is not a valid name for a pin");
            create = false;
        }
        float lat = 0;
        try {
            lat = Float.parseFloat(latitude);
        } catch (NumberFormatException e) {
            Debug.trace(message + "illegal latitude value: " + latitude);
            create = false;
        }
        float lon = 0;
        try {
            lon = Float.parseFloat(longitude);
        } catch (NumberFormatException e) {
            Debug.trace(message + "illegal longitude value: " + longitude);
            create = false;
        }

        PinSymbol pinSymbol = PinSymbol.createDefaultPinSymbol();

        Color fillColor = createColor(element.getChild(DimapProductConstants.TAG_PIN_FILL_COLOR));
        if (fillColor != null) {
            pinSymbol.setFillPaint(fillColor);
        }
        Color outlineColor = createColor(element.getChild(DimapProductConstants.TAG_PIN_OUTLINE_COLOR));
        if (outlineColor != null) {
            pinSymbol.setOutlinePaint(outlineColor);
        }

        if (create) {
            Pin pin = new Pin(name);
            if (label != null) {
                pin.setLabel(label);
            }
            pin.setLatitude(lat);
            pin.setLongitude(lon);
            pin.setDescription(description);
            pin.setSymbol(pinSymbol);
            return pin;
        } else {
            return null;
        }
    }

    private static Color createColor(Element elem) {
        if (elem != null) {
            Element colorElem = elem.getChild(DimapProductConstants.TAG_COLOR);
            if (colorElem != null) {
                try {
                    return XmlHelper.createColor(colorElem);
                } catch (NumberFormatException e) {
                    Debug.trace(e);
                } catch (IllegalArgumentException e) {
                    Debug.trace(e);
                }
            }
        }
        return null;
    }

    public GeoPos getGeoPos() {
        return new GeoPos(getLatitude(), getLongitude());
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
        if (_symbol != null) {
            _symbol.dispose();
            _symbol = null;
        }
        super.dispose();
    }

    private String generateLabel(String name) {
        return name.replace('_', ' ');
    }
}
