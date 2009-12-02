package org.esa.beam.visat.toolviews.layermanager.layersrc.shapefile;

import com.bc.ceres.swing.apps.FigureEditorApp;
import com.bc.ceres.swing.figure.FigureCollection;
import com.bc.ceres.swing.figure.FigureStyle;
import com.bc.ceres.swing.figure.Figure;
import com.bc.ceres.swing.figure.support.DefaultShapeFigure;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.Point;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.feature.FeatureCollection;
import org.geotools.geometry.jts.LiteShape2;
import org.geotools.geometry.jts.Decimator;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import javax.swing.JOptionPane;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

public class FeatureFigureEditorApp extends FigureEditorApp {

    public static void main(String[] args) {
        run(new FeatureFigureEditorApp());
    }

    @Override
    protected void loadFigureCollection(File file, FigureCollection figureCollection) {
        try {
            FeatureSource<SimpleFeatureType, SimpleFeature> featureFeatureSource;
            FeatureCollection<SimpleFeatureType, SimpleFeature> featureTypeSimpleFeatureFeatureCollection;
            featureFeatureSource = getFeatureSource(file);
            featureTypeSimpleFeatureFeatureCollection = featureFeatureSource.getFeatures();
            Iterator<SimpleFeature> featureIterator = featureTypeSimpleFeatureFeatureCollection.iterator();
            int numFeatures = 0;
            while (featureIterator.hasNext()) {
                SimpleFeature simpleFeature = featureIterator.next();
                numFeatures++;
                System.out.println("Loaded feature " + numFeatures);
                figureCollection.addFigure(new SimpleFeatureFigure(simpleFeature));
            }
            System.out.println("Done loading " + numFeatures + " features");
        } catch (IOException e) {
            JOptionPane.showMessageDialog(getFrame(), "Error: " + e.getMessage());
        }
    }

    @Override
    protected void storeFigureCollection(FigureCollection figureCollection, File file) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    public static FeatureSource<SimpleFeatureType, SimpleFeature> getFeatureSource(File file) throws IOException {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(ShapefileDataStoreFactory.URLP.key, file.toURI().toURL());
        map.put(ShapefileDataStoreFactory.CREATE_SPATIAL_INDEX.key, Boolean.TRUE);
        DataStore shapefileStore = DataStoreFinder.getDataStore(map);
        String typeName = shapefileStore.getTypeNames()[0]; // Shape files do only have one type name
        FeatureSource<SimpleFeatureType, SimpleFeature> featureSource;
        featureSource = shapefileStore.getFeatureSource(typeName);
        return featureSource;
    }

    private static class SimpleFeatureFigure extends DefaultShapeFigure {
        private final SimpleFeature simpleFeature;
        private final Geometry geometry;

        public SimpleFeatureFigure(SimpleFeature simpleFeature) {
            getNormalStyle().setValue(FigureStyle.FILL.getName(), Color.WHITE);
            getNormalStyle().setValue(FigureStyle.STROKE.getName(), Color.BLACK);

            this.simpleFeature = simpleFeature;
            Object o = simpleFeature.getDefaultGeometry();
            if (!(o instanceof Geometry)) {
                throw new IllegalArgumentException("simpleFeature");
            }
            geometry = (Geometry) o;
            try {
                setShape(new LiteShape2(geometry, null, null, true));
            } catch (Exception e) {
                throw new IllegalArgumentException("simpleFeature");
            }
            Rank rank = getRank(geometry);
            System.out.println("rank = " + rank);
            setRank(rank);
        }

        public SimpleFeature getSimpleFeature() {
            return simpleFeature;
        }

        public static Rank getRank(Geometry geometry) {
            if (geometry instanceof Point) {
                return Rank.PUNCTUAL;
            }
            if (geometry instanceof MultiPoint) {
                return Rank.PUNCTUAL;
            }
            if (geometry instanceof LineString) {
                return Rank.LINEAL;
            }
            if (geometry instanceof MultiLineString) {
                return Rank.LINEAL;
            }
            if (geometry instanceof GeometryCollection) {
                return Rank.COLLECTION;
            }
            return Rank.POLYGONAL;
        }
    }
}
