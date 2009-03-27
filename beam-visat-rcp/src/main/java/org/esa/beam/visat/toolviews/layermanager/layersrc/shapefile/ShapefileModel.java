package org.esa.beam.visat.toolviews.layermanager.layersrc.shapefile;

import org.geotools.feature.FeatureCollection;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.styling.Style;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import java.io.File;

public class ShapefileModel {

    private File file;
    private FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection;
    private ReferencedEnvelope featureSourceEnvelope;
    private SimpleFeatureType schema;
    private Style[] styles;
    private Style selectedStyle;


    public void setFile(File file) {
        this.file = file;
    }

    public File getFile() {
        return file;
    }

    public void setFeatureCollection(FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection) {
        this.featureCollection = featureCollection;
    }

    public FeatureCollection<SimpleFeatureType, SimpleFeature> getFeatureCollection() {
        return featureCollection;
    }

    public void setFeatureSourceEnvelope(ReferencedEnvelope featureSourceEnvelope) {
        this.featureSourceEnvelope = featureSourceEnvelope;
    }

    public ReferencedEnvelope getFeatureSourceEnvelope() {
        return featureSourceEnvelope;
    }

    public void setSchema(SimpleFeatureType schema) {
        this.schema = schema;
    }

    public SimpleFeatureType getSchema() {
        return schema;
    }

    public Style[] getStyles() {
        return styles;
    }

    public void setStyles(Style[] styles) {
        this.styles = styles.clone();
    }

    public void setSelectedStyle(Style selectedStyle) {
        this.selectedStyle = selectedStyle;
    }

    public Style getSelectedStyle() {
        return selectedStyle;
    }
}
