/*
 * $Id: Placemark.java,v 1.1.1.1 2006/09/11 08:16:45 norman Exp $
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

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.CoordinateSequenceFactory;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import org.esa.beam.dataio.dimap.DimapProductConstants;
import org.esa.beam.dataio.dimap.DimapProductHelpers;
import org.esa.beam.framework.dataio.ProductSubsetDef;
import org.esa.beam.util.Debug;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.XmlWriter;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.jdom.Element;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.Shape;
import java.text.MessageFormat;

// todo - rename to Placemark (se - 20090126) 

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

    public static final String PROPERTY_NAME_LABEL = "label";
    public static final String PROPERTY_NAME_PIXELPOS = "pixelPos";
    public static final String PROPERTY_NAME_GEOPOS = "geoPos";
    public static final String PROPERTY_NAME_PINSYMBOL = "pinSymbol";

    private final SimpleFeature feature;

    private PlacemarkSymbol symbol;

    /**
     * Returns the type of features underlying all pins.
     *
     * @return the type of features underlying all pins.
     *
     * @since BEAM 4.7
     */
    public static SimpleFeatureType getPinFeatureType() {
        return Holder.PIN_FEATURE_TYPE;
    }

    /**
     * Creates a new pin.
     *
     * @param name     the pin's name.
     * @param label    the pin's label.
     * @param pixelPos the pin's pixel position.
     *
     * @deprecated since 4.1, use {@link Pin#Pin(String, String, String, PixelPos, GeoPos, PlacemarkSymbol)}
     */
    @Deprecated
    public Pin(String name, String label, PixelPos pixelPos) {
        this(name, label, "", pixelPos, null, PlacemarkSymbol.createDefaultPinSymbol());
    }

    /**
     * Creates a new pin.
     *
     * @param name   the pin's name.
     * @param label  the pin's label.
     * @param geoPos the pin's geo-position.
     *
     * @deprecated since 4.1, use {@link Pin#Pin(String, String, String, PixelPos, GeoPos, PlacemarkSymbol)}
     */
    @Deprecated
    public Pin(String name, String label, GeoPos geoPos) {
        this(name, label, "", null, geoPos, PlacemarkSymbol.createDefaultPinSymbol());
    }

    public Pin(String name, String label, String description, PixelPos pixelPos, GeoPos geoPos,
               PlacemarkSymbol symbol) {
        super(name, description);
        if (pixelPos == null && geoPos == null) {
            throw new IllegalArgumentException("pixelPos == null && geoPos == null");
        }
        feature = createPinFeature(name, label, pixelPos, geoPos);
        this.symbol = symbol;
    }

    /**
     * Returns the {@link SimpleFeature}, underlying this pin.
     *
     * @return the {@link SimpleFeature} underlying this pin.
     *
     * @since BEAM 4.7
     */
    public SimpleFeature getFeature() {
        return feature;
    }

    /**
     * Sets this pin's label.
     *
     * @param label the label, if {@code null} an empty label is set.
     */
    public void setLabel(String label) {
        setFeatureLabel(label);
    }

    /**
     * Returns this pin's label.
     *
     * @return the label, cannot be {@code null}.
     */
    public String getLabel() {
        return getFeatureLabel();
    }

    /**
     * Returns an estimated, raw storage size in bytes of this pin.
     *
     * @param subsetDef if not {@code null} the subset may limit the size returned.
     *
     * @return the estimated size in bytes.
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

    public PlacemarkSymbol getSymbol() {
        return symbol;
    }

    public void setSymbol(final PlacemarkSymbol symbol) {
        Guardian.assertNotNull("symbol", symbol);
        if (this.symbol != symbol) {
            this.symbol = symbol;
            fireProductNodeChanged(PROPERTY_NAME_PINSYMBOL);
        }
    }

    /**
     * Tests if a certain pixel position is contained (covered) by this pin's symbol shape.
     *
     * @param pixelX the pixel x-coordinate.
     * @param pixelY the pixel y-coordinate.
     *
     * @return {@code true} if the given pixel position is contained in this pin's
     *         symbol shape, {@code false} otherwise
     *
     * @deprecated in 4.1, no replacement. Pin symbols are not in raster coordinates anymore
     */
    @Deprecated
    public boolean isPixelPosContainedInSymbolShape(float pixelX, float pixelY) {
        final Shape shape = getSymbol().getShape();
        if (shape != null) {
            final PixelPos pixelPos = getPixelPos();
            if (pixelPos != null) {
                final PixelPos refPoint = symbol.getRefPoint();
                double x = pixelX - pixelPos.getX();
                double y = pixelY - pixelPos.getY();
                if (refPoint != null) {
                    x += refPoint.getX();
                    y += refPoint.getY();
                }
                return shape.contains(x, y);
            }
        }

        return false;
    }

    public void setPixelPos(PixelPos pixelPos) {
        setPixelCoordinate(pixelPos);
    }

    /**
     * Returns this pin's pixel position.
     *
     * @return this pin's pixel position. If this pin's pixel position is {@code null}, the pixel
     *         position returned is calculated from this pin's geo-position, if possible.
     */
    public PixelPos getPixelPos() {
        PixelPos pixelPos = toPixelPos(getPixelCoordinate());
        if (pixelPos == null && canComputePixelPos()) {
            final GeoPos geoPos = toGeoPos(getGeoCoordinate());
            if (geoPos != null) {
                pixelPos = getProduct().getGeoCoding().getPixelPos(geoPos, null);
            }
        }
        if (pixelPos == null) {
            return null;
        }
        if (getProduct() != null) {
            final int w = getProduct().getSceneRasterWidth();
            final int h = getProduct().getSceneRasterHeight();
            final Rectangle bounds = new Rectangle(0, 0, w, h);
            if (!bounds.contains(pixelPos.x, pixelPos.y)) {
                return null;
            }
        }

        return pixelPos;
    }

    public void setGeoPos(GeoPos geoPos) {
        setGeoCoordinate(geoPos);
    }

    public GeoPos getGeoPos() {
        GeoPos geoPos = toGeoPos(getGeoCoordinate());
        if (geoPos == null && canComputeGeoPos()) {
            final PixelPos pixelPos = toPixelPos(getPixelCoordinate());
            if (pixelPos != null) {
                geoPos = getProduct().getGeoCoding().getGeoPos(pixelPos, null);
            }
        }
        if (geoPos == null) {
            return null;
        }
        return geoPos;
    }

    public void writeXML(XmlWriter writer, int indent) {
        Guardian.assertNotNull("writer", writer);
        Guardian.assertGreaterThan("indent", indent, -1);

        final String[][] attributes = {new String[]{DimapProductConstants.ATTRIB_NAME, getName()}};
        final String[] pinTags = XmlWriter.createTags(indent, DimapProductConstants.TAG_PLACEMARK, attributes);
        writer.println(pinTags[0]);
        indent++;
        writer.printLine(indent, DimapProductConstants.TAG_PLACEMARK_LABEL, getLabel());
        writer.printLine(indent, DimapProductConstants.TAG_PLACEMARK_DESCRIPTION, getDescription());
        final GeoPos geoPos = getGeoPos();
        if (geoPos != null) {
            writer.printLine(indent, DimapProductConstants.TAG_PLACEMARK_LATITUDE, geoPos.lat);
            writer.printLine(indent, DimapProductConstants.TAG_PLACEMARK_LONGITUDE, geoPos.lon);
        }
        final PixelPos pixelPos = getPixelPos();
        if (pixelPos != null) {
            writer.printLine(indent, DimapProductConstants.TAG_PLACEMARK_PIXEL_X, pixelPos.x);
            writer.printLine(indent, DimapProductConstants.TAG_PLACEMARK_PIXEL_Y, pixelPos.y);
        }
        final Color fillColor = (Color) symbol.getFillPaint();
        if (fillColor != null) {
            writeColor(DimapProductConstants.TAG_PLACEMARK_FILL_COLOR, indent, fillColor, writer);
        }
        final Color outlineColor = symbol.getOutlineColor();
        if (outlineColor != null) {
            writeColor(DimapProductConstants.TAG_PLACEMARK_OUTLINE_COLOR, indent, outlineColor, writer);
        }
        writer.println(pinTags[1]);
    }

    // todo - move this method into a new DimapPersistable

    private void writeColor(final String tagName, final int indent, final Color color, final XmlWriter writer) {
        final String[] colorTags = XmlWriter.createTags(indent, tagName);
        writer.println(colorTags[0]);
        DimapProductHelpers.printColorTag(indent + 1, color, writer);
        writer.println(colorTags[1]);
    }

    // todo - move this methods away from here

    public static Pin createGcp(Element element) {
        return createPlacemark(element, PlacemarkSymbol.createDefaultGcpSymbol());
    }

    public static Pin createPin(Element element) {
        return createPlacemark(element, PlacemarkSymbol.createDefaultPinSymbol());
    }

    /**
     * Creates a new pin from an XML element and a given symbol.
     *
     * @param element the element.
     * @param symbol  the symbol.
     *
     * @return the pin created.
     *
     * @throws NullPointerException     if element is null
     * @throws IllegalArgumentException if element is invalid
     */
    public static Pin createPlacemark(Element element, PlacemarkSymbol symbol) {
        if (!DimapProductConstants.TAG_PLACEMARK.equals(element.getName()) &&
            !DimapProductConstants.TAG_PIN.equals(element.getName())) {
            throw new IllegalArgumentException(MessageFormat.format("Element ''{0}'' or ''{1}'' expected.",
                                                                    DimapProductConstants.TAG_PLACEMARK,
                                                                    DimapProductConstants.TAG_PIN));
        }
        final String name = element.getAttributeValue(DimapProductConstants.ATTRIB_NAME);
        if (name == null) {
            throw new IllegalArgumentException(MessageFormat.format("Missing attribute ''{0}''.",
                                                                    DimapProductConstants.ATTRIB_NAME));
        }
        if (!Pin.isValidNodeName(name)) {
            throw new IllegalArgumentException(MessageFormat.format("Invalid placemark name ''{0}''.", name));
        }

        String label = element.getChildTextTrim(DimapProductConstants.TAG_PLACEMARK_LABEL);
        if (label == null) {
            label = name;
        }
        final String description = element.getChildTextTrim(DimapProductConstants.TAG_PLACEMARK_DESCRIPTION);
        final String latText = element.getChildTextTrim(DimapProductConstants.TAG_PLACEMARK_LATITUDE);
        final String lonText = element.getChildTextTrim(DimapProductConstants.TAG_PLACEMARK_LONGITUDE);
        final String posXText = element.getChildTextTrim(DimapProductConstants.TAG_PLACEMARK_PIXEL_X);
        final String posYText = element.getChildTextTrim(DimapProductConstants.TAG_PLACEMARK_PIXEL_Y);

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
        if (posXText != null && posYText != null) {
            try {
                float pixelX = Float.parseFloat(posXText);
                float pixelY = Float.parseFloat(posYText);
                pixelPos = new PixelPos(pixelX, pixelY);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid pixel-position.");
            }
        }
        if (geoPos == null && pixelPos == null) {
            throw new IllegalArgumentException("Neither geo-position nor pixel-position given.");
        }
        final Color fillColor = createColor(element.getChild(DimapProductConstants.TAG_PLACEMARK_FILL_COLOR));
        if (fillColor != null) {
            symbol.setFillPaint(fillColor);
        }
        final Color outlineColor = createColor(element.getChild(DimapProductConstants.TAG_PLACEMARK_OUTLINE_COLOR));
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
            setPixelPos(geoCoding.getPixelPos(getGeoPos(), null));
        }
    }

    private void setFeatureLabel(String label) {
        if (label == null) {
            label = "";
        }
        if (!label.equals(getFeatureLabel())) {
            feature.setAttribute(PROPERTY_NAME_LABEL, label);
            fireProductNodeChanged(PROPERTY_NAME_LABEL);
        }
    }

    private String getFeatureLabel() {
        return (String) feature.getAttribute(PROPERTY_NAME_LABEL);
    }

    private void setPixelCoordinate(PixelPos pixelPos) {
        final Coordinate newCoordinate = toCoordinate(pixelPos);
        final Coordinate oldCoordinate = getPixelCoordinate();
        if (oldCoordinate != newCoordinate) {
            if (oldCoordinate == null) {
                feature.setAttribute(PROPERTY_NAME_PIXELPOS, createPoint(newCoordinate, PROPERTY_NAME_PIXELPOS));
                fireProductNodeChanged(PROPERTY_NAME_PIXELPOS);
            } else if (!oldCoordinate.equals2D(newCoordinate)) {
                final Point point = (Point) feature.getAttribute(PROPERTY_NAME_PIXELPOS);
                point.getCoordinate().setCoordinate(newCoordinate);
                point.geometryChanged();
            }
        }
    }

    private Coordinate getPixelCoordinate() {
        final Point point = (Point) feature.getAttribute(PROPERTY_NAME_PIXELPOS);
        if (point != null) {
            return point.getCoordinate();
        }
        return null;
    }

    private void setGeoCoordinate(GeoPos geoPos) {
        final Coordinate newCoordinate = toCoordinate(geoPos);
        final Coordinate oldCoordinate = getGeoCoordinate();
        if (oldCoordinate != newCoordinate) {
            if (oldCoordinate == null) {
                feature.setAttribute(PROPERTY_NAME_GEOPOS, createPoint(newCoordinate, PROPERTY_NAME_GEOPOS));
                fireProductNodeChanged(PROPERTY_NAME_GEOPOS);
            } else if (!oldCoordinate.equals2D(newCoordinate)) {
                final Point point = (Point) feature.getAttribute(PROPERTY_NAME_GEOPOS);
                point.getCoordinate().setCoordinate(newCoordinate);
                point.geometryChanged();
            }
        }
    }

    private Coordinate getGeoCoordinate() {
        final Point point = (Point) feature.getAttribute(PROPERTY_NAME_GEOPOS);
        if (point != null) {
            return point.getCoordinate();
        }
        return null;
    }

    private SimpleFeature createPinFeature(String name, String label, PixelPos pixelPos, GeoPos geoPos) {
        final SimpleFeatureBuilder builder = new SimpleFeatureBuilder(Holder.PIN_FEATURE_TYPE);

        if (label == null) {
            builder.set(PROPERTY_NAME_LABEL, "");
        } else {
            builder.set(PROPERTY_NAME_LABEL, label);
        }
        if (pixelPos != null) {
            final Coordinate coordinate = new Coordinate(pixelPos.getX(), pixelPos.getY());
            builder.set(PROPERTY_NAME_PIXELPOS, createPoint(coordinate, PROPERTY_NAME_PIXELPOS));
        }
        if (geoPos != null) {
            final Coordinate coordinate = new Coordinate(geoPos.getLon(), geoPos.getLat());
            builder.set(PROPERTY_NAME_GEOPOS, createPoint(coordinate, PROPERTY_NAME_GEOPOS));
        }

        return builder.buildFeature(name);
    }

    private Point createPoint(final Coordinate coordinate, final String propertyName) {
        final CoordinateSequenceFactory factory = Holder.GEOMETRY_FACTORY.getCoordinateSequenceFactory();
        return new Point(factory.create(new Coordinate[]{coordinate}), Holder.GEOMETRY_FACTORY) {
            @Override
            protected void geometryChangedAction() {
                super.geometryChangedAction();
                fireProductNodeChanged(propertyName);
            }
        };
    }

    private static SimpleFeatureType createPlacemarkFeatureType(String typeName, String defaultGeometryName) {
        final CoordinateReferenceSystem crs = DefaultGeographicCRS.WGS84;
        final SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();

        builder.setCRS(crs);
        builder.setName(typeName);
        builder.add(PROPERTY_NAME_LABEL, String.class);
        // todo: rq/rq - description
        // todo: rq/rq - imageCRS?
        builder.add(PROPERTY_NAME_PIXELPOS, Point.class, crs);
        // todo: rq/rq - geoCRS? mapCRS??
        builder.add(PROPERTY_NAME_GEOPOS, Point.class, crs);
        // todo: rq/rq - symbol
        builder.setDefaultGeometry(defaultGeometryName);

        return builder.buildFeatureType();
    }

    private static Coordinate toCoordinate(GeoPos geoPos) {
        if (geoPos != null) {
            return new Coordinate(geoPos.getLon(), geoPos.getLat());
        }
        return null;
    }

    private static Coordinate toCoordinate(PixelPos pixelPos) {
        if (pixelPos != null) {
            return new Coordinate(pixelPos.getX(), pixelPos.getY());
        }
        return null;
    }

    private static GeoPos toGeoPos(Coordinate coordinate) {
        if (coordinate != null) {
            return new GeoPos((float) coordinate.y, (float) coordinate.x);
        }
        return null;
    }

    private static PixelPos toPixelPos(Coordinate coordinate) {
        if (coordinate != null) {
            return new PixelPos((float) coordinate.x, (float) coordinate.y);
        }
        return null;
    }

    private static class Holder {

        private static final GeometryFactory GEOMETRY_FACTORY = new GeometryFactory();
        private static final SimpleFeatureType PIN_FEATURE_TYPE =
                createPlacemarkFeatureType(Product.PIN_FEATURE_TYPE_NAME, PROPERTY_NAME_PIXELPOS);
    }
}
