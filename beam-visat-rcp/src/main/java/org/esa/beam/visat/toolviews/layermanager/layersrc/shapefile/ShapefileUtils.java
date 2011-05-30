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

import com.bc.ceres.swing.figure.FigureStyle;
import com.bc.ceres.swing.figure.support.DefaultFigureStyle;
import com.vividsolutions.jts.geom.Geometry;
import org.esa.beam.framework.datamodel.PlainFeatureFactory;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.util.FeatureCollectionClipper;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFinder;
import org.geotools.data.FeatureSource;
import org.geotools.data.shapefile.ShapefileDataStoreFactory;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureTypes;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.LineSymbolizer;
import org.geotools.styling.PolygonSymbolizer;
import org.geotools.styling.Rule;
import org.geotools.styling.SLD;
import org.geotools.styling.SLDParser;
import org.geotools.styling.Stroke;
import org.geotools.styling.Style;
import org.geotools.styling.Symbolizer;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.awt.BasicStroke;
import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Unstable API. Use at own risk.
 */
public class ShapefileUtils {

    private static final org.geotools.styling.StyleFactory styleFactory = CommonFactoryFinder.getStyleFactory(null);

    public static FeatureCollection<SimpleFeatureType, SimpleFeature> createFeatureCollection(URL url,
                                                                                              CoordinateReferenceSystem targetCrs,
                                                                                              Geometry clipGeometry) throws
                                                                                                                     IOException {
        FeatureSource<SimpleFeatureType, SimpleFeature> featureSource = getFeatureSource(url);
        FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection = featureSource.getFeatures();
        featureCollection = FeatureCollectionClipper.doOperation(featureCollection, DefaultGeographicCRS.WGS84,
                                                                 clipGeometry, DefaultGeographicCRS.WGS84,
                                                                 null, targetCrs);
        return featureCollection;
    }

    public static FeatureSource<SimpleFeatureType, SimpleFeature> getFeatureSource(URL url) throws IOException {
        Map<String, Object> map = new HashMap<String, Object>();
        map.put(ShapefileDataStoreFactory.URLP.key, url);
        map.put(ShapefileDataStoreFactory.CREATE_SPATIAL_INDEX.key, Boolean.TRUE);
        DataStore shapefileStore = DataStoreFinder.getDataStore(map);
        String typeName = shapefileStore.getTypeNames()[0]; // Shape files do only have one type name
        return shapefileStore.getFeatureSource(typeName);
    }

    /*
    * Figure out the URL for the "sld" file
    */

    private static File getSLDFile(File shapeFile) {
        String filename = shapeFile.getAbsolutePath();
        if (filename.endsWith(".shp") || filename.endsWith(".dbf")
            || filename.endsWith(".shx")) {
            filename = filename.substring(0, filename.length() - 4);
            filename += ".sld";
        } else if (filename.endsWith(".SHP") || filename.endsWith(".DBF")
                   || filename.endsWith(".SHX")) {
            filename = filename.substring(0, filename.length() - 4);
            filename += ".SLD";
        }
        return new File(filename);
    }

    public static Style[] loadSLD(File shapeFile) {
        File sld = ShapefileUtils.getSLDFile(shapeFile);
        if (sld.exists()) {
            return createFromSLD(sld);
        } else {
            return new Style[0];
        }
    }

    private static Style[] createFromSLD(File sld) {
        try {
            SLDParser stylereader = new SLDParser(styleFactory, sld.toURI().toURL());
            return stylereader.readXML();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return new Style[0];
    }

    /**
     * Converts the styling information in the style into CSS styles for all given features in the collection.
     *
     * @param style             The style.
     * @param defaultCss        The CSS default value.
     * @param featureCollection The collection that should be styled.
     * @param styledCollection  the collection that will contain the styled features.
     */
    public static void applyStyle(Style style, String defaultCss,
                                  FeatureCollection<SimpleFeatureType, SimpleFeature> featureCollection,
                                  FeatureCollection<SimpleFeatureType, SimpleFeature> styledCollection) {

        List<FeatureTypeStyle> featureTypeStyles = style.featureTypeStyles();
        SimpleFeatureType featureType = featureCollection.getSchema();
        SimpleFeatureType styledFeatureType = styledCollection.getSchema();

        List<SimpleFeature> featuresToStyle = new ArrayList<SimpleFeature>(featureCollection.size());
        Iterator<SimpleFeature> iterator = featureCollection.iterator();
        while (iterator.hasNext()) {
            featuresToStyle.add(iterator.next());
        }

        for (FeatureTypeStyle fts : featureTypeStyles) {
            if (isFeatureTypeStyleActive(featureType, fts)) {
                List<Rule> ruleList = new ArrayList<Rule>();
                List<Rule> elseRuleList = new ArrayList<Rule>();
                for (Rule rule : fts.rules()) {
                    if (rule.isElseFilter()) {
                        elseRuleList.add(rule);
                    } else {
                        ruleList.add(rule);
                    }
                }
                Iterator<SimpleFeature> featureIterator = featuresToStyle.iterator();
                while (featureIterator.hasNext()) {
                    SimpleFeature simpleFeature = featureIterator.next();
                    SimpleFeature styledFeature = processRules(simpleFeature, styledFeatureType, ruleList,
                                                               elseRuleList);
                    if (styledFeature != null) {
                        styledCollection.add(styledFeature);
                        featureIterator.remove();
                    }
                }
            }
        }
        for (SimpleFeature simpleFeature : featuresToStyle) {
            styledCollection.add(createStyledFeature(styledFeatureType, simpleFeature, defaultCss));
        }
    }

    private static SimpleFeature processRules(SimpleFeature sf, SimpleFeatureType styledSFT, List<Rule> ruleList,
                                              List<Rule> elseRuleList) {
        Filter filter;
        boolean doElse = true;
        Symbolizer[] symbolizers;
        for (Rule rule : ruleList) {
            filter = rule.getFilter();
            if ((filter == null) || filter.evaluate(sf)) {
                doElse = false;
                symbolizers = rule.getSymbolizers();
                SimpleFeature styledFeature = processSymbolizers(styledSFT, sf, symbolizers);
                if (styledFeature != null) {
                    return styledFeature;
                }
            }
        }
        if (doElse) {
            for (Rule rule : elseRuleList) {
                symbolizers = rule.getSymbolizers();
                SimpleFeature styledFeature = processSymbolizers(styledSFT, sf, symbolizers);
                if (styledFeature != null) {
                    return styledFeature;
                }
            }
        }
        return null;
    }

    private static SimpleFeature processSymbolizers(SimpleFeatureType sft, SimpleFeature feature,
                                                    Symbolizer[] symbolizers) {
        for (Symbolizer symbolizer : symbolizers) {
            if (symbolizer instanceof LineSymbolizer) {
                LineSymbolizer lineSymbolizer = (LineSymbolizer) symbolizer;
                Stroke stroke = lineSymbolizer.getStroke();
                Color strokeColor = SLD.color(stroke);
                int width = SLD.width(stroke);
                FigureStyle figureStyle = DefaultFigureStyle.createLineStyle(strokeColor, new BasicStroke(width));
                String cssStyle = figureStyle.toCssString();
                return createStyledFeature(sft, feature, cssStyle);
            } else if (symbolizer instanceof PolygonSymbolizer) {
                PolygonSymbolizer polygonSymbolizer = (PolygonSymbolizer) symbolizer;
                Color fillColor = SLD.color(polygonSymbolizer.getFill());
                Stroke stroke = polygonSymbolizer.getStroke();
                Color strokeColor = SLD.color(stroke);
                int width = SLD.width(stroke);
                FigureStyle figureStyle = DefaultFigureStyle.createPolygonStyle(fillColor, strokeColor,
                                                                                new BasicStroke(width));
                String cssStyle = figureStyle.toCssString();
                return createStyledFeature(sft, feature, cssStyle);
            }
        }
        return null;
    }

    private static boolean isFeatureTypeStyleActive(SimpleFeatureType ftype, FeatureTypeStyle fts) {
        return ((ftype.getTypeName() != null)
                && (ftype.getTypeName().equalsIgnoreCase(fts.getFeatureTypeName()) ||
                    FeatureTypes.isDecendedFrom(ftype, null, fts.getFeatureTypeName())));
    }

    public static SimpleFeatureType createStyledFeatureType(SimpleFeatureType type) {
        SimpleFeatureTypeBuilder sftb = new SimpleFeatureTypeBuilder();
        sftb.init(type);
        sftb.add(PlainFeatureFactory.ATTRIB_NAME_STYLE_CSS, String.class);
        return sftb.buildFeatureType();
    }

    private static SimpleFeature createStyledFeature(SimpleFeatureType type, SimpleFeature feature, String styleCSS) {
        SimpleFeatureBuilder sfb = new SimpleFeatureBuilder(type);
        sfb.init(feature);
        sfb.set(PlainFeatureFactory.ATTRIB_NAME_STYLE_CSS, styleCSS);
        return sfb.buildFeature(feature.getID());
    }
}
