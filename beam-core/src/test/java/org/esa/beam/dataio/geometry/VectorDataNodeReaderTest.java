package org.esa.beam.dataio.geometry;

import com.bc.ceres.binding.Converter;
import com.bc.ceres.binding.ConversionException;
import com.vividsolutions.jts.geom.Geometry;
import junit.framework.TestCase;
import org.esa.beam.util.io.CsvReader;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;

import java.io.IOException;
import java.io.StringReader;
import java.util.List;

public class VectorDataNodeReaderTest extends TestCase {
    public void testFeatureTypeDef() throws IOException {

        StringReader reader = new StringReader(
                "Test_FT\n" +
                        "name:String\tgeom:Geometry\tpixelX:Integer\tdescription\n");

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

        assertEquals("pixelX", ad2.getType().getName().getLocalPart());
        assertEquals(Integer.class, ad2.getType().getBinding());

        assertEquals("description", ad3.getType().getName().getLocalPart());
        assertEquals(String.class, ad3.getType().getBinding());

        //Converter[] converters = new Converter[line2.length];
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
                attributeName = colonPos == 0 ? ("attribute_"+i) : token.substring(0, colonPos);
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
