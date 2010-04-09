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
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import org.esa.beam.dataio.dimap.DimapProductConstants;
import org.esa.beam.dataio.dimap.DimapProductHelpers;
import org.esa.beam.framework.dataio.ProductSubsetDef;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.util.Debug;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.ObjectUtils;
import org.esa.beam.util.XmlWriter;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.jdom.Element;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.text.MessageFormat;
import java.util.List;

/**
 * This class represents a placemark.
 * <p/>
 * Placemarks are displayed as symbols at the image's pixel position corresponding to their geographical position. The name is
 * displayed as label next to the symbol. If the user moves the mouse over a placemark, the textual description property shall
 * appear as tool-tip text. Single placemarks can be selected either by mouse-click or by the ? Prev./Next Placemark tool.
 * Placemarks are contained in the active product and stored in DIMAP format. To share placemarks between products,
 * the placemarks of a product can be imported and exported.
 *
 * @author Sabine Embacher
 * @version $Revision$ $Date$
 */
public class Placemark extends ProductNode {

    public static final String PLACEMARK_FEATURE_TYPE_NAME = "Placemark";

    public static final String PROPERTY_NAME_LABEL = "label";
    public static final String PROPERTY_NAME_PIXELPOS = "pixelPos";
    public static final String PROPERTY_NAME_GEOPOS = "geoPos";
    public static final String PROPERTY_NAME_PINSYMBOL = "pinSymbol";

    private SimpleFeature feature;
    private PlacemarkDescriptor placemarkDescriptor;


    /**
     * Creates a new placemark.
     *
     * @param name        the placemark's name.
     * @param label       the placemark's label.
     * @param description the placemark's description
     * @param pixelPos    the placemark's pixel position
     * @param geoPos      the placemark's geo-position.
     * @param descriptor  the placemark's descriptor.
     * @param geoCoding   the placemark's geo-coding.
     */
    public Placemark(String name, String label, String description, PixelPos pixelPos, GeoPos geoPos,
                     PlacemarkDescriptor descriptor, GeoCoding geoCoding) {
        this(createFeature(name, label, pixelPos, geoPos, descriptor, geoCoding), description, descriptor);
    }

    Placemark(SimpleFeature feature, String description, PlacemarkDescriptor descriptor) {
        super(feature.getID(), description);
        this.feature = feature;
        placemarkDescriptor = descriptor;
        setSymbol(placemarkDescriptor.createDefaultSymbol());
    }

    /**
     * Returns the type of features underlying all placemarks.
     *
     * @return the type of features underlying all placemarks.
     *
     * @since BEAM 4.7
     */
    public static SimpleFeatureType getFeatureType() {
        return Holder.PLACEMARK_FEATURE_TYPE;
    }

    /**
     * Returns the {@link SimpleFeature}, underlying this placemark.
     *
     * @return the {@link SimpleFeature} underlying this placemark.
     *
     * @since BEAM 4.7
     */
    public final SimpleFeature getFeature() {
        return feature;
    }

    /**
     * Sets this placemark's label.
     *
     * @param label the label, if {@code null} an empty label is set.
     */
    public void setLabel(String label) {
        if (label == null) {
            label = "";
        }
        if (!label.equals(feature.getAttribute(PROPERTY_NAME_LABEL))) {
            feature.setAttribute(PROPERTY_NAME_LABEL, label);
            fireProductNodeChanged(PROPERTY_NAME_LABEL);
        }
    }

    /**
     * Returns this placemark's label.
     *
     * @return the label, cannot be {@code null}.
     */
    public String getLabel() {
        return (String) feature.getAttribute(PROPERTY_NAME_LABEL);
    }

    /**
     * Returns an estimated, raw storage size in bytes of this placemark.
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
        return (PlacemarkSymbol) feature.getAttribute("symbol");
    }

    public void setSymbol(final PlacemarkSymbol symbol) {
        Guardian.assertNotNull("symbol", symbol);
        if (getSymbol() != symbol) {
            feature.setAttribute("symbol", symbol);
            fireProductNodeChanged(PROPERTY_NAME_PINSYMBOL);
        }
    }

    public void setPixelPos(PixelPos pixelPos) {
        setPixelCoordinate(pixelPos, true);
        final Product product = getProduct();
        if (product != null) {
            final GeoPos geoPos = toGeoPos(getGeoCoordinate());
            placemarkDescriptor.updateGeoPos(product.getGeoCoding(), pixelPos, geoPos);
            setGeoCoordinate(geoPos);
        }
    }

    /**
     * Returns this placemark's pixel position.
     *
     * @return this placemark's pixel position. If this placemark's pixel position is {@code null}, the pixel
     *         position returned is calculated from this placemark's geo-position, if possible.
     */
    public PixelPos getPixelPos() {
        PixelPos pixelPos = toPixelPos(getPixelCoordinate());
        final Product product = getProduct();
        if (pixelPos != null && product != null) {
            final int w = product.getSceneRasterWidth();
            final int h = product.getSceneRasterHeight();
            final Rectangle bounds = new Rectangle(0, 0, w, h);
            if (!bounds.contains(pixelPos.x, pixelPos.y)) {
                pixelPos.setInvalid();
            }
        }
        return pixelPos;
    }

    public void setGeoPos(GeoPos geoPos) {
        setGeoCoordinate(geoPos);
        final PixelPos pixelPos = toPixelPos(getPixelCoordinate());
        placemarkDescriptor.updatePixelPos(getProduct().getGeoCoding(), geoPos, pixelPos);
        setPixelCoordinate(pixelPos, true);
    }

    public GeoPos getGeoPos() {
        return toGeoPos(getGeoCoordinate());
    }

    /**
     * Updates pixel and geo position according to the current geometry (model coordinates).
     */
    public void updatePositions() {
        final Point point = (Point) feature.getDefaultGeometry();
        if (point != null) {
            if (getProduct() != null) {
                final GeoCoding geoCoding = getProduct().getGeoCoding();
                final AffineTransform i2m = ImageManager.getImageToModelTransform(geoCoding);
                PixelPos pixelPos = new PixelPos((float) point.getX(), (float) point.getY());
                try {
                    i2m.inverseTransform(pixelPos, pixelPos);
                } catch (NoninvertibleTransformException ignored) {
                    // ignore
                }
                setPixelCoordinate(pixelPos, false);

                GeoPos geoPos = toGeoPos(getGeoCoordinate());
                placemarkDescriptor.updateGeoPos(geoCoding, pixelPos, geoPos);
                setGeoCoordinate(geoPos);
            }

        }
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
        if (pixelPos != null && pixelPos.isValid()) {
            writer.printLine(indent, DimapProductConstants.TAG_PLACEMARK_PIXEL_X, pixelPos.x);
            writer.printLine(indent, DimapProductConstants.TAG_PLACEMARK_PIXEL_Y, pixelPos.y);
        }
        final Color fillColor = (Color) getSymbol().getFillPaint();
        if (fillColor != null) {
            writeColor(DimapProductConstants.TAG_PLACEMARK_FILL_COLOR, indent, fillColor, writer);
        }
        final Color outlineColor = getSymbol().getOutlineColor();
        if (outlineColor != null) {
            writeColor(DimapProductConstants.TAG_PLACEMARK_OUTLINE_COLOR, indent, outlineColor, writer);
        }
        writer.println(pinTags[1]);
    }

    /**
     * Creates a new placemark from an XML element and a given symbol.
     *
     * @param element    the element.
     * @param descriptor the descriptor of the placemark.
     * @param geoCoding  the geoCoding to used by the placemark. Can be <code>null</code>.
     *
     * @return the placemark created.
     *
     * @throws NullPointerException     if element is null
     * @throws IllegalArgumentException if element is invalid
     */
    public static Placemark createPlacemark(Element element, PlacemarkDescriptor descriptor, GeoCoding geoCoding) {
        if (!DimapProductConstants.TAG_PLACEMARK.equals(element.getName()) &&
            !DimapProductConstants.TAG_PIN.equals(element.getName())) {
            throw new IllegalArgumentException(MessageFormat.format("Element ''{0}'' or ''{1}'' expected.",
                                                                    DimapProductConstants.TAG_PLACEMARK,
                                                                    DimapProductConstants.TAG_PIN));
        }
        final String name1 = element.getAttributeValue(DimapProductConstants.ATTRIB_NAME);
        if (name1 == null) {
            throw new IllegalArgumentException(MessageFormat.format("Missing attribute ''{0}''.",
                                                                    DimapProductConstants.ATTRIB_NAME));
        }
        if (!ProductNode.isValidNodeName(name1)) {
            throw new IllegalArgumentException(MessageFormat.format("Invalid placemark name ''{0}''.", name1));
        }

        String label = element.getChildTextTrim(DimapProductConstants.TAG_PLACEMARK_LABEL);
        if (label == null) {
            label = name1;
        }
        final String description1 = element.getChildTextTrim(DimapProductConstants.TAG_PLACEMARK_DESCRIPTION);
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
                throw new IllegalArgumentException("Invalid geo-position.", e);
            }
        }
        PixelPos pixelPos = null;
        if (posXText != null && posYText != null) {
            try {
                float pixelX = Float.parseFloat(posXText);
                float pixelY = Float.parseFloat(posYText);
                pixelPos = new PixelPos(pixelX, pixelY);
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid pixel-position.", e);
            }
        }
        if (geoPos == null && pixelPos == null) {
            throw new IllegalArgumentException("Neither geo-position nor pixel-position given.");
        }

        final Placemark placemark = new Placemark(name1, label, description1, pixelPos, geoPos, descriptor, geoCoding);

        PlacemarkSymbol symbol = descriptor.createDefaultSymbol();
        final Color fillColor = createColor(element.getChild(DimapProductConstants.TAG_PLACEMARK_FILL_COLOR));
        if (fillColor != null) {
            symbol.setFillPaint(fillColor);
        }
        final Color outlineColor = createColor(element.getChild(DimapProductConstants.TAG_PLACEMARK_OUTLINE_COLOR));
        if (outlineColor != null) {
            symbol.setOutlineColor(outlineColor);
        }

        placemark.setSymbol(symbol);

        return placemark;

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
        if (feature != null) {
            final PlacemarkSymbol symbol = getSymbol();
            if (symbol != null) {
                symbol.dispose();
            }
            for (final AttributeDescriptor attributeDescriptor : feature.getFeatureType().getAttributeDescriptors()) {
                feature.setAttribute(attributeDescriptor.getLocalName(), null);
            }
            feature = null;
        }
        super.dispose();
    }

    private void updateGeometry(PixelPos pixelPos) {
        final Product product = getProduct();
        final Point2D.Double geometryPoint = new Point2D.Double(pixelPos.x, pixelPos.y);
        if (product != null) {
            final AffineTransform i2m = ImageManager.getImageToModelTransform(product.getGeoCoding());
            i2m.transform(pixelPos, geometryPoint);
        }
        final Point point = (Point) feature.getDefaultGeometry();
        point.getCoordinate().setCoordinate(toCoordinate(geometryPoint));
        point.geometryChanged();
    }

    private Coordinate getPixelCoordinate() {
        final Point point = (Point) feature.getAttribute(PROPERTY_NAME_PIXELPOS);
        if (point != null) {
            return point.getCoordinate();
        }
        return null;
    }

    private void setPixelCoordinate(PixelPos pixelPos, boolean updateGeometry) {
        final Coordinate newCoordinate = toCoordinate(pixelPos);
        final Coordinate oldCoordinate = getPixelCoordinate();
        if (!ObjectUtils.equalObjects(oldCoordinate, newCoordinate)) {
            if (oldCoordinate == null) {
                final GeometryFactory geometryFactory = new GeometryFactory();
                feature.setAttribute(PROPERTY_NAME_PIXELPOS, geometryFactory.createPoint(newCoordinate));
            } else {
                final Point point = (Point) feature.getAttribute(PROPERTY_NAME_PIXELPOS);
                point.getCoordinate().setCoordinate(newCoordinate);
                point.geometryChanged();
            }
            if (updateGeometry) {
                updateGeometry(pixelPos);
            }
            fireProductNodeChanged(PROPERTY_NAME_PIXELPOS);
        }
    }

    private Coordinate getGeoCoordinate() {
        final Point point = (Point) feature.getAttribute(PROPERTY_NAME_GEOPOS);
        if (point != null) {
            return point.getCoordinate();
        }
        return null;
    }

    private void setGeoCoordinate(GeoPos geoPos) {
        final Coordinate newCoordinate = toCoordinate(geoPos);
        final Coordinate oldCoordinate = getGeoCoordinate();
        if (!ObjectUtils.equalObjects(oldCoordinate, newCoordinate)) {
            if (oldCoordinate == null) {
                final GeometryFactory geometryFactory = new GeometryFactory();
                feature.setAttribute(PROPERTY_NAME_GEOPOS, geometryFactory.createPoint(newCoordinate));
            } else {
                final Point point = (Point) feature.getAttribute(PROPERTY_NAME_GEOPOS);
                point.getCoordinate().setCoordinate(newCoordinate);
                point.geometryChanged();
            }
            fireProductNodeChanged(PROPERTY_NAME_GEOPOS);
        }
    }

    private static SimpleFeature createFeature(String name, String label, PixelPos pixelPos, GeoPos geoPos,
                                               PlacemarkDescriptor descriptor, GeoCoding geoCoding) {
        if (pixelPos == null && geoPos == null) {
            throw new IllegalArgumentException("pixelPos == null && geoPos == null");
        }
        final GeometryFactory geometryFactory = new GeometryFactory();
        final AffineTransform i2m = ImageManager.getImageToModelTransform(geoCoding);
        PixelPos imagePos = pixelPos;
        if (imagePos == null) {
            descriptor.updatePixelPos(geoCoding, geoPos, imagePos);
        }
        if (imagePos == null) {
            imagePos = new PixelPos();
            imagePos.setInvalid();
        }


        final Point geometry = geometryFactory.createPoint(toCoordinate(i2m.transform(imagePos, null)));
        final SimpleFeatureType featureType = getFeatureType();
        final SimpleFeature feature = PlainFeatureFactory.createPlainFeature(featureType, name, geometry, null);

        feature.setAttribute(Placemark.PROPERTY_NAME_PIXELPOS, geometryFactory.createPoint(toCoordinate(imagePos)));

        if (geoPos != null) {
            descriptor.updateGeoPos(geoCoding, imagePos, geoPos);
        } else if (geoCoding != null && geoCoding.canGetGeoPos()) {
            geoPos = geoCoding.getGeoPos(imagePos, geoPos);
        }

        if (geoPos != null) {
            feature.setAttribute(Placemark.PROPERTY_NAME_GEOPOS, geometryFactory.createPoint(toCoordinate(geoPos)));
        }
        if (label == null) {
            feature.setAttribute(Placemark.PROPERTY_NAME_LABEL, "");
        } else {
            feature.setAttribute(Placemark.PROPERTY_NAME_LABEL, label);
        }
        feature.setAttribute("symbol", descriptor.createDefaultSymbol());

        return feature;
    }

    private static Coordinate toCoordinate(GeoPos geoPos) {
        if (geoPos != null) {
            return new Coordinate(geoPos.getLon(), geoPos.getLat());
        }
        return null;
    }

    private static Coordinate toCoordinate(Point2D pixelPos) {
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
    // todo - move this method into a new DimapPersistable

    private static void writeColor(final String tagName, final int indent, final Color color, final XmlWriter writer) {
        final String[] colorTags = XmlWriter.createTags(indent, tagName);
        writer.println(colorTags[0]);
        DimapProductHelpers.printColorTag(indent + 1, color, writer);
        writer.println(colorTags[1]);
    }


    private static SimpleFeatureType createFeatureType(String name) {
        final SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();

        final SimpleFeatureType superType = PlainFeatureFactory.createPlainFeatureType(name, Point.class, null);
        final List<AttributeDescriptor> list = superType.getAttributeDescriptors();
        for (AttributeDescriptor descriptor : list) {
            builder.add(descriptor);
        }
        builder.setName(name);
        builder.add(Placemark.PROPERTY_NAME_LABEL, String.class);
        builder.add(Placemark.PROPERTY_NAME_PIXELPOS, Point.class);
        builder.add(Placemark.PROPERTY_NAME_GEOPOS, Point.class);
        builder.add("symbol", PlacemarkSymbol.class);

        return builder.buildFeatureType();
    }

    private static class Holder {

        private static final SimpleFeatureType PLACEMARK_FEATURE_TYPE = createFeatureType(PLACEMARK_FEATURE_TYPE_NAME);

        private Holder() {}
    }


    ///////////////////////////////////////////////
    // deprecated stuff

    /**
     * Creates a new placemark.
     *
     * @param name        the placemark's name.
     * @param label       the placemark's label.
     * @param description the placemark's description
     * @param pixelPos    the placemark's pixel position
     * @param geoPos      the placemark's geo-position.
     * @param symbol      the placemark's symbol.
     *
     * @deprecated since 4.7, use {@link Placemark#Placemark(String, String, String, PixelPos, GeoPos, PlacemarkDescriptor, GeoCoding)}
     */
    @Deprecated
    public Placemark(String name, String label, String description, PixelPos pixelPos, GeoPos geoPos, PlacemarkSymbol symbol) {
        this(name, label, description, pixelPos, geoPos, PinDescriptor.INSTANCE, null);
    }


    /**
     * Creates a new placemark.
     *
     * @param name        the placemark's name.
     * @param label       the placemark's label.
     * @param description the placemark's description
     * @param pixelPos    the placemark's pixel position
     * @param geoPos      the placemark's geo-position.
     * @param symbol      the placemark's symbol.
     * @param geoCoding   the placemark's geo-coding.
     *
     * @deprecated since BEAM 4.7.1, use {@link #Placemark(String, String, String, PixelPos, GeoPos, PlacemarkDescriptor, GeoCoding)} instead.
     *             This Method creates by default a placemark with the behavior of a pin.
     */
    @Deprecated
    public Placemark(String name, String label, String description, PixelPos pixelPos, GeoPos geoPos,
                     PlacemarkSymbol symbol, GeoCoding geoCoding) {
        this(name, label, description, pixelPos, geoPos, PinDescriptor.INSTANCE, geoCoding);
    }

    /**
     * Updates pixel and geo position according to the current geometry (model coordinates).
     *
     * @deprecated since BEAM 4.7.1, use {@link #updatePositions()} instead
     */
    @Deprecated
    public void updatePixelPos() {
        updatePositions();
    }

    /**
     * Creates a new GCP from an XML element.
     *
     * @param element the element.
     *
     * @return the placemark created.
     *
     * @throws NullPointerException     if element is null
     * @throws IllegalArgumentException if element is invalid
     * @deprecated since BEAM 4.7, use {@link #createPlacemark(Element, PlacemarkDescriptor, GeoCoding)} instead
     */
    @Deprecated
    public static Placemark createGcp(Element element) {
        return createPlacemark(element, GcpDescriptor.INSTANCE, null);
    }

    /**
     * Creates a new Pin from an XML element.
     *
     * @param element the element.
     *
     * @return the placemark created.
     *
     * @throws NullPointerException     if element is null
     * @throws IllegalArgumentException if element is invalid
     * @deprecated since BEAM 4.7, use {@link #createPlacemark(org.jdom.Element, PlacemarkDescriptor, GeoCoding)} instead
     */
    @Deprecated
    public static Placemark createPin(Element element) {
        return createPlacemark(element, PinDescriptor.INSTANCE, null);
    }

    /**
     * Creates a new placemark from an XML element and a given symbol.
     *
     * @param element the element.
     * @param symbol  the symbol.
     *
     * @return the placemark created.
     *
     * @throws NullPointerException     if element is null
     * @throws IllegalArgumentException if element is invalid
     * @deprecated since BEAM 4.7, use {@link #createPlacemark(Element, PlacemarkDescriptor, GeoCoding)} instead
     */
    @Deprecated
    public static Placemark createPlacemark(Element element, PlacemarkSymbol symbol) {
        final Placemark placemark = createPlacemark(element, PinDescriptor.INSTANCE, null);
        placemark.setSymbol(symbol);
        return placemark;
    }

    /**
     * Creates a new placemark from an XML element and a given symbol.
     *
     * @param element   the element.
     * @param symbol    the symbol.
     * @param geoCoding the geoCoding to used by the placemark. Can be <code>null</code>.
     *
     * @return the placemark created.
     *
     * @throws NullPointerException     if element is null
     * @throws IllegalArgumentException if element is invalid
     * @deprecated since BEAM 4.7.1, use {@link #createPlacemark(org.jdom.Element, org.esa.beam.framework.datamodel.PlacemarkDescriptor, GeoCoding)}
     *             This method generates by default a Placemark with the behavior of a pin.
     */
    @Deprecated
    public static Placemark createPlacemark(Element element, PlacemarkSymbol symbol, GeoCoding geoCoding) {
        final Placemark placemark = createPlacemark(element, PinDescriptor.INSTANCE, geoCoding);
        placemark.setSymbol(symbol);
        return placemark;
    }


}
