/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.beam.visat.toolviews.layermanager.layersrc.shapefile;

import com.bc.ceres.swing.demo.FigureEditorApp;
import com.bc.ceres.swing.figure.Figure;
import com.bc.ceres.swing.figure.FigureCollection;
import com.bc.ceres.swing.figure.FigureFactory;
import com.bc.ceres.swing.figure.support.DefaultFigureStyle;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import org.esa.beam.framework.ui.product.SimpleFeatureFigure;
import org.esa.beam.framework.ui.product.SimpleFeatureFigureFactory;
import org.esa.beam.framework.ui.product.SimpleFeaturePointFigure;
import org.esa.beam.framework.ui.product.SimpleFeatureShapeFigure;
import org.esa.beam.visat.actions.ExportGeometryAction;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

public class FeatureFigureEditorApp extends FigureEditorApp {

    private final SimpleFeatureType featureType;

    public FeatureFigureEditorApp() {
        featureType = createSimpleFeatureType("X", Geometry.class, null);
    }

    static class XYZ {
        Class<?> geometryType;
        SimpleFeatureType defaults;
        ArrayList<SimpleFeature> features = new ArrayList<SimpleFeature>();
    }

    private SimpleFeatureType createSimpleFeatureType(String typeName, Class<?> geometryType, SimpleFeatureType defaults) {
        SimpleFeatureTypeBuilder sftb = new SimpleFeatureTypeBuilder();
        if (defaults != null) {
            //sftb.init(defaults);
        }
        DefaultGeographicCRS crs = DefaultGeographicCRS.WGS84;
        sftb.setCRS(crs);
        sftb.setName(typeName);
        sftb.add("geom", geometryType);
        sftb.add("style", String.class);
        sftb.setDefaultGeometry("geom");
        return sftb.buildFeatureType();
    }

    public static void main(String[] args) {
        run(new FeatureFigureEditorApp());
    }

    @Override
    protected FigureFactory getFigureFactory() {
        return new SimpleFeatureFigureFactory(featureType);
    }

    @Override
    protected void loadFigureCollection(File file, FigureCollection figureCollection) throws IOException {
        FeatureSource<SimpleFeatureType, SimpleFeature> featureFeatureSource;
        FeatureCollection<SimpleFeatureType, SimpleFeature> featureTypeSimpleFeatureFeatureCollection;
        featureFeatureSource = getFeatureSource(file);
        featureTypeSimpleFeatureFeatureCollection = featureFeatureSource.getFeatures();
        Iterator<SimpleFeature> featureIterator = featureTypeSimpleFeatureFeatureCollection.iterator();
        while (featureIterator.hasNext()) {
            SimpleFeature simpleFeature = featureIterator.next();
            DefaultFigureStyle figureStyle = createDefaultFigureStyle();
            Object o = simpleFeature.getDefaultGeometry();
            if (o instanceof Point) {
                figureCollection.addFigure(new SimpleFeaturePointFigure(simpleFeature, figureStyle));
            } else {
                figureCollection.addFigure(new SimpleFeatureShapeFigure(simpleFeature, figureStyle));
            }
        }
    }

    @Override
    protected void storeFigureCollection(FigureCollection figureCollection, File file) throws IOException {

        Figure[] figures = figureCollection.getFigures();
        Map<Class<?>, List<SimpleFeature>> featureListMap = new HashMap<Class<?>, List<SimpleFeature>>();
        for (Figure figure : figures) {
            SimpleFeatureFigure simpleFeatureFigure = (SimpleFeatureFigure) figure;
            SimpleFeature simpleFeature = simpleFeatureFigure.getSimpleFeature();
            Class<?> geometryType = simpleFeature.getDefaultGeometry().getClass();
            List<SimpleFeature> featureList = featureListMap.get(geometryType);
            if (featureList == null) {
                featureList = new ArrayList<SimpleFeature>();
                featureListMap.put(geometryType, featureList);
            }
            featureList.add(simpleFeature);
        }
        Set<Map.Entry<Class<?>, List<SimpleFeature>>> entries = featureListMap.entrySet();
        for (Map.Entry<Class<?>, List<SimpleFeature>> entry : entries) {
            Class<?> geomType = entry.getKey();
            List<SimpleFeature> features = entry.getValue();
            ExportGeometryAction.writeEsriShapefile(geomType, features,file);
        }
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

    public static DefaultFigureStyle createDefaultFigureStyle() {
        DefaultFigureStyle figureStyle = new DefaultFigureStyle();
        figureStyle.setStrokeColor(Color.BLACK);
        figureStyle.setStrokeWidth(1.0);
        figureStyle.setFillColor(Color.WHITE);
        return figureStyle;
    }

}
