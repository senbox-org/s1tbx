package org.esa.beam.visat.actions.session;

import com.bc.ceres.binding.Converter;
import com.bc.ceres.binding.ConversionException;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.WKTReader;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTWriter;

import java.awt.Shape;
import java.awt.Point;
import java.awt.geom.Point2D;

import org.geotools.geometry.text.WKTParser;

/**
 * A converter for {@link java.awt.Shape}s.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @since BEAM 4.6
 */
public class PointConverter implements Converter {
    private final GeometryFactory geometryFactory;

    public PointConverter() {
        geometryFactory = new GeometryFactory();
    }

    @Override
    public Class getValueType() {
        return Point2D.class;
    }

    @Override
    public Object parse(String text) throws ConversionException {
        try {
            Geometry geometry = new WKTReader(geometryFactory).read(text);
            if (geometry instanceof com.vividsolutions.jts.geom.Point) {
                com.vividsolutions.jts.geom.Point point = (com.vividsolutions.jts.geom.Point) geometry;
                return new Point2D.Double(point.getX(), point.getY());
            } else {
                throw new ConversionException("Failed to parse point geometry WKT.");
            }
        } catch (ParseException e) {
            throw new ConversionException("Failed to parse point geometry WKT.", e);
        }
    }

    @Override
    public String format(Object value) {
        Point2D point = (Point2D) value;
        return new WKTWriter().write(geometryFactory.createPoint(new Coordinate(point.getX(), point.getY())));
    }
}