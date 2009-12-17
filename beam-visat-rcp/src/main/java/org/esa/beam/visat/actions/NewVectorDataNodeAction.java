/*
 * $Id: CreateFilteredBandAction.java,v 1.1 2007/04/19 10:16:12 marcop Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
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
package org.esa.beam.visat.actions;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.Validator;
import com.bc.ceres.glayer.LayerFilter;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.support.LayerUtils;
import com.vividsolutions.jts.geom.Geometry;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.VectorDataNode;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.PropertyPane;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.framework.ui.product.SimpleFeatureFigureFactory;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.framework.ui.product.VectorDataLayer;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.visat.VisatApp;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import javax.swing.JPanel;
import java.awt.Dimension;
import java.text.MessageFormat;

/**
 * Creates a new vector data node.
 *
 * @author Norman Fomferra
 * @since BEAM 4.7
 */
public class NewVectorDataNodeAction extends ExecCommand {
    private static final String DIALOG_TITLE = "New Geometry Container";

    public static final String VECTOR_DATA_NAME = "vectorDataName";

    // todo - add help (nf)
    private static final String HELP_ID = "";
    static int numItems = 0;

    public String getVectorDataName() {
        return (String) getProperty(VECTOR_DATA_NAME);
    }

    @Override
    public void actionPerformed(CommandEvent event) {
        run();
    }

    public void run() {
        final Product product = VisatApp.getApp().getSelectedProduct();
        DialogData dialogData = new DialogData();
        PropertySet propertySet = PropertyContainer.createObjectBacked(dialogData);
        propertySet.getDescriptor("name").setNotNull(true);
        propertySet.getDescriptor("name").setNotEmpty(true);
        propertySet.getDescriptor("name").setValidator(new NameValidator(product));
        propertySet.getDescriptor("description").setNotNull(true);

        final PropertyPane propertyPane = new PropertyPane(propertySet);
        JPanel panel = propertyPane.createPanel();
        panel.setPreferredSize(new Dimension(400, 100));
        ModalDialog dialog = new MyModalDialog(propertyPane);
        dialog.setContent(panel);
        int i = dialog.show();
        if (i == ModalDialog.ID_OK) {
            createVectorDataNode(product, dialogData.name, dialogData.description);
            setProperty(VECTOR_DATA_NAME, dialogData.name);
        } else {
            setProperty(VECTOR_DATA_NAME, null);
        }
    }

    public static VectorDataNode createVectorDataNode(Product product, String name, String description) {
        CoordinateReferenceSystem modelCrs = ImageManager.getModelCrs(product.getGeoCoding());
        SimpleFeatureType type = SimpleFeatureFigureFactory.createSimpleFeatureType(Product.GEOMETRY_FEATURE_TYPE_NAME, Geometry.class, modelCrs);
        VectorDataNode vectorDataNode = new VectorDataNode(name, type);
        vectorDataNode.setDescription(description);
        product.getVectorDataGroup().add(vectorDataNode);
        final ProductSceneView sceneView = VisatApp.getApp().getSelectedProductSceneView();
        if (sceneView != null) {
            VisatApp.getApp().setSelectedProductNode(vectorDataNode);
            setSelectedVectorDataNode(sceneView, vectorDataNode);
        }
        return vectorDataNode;
    }

    /**
     * Causes this command to fire the 'check status' event to all of its listeners.
     */
    @Override
    public void updateState() {
        VisatApp app = VisatApp.getApp();
        Product product = null;
        if (app != null) {
            product = app.getSelectedProduct();
        }
        setEnabled(product != null);
    }

    // todo - same code in org.esa.beam.visat.ProductsToolView (nf)
    public static void setSelectedVectorDataNode(ProductSceneView sceneView, final VectorDataNode vectorDataNode) {
        final LayerFilter layerFilter = new LayerFilter() {
            @Override
            public boolean accept(Layer layer) {
                return layer instanceof VectorDataLayer && ((VectorDataLayer) layer).getVectorDataNode() == vectorDataNode;
            }
        };
        Layer layer = LayerUtils.getChildLayer(sceneView.getRootLayer(), layerFilter, LayerUtils.SearchMode.DEEP);
        if (layer != null) {
            layer.setVisible(true);
            sceneView.setSelectedLayer(layer);
        }
    }

    private static class NameValidator implements Validator {
        private final Product product;

        public NameValidator(Product product) {
            this.product = product;
        }

        @Override
        public void validateValue(Property property, Object value) throws ValidationException {
            String name = (String) value;
            if (product.getVectorDataGroup().contains(name)) {
                final String pattern = "A geometry container with name ''{0}'' already exists.\n" +
                        "Please choose another one.";
                throw new ValidationException(MessageFormat.format(pattern, name));
            }
        }
    }

    private static class MyModalDialog extends ModalDialog {
        private final PropertyPane propertyPane;

        public MyModalDialog(PropertyPane propertyPane) {
            super(VisatApp.getApp().getMainFrame(),
                  DIALOG_TITLE,
                  ModalDialog.ID_OK_CANCEL_HELP,
                  HELP_ID);
            this.propertyPane = propertyPane;
        }

        /**
         * Called in order to perform input validation.
         *
         * @return {@code true} if and only if the validation was successful.
         */
        @Override
        protected boolean verifyUserInput() {
            return !propertyPane.getBindingContext().hasProblems();
        }
    }

    // todo - add validators (nf)
    static class DialogData {
        String name = "geometry_" + (++numItems);
        String description = "";
    }


}