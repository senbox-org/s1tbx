package org.esa.beam.dataio.geometry;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.Converter;
import com.bc.ceres.binding.ConverterRegistry;
import com.vividsolutions.jts.geom.Geometry;
import junit.framework.TestCase;
import org.esa.beam.util.io.CsvReader;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureCollection;
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
                "Test_FT\n"
                        + "name:String\tgeom:Geometry\tpixel:Integer\tdescription\n"
                        + "mark1\tPOINT(12.3 45.6)\t0\tThis is mark1.\n"
                        + "mark2\tPOINT(78.9  0.1)\t1\tThis is mark2.\n"
                        + "mark3\tPOINT(2.3 3.4)\t2\tThis is mark3.\n"
        );

        JtsGeometryConverter.registerConverter();

        CsvReader csvReader = new CsvReader(reader, new char[]{'\t'});

        String typeName = readTypeName(csvReader);
        assertEquals("Test_FT", typeName);

        SimpleFeatureType simpleFeatureType = readFeatureType(csvReader, typeName);
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

        assertEquals(0, fc.size());
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
        long id = System.nanoTime();
        while (true) {
            String[] tokens = csvReader.readRecord();
            if (tokens == null) {
                break;
            }
            if (tokens.length != simpleFeatureType.getAttributeCount()) {
                throw new IOException("Shit.");  // todo - msg
            }
            for (int i = 0; i < tokens.length; i++) {
                String token = tokens[i];
                try {
                    Object value = converters[i].parse(token);
                    sfb.set(simpleFeatureType.getDescriptor(i).getLocalName(), value);
                } catch (ConversionException e) {
                    throw new IOException("Shit.", e);  // todo - msg
                }
            }
            fc.add(sfb.buildFeature("FID_" + Long.toHexString(id++)));
        }
        return fc;
    }

    private String readTypeName(CsvReader csvReader) throws IOException {
        String[] tokens = csvReader.readRecord();
        if (tokens == null || tokens.length != 1) {
            throw new IOException("Missing feature type name in first line.");
        }
        return tokens[0];
    }

    private SimpleFeatureType readFeatureType(CsvReader csvReader, String typeName) throws IOException {
        String[] tokens = csvReader.readRecord();
        if (tokens == null || tokens.length == 0) {
            throw new IOException("Missing feature type definition in second line.");
        }
        return createFeatureType(typeName, tokens);
    }

    private SimpleFeatureType createFeatureType(String typeName, String[] tokens) throws IOException {
        SimpleFeatureTypeBuilder sftb = new SimpleFeatureTypeBuilder();
        sftb.setName(typeName);
        JavaTypeConverter jtc = new JavaTypeConverter();
        for (int i = 0; i < tokens.length; i++) {
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
        return sftb.buildFeatureType();
    }
}
