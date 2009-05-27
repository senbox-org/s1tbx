package org.esa.beam.visat.toolviews.layermanager.layersrc.shapefile;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.util.ProductUtils;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.awt.Shape;
import java.awt.geom.Area;
import java.awt.geom.Path2D;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Unstable API. Use at own risk.
 */
public class ShapefileUtils {

    public static FeatureCollection<SimpleFeatureType, SimpleFeature> loadShapefile(File file,
                                                                                    RasterDataNode targetRaster)
            throws IOException {
        return loadShapefile(file.toURI().toURL(), targetRaster);
    }

    public static FeatureCollection<SimpleFeatureType, SimpleFeature> loadShapefile(URL fileUrl,
                                                                                    RasterDataNode targetRaster)
            throws IOException {
        final CoordinateReferenceSystem targetCrs = targetRaster.getGeoCoding().getModelCRS();
        final Geometry clipGeometry = createGeoBoundaryPolygon(targetRaster);
        return createFeatureCollection(fileUrl, targetCrs, clipGeometry);
    }

    public static FeatureCollection<SimpleFeatureType, SimpleFeature> createFeatureCollection(URL url,
                                                                                              CoordinateReferenceSystem targetCrs,
                                                                                              Geometry clipGeometry
    ) throws IOException {
        FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection;
        FeatureSource<SimpleFeatureType, SimpleFeature> featureSource = getFeatureSource(url);
        featureCollection = featureSource.getFeatures();
        featureCollection = FeatureCollectionClipper.doOperation(featureCollection, clipGeometry, targetCrs);
        return featureCollection;
    }

    public static FeatureSource<SimpleFeatureType, SimpleFeature> getFeatureSource(URL url) throws IOException {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(ShapefileDataStoreFactory.URLP.key, url);
        map.put(ShapefileDataStoreFactory.CREATE_SPATIAL_INDEX.key, Boolean.TRUE);
        DataStore shapefileStore = DataStoreFinder.getDataStore(map);
        String typeName = shapefileStore.getTypeNames()[0]; // Shape files do only have one type name
        FeatureSource<SimpleFeatureType, SimpleFeature> featureSource;
        featureSource = shapefileStore.getFeatureSource(typeName);
        return featureSource;
    }

    public static Geometry createGeoBoundaryPolygon(RasterDataNode targetRaster) {
        GeometryFactory gf = new GeometryFactory();
        GeoPos[] geoPositions = ProductUtils.createGeoBoundary(targetRaster, null, 100);
        Coordinate[] coordinates = new Coordinate[geoPositions.length + 1];
        for (int i = 0; i < geoPositions.length; i++) {
            GeoPos geoPos = geoPositions[i];
            coordinates[i] = new Coordinate(geoPos.lon, geoPos.lat);
        }
        coordinates[coordinates.length - 1] = coordinates[0];
        return gf.createPolygon(gf.createLinearRing(coordinates), null);
    }
}
