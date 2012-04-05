package org.esa.beam.visat.toolviews.layermanager.layersrc.shapefile;

import com.bc.ceres.swing.figure.FigureStyle;
import com.bc.ceres.swing.figure.support.DefaultFigureStyle;
import org.esa.beam.framework.datamodel.PlainFeatureFactory;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureTypes;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.geotools.styling.*;
import org.geotools.styling.Stroke;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.filter.Filter;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class SLDUtils {
    private static final StyleFactory styleFactory = CommonFactoryFinder.getStyleFactory(null);

    public static File getSLDFile(File shapeFile) {
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
        File sld = getSLDFile(shapeFile);
        if (sld.exists()) {
            return createFromSLD(sld);
        } else {
            return new Style[0];
        }
    }

    public static Style[] createFromSLD(File sld) {
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

    public static SimpleFeature processRules(SimpleFeature sf, SimpleFeatureType styledSFT, List<Rule> ruleList,
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

    public static SimpleFeature processSymbolizers(SimpleFeatureType sft, SimpleFeature feature,
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

    public static boolean isFeatureTypeStyleActive(SimpleFeatureType ftype, FeatureTypeStyle fts) {
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

    public static SimpleFeature createStyledFeature(SimpleFeatureType type, SimpleFeature feature, String styleCSS) {
        SimpleFeatureBuilder sfb = new SimpleFeatureBuilder(type);
        sfb.init(feature);
        sfb.set(PlainFeatureFactory.ATTRIB_NAME_STYLE_CSS, styleCSS);
        return sfb.buildFeature(feature.getID());
    }
}