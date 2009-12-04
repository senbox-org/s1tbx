package org.esa.beam.visat.toolviews.layermanager.layersrc.shapefile;

import com.bc.ceres.swing.demo.FigureEditorApp;
import com.bc.ceres.swing.figure.Figure;
import com.bc.ceres.swing.figure.FigureCollection;
import com.bc.ceres.swing.figure.FigureFactory;
import com.bc.ceres.swing.figure.FigureStyle;
import com.bc.ceres.swing.figure.ShapeFigure;
import com.bc.ceres.swing.figure.support.DefaultFigureStyle;
import com.bc.ceres.swing.figure.support.DefaultShapeFigure;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryCollection;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LineString;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.MultiLineString;
import com.vividsolutions.jts.geom.MultiPoint;
import com.vividsolutions.jts.geom.Point;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.feature.DefaultFeatureCollection;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.geometry.jts.LiteShape2;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import javax.swing.JOptionPane;
import java.awt.Color;
import java.awt.Shape;
import java.awt.geom.PathIterator;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

public class FeatureFigureEditorApp extends FigureEditorApp {

    private FeatureCollection featureCollection;

    public FeatureFigureEditorApp() {
        SimpleFeatureTypeBuilder ftb = new SimpleFeatureTypeBuilder();
        DefaultGeographicCRS crs = DefaultGeographicCRS.WGS84;
        ftb.setCRS(crs);
        ftb.setName("X");
        ftb.add("geometry", Geometry.class);
        ftb.setDefaultGeometry("geometry");
        SimpleFeatureType ft = ftb.buildFeatureType();
        this.featureCollection = new DefaultFeatureCollection("X", ft);
    }

    public static void main(String[] args) {
        run(new FeatureFigureEditorApp());
    }

    @Override
    protected FigureFactory getFigureFactory() {
        return new SimpleFeatureFigureFactory(featureCollection);
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
                System.out.printf("Loaded feature %d%n", numFeatures);
                DefaultFigureStyle figureStyle = createDefaultStyle();
                figureCollection.addFigure(new SimpleFeatureFigure(simpleFeature, figureStyle));
            }
            System.out.printf("Done loading %d features%n", numFeatures);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(getFrame(), "Error: " + e.getMessage());
        }
    }

    private static DefaultFigureStyle createDefaultStyle() {
        DefaultFigureStyle figureStyle = new DefaultFigureStyle();
        figureStyle.setStrokePaint(Color.BLACK);
        figureStyle.setFillPaint(Color.WHITE);
        return figureStyle;
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

        private SimpleFeatureFigure(SimpleFeature simpleFeature, FigureStyle style) {

            this.simpleFeature = simpleFeature;
            Object o = simpleFeature.getDefaultGeometry();
            if (!(o instanceof Geometry)) {
                throw new IllegalArgumentException("simpleFeature");
            }
            geometry = (Geometry) o;
            try {
                setShape(new LiteShape2(geometry, null, null, true));
            } catch (Exception e) {
                throw new IllegalArgumentException("simpleFeature", e);
            }
            Rank rank = getRank(geometry);
            System.out.println("rank = " + rank);
            setRank(rank);
            setNormalStyle(style);
        }

        public SimpleFeature getSimpleFeature() {
            return simpleFeature;
        }

        public Geometry getGeometry() {
            return geometry;
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

    private static class SimpleFeatureFigureFactory implements FigureFactory {

        private final FeatureCollection featureCollection;
        private int id;
        private GeometryFactory geometryFactory;


        private SimpleFeatureFigureFactory(FeatureCollection featureCollection) {
            this.featureCollection = featureCollection;
            this.geometryFactory = new GeometryFactory();
        }

        @Override
        public Figure createPunctualFigure(Point2D geometry, FigureStyle style) {
            return null;
        }

        @Override
        public ShapeFigure createLinealFigure(Shape geometry, FigureStyle style) {
            List<LineString> lineStringList = stringList(geometry, 1.0);
            if (lineStringList.size() == 1) {
                return createShapeFigure(lineStringList.get(0), style);
            } else {
                LineString[] lineStrings = lineStringList.toArray(new LineString[lineStringList.size()]);
                return createShapeFigure(geometryFactory.createMultiLineString(lineStrings), style);
            }
        }

        @Override
        public ShapeFigure createPolygonalFigure(Shape geometry, FigureStyle style) {
            List<LinearRing> linearRings = ringList(geometry, 1.0);
            LinearRing exteriorRing = linearRings.get(linearRings.size() - 1);
            LinearRing[] interiorRings = null;
            if (linearRings.size() > 1) {
                List<LinearRing> subList = linearRings.subList(0, linearRings.size() - 1);
                interiorRings = subList.toArray(new LinearRing[subList.size()]);
            }
            return createShapeFigure(geometryFactory.createPolygon(exteriorRing, interiorRings), style);
        }

        @Override
        public Figure createCollectionFigure(Figure... figures) {
            ArrayList<Geometry> geometryList = new ArrayList<Geometry>();
            for (Figure figure : figures) {
                SimpleFeatureFigure featureFigure = (SimpleFeatureFigure) figure;
                geometryList.add(featureFigure.getGeometry());
            }
            Geometry[] geometries = geometryList.toArray(new Geometry[geometryList.size()]);
            GeometryCollection geometry1 = geometryFactory.createGeometryCollection(geometries);
            return createShapeFigure(geometry1, createDefaultStyle());
        }

        private ShapeFigure createShapeFigure(Geometry geometry1, FigureStyle style) {
            SimpleFeatureType ft = (SimpleFeatureType) featureCollection.getSchema();
            SimpleFeatureBuilder sfb = new SimpleFeatureBuilder(ft);
            sfb.set("geometry", geometry1);
            return new SimpleFeatureFigure(sfb.buildFeature(String.valueOf(id++)), style);
        }

        private List<LinearRing> ringList(Shape geometry, double flatness) {
            List<List<Coordinate>> pathList = coordinates(geometry, flatness, true);
            List<LinearRing> rings = new ArrayList<LinearRing>();
            System.out.println("coordinates!!!!!!");
            for (List<Coordinate> path : pathList) {
                Coordinate[] coordinates = path.toArray(new Coordinate[path.size()]);
                for (Coordinate coordinate : coordinates) {
                    System.out.printf("coordinate = %s%n", coordinate);
                }
                rings.add(geometryFactory.createLinearRing(coordinates));
            }
            return rings;
        }

        private List<LineString> stringList(Shape geometry, double flatness) {
            List<List<Coordinate>> pathList = coordinates(geometry, flatness, false);
            List<LineString> strings = new ArrayList<LineString>();
            for (List<Coordinate> path : pathList) {
                strings.add(geometryFactory.createLineString(path.toArray(new Coordinate[path.size()])));
            }
            return strings;
        }

        private List<List<Coordinate>> coordinates(Shape geometry, double flatness, boolean forceClosedRings) {
            List<Coordinate> coordinates = new ArrayList<Coordinate>(16);
            List<List<Coordinate>> pathList = new ArrayList<List<Coordinate>>(4);
            PathIterator pathIterator = flatness > 0.0 ? geometry.getPathIterator(null,
                                                                                  flatness) : geometry.getPathIterator(
                    null);
            double[] seg = new double[6];
            int segType = -1;
            while (!pathIterator.isDone()) {
                segType = pathIterator.currentSegment(seg);
                if (segType == PathIterator.SEG_CLOSE) {
                    if (forceClosedRings) {
                        forceClosedRing(coordinates);
                    }
                    pathList.add(coordinates);
                    coordinates = new ArrayList<Coordinate>(16);
                } else {
                    coordinates.add(new Coordinate(seg[0], seg[1]));
                }
                pathIterator.next();
            }
            if (forceClosedRings && segType != PathIterator.SEG_CLOSE) {
                forceClosedRing(coordinates);
            }
            return pathList;
        }

        private void forceClosedRing(List<Coordinate> coordinates) {
            Coordinate first = coordinates.get(0);
            Coordinate last = coordinates.get(coordinates.size() - 1);
            if (!first.equals2D(last)) {
                coordinates.add(new Coordinate(first.x, first.y));
            }
        }


    }
}
