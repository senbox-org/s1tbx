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
package org.esa.beam.visat.actions;

import com.bc.ceres.binding.*;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerFilter;
import com.bc.ceres.glayer.support.LayerUtils;
import com.bc.ceres.swing.binding.PropertyPane;
import org.esa.beam.framework.datamodel.PlainFeatureFactory;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNodeGroup;
import org.esa.beam.framework.datamodel.VectorDataNode;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.framework.ui.product.VectorDataLayerFilterFactory;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.visat.VisatActivator;
import org.esa.beam.visat.VisatApp;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import javax.swing.*;
import java.awt.*;
import java.text.MessageFormat;

/**
 * Creates a new vector data node.
 *
 * @author Norman Fomferra
 * @since BEAM 4.7
 */
public class CreateVectorDataNodeAction extends ExecCommand {
    private static final String DIALOG_TITLE = "New Vector Data Container";
    private static final String KEY_VECTOR_DATA_INITIAL_NAME = "geometry.initialName";


    // todo - add help (nf)
    private static final String HELP_ID = "vectorDataManagement";
    private static int numItems = 1;

    @Override
    public void actionPerformed(CommandEvent event) {
        if (VisatApp.getApp().getSelectedProduct() != null) {
            run();
        }
    }

    public VectorDataNode run() {
        Product product = VisatApp.getApp().getSelectedProduct();
        DialogData dialogData = new DialogData(product.getVectorDataGroup());
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
            return createDefaultVectorDataNode(product, dialogData.name, dialogData.description);
        } else {
            return null;
        }
    }

    public static VectorDataNode createDefaultVectorDataNode(Product product) {
        return createDefaultVectorDataNode(product,
                                           getDefaultVectorDataNodeName(),
                                           "Default vector data container for geometries (automatically created)");
    }

    public static VectorDataNode createDefaultVectorDataNode(Product product, String name, String description) {
        CoordinateReferenceSystem modelCrs = ImageManager.getModelCrs(product.getGeoCoding());
        SimpleFeatureType type = PlainFeatureFactory.createDefaultFeatureType(modelCrs);
        VectorDataNode vectorDataNode = new VectorDataNode(name, type);
        vectorDataNode.setDescription(description);
        product.getVectorDataGroup().add(vectorDataNode);
        vectorDataNode.getPlacemarkGroup();

        final ProductSceneView sceneView = VisatApp.getApp().getSelectedProductSceneView();
        if (sceneView != null) {
            VisatApp.getApp().getProductTree().expand(vectorDataNode);
            sceneView.selectVectorDataLayer(vectorDataNode);

            final LayerFilter nodeFilter = VectorDataLayerFilterFactory.createNodeFilter(vectorDataNode);
            Layer vectorDataLayer = LayerUtils.getChildLayer(sceneView.getRootLayer(),
                                                             LayerUtils.SEARCH_DEEP,
                                                             nodeFilter);
            if (vectorDataLayer != null) {
                vectorDataLayer.setVisible(true);
            }
        }
        return vectorDataNode;
    }

    public static String getDefaultVectorDataNodeName() {
        return VisatActivator.getInstance().getModuleContext().getRuntimeConfig().getContextProperty(KEY_VECTOR_DATA_INITIAL_NAME, "geometry");
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

    private static class NameValidator implements Validator {
        private final Product product;

        private NameValidator(Product product) {
            this.product = product;
        }

        @Override
        public void validateValue(Property property, Object value) throws ValidationException {
            String name = (String) value;
            if (product.getVectorDataGroup().contains(name)) {
                final String pattern = "A vector data container with name ''{0}'' already exists.\n" +
                        "Please choose another one.";
                throw new ValidationException(MessageFormat.format(pattern, name));
            }
        }
    }

    private static class MyModalDialog extends ModalDialog {
        private final PropertyPane propertyPane;

        private MyModalDialog(PropertyPane propertyPane) {
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

    private static class DialogData {
        private String name;
        private String description;

        private DialogData(ProductNodeGroup<VectorDataNode> vectorGroup) {
            String defaultPrefix = getDefaultVectorDataNodeName() + "_";
            name = defaultPrefix + (numItems++);
            while (vectorGroup.contains(name)) {
                name = defaultPrefix + (numItems++);
            }
            description = "";
        }
    }


}