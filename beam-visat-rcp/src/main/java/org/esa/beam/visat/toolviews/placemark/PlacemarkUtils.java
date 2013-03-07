package org.esa.beam.visat.toolviews.placemark;


import com.bc.ceres.swing.figure.Figure;
import com.bc.ceres.swing.figure.FigureCollection;
import com.bc.ceres.swing.figure.support.DefaultFigureStyle;
import org.esa.beam.framework.datamodel.Placemark;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.framework.ui.product.SimpleFeaturePointFigure;
import org.esa.beam.visat.VisatApp;
import org.opengis.feature.simple.SimpleFeature;

import java.awt.Color;

public class PlacemarkUtils {

    public static Color getPlacemarkColor(Placemark placemark) {
        return getPlacemarkColor(placemark, VisatApp.getApp().getSelectedProductSceneView());
    }

    public static Color getPlacemarkColor(Placemark placemark, ProductSceneView view) {
        final String styleCss = placemark.getStyleCss();
        if (styleCss.contains(DefaultFigureStyle.FILL_COLOR.getName())) {
            return DefaultFigureStyle.createFromCss(styleCss).getFillColor();
        }
        final FigureCollection figureCollection = view.getFigureEditor().getFigureCollection();
        final Figure[] figures = figureCollection.getFigures();
        for (Figure figure : figures) {
            if (figure instanceof SimpleFeaturePointFigure) {
                final SimpleFeature simpleFeature = ((SimpleFeaturePointFigure) figure).getSimpleFeature();
                if (simpleFeature.getID().equals(placemark.getName())) {
                    return figure.getNormalStyle().getFillColor();
                }
            }
        }
        return Color.BLUE;
    }

}
