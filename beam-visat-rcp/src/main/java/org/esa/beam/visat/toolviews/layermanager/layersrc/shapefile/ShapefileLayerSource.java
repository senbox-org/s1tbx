/*
 * $Id: $
 *
 * Copyright (C) 2009 by Brockmann Consult (info@brockmann-consult.de)
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
package org.esa.beam.visat.toolviews.layermanager.layersrc.shapefile;

import com.bc.ceres.glayer.Layer;
import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.visat.toolviews.layermanager.LayerSource;
import org.esa.beam.visat.toolviews.layermanager.layersrc.AbstractLayerSourceAssistantPage;
import org.esa.beam.visat.toolviews.layermanager.layersrc.LayerSourcePageContext;

/**
 * todo - add API doc
 *
 * @author Marco Zuehlke
 * @version $Revision$ $Date$
 * @since BEAM 4.6
 */
public class ShapefileLayerSource implements LayerSource {

    private ShapefileModel model;

    @Override
    public boolean isApplicable(LayerSourcePageContext pageContext) {
        return true;
    }

    @Override
    public boolean hasFirstPage() {
        return true;
    }

    @Override
    public AbstractLayerSourceAssistantPage getFirstPage(LayerSourcePageContext pageContext) {
        model = new ShapefileModel();
        return new ShapefileAssistantPage(model);
    }

    @Override
    public boolean canFinish(LayerSourcePageContext pageContext) {
        return false;
    }

    @Override
    public void cancel() {
    }

    @Override
    public boolean finish(LayerSourcePageContext pageContext) {
        FeatureLayer featureLayer = new FeatureLayer(model.getFeatureCollection(), model.getSelectedStyle());
        featureLayer.setName(model.getFile().getName());
        featureLayer.setVisible(true);

        ProductSceneView sceneView = pageContext.getAppContext().getSelectedProductSceneView();
        final Layer rootLayer = sceneView.getRootLayer();
        rootLayer.getChildren().add(sceneView.getFirstImageLayerIndex(), featureLayer);
        return true;
    }

    public static Geometry createProductGeometry(Product targetProduct) {
        GeometryFactory gf = new GeometryFactory();
        GeoPos[] geoPoses = ProductUtils.createGeoBoundary(targetProduct, 100);
        Coordinate[] coordinates = new Coordinate[geoPoses.length + 1];
        for (int i = 0; i < geoPoses.length; i++) {
            GeoPos geoPose = geoPoses[i];
            coordinates[i] = new Coordinate(geoPose.lon, geoPose.lat);
        }
        coordinates[coordinates.length - 1] = coordinates[0];

        return gf.createPolygon(gf.createLinearRing(coordinates), null);
    }

}
