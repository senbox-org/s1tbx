package org.esa.snap.csv.dataio.writer;

import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.NameImpl;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.feature.type.FeatureTypeImpl;
import org.junit.Test;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import java.io.IOException;

import static org.junit.Assert.*;

/**
 * @author Thomas Storm
 */
public class FeatureCsvWriterTest {

    @Test
    public void testWriteCsv() throws Exception {
        final StringBuilder s = new StringBuilder();
        final CsvWriter csvWriter = new FeatureCsvWriter(new WriteStrategy() {
            @Override
            public void writeCsv(String fullOutput) throws IOException {
                s.append(fullOutput);
            }
        }, new CsvWriterBuilder.BeamOutputFormat());

        SimpleFeatureTypeBuilder featureTypeBuilder = new SimpleFeatureTypeBuilder();
        featureTypeBuilder.setName(new NameImpl(""));
        featureTypeBuilder.add("attr_1", String.class);
        featureTypeBuilder.add("attr_2", Float.class);
        final SimpleFeatureType featureType = featureTypeBuilder.buildFeatureType();

        SimpleFeatureBuilder featureBuilder = new SimpleFeatureBuilder(featureType);
        featureBuilder.set("attr_1", "val_1");
        featureBuilder.set("attr_2", 10.0f);
        final SimpleFeature firstFeature = featureBuilder.buildFeature("0");

        featureBuilder.reset();
        featureBuilder.set("attr_1", "val_2");
        featureBuilder.set("attr_2", 20.0f);
        final SimpleFeature secondFeature = featureBuilder.buildFeature("1");

        final DefaultFeatureCollection featureCollection = new DefaultFeatureCollection("id", featureType);
        featureCollection.add(firstFeature);
        featureCollection.add(secondFeature);

        csvWriter.writeCsv(featureType, featureCollection);

        final String csv = s.toString();
        final StringBuilder expected = new StringBuilder();
        expected.append("featureId\tattr_1:string\tattr_2:float");
        expected.append("\n");
        expected.append("0\tval_1\t10.0");
        expected.append("\n");
        expected.append("1\tval_2\t20.0");
        expected.append("\n");
        assertEquals(expected.toString(), csv);
    }

    @Test
    public void testIsValidInput() throws Exception {
        final FeatureCsvWriter csvWriter = new FeatureCsvWriter(null, null);
        final SimpleFeatureTypeBuilder builder = new SimpleFeatureTypeBuilder();
        builder.setName(new NameImpl(""));
        final SimpleFeatureType simpleFeatureType = builder.buildFeatureType();
        assertTrue(csvWriter.isValidInput(simpleFeatureType, new DefaultFeatureCollection("id", simpleFeatureType)));

        final FeatureTypeImpl featureType = new FeatureTypeImpl(new NameImpl(""), null, null, false, null, null, null);
        assertFalse(csvWriter.isValidInput(featureType, new DefaultFeatureCollection("id", simpleFeatureType)));
        assertFalse(csvWriter.isValidInput());
    }

}
