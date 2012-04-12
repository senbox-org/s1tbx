/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.visat.toolviews.mask;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.glevel.MultiLevelImage;
import com.bc.ceres.grender.Viewport;
import com.bc.jexp.impl.Tokenizer;
import org.esa.beam.dataio.dimap.DimapProductConstants;
import org.esa.beam.dataio.dimap.spi.DimapPersistable;
import org.esa.beam.dataio.dimap.spi.DimapPersistence;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.BitmaskDef;
import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.Mask.ImageType;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNodeGroup;
import org.esa.beam.framework.datamodel.VectorDataNode;
import org.esa.beam.framework.dataop.barithm.BandArithmetic;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.ui.AbstractDialog;
import org.esa.beam.framework.ui.application.support.AbstractToolView;
import org.esa.beam.framework.ui.product.ProductExpressionPane;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.util.PropertyMap;
import org.esa.beam.util.StringUtils;
import org.esa.beam.util.io.BeamFileChooser;
import org.esa.beam.util.io.BeamFileFilter;
import org.esa.beam.util.io.FileUtils;
import org.esa.beam.visat.VisatApp;
import org.esa.beam.visat.actions.CreateVectorDataNodeAction;
import org.esa.beam.visat.internal.RasterDataNodeDeleter;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;
import java.awt.Color;
import java.awt.Component;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Marco Peters
 * @version $ Revision $ Date $
 * @since BEAM 4.7
 */
class MaskFormActions {

    private final MaskAction[] maskActions;

    MaskFormActions(AbstractToolView maskToolView, MaskForm maskForm) {
        maskActions = new MaskAction[]{
                new NewBandMathsAction(maskForm), new NewRangeAction(maskForm),
                new NewVectorDataNodeAction(maskForm), new NullAction(maskForm),
                new NewUnionAction(maskForm), new NewIntersectionAction(maskForm),
                new NewDifferenceAction(maskForm), new NewInvDifferenceAction(maskForm),
                new NewComplementAction(maskForm), new NullAction(maskForm),
                new CopyAction(maskForm), new EditAction(maskForm),
                new RemoveAction(maskForm), new TransferAction(maskForm),
                new ImportAction(maskToolView, maskForm), new ExportAction(maskToolView, maskForm),
                new ZoomToVectorMaskAction(maskToolView, maskForm), new NullAction(maskForm),
        };
    }

    private MaskAction getMaskAction(Class<?> type) {
        for (MaskAction maskAction : maskActions) {
            if (type.getName().equals(maskAction.getValue(Action.ACTION_COMMAND_KEY))) {
                return maskAction;
            }
        }
        return null;
    }

    public MaskAction[] getAllActions() {
        return maskActions.clone();
    }

    public MaskAction getNewBandMathAction() {
        return getMaskAction(NewBandMathsAction.class);
    }

    public MaskAction getNewRangeAction() {
        return getMaskAction(NewRangeAction.class);
    }

    public MaskAction getNewIntersectionAction() {
        return getMaskAction(NewIntersectionAction.class);
    }

    public MaskAction getNewSubtractionAction() {
        return getMaskAction(NewDifferenceAction.class);
    }

    public MaskAction getNewUnionAction() {
        return getMaskAction(NewUnionAction.class);
    }

    public MaskAction getNewInversionAction() {
        return getMaskAction(NewComplementAction.class);
    }

    public MaskAction getCopyAction() {
        return getMaskAction(CopyAction.class);
    }

    public MaskAction getEditAction() {
        return getMaskAction(EditAction.class);
    }

    public MaskAction getExportAction() {
        return getMaskAction(ExportAction.class);
    }

    public MaskAction getImportAction() {
        return getMaskAction(ImportAction.class);
    }

    public MaskAction getRemoveAction() {
        return getMaskAction(RemoveAction.class);
    }

    public MaskAction getNullAction() {
        return getMaskAction(NullAction.class);
    }

    private static class NewBandMathsAction extends BandMathsAction {

        private NewBandMathsAction(MaskForm maskForm) {
            super(maskForm, "BandMath24.png", "bandMathButton",
                  "Creates a new mask based on a logical band maths expression");
        }

        @Override
        String getCode(ActionEvent e) {
            Product product = getMaskForm().getProduct();
            ProductExpressionPane expressionPane = ProductExpressionPane.createBooleanExpressionPane(
                    new Product[]{product}, product, null);
            expressionPane.setEmptyExpressionAllowed(false);
            expressionPane.setCode("");
            if (expressionPane.showModalDialog(null, "New Logical Band Maths Expression") == AbstractDialog.ID_OK) {
                final String code = expressionPane.getCode();
                if (!code.isEmpty()) {
                    return code;
                }
            }
            return null;
        }

        @Override
        void updateState() {
            setEnabled(getMaskForm().isInManagementMode());
        }

    }

    private static class NewVectorDataNodeAction extends MaskAction {

        private CreateVectorDataNodeAction action;

        private NewVectorDataNodeAction(MaskForm maskForm) {
            super(maskForm,
                  "NewVectorDataNode24.gif",
                  "newGeometry",
                  "Creates a new mask based on a new geometry container (lines and polygons))");
            action = new CreateVectorDataNodeAction();
        }

        @Override
        void updateState() {
            action.updateState();
            setEnabled(action.isEnabled());
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            action.run();
        }
    }

    private static class NewIntersectionAction extends BandMathsAction {

        private NewIntersectionAction(MaskForm maskForm) {
            super(maskForm, "Intersection24.png", "intersectionButton",
                  "Creates the intersection of the selected masks");
        }

        @Override
        String getCode(ActionEvent e) {
            return createCodeFromSelection("&&");
        }

        @Override
        void updateState() {
            setEnabled(getMaskForm().isInManagementMode() && getMaskForm().getSelectedRowCount() > 1);
        }
    }

    private static class NewComplementAction extends BandMathsAction {

        private NewComplementAction(MaskForm maskForm) {
            super(maskForm, "Complement24.png", "complementButton",
                  "Creates the complement (of the union) of the selected mask(s)");
        }

        @Override
        String getCode(ActionEvent e) {
            Mask[] selectedMasks = getMaskForm().getSelectedMasks();
            StringBuilder code = new StringBuilder();
            code.append("!(");
            code.append(createCodeFromSelection("||", selectedMasks, 0));
            code.append(")");

            return code.toString();
        }

        @Override
        void updateState() {
            setEnabled(getMaskForm().isInManagementMode() && getMaskForm().getSelectedRowCount() >= 1);
        }
    }

    private static class NewRangeAction extends MaskAction {

        private NewRangeAction(MaskForm maskForm) {
            super(maskForm, "Range24.png", "rangeButton",
                  "Creates a new mask based on a value range");
        }

        @Override
        void updateState() {
            setEnabled(getMaskForm().isInManagementMode());
        }


        @Override
        public void actionPerformed(ActionEvent e) {
            final Product product = getMaskForm().getProduct();
            final String[] rasterNames = StringUtils.addArrays(product.getBandNames(),
                                                               product.getTiePointGridNames());

            final RangeEditorDialog.Model model = new RangeEditorDialog.Model(rasterNames);
            model.setMinValue(0.0);
            model.setMaxValue(1.0);
            model.setRasterName(rasterNames[0]);

            final RangeEditorDialog rangeEditorDialog = new RangeEditorDialog(getWindow(e), model);
            if (rangeEditorDialog.show() == AbstractDialog.ID_OK) {
                final Mask mask = createNewMask(Mask.RangeType.INSTANCE);
                final String externalName = Tokenizer.createExternalName(model.getRasterName());
                mask.setDescription(model.getMinValue() + " <= " + externalName + " <= " + model.getMaxValue());

                final PropertyContainer imageConfig = mask.getImageConfig();
                imageConfig.setValue(Mask.RangeType.PROPERTY_NAME_MINIMUM, model.getMinValue());
                imageConfig.setValue(Mask.RangeType.PROPERTY_NAME_MAXIMUM, model.getMaxValue());
                imageConfig.setValue(Mask.RangeType.PROPERTY_NAME_RASTER, externalName);
                imageConfig.addPropertyChangeListener(new PropertyChangeListener() {
                    @Override
                    public void propertyChange(PropertyChangeEvent evt) {
                        final String oldText = evt.getOldValue().toString();
                        final String newText = evt.getNewValue().toString();
                        mask.setDescription(mask.getDescription().replace(oldText, newText));
                    }
                });
                getMaskForm().addMask(mask);
            }
        }

    }

    private static class NewDifferenceAction extends BandMathsAction {

        private NewDifferenceAction(MaskForm maskForm) {
            super(maskForm, "Difference24.png", "differenceButton",
                  "Creates the difference of the selected masks (in top-down order)");
        }

        @Override
        String getCode(ActionEvent e) {
            Mask[] selectedMasks = getMaskForm().getSelectedMasks();
            StringBuilder code = new StringBuilder();
            code.append(BandArithmetic.createExternalName(selectedMasks[0].getName()));
            if (selectedMasks.length > 1) {
                code.append(" && !(");
                code.append(createCodeFromSelection("||", selectedMasks, 1));
                code.append(")");
            }
            return code.toString();
        }

        @Override
        void updateState() {
            setEnabled(getMaskForm().isInManagementMode() && getMaskForm().getSelectedRowCount() > 1);
        }

    }

    private static class NewInvDifferenceAction extends BandMathsAction {

        private NewInvDifferenceAction(MaskForm maskForm) {
            super(maskForm, "InvDifference24.png", "invDifferenceButton",
                  "Creates the difference of the selected masks (in bottom-up order)");
        }

        @Override
        String getCode(ActionEvent e) {
            Mask[] selectedMasks = getMaskForm().getSelectedMasks();
            final List<Mask> reverseList = new ArrayList<Mask>(Arrays.asList(selectedMasks));
            Collections.reverse(reverseList);
            selectedMasks = reverseList.toArray(new Mask[selectedMasks.length]);
            StringBuilder code = new StringBuilder();
            code.append(BandArithmetic.createExternalName(selectedMasks[0].getName()));
            if (selectedMasks.length > 1) {
                code.append(" && !(");
                code.append(createCodeFromSelection("||", selectedMasks, 1));
                code.append(")");
            }
            return code.toString();
        }

        @Override
        void updateState() {
            setEnabled(getMaskForm().isInManagementMode() && getMaskForm().getSelectedRowCount() > 1);
        }

    }

    private static class NewUnionAction extends BandMathsAction {

        private NewUnionAction(MaskForm maskForm) {
            super(maskForm, "Union24.png", "unionButton",
                  "Creates the union of the selected masks");
        }

        @Override
        String getCode(ActionEvent e) {
            return createCodeFromSelection("||");
        }

        @Override
        void updateState() {
            setEnabled(getMaskForm().isInManagementMode() && getMaskForm().getSelectedRowCount() > 1);
        }
    }

    private static class NullAction extends MaskAction {

        private NullAction(MaskForm maskForm) {
            super(maskForm, "", "", "");
        }

        @Override
        JComponent createComponent() {
            return new JPanel();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
        }
    }

    private static class RemoveAction extends MaskAction {

        private RemoveAction(MaskForm maskForm) {
            super(maskForm, "icons/Remove24.gif", "removeButton", "Remove the selected mask.");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Mask[] selectedMasks = getMaskForm().getSelectedMasks();
            getMaskForm().getMaskTable().clearSelection();
            RasterDataNodeDeleter.deleteRasterDataNodes(selectedMasks);
        }

        @Override
        void updateState() {
            setEnabled(getMaskForm().isInManagementMode() && getMaskForm().getSelectedRowCount() > 0);
        }
    }

    private static class ImportAction extends MaskIOAction {

        private ImportAction(AbstractToolView maskToolView, MaskForm maskForm) {
            super(maskToolView, maskForm, "icons/Import24.gif", "importButton", "Import masks from file.");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            importMasks();
        }

        @Override
        void updateState() {
            setEnabled(getMaskForm().isInManagementMode());
        }

        private void importMasks() {
            final JFileChooser fileChooser = new BeamFileChooser();
            fileChooser.setDialogTitle("Import Masks from file");
            final FileFilter bmdFilter = new BeamFileFilter("BITMASK_DEFINITION_FILE", ".bmd",
                                                            "Bitmask definition files (*.bmd)");
            fileChooser.addChoosableFileFilter(bmdFilter);
            final FileFilter bmdxFilter = new BeamFileFilter("BITMASK_DEFINITION_FILE_XML", ".bmdx",
                                                             "Bitmask definition xml files (*.bmdx)");
            fileChooser.addChoosableFileFilter(bmdxFilter);
            final FileFilter xmlFilter = new BeamFileFilter("XML", ".xml", "XML files (*.xml)");
            fileChooser.setFileFilter(xmlFilter);
            fileChooser.setCurrentDirectory(getDirectory());

            if (fileChooser.showOpenDialog(getMaskToolView().getPaneWindow()) == JFileChooser.APPROVE_OPTION) {
                final File file = fileChooser.getSelectedFile();
                if (file != null) {
                    setDirectory(file.getAbsoluteFile().getParentFile());
                    if (file.canRead()) {
                        if (bmdFilter.accept(file)) {
                            importMaskFromBmd(file);
                        } else if (bmdxFilter.accept(file)) {
                            importMasksFromBmdx(file);
                        } else {
                            importMasksFromXml(file);
                        }

                    }
                }
            }
        }

        private void importMaskFromBmd(File file) {
            final PropertyMap propertyMap = new PropertyMap();
            try {
                propertyMap.load(file); // Overwrite existing values
                final String name = propertyMap.getPropertyString("bitmaskName", "bitmask");
                final String description = propertyMap.getPropertyString("bitmaskDesc", null);
                final String expression = propertyMap.getPropertyString("bitmaskExpr", "");
                final Color color = propertyMap.getPropertyColor("bitmaskColor", Color.yellow);
                final float transparency = (float) propertyMap.getPropertyDouble("bitmaskTransparency", 0.5);
                final BitmaskDef def = new BitmaskDef(name, description, expression, color, transparency);
                final Product product = getMaskForm().getProduct();
                final Mask mask = def.createMask(product.getSceneRasterWidth(), product.getSceneRasterHeight());
                addMaskToProductIfPossible(mask, product);
            } catch (Exception e) {
                showErrorDialog(String.format("Failed to import mask(s): %s", e.getMessage()));
            }
        }

        private void importMasksFromBmdx(File file) {
            try {
                final SAXBuilder saxBuilder = new SAXBuilder();
                final Document document = saxBuilder.build(file);
                final Element rootElement = document.getRootElement();
                @SuppressWarnings({"unchecked"})
                final List<Element> children = rootElement.getChildren(DimapProductConstants.TAG_BITMASK_DEFINITION);
                final Product product = getMaskForm().getProduct();
                for (Element element : children) {
                    final BitmaskDef def = BitmaskDef.createBitmaskDef(element);
                    if (def != null) {
                        final Mask mask = def.createMask(product.getSceneRasterWidth(), product.getSceneRasterHeight());
                        addMaskToProductIfPossible(mask, product);
                    }
                }
            } catch (Exception e) {
                showErrorDialog(String.format("Failed to import mask(s): %s", e.getMessage()));
            }
        }

        private void importMasksFromXml(File file) {
            try {
                final SAXBuilder saxBuilder = new SAXBuilder();
                final Document document = saxBuilder.build(file);
                final Element rootElement = document.getRootElement();
                @SuppressWarnings({"unchecked"})
                final List<Element> children = rootElement.getChildren(DimapProductConstants.TAG_MASK);
                final Product product = getMaskForm().getProduct();
                for (final Element child : children) {
                    final DimapPersistable persistable = DimapPersistence.getPersistable(child);
                    if (persistable != null) {
                        final Mask mask = (Mask) persistable.createObjectFromXml(child, product);
                        addMaskToProductIfPossible(mask, product);
                    }
                }
            } catch (Exception e) {
                showErrorDialog(String.format("Failed to import mask(s): %s", e.getMessage()));
            }
        }

        private void addMaskToProductIfPossible(Mask mask, Product product) throws Exception {
            if (mask.getImageType().canTransferMask(mask, product)) {
                product.getMaskGroup().add(mask);
            } else {
                throw new Exception(String.format("Cannot add mask '%s' to selected product.", mask.getName()));
            }
        }
    }

    private static class ExportAction extends MaskIOAction {

        private ExportAction(AbstractToolView maskToolView, MaskForm maskForm) {
            super(maskToolView, maskForm, "icons/Export24.gif", "exportButton", "Export masks to file.");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            exportSelectedMasks();
        }

        @Override
        void updateState() {
            setEnabled(getMaskForm().isInManagementMode() && getMaskForm().getSelectedRowCount() > 0);
        }

        private void exportSelectedMasks() {
            final Mask[] masks = getMaskForm().getSelectedMasks();
            if (masks.length == 0) {
                return;
            }
            final JFileChooser fileChooser = new BeamFileChooser();
            fileChooser.setDialogTitle("Export Masks to XML");
            final FileFilter fileFilter = new BeamFileFilter("XML", ".xml", "XML files (*.xml)");
            fileChooser.setFileFilter(fileFilter);
            final File targetDirectory = getDirectory();
            fileChooser.setCurrentDirectory(targetDirectory);
            fileChooser.setSelectedFile(new File(targetDirectory, masks[0].getName()));
            final int result = fileChooser.showSaveDialog(getMaskToolView().getPaneWindow());
            if (result == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                if (file != null) {
                    if (!VisatApp.getApp().promptForOverwrite(file)) {
                        return;
                    }
                    setDirectory(file.getAbsoluteFile().getParentFile());
                    file = FileUtils.ensureExtension(file, ".xml");

                    final Document document = new Document(new Element(DimapProductConstants.TAG_MASKS));
                    for (final Mask mask : masks) {
                        final DimapPersistable persistable = DimapPersistence.getPersistable(mask);
                        if (persistable != null) {
                            final Element element = persistable.createXmlFromObject(mask);
                            document.getRootElement().addContent(element);
                        }
                    }

                    FileWriter writer = null;
                    try {
                        writer = new FileWriter(file);
                        final Format format = Format.getPrettyFormat();
                        final XMLOutputter outputter = new XMLOutputter(format);
                        outputter.output(document, writer);
                    } catch (IOException e) {
                        showErrorDialog("Failed to export mask(s): " + e.getMessage());
                    } finally {
                        if (writer != null) {
                            try {
                                writer.close();
                            } catch (IOException e) {
                                // ignore
                            }
                        }
                    }
                }
            }
        }
    }

    static Window getWindow(ActionEvent e) {
        Object source = e.getSource();
        Window window = null;
        if (source instanceof Component) {
            Component component = (Component) source;
            window = SwingUtilities.getWindowAncestor(component);
        }
        return window;
    }

    private static class EditAction extends MaskAction {

        private EditAction(MaskForm maskForm) {
            super(maskForm, "icons/Edit24.gif", "editButton", "Edit the selected mask.");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Window window = getWindow(e);
            Mask selectedMask = getMaskForm().getSelectedMask();
            PropertyContainer selectedMaskConfig = selectedMask.getImageConfig();
            Mask.ImageType type = selectedMask.getImageType();
            if (type == Mask.BandMathsType.INSTANCE) {
                Product product = getMaskForm().getProduct();
                ProductExpressionPane expressionPane = ProductExpressionPane.createBooleanExpressionPane(
                        new Product[]{product}, product, null);
                expressionPane.setEmptyExpressionAllowed(false);
                expressionPane.setCode((String) selectedMaskConfig.getValue("expression"));
                if (expressionPane.showModalDialog(window, "Edit Band Maths Mask") == AbstractDialog.ID_OK) {
                    String code = expressionPane.getCode();
                    selectedMaskConfig.setValue("expression", code);
                    selectedMask.setDescription(code);
                }
            } else if (type == Mask.RangeType.INSTANCE) {
                final Product product = getMaskForm().getProduct();
                final String[] rasterNames = StringUtils.addArrays(product.getBandNames(),
                                                                   product.getTiePointGridNames());
                final RangeEditorDialog.Model model = new RangeEditorDialog.Model(rasterNames);
                model.setMinValue((Double) selectedMaskConfig.getValue(Mask.RangeType.PROPERTY_NAME_MINIMUM));
                model.setMaxValue((Double) selectedMaskConfig.getValue(Mask.RangeType.PROPERTY_NAME_MAXIMUM));
                model.setRasterName((String) selectedMaskConfig.getValue(Mask.RangeType.PROPERTY_NAME_RASTER));
                final RangeEditorDialog rangeEditorDialog = new RangeEditorDialog(window, model);
                if (rangeEditorDialog.show() == AbstractDialog.ID_OK) {
                    final String description = String.format("%s <= %s <= %s",
                                                             model.getMinValue(), model.getRasterName(),
                                                             model.getMaxValue());
                    selectedMask.setDescription(description);
                    selectedMaskConfig.setValue(Mask.RangeType.PROPERTY_NAME_MINIMUM, model.getMinValue());
                    selectedMaskConfig.setValue(Mask.RangeType.PROPERTY_NAME_MAXIMUM, model.getMaxValue());
                    selectedMaskConfig.setValue(Mask.RangeType.PROPERTY_NAME_RASTER, model.getRasterName());
                }
            } else if (type == Mask.VectorDataType.INSTANCE) {
                JOptionPane.showMessageDialog(window,
                                              "Use the VISAT geometry tools to add new points, lines or polygons.\n" +
                                              "You can then use the select tool to select and modify the shape\n" +
                                              "and position of the geometries.",
                                              "Edit Geometry Mask",
                                              JOptionPane.INFORMATION_MESSAGE);
            } else {
                // todo - implement for other types too

            }
        }

        @Override
        void updateState() {
            setEnabled(getMaskForm().isInManagementMode() && getMaskForm().getSelectedRowCount() == 1);
        }
    }

    private static class CopyAction extends MaskAction {

        private CopyAction(MaskForm maskForm) {
            super(maskForm, "icons/Copy24.gif", "copyButton", "Copy the selected mask.");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Mask selectedMask = getMaskForm().getSelectedMask();
            final Mask mask = createNewMask(selectedMask.getImageType());
            mask.setName("Copy_of_" + selectedMask.getName());
            mask.setDescription(selectedMask.getDescription());
            PropertyContainer selectedConfig = selectedMask.getImageConfig();
            Property[] models = selectedConfig.getProperties();
            for (Property model : models) {
                mask.getImageConfig().setValue(model.getDescriptor().getName(),
                                               model.getValue());
            }
            getMaskForm().addMask(mask);
        }

        @Override
        void updateState() {
            setEnabled(getMaskForm().isInManagementMode() && getMaskForm().getSelectedRowCount() == 1);
        }
    }

    private abstract static class BandMathsAction extends MaskAction {

        BandMathsAction(MaskForm maskForm, String iconPath, String buttonName, String description) {
            super(maskForm, iconPath, buttonName, description);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            String code = getCode(e);
            if (code != null) {
                addBandMathMask(code);
            }
        }

        abstract String getCode(ActionEvent e);

        String createCodeFromSelection(String op) {
            return createCodeFromSelection(op, 0);
        }

        String createCodeFromSelection(String op, int selectionOffset) {
            return createCodeFromSelection(op, getMaskForm().getSelectedMasks(), selectionOffset);
        }

        String createCodeFromSelection(String op, Mask[] selectedMasks, int selectionOffset) {
            StringBuilder code = new StringBuilder();
            for (int i = selectionOffset; i < selectedMasks.length; i++) {
                Mask mask = selectedMasks[i];
                if (code.length() > 0) {
                    code.append(" ");
                    code.append(op);
                    code.append(" ");
                }
                code.append(BandArithmetic.createExternalName(mask.getName()));
            }
            return code.toString();
        }


        void addBandMathMask(String code) {
            final Mask mask = createNewMask(Mask.BandMathsType.INSTANCE);
            final PropertyContainer imageConfig = mask.getImageConfig();
            final String propertyNameExpression = Mask.BandMathsType.PROPERTY_NAME_EXPRESSION;
            imageConfig.setValue(propertyNameExpression, code);
            imageConfig.addPropertyChangeListener(propertyNameExpression, new PropertyChangeListener() {
                @Override
                public void propertyChange(PropertyChangeEvent evt) {
                    if (evt.getOldValue().equals(mask.getDescription())) {
                        mask.setDescription((String) evt.getNewValue());
                    } else {
                        // description my have been set by user, ignore change
                    }
                }
            });
            mask.setDescription(code);
            getMaskForm().addMask(mask);
        }
    }

    private static class TransferAction extends MaskAction {

        private TransferAction(MaskForm maskForm) {
            super(maskForm, "icons/MultiAssignProducts24.gif", "transferButton",
                  "Transfer the selected mask(s) to other products.");
        }

        @Override
        void updateState() {
            setEnabled(getMaskForm().isInManagementMode() &&
                       getMaskForm().getSelectedRowCount() > 0 &&
                       VisatApp.getApp().getProductManager().getProducts().length > 1);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Window window = getWindow(e);
            final Product sourcProduct = getMaskForm().getProduct();
            Mask[] selectedMasks = getMaskForm().getSelectedMasks();
            Product[] allProducts = VisatApp.getApp().getProductManager().getProducts();
            final TransferMaskDialog dialog = new TransferMaskDialog(window, sourcProduct, allProducts, selectedMasks);
            if (dialog.show() == AbstractDialog.ID_OK) {
                Product[] maskPixelTargetProducts = dialog.getMaskPixelTargets();
                copyMaskPixel(selectedMasks, sourcProduct, maskPixelTargetProducts);

                Product[] maskDefinitionTargetProducts = dialog.getMaskDefinitionTargets();
                copyMaskDefinition(selectedMasks, maskDefinitionTargetProducts);
            }
        }

        private static void copyMaskDefinition(Mask[] selectedMasks, Product[] maskPixelTargetProducts) {
            for (Product targetProduct : maskPixelTargetProducts) {
                for (Mask selectedMask : selectedMasks) {
                    ImageType imageType = selectedMask.getImageType();
                    if (imageType.canTransferMask(selectedMask, targetProduct)) {
                        imageType.transferMask(selectedMask, targetProduct);
                    }
                }
            }
        }

        private static void copyMaskPixel(Mask[] selectedMasks, Product sourcProduct,
                                          Product[] maskPixelTargetProducts) {
            for (Product targetProduct : maskPixelTargetProducts) {
                if (sourcProduct.isCompatibleProduct(targetProduct, 1.0e-3f)) {
                    copyBandData(selectedMasks, targetProduct);
                } else {
                    reprojectBandData(selectedMasks, sourcProduct, targetProduct);
                }
            }
        }

        private static void copyBandData(Mask[] selectedMasks, Product targetProduct) {
            for (Mask mask : selectedMasks) {
                Band band = createBandCopy(targetProduct, mask);
                band.setSourceImage(mask.getSourceImage());
            }
        }


        private static void reprojectBandData(Mask[] selectedMasks, Product sourceProduct, Product targetProduct) {
            final Map<String, Object> projParameters = Collections.EMPTY_MAP;
            Map<String, Product> projProducts = new HashMap<String, Product>();
            projProducts.put("source", sourceProduct);
            projProducts.put("collocateWith", targetProduct);
            Product reprojectedProduct = GPF.createProduct("Reproject", projParameters, projProducts);

            for (Mask mask : selectedMasks) {
                Band band = createBandCopy(targetProduct, mask);
                MultiLevelImage image = reprojectedProduct.getMaskGroup().get(mask.getName()).getSourceImage();
                band.setSourceImage(image);
            }
        }

        private static Band createBandCopy(Product targetProduct, Mask mask) {
            String bandName = getAvaliableBandName("mask_" + mask.getName(), targetProduct);
            String maskName = getAvailableMaskName(mask.getName(), targetProduct.getMaskGroup());
            int dataType = mask.getDataType();
            Band band = targetProduct.addBand(bandName, dataType);
            String description = mask.getDescription() + " (from " + mask.getProduct().getDisplayName() + ")";
            targetProduct.addMask(maskName, description, bandName, mask.getImageColor(), mask.getImageTransparency());
            return band;
        }

        private static String getAvailableMaskName(String name, ProductNodeGroup<Mask> maskGroup) {
            int index = 1;
            String foundName = name;
            while (maskGroup.contains(foundName)) {
                foundName = name + "_" + index;
            }
            return foundName;
        }

        private static String getAvaliableBandName(String name, Product product) {
            int index = 1;
            String foundName = name;
            while (product.containsBand(foundName)) {
                foundName = name + "_" + index;
            }
            return foundName;
        }
    }

    private static class ZoomToVectorMaskAction extends MaskAction {

        private final AbstractToolView toolView;

        private ZoomToVectorMaskAction(AbstractToolView toolView, MaskForm maskForm) {
            super(maskForm, "icons/ZoomTo24.gif", "zoomToButton",
                  "Zooms to the selected mask.");
            this.toolView = toolView;
        }

        @Override
        void updateState() {
            setEnabled(getMaskForm().getSelectedRowCount() == 1 &&
                       VisatApp.getApp().getSelectedProductSceneView() != null);

        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Mask mask = getMaskForm().getSelectedMask();
            ImageType imageType = mask.getImageType();
            ProductSceneView productSceneView = VisatApp.getApp().getSelectedProductSceneView();
            if (productSceneView != null) {
                Rectangle2D modelBounds;
                if (imageType == Mask.VectorDataType.INSTANCE) {
                    modelBounds = handleVectorMask(mask);
                } else {
                    modelBounds = handleImageMask(mask,
                                                  productSceneView.getBaseImageLayer().getImageToModelTransform());
                }
                if (modelBounds != null) {
                    Viewport viewport = productSceneView.getViewport();
                    final AffineTransform m2vTransform = viewport.getModelToViewTransform();
                    final AffineTransform v2mTransform = viewport.getViewToModelTransform();
                    final Rectangle2D viewBounds = m2vTransform.createTransformedShape(modelBounds).getBounds2D();
                    viewBounds.setFrameFromDiagonal(viewBounds.getMinX() - 10, viewBounds.getMinY() - 10,
                                                    viewBounds.getMaxX() + 10, viewBounds.getMaxY() + 10);
                    final Shape transformedModelBounds = v2mTransform.createTransformedShape(viewBounds);
                    viewport.zoom(transformedModelBounds.getBounds2D());
                } else {
                    JOptionPane.showMessageDialog(toolView.getPaneWindow(),
                                                  "The selected mask is empty.",
                                                  "Zoom to Mask",
                                                  JOptionPane.INFORMATION_MESSAGE);
                }
            }
        }


        private static Rectangle2D handleVectorMask(Mask mask) {
            VectorDataNode vectorData = Mask.VectorDataType.getVectorData(mask);
            ReferencedEnvelope envelope = vectorData.getEnvelope();
            if (!envelope.isEmpty()) {
                return new Rectangle2D.Double(envelope.getMinX(), envelope.getMinY(),
                                              envelope.getWidth(), envelope.getHeight());
            }
            return null;
        }

        private Rectangle2D handleImageMask(Mask mask, AffineTransform i2m) {
            RenderedImage image = mask.getSourceImage().getImage(0);
            final int minTileX = image.getMinTileX();
            final int minTileY = image.getMinTileY();
            final int numXTiles = image.getNumXTiles();
            final int numYTiles = image.getNumYTiles();
            final int width = image.getWidth();
            final int height = image.getHeight();
            int minX = width;
            int maxX = 0;
            int minY = height;
            int maxY = 0;

            for (int tileX = minTileX; tileX < minTileX + numXTiles; ++tileX) {
                for (int tileY = minTileY; tileY < minTileY + numYTiles; ++tileY) {
                    final Raster data = image.getTile(tileX, tileY);
                    for (int x = data.getMinX(); x < data.getMinX() + data.getWidth(); x++) {
                        for (int y = data.getMinY(); y < data.getMinY() + data.getHeight(); y++) {
                            if (data.getSample(x, y, 0) != 0) {
                                minX = Math.min(x, minX);
                                maxX = Math.max(x, maxX);
                                minY = Math.min(y, minY);
                                maxY = Math.max(y, maxY);
                            }
                        }
                    }
                }
            }
            Rectangle rect = new Rectangle(minX, minY, maxX - minX + 1, maxY - minY + 1);
            if (rect.isEmpty()) {
                return null;
            } else {
                return i2m.createTransformedShape(rect).getBounds2D();
            }
        }
    }
}
