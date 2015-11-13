/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.snap.core.datamodel;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import org.esa.snap.core.dataio.ProductSubsetDef;
import org.esa.snap.core.util.Debug;
import org.esa.snap.core.util.ObjectUtils;
import org.esa.snap.runtime.Config;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;

import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;
import java.util.Date;
import java.util.List;

/**
 * Placemarks are displayed as symbols at the image's pixel position corresponding to their geographical position. The name is
 * displayed as label next to the symbol. If the user moves the mouse over a placemark, the textual description property shall
 * appear as tool-tip text. Single placemarks can be selected either by mouse-click or by the ? Prev./Next Placemark tool.
 * Placemarks are contained in the active product and stored in CSV format. To share placemarks between products,
 * the placemarks of a product can be imported and exported.
 *
 * @author Norman Fomferra
 * @version 2.0
 * @since BEAM 2.0 (full revision since BEAM 4.10)
 */
public class Placemark extends ProductNode {

    public static final String PREFERENCE_KEY_ADJUST_PIN_GEO_POS = "snap.adjustPinGeoPos";

    // feature properties
    public static final String PROPERTY_NAME_LABEL = "label";
    public static final String PROPERTY_NAME_TEXT = "text";
    public static final String PROPERTY_NAME_PIXELPOS = "pixelPos";
    public static final String PROPERTY_NAME_GEOPOS = "geoPos";
    public static final String PROPERTY_NAME_DATETIME = "dateTime";
    public static final String PROPERTY_NAME_STYLE_CSS = PlainFeatureFactory.ATTRIB_NAME_STYLE_CSS;

    private final PlacemarkDescriptor descriptor;
    private final SimpleFeature feature;

    /**
     * Creates a point placemark.
     *
     * @param descriptor The placemark descriptor that created this placemark.
     * @param name       The placemark's name.
     * @param label      The placemark's label. May be {@code null}.
     * @param text       The placemark's (XHTML) text. May be {@code null}.
     * @param pixelPos   The placemark's pixel position in scene image coordinates. May be {@code null}, if {@code geoPos} is given.
     * @param geoPos     The placemark's pixel position. May be {@code null}, if {@code pixelPos} is given.
     * @param geoCoding  The product's scene geo-coding. Used to compute {@code pixelPos} from {@code geoPos},
     *                   if {@code pixelPos} is {@code null}.
     * @return A new point placemark.
     */
    public static Placemark createPointPlacemark(PlacemarkDescriptor descriptor,
                                                 String name,
                                                 String label,
                                                 String text,
                                                 PixelPos pixelPos,
                                                 GeoPos geoPos,
                                                 GeoCoding geoCoding) {
        SimpleFeature pointFeature = createPointFeature(descriptor,
                                                        name, label, text,
                                                        pixelPos, geoPos, geoCoding);
        return new Placemark(descriptor, pointFeature);
    }

    /**
     * Constructor.
     *
     * @param descriptor The placemark descriptor that created this placemark.
     * @param feature    The wrapped feature.
     */
    public Placemark(PlacemarkDescriptor descriptor, SimpleFeature feature) {
        super(feature.getID(), getStringAttribute(feature, PROPERTY_NAME_TEXT));
        this.descriptor = descriptor;
        this.feature = feature;
        Debug.trace(
                "Placemark created: descriptor=" + descriptor.getClass() + ", featureType=" + feature.getFeatureType().getTypeName() + ", feature=" + feature);
    }

    /**
     * @return The placemark descriptor that created this placemark.
     * @since BEAM 4.10
     */
    public PlacemarkDescriptor getDescriptor() {
        return descriptor;
    }

    /**
     * @return The wrapped {@link SimpleFeature} underlying this placemark.
     * @since BEAM 4.7
     */
    public final SimpleFeature getFeature() {
        return feature;
    }

    /**
     * Gets the attribute value of the underlying feature.
     *
     * @param attributeName The feature's attribute name.
     * @return The feature's attribute value, may be {@code null}.
     */
    public Object getAttributeValue(String attributeName) {
        return feature.getAttribute(attributeName);
    }

    /**
     * Sets the attribute value of the underlying feature.
     *
     * @param attributeName  The feature's attribute name.
     * @param attributeValue The feature's attribute value, may be {@code null}.
     */
    public void setAttributeValue(String attributeName, Object attributeValue) {
        final int index = feature.getFeatureType().indexOf(attributeName);
        if (index != -1 && !ObjectUtils.equalObjects(attributeValue, getAttributeValue(attributeName))) {
            feature.setAttribute(index, attributeValue);
            fireProductNodeChanged(attributeName);
        }
    }

    /**
     * Sets this placemark's label.
     *
     * @param label the label, if {@code null} an empty label is set.
     */
    public void setLabel(String label) {
        setAttributeValue(PROPERTY_NAME_LABEL, label != null ? label : "");
    }

    /**
     * @return This placemark's label, cannot be {@code null}.
     */
    public String getLabel() {
        return (String) getAttributeValue(PROPERTY_NAME_LABEL);
    }

    /**
     * Sets this placemark's (XHTML) text.
     *
     * @param text The text, if {@code null} an empty text is set.
     */
    public void setText(String text) {
        setAttributeValue(PROPERTY_NAME_TEXT, text != null ? text : "");
    }

    /**
     * @return This placemark's (XHTML) text, cannot be {@code null}.
     */
    public String getText() {
        return getStringAttribute(feature, PROPERTY_NAME_TEXT);
    }

    /**
     * Sets this placemark's CSS style.
     *
     * @param styleCss The text, if {@code null} an empty text is set.
     * @since BEAM 4.10
     */
    public void setStyleCss(String styleCss) {
        setAttributeValue(PROPERTY_NAME_STYLE_CSS, styleCss != null ? styleCss : "");
    }

    /**
     * @return This placemark's CSS style, cannot be {@code null}.
     * @since BEAM 4.10
     */
    public String getStyleCss() {
        return getStringAttribute(feature, PROPERTY_NAME_STYLE_CSS);
    }

    /**
     * Returns an estimated, raw storage size in bytes of this placemark.
     *
     * @param subsetDef if not {@code null} the subset may limit the size returned.
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

    public PixelPos getPixelPos() {
        return toPixelPos(getPixelPosAttribute());
    }

    public void setPixelPos(PixelPos pixelPos) {
        setPixelPosAttribute(pixelPos, true, true);
    }

    public GeoPos getGeoPos() {
        return toGeoPos(getGeoPosAttribute());
    }

    public void setGeoPos(GeoPos geoPos) {
        setGeoPosAttribute(geoPos, true);
    }

    /**
     * Updates pixel and geo position according to the current geometry (model coordinates).
     */
    public void updatePositions() {
        Object defaultGeometry = feature.getDefaultGeometry();
        if (defaultGeometry instanceof Point) {
            final Point point = (Point) defaultGeometry;
            if (getProduct() != null) {
                GeoCoding geoCoding = getProduct().getSceneGeoCoding();
                AffineTransform i2m = Product.findImageToModelTransform(geoCoding);
                PixelPos pixelPos = new PixelPos(point.getX(), point.getY());
                try {
                    i2m.inverseTransform(pixelPos, pixelPos);
                } catch (NoninvertibleTransformException ignored) {
                    // ignore
                }
                setPixelPosAttribute(pixelPos, true, false);
            }
        }
    }

    private Coordinate getPixelPosAttribute() {
        final Point point = (Point) getAttributeValue(PROPERTY_NAME_PIXELPOS);
        if (point != null) {
            return point.getCoordinate();
        }
        return null;
    }

    private void setPixelPosAttribute(PixelPos pixelPos, boolean updateGeoPos, boolean updateDefaultGeometry) {
        final Coordinate newCoordinate = toCoordinate(pixelPos);
        final Coordinate oldCoordinate = getPixelPosAttribute();
        if (!ObjectUtils.equalObjects(oldCoordinate, newCoordinate)) {
            if (oldCoordinate == null) {
                final GeometryFactory geometryFactory = new GeometryFactory();
                setAttributeValue(PROPERTY_NAME_PIXELPOS, geometryFactory.createPoint(newCoordinate));
            } else {
                final Point point = (Point) getAttributeValue(PROPERTY_NAME_PIXELPOS);
                point.getCoordinate().setCoordinate(newCoordinate);
                point.geometryChanged();
            }

            // Make sure, object is in a consistent state
            if (updateDefaultGeometry) {
                updateDefaultGeometryAttribute(pixelPos);
            }

            if (updateGeoPos && getProduct() != null) {
                final GeoPos geoPos = getGeoPos();
                descriptor.updateGeoPos(getProduct().getSceneGeoCoding(), pixelPos, geoPos);
                setGeoPosAttribute(geoPos, false);
            }

            fireProductNodeChanged(PROPERTY_NAME_PIXELPOS);
        }
    }

    private Coordinate getGeoPosAttribute() {
        final Point point = (Point) getAttributeValue(PROPERTY_NAME_GEOPOS);
        if (point != null) {
            return point.getCoordinate();
        }
        return null;
    }

    private void setGeoPosAttribute(GeoPos geoPos, boolean updatePixelPos) {
        final Coordinate newCoordinate = toCoordinate(geoPos);
        final Coordinate oldCoordinate = getGeoPosAttribute();
        if (!ObjectUtils.equalObjects(oldCoordinate, newCoordinate)) {
            if (oldCoordinate == null) {
                final GeometryFactory geometryFactory = new GeometryFactory();
                setAttributeValue(PROPERTY_NAME_GEOPOS, geometryFactory.createPoint(newCoordinate));
            } else if (newCoordinate != null) {
                final Point point = (Point) getAttributeValue(PROPERTY_NAME_GEOPOS);
                point.getCoordinate().setCoordinate(newCoordinate);
                point.geometryChanged();
            }

            if (updatePixelPos && getProduct() != null) {
                final PixelPos pixelPos = getPixelPos();
                descriptor.updatePixelPos(getProduct().getSceneGeoCoding(), geoPos, pixelPos);
                setPixelPosAttribute(pixelPos, false, true);
            }

            fireProductNodeChanged(PROPERTY_NAME_GEOPOS);
        }
    }

    private void updateDefaultGeometryAttribute(PixelPos pixelPos) {
        final Product product = getProduct();
        final Point2D.Double geometryPoint = new Point2D.Double(pixelPos.x, pixelPos.y);
        if (product != null) {
            final AffineTransform i2m = Product.findImageToModelTransform(product.getSceneGeoCoding());
            i2m.transform(pixelPos, geometryPoint);
        }
        final Point point = (Point) feature.getDefaultGeometry();
        point.getCoordinate().setCoordinate(toCoordinate(geometryPoint));
        point.geometryChanged();
    }

    private static SimpleFeature createPointFeature(PlacemarkDescriptor descriptor,
                                                    String name,
                                                    String label,
                                                    String text,
                                                    PixelPos pixelPos,
                                                    GeoPos geoPos,
                                                    GeoCoding geoCoding) {
        if (pixelPos == null && geoPos == null) {
            throw new IllegalArgumentException("pixelPos == null && geoPos == null");
        }
        final GeometryFactory geometryFactory = new GeometryFactory();
        PixelPos imagePos = pixelPos;

        // todo - remove instanceof - bad code smell  (nf while revising Placemark API)
        if ((descriptor instanceof PinDescriptor || imagePos == null)
                && geoPos != null
                && geoCoding != null
                && geoCoding.canGetPixelPos()) {
            imagePos = geoCoding.getPixelPos(geoPos, imagePos);
        }

        if (imagePos == null) {
            imagePos = new PixelPos();
            imagePos.setInvalid();
        }

        Point2D scenePos = Product.findImageToModelTransform(geoCoding).transform(imagePos, new Point2D.Double());
        final Point geometry = geometryFactory.createPoint(toCoordinate(scenePos));
        final SimpleFeature feature = PlainFeatureFactory.createPlainFeature(descriptor.getBaseFeatureType(),
                                                                             name,
                                                                             geometry,
                                                                             null);

        feature.setAttribute(Placemark.PROPERTY_NAME_PIXELPOS, geometryFactory.createPoint(toCoordinate(imagePos)));

        if (geoPos == null) {
            if (geoCoding != null && geoCoding.canGetGeoPos()) {
                geoPos = geoCoding.getGeoPos(imagePos, geoPos);
            }
        } else if (Config.instance().preferences().getBoolean(PREFERENCE_KEY_ADJUST_PIN_GEO_POS, true)) {
            descriptor.updateGeoPos(geoCoding, imagePos, geoPos);
        }

        if (geoPos != null) {
            feature.setAttribute(Placemark.PROPERTY_NAME_GEOPOS, geometryFactory.createPoint(toCoordinate(geoPos)));
        }

        feature.setAttribute(Placemark.PROPERTY_NAME_LABEL, label != null ? label : "");
        feature.setAttribute(Placemark.PROPERTY_NAME_TEXT, text != null ? text : "");

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
            return new GeoPos(coordinate.y, coordinate.x);
        }
        return null;
    }

    private static PixelPos toPixelPos(Coordinate coordinate) {
        if (coordinate != null) {
            return new PixelPos(coordinate.x, coordinate.y);
        }
        return null;
    }

    public static SimpleFeatureType createPinFeatureType() {
        return createPointFeatureType("org.esa.snap.Pin");
    }

    public static SimpleFeatureType createGcpFeatureType() {
        return createPointFeatureType("org.esa.snap.GroundControlPoint");
    }

    public static SimpleFeatureType createGeometryFeatureType() {
        return PlainFeatureFactory.createDefaultFeatureType();
    }

    public static SimpleFeatureType createPointFeatureType(String name) {
        final SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();

        final SimpleFeatureType superType = PlainFeatureFactory.createPlainFeatureType(name, Point.class, null);
        final List<AttributeDescriptor> list = superType.getAttributeDescriptors();
        for (AttributeDescriptor descriptor : list) {
            builder.add(descriptor);
        }
        builder.setName(name);
        builder.add(PROPERTY_NAME_LABEL, String.class);
        builder.add(PROPERTY_NAME_TEXT, String.class);
        builder.add(PROPERTY_NAME_PIXELPOS, Point.class);
        builder.add(PROPERTY_NAME_GEOPOS, Point.class);
        builder.add(PROPERTY_NAME_DATETIME, Date.class);

        return builder.buildFeatureType();
    }

    private static String getStringAttribute(SimpleFeature feature, String attributeName) {
        Object attribute = feature.getAttribute(attributeName);
        if (attribute != null) {
            return attribute.toString();
        }
        return "";
    }

}
