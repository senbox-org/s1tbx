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
package org.esa.beam.framework.datamodel;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import org.esa.beam.framework.dataio.ProductSubsetDef;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.ObjectUtils;
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
 * This class represents a placemark.
 * <p/>
 * Placemarks are displayed as symbols at the image's pixel position corresponding to their geographical position. The name is
 * displayed as label next to the symbol. If the user moves the mouse over a placemark, the textual description property shall
 * appear as tool-tip text. Single placemarks can be selected either by mouse-click or by the ? Prev./Next Placemark tool.
 * Placemarks are contained in the active product and stored in DIMAP format. To share placemarks between products,
 * the placemarks of a product can be imported and exported.
 *
 * @author Norman Fomferra
 * @version 2.0
 * @since BEAM 2.0 (full revision since BEAM 4.10)
 */
public class Placemark extends ProductNode {
    @Deprecated
    public static final String PLACEMARK_FEATURE_TYPE_NAME = "Placemark";

    // feature properties
    public static final String PROPERTY_NAME_LABEL = "label";
    public static final String PROPERTY_NAME_TEXT = "text";
    public static final String PROPERTY_NAME_PIXELPOS = "pixelPos";
    public static final String PROPERTY_NAME_GEOPOS = "geoPos";
    public static final String PROPERTY_NAME_DATETIME = "dateTime";
    private static final String PROPERTY_NAME_SYMBOL = "symbol";

    public static final String PROPERTY_NAME_PINSYMBOL = "pinSymbol";

    private final PlacemarkDescriptor descriptor;
    private final SimpleFeature feature;

    /**
     * Creates a point placemark.
     *
     * @param descriptor The placemark descriptor that created this placemark.
     * @param name       The placemark's name.
     * @param label      The placemark's label. May be {@code null}.
     * @param text       The placemark's (XHTML) text. May be {@code null}.
     * @param pixelPos   The placemark's pixel position. May be {@code null}, if {@code geoPos} is given.
     * @param geoPos     The placemark's pixel position. May be {@code null}, if {@code pixelPos} is given.
     * @param geoCoding  The placemark's geo-coding. Used to compute {@code pixelPos} from {@code geoPos}, if {@code pixelPos} is {@code null}.
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
        super(feature.getID(), getText(feature));
        this.descriptor = descriptor;
        this.feature = feature;
        setSymbol(this.descriptor.createDefaultSymbol()); // todo - remove
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
     * @return This placemark's label, cannot be {@code null}.
     */
    public String getLabel() {
        return (String) feature.getAttribute(PROPERTY_NAME_LABEL);
    }

    /**
     * Sets this placemark's (XHTML) text.
     *
     * @param text The text, if {@code null} an empty text is set.
     */
    public void setText(String text) {
        if (text == null) {
            text = "";
        }
        if (!text.equals(feature.getAttribute(PROPERTY_NAME_TEXT))) {
            feature.setAttribute(PROPERTY_NAME_TEXT, text);
            fireProductNodeChanged(PROPERTY_NAME_TEXT);
        }
    }

    /**
     * @return This placemark's (XHTML) text, cannot be {@code null}.
     */
    public String getText() {
        return getText(feature);
    }

    /**
     * Gets the (XHTML) text value of the given feature.
     *
     * @param feature The feature that provides the text.
     * @return the label, cannot be {@code null}.
     */
    public static String getText(SimpleFeature feature) {
        Object attribute = feature.getAttribute(PROPERTY_NAME_TEXT);
        if (attribute != null) {
            return attribute.toString();
        }
        return "";
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

    public PlacemarkSymbol getSymbol() {
        return (PlacemarkSymbol) feature.getAttribute(PROPERTY_NAME_SYMBOL);
    }

    public void setSymbol(final PlacemarkSymbol symbol) {
        Guardian.assertNotNull("symbol", symbol);
        if (getSymbol() != symbol) {
            feature.setAttribute(PROPERTY_NAME_SYMBOL, symbol);
            fireProductNodeChanged(PROPERTY_NAME_PINSYMBOL);
        }
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
        final Point point = (Point) feature.getDefaultGeometry();
        if (getProduct() != null) {
            final GeoCoding geoCoding = getProduct().getGeoCoding();
            final AffineTransform i2m = ImageManager.getImageToModelTransform(geoCoding);
            PixelPos pixelPos = new PixelPos((float) point.getX(), (float) point.getY());
            try {
                i2m.inverseTransform(pixelPos, pixelPos);
            } catch (NoninvertibleTransformException ignored) {
                // ignore
            }
            setPixelPosAttribute(pixelPos, true, false);
        }
    }

    private Coordinate getPixelPosAttribute() {
        final Point point = (Point) feature.getAttribute(PROPERTY_NAME_PIXELPOS);
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
                feature.setAttribute(PROPERTY_NAME_PIXELPOS, geometryFactory.createPoint(newCoordinate));
            } else {
                final Point point = (Point) feature.getAttribute(PROPERTY_NAME_PIXELPOS);
                point.getCoordinate().setCoordinate(newCoordinate);
                point.geometryChanged();
            }

            // Make sure, object is in a consistent state
            if (updateDefaultGeometry) {
                updateDefaultGeometryAttribute(pixelPos);
            }

            if (updateGeoPos && getProduct() != null) {
                final GeoPos geoPos = getGeoPos();
                descriptor.updateGeoPos(getProduct().getGeoCoding(), pixelPos, geoPos);
                setGeoPosAttribute(geoPos, false);
            }

            fireProductNodeChanged(PROPERTY_NAME_PIXELPOS);
        }
    }

    private Coordinate getGeoPosAttribute() {
        final Point point = (Point) feature.getAttribute(PROPERTY_NAME_GEOPOS);
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
                feature.setAttribute(PROPERTY_NAME_GEOPOS, geometryFactory.createPoint(newCoordinate));
            } else {
                final Point point = (Point) feature.getAttribute(PROPERTY_NAME_GEOPOS);
                point.getCoordinate().setCoordinate(newCoordinate);
                point.geometryChanged();
            }

            if (updatePixelPos && getProduct() != null) {
                final PixelPos pixelPos = getPixelPos();
                descriptor.updatePixelPos(getProduct().getGeoCoding(), geoPos, pixelPos);
                setPixelPosAttribute(pixelPos, false, true);
            }

            fireProductNodeChanged(PROPERTY_NAME_GEOPOS);
        }
    }

    private void updateDefaultGeometryAttribute(PixelPos pixelPos) {
        final Product product = getProduct();
        final Point2D.Float geometryPoint = new Point2D.Float(pixelPos.x, pixelPos.y);
        if (product != null) {
            final AffineTransform i2m = ImageManager.getImageToModelTransform(product.getGeoCoding());
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
        final AffineTransform i2m = ImageManager.getImageToModelTransform(geoCoding);
        PixelPos imagePos = pixelPos;

        // todo - remove instanceof - bad code smell  (nf while revisioning Placemark API)
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

        final Point geometry = geometryFactory.createPoint(toCoordinate(i2m.transform(imagePos, null)));
        final SimpleFeature feature = PlainFeatureFactory.createPlainFeature(descriptor.getBaseFeatureType(),
                                                                             name,
                                                                             geometry,
                                                                             null);

        feature.setAttribute(Placemark.PROPERTY_NAME_PIXELPOS, geometryFactory.createPoint(toCoordinate(imagePos)));

        if (geoPos == null) {
            if (geoCoding != null && geoCoding.canGetGeoPos()) {
                geoPos = geoCoding.getGeoPos(imagePos, geoPos);
            }
        } else {
            descriptor.updateGeoPos(geoCoding, imagePos, geoPos);
        }

        if (geoPos != null) {
            feature.setAttribute(Placemark.PROPERTY_NAME_GEOPOS, geometryFactory.createPoint(toCoordinate(geoPos)));
        }
        if (label == null) {
            feature.setAttribute(Placemark.PROPERTY_NAME_LABEL, "");
        } else {
            feature.setAttribute(Placemark.PROPERTY_NAME_LABEL, label);
        }
        if (text == null) {
            feature.setAttribute(Placemark.PROPERTY_NAME_TEXT, "");
        } else {
            feature.setAttribute(Placemark.PROPERTY_NAME_TEXT, text);
        }
        feature.setAttribute(PROPERTY_NAME_SYMBOL, descriptor.createDefaultSymbol());

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

    public static SimpleFeatureType createPinFeatureType() {
        return createPointFeatureType("org.esa.beam.Pin");
    }

    public static SimpleFeatureType createGcpFeatureType() {
        return createPointFeatureType("org.esa.beam.GroundControlPoint");
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
        builder.add(PROPERTY_NAME_SYMBOL, PlacemarkSymbol.class);
        builder.add(PROPERTY_NAME_DATETIME, Date.class);

        return builder.buildFeatureType();
    }
}
