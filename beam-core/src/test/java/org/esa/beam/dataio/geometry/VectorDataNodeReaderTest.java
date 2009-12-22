package org.esa.beam.dataio.geometry;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.Converter;
import com.bc.ceres.binding.ConverterRegistry;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Coordinate;
import junit.framework.TestCase;
import org.esa.beam.util.io.CsvReader;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.GeometryDescriptor;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;

public class VectorDataNodeReaderTest extends TestCase {
    public void testReader() throws IOException {

        StringReader reader = new StringReader(
                "Test_FT\tname:String\tgeom:Geometry\tpixel:Integer\tdescription\n"
                        + "ID65\tmark1\tPOINT(12.3 45.6)\t0\tThis is mark1.\n"
                        + "ID66\tmark2\tPOINT(78.9  0.1)\t1\tThis is mark2.\n"
                        + "ID67\tmark3\tPOINT(2.3 3.4)\t2\tThis is mark3.\n"
        );

        JtsGeometryConverter.registerConverter();

        CsvReader csvReader = new CsvReader(reader, new char[]{'\t'});

        SimpleFeatureType simpleFeatureType = readFeatureType(csvReader);
        assertNotNull(simpleFeatureType);
        assertEquals("Test_FT", simpleFeatureType.getTypeName());
        assertEquals(4, simpleFeatureType.getAttributeCount());

        List<AttributeDescriptor> list = simpleFeatureType.getAttributeDescriptors();
        AttributeDescriptor ad0 = list.get(0);
        AttributeDescriptor ad1 = list.get(1);
        AttributeDescriptor ad2 = list.get(2);
        AttributeDescriptor ad3 = list.get(3);

        assertEquals("name", ad0.getType().getName().getLocalPart());
        assertEquals(String.class, ad0.getType().getBinding());

        assertEquals("geom", ad1.getType().getName().getLocalPart());
        assertEquals(Geometry.class, ad1.getType().getBinding());

        assertEquals("pixel", ad2.getType().getName().getLocalPart());
        assertEquals(Integer.class, ad2.getType().getBinding());

        assertEquals("description", ad3.getType().getName().getLocalPart());
        assertEquals(String.class, ad3.getType().getBinding());

        GeometryDescriptor geometryDescriptor = simpleFeatureType.getGeometryDescriptor();
        assertEquals("geom", geometryDescriptor.getType().getName().getLocalPart());

        FeatureCollection<SimpleFeatureType, SimpleFeature> fc = readerFeatures(csvReader, simpleFeatureType);

        GeometryFactory gf = new GeometryFactory();

        assertEquals(3, fc.size());
        FeatureIterator<SimpleFeature> featureIterator = fc.features();

        SimpleFeature f1 = featureIterator.next();
        assertEquals("ID65", f1.getID());
        assertEquals("mark1", f1.getAttribute(0));
        //assertEquals(gf.createPoint(new Coordinate(12.3, 45.6)), f1.getAttribute(1));
        assertEquals(0, f1.getAttribute(2));
        assertEquals("This is mark1.", f1.getAttribute(3));

        SimpleFeature f2 = featureIterator.next();
        assertEquals("ID66", f2.getID());
        assertEquals("mark2", f2.getAttribute(0));
        //assertEquals(gf.createPoint(new Coordinate(78.9,  0.1)), f2.getAttribute(1));
        assertEquals(1, f2.getAttribute(2));
        assertEquals("This is mark2.", f2.getAttribute(3));
    }

    private FeatureCollection<SimpleFeatureType, SimpleFeature> readerFeatures(CsvReader csvReader, SimpleFeatureType simpleFeatureType) throws IOException {
        Converter<?>[] converters = new Converter<?>[simpleFeatureType.getAttributeCount()];
        for (int i = 0; i < converters.length; i++) {
            Class<?> attributeType = simpleFeatureType.getType(i).getBinding();
            Converter<?> converter = ConverterRegistry.getInstance().getConverter(attributeType);
            if (converter == null) {
                throw new IOException(String.format("No converter for type %s found.", attributeType));
            }
            converters[i] = converter;
        }

        DefaultFeatureCollection fc = new DefaultFeatureCollection("?", simpleFeatureType);
        SimpleFeatureBuilder sfb = new SimpleFeatureBuilder(simpleFeatureType);
        while (true) {
            String[] tokens = csvReader.readRecord();
            if (tokens == null) {
                break;
            }
            if (tokens.length != 1 + simpleFeatureType.getAttributeCount()) {
                throw new IOException("Shit.");  // todo - msg
            }
            sfb.reset();
            String fid = null;
            for (int i = 0; i < tokens.length; i++) {
                String token = tokens[i];
                if (i == 0) {
                    fid = token;
                } else {
                try {
                    Object value = converters[i-1].parse(token);
                    sfb.set(simpleFeatureType.getDescriptor(i-1).getLocalName(), value);
                } catch (ConversionException e) {
                    throw new IOException("Shit.", e);  // todo - msg
                }
                }
            }
            SimpleFeature simpleFeature = sfb.buildFeature(fid);
            fc.add(simpleFeature);
        }
        return fc;
    }

    private SimpleFeatureType readFeatureType(CsvReader csvReader) throws IOException {
        String[] tokens = csvReader.readRecord();
        if (tokens == null || tokens.length <= 1) {
            throw new IOException("Missing feature type definition in first line.");
        }
        return createFeatureType(tokens);
    }

    private SimpleFeatureType createFeatureType(String[] tokens) throws IOException {
        SimpleFeatureTypeBuilder sftb = new SimpleFeatureTypeBuilder();
        JavaTypeConverter jtc = new JavaTypeConverter();
        for (int i = 0; i < tokens.length; i++) {
            if (i == 0) {
                sftb.setName(tokens[0]);
            } else {
                String token = tokens[i];
                final int colonPos = token.indexOf(':');
                String attributeName = token;
                Class<?> attributeType = String.class;
                if (colonPos >= 0) {
                    attributeName = colonPos == 0 ? ("attribute_" + i) : token.substring(0, colonPos);
                    String attributeTypeName = token.substring(colonPos + 1).trim();
                    try {
                        attributeType = jtc.parse(attributeTypeName);
                    } catch (ConversionException e) {
                        throw new IOException(
                                String.format("Type for attribute '%s' unknown: %s", attributeName, attributeTypeName), e);
                    }
                }
                sftb.add(attributeName, attributeType);
            }
        }
        return sftb.buildFeatureType();
    }


}
