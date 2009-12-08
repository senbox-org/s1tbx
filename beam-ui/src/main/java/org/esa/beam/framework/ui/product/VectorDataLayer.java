/*
 * $Id$
 *
 * Copyright (C) 2008 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.framework.ui.product;

import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.grender.Rendering;
import com.bc.ceres.swing.figure.Figure;
import com.bc.ceres.swing.figure.FigureCollection;
import com.bc.ceres.swing.figure.support.DefaultFigureSelection;
import org.esa.beam.framework.datamodel.ProductNodeEvent;
import org.esa.beam.framework.datamodel.ProductNodeListenerAdapter;
import org.esa.beam.framework.datamodel.VectorData;
import org.geotools.feature.FeatureCollection;
import org.geotools.feature.FeatureIterator;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import java.awt.geom.Rectangle2D;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class VectorDataLayer extends Layer {

    private FigureCollection figureCollection;
    private final VectorData vectorData;

    public VectorDataLayer(VectorDataLayerType type, VectorData vectorData, PropertyContainer configuration) {
        super(type, configuration);
        this.vectorData = vectorData;
        setName("Figures");
        figureCollection = new DefaultFigureSelection();
        vectorData.getProduct().addProductNodeListener(new ProductNodeListenerAdapter() {
            @Override
            public void nodeDataChanged(ProductNodeEvent event) {
                if (event.getSourceNode() == VectorDataLayer.this.vectorData) {
                    updateFigureCollection();
                }
            }
        });
    }

    private void updateFigureCollection() {
        FeatureCollection<SimpleFeatureType,SimpleFeature> featureCollection = vectorData.getFeatureCollection();

        Figure[] figures = figureCollection.getFigures();
        Map<SimpleFeature, SimpleFeatureFigure> figureMap = new HashMap<SimpleFeature, SimpleFeatureFigure>();
        for (Figure figure : figures) {
            if (figure instanceof SimpleFeatureFigure) {
                SimpleFeatureFigure simpleFeatureFigure = (SimpleFeatureFigure) figure;
                figureMap.put(simpleFeatureFigure.getSimpleFeature(), simpleFeatureFigure);
            }
        }

        FeatureIterator<SimpleFeature> featureIterator = featureCollection.features();
        while (featureIterator.hasNext()) {
            SimpleFeature simpleFeature = featureIterator.next();
            if (figureMap.containsKey(simpleFeature)) {
                figureMap.remove(simpleFeature);
            } else {
                figureCollection.addFigure(new SimpleFeatureFigureFactory(featureCollection).cre(simpleFeature));
            }
        }

        Collection<SimpleFeatureFigure> remainingFigures = figureMap.values();
        figureCollection.removeFigures(remainingFigures.toArray(new Figure[remainingFigures.size()]));
        
        fireLayerDataChanged(null);
    }

    public FigureCollection getFigureCollection() {
        return figureCollection;
    }

    @Override
    protected Rectangle2D getLayerModelBounds() {
        return figureCollection.getBounds();
    }

    @Override
    protected void renderLayer(Rendering rendering) {
        figureCollection.draw(rendering);
    }

}