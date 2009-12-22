package org.esa.beam.dataio;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.Converter;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.io.ParseException;
import com.vividsolutions.jts.io.WKTReader;
import com.vividsolutions.jts.io.WKTWriter;

public class JtsGeometryConverter implements Converter<Geometry> {

    @Override
    public Class<? extends Geometry> getValueType() {
        return Geometry.class;
    }

    @Override
    public Geometry parse(String text) throws ConversionException {
        try {
            return new WKTReader().read(text);
        } catch (ParseException e) {
            throw new ConversionException("Could not parse geometry.", e);
        }
    }

    @Override
    public String format(Geometry value) {
        return new WKTWriter().write(value);
    }
}
