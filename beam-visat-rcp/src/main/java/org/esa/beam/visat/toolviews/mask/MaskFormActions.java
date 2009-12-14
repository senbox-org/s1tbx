package org.esa.beam.visat.toolviews.mask;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.glevel.MultiLevelImage;
import com.bc.jexp.impl.Tokenizer;
import org.esa.beam.dataio.dimap.DimapProductConstants;
import org.esa.beam.dataio.dimap.spi.DimapPersistable;
import org.esa.beam.dataio.dimap.spi.DimapPersistence;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.BitmaskDef;
import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.Mask.BandMathType;
import org.esa.beam.framework.datamodel.Mask.ImageType;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNodeGroup;
import org.esa.beam.framework.dataop.barithm.BandArithmetic;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.ui.AbstractDialog;
import org.esa.beam.framework.ui.application.support.AbstractToolView;
import org.esa.beam.framework.ui.product.ProductExpressionPane;
import org.esa.beam.util.PropertyMap;
import org.esa.beam.util.StringUtils;
import org.esa.beam.util.io.BeamFileChooser;
import org.esa.beam.util.io.BeamFileFilter;
import org.esa.beam.util.io.FileUtils;
import org.esa.beam.visat.VisatApp;
import org.esa.beam.visat.actions.NewVectorDataNodeAction;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.JDOMException;
import org.jdom.input.DOMBuilder;
import org.jdom.input.SAXBuilder;
import org.jdom.output.Format;
import org.jdom.output.XMLOutputter;
import org.xml.sax.SAXException;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.SwingUtilities;
import javax.swing.filechooser.FileFilter;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.Color;
import java.awt.Component;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Arrays;
import java.util.ArrayList;

/**
 * @author Marco Peters
 * @version $ Revision $ Date $
 * @since BEAM 4.7
 */
class MaskFormActions {

    private final MaskAction[] maskActions;

    MaskFormActions(AbstractToolView maskToolView, MaskForm maskForm) {
        maskActions = new MaskAction[]{
                new NewBandMathAction(maskForm), new NewRangeAction(maskForm),
                new _NewVectorDataNodeAction(maskForm), new NewUnionAction(maskForm),
                new NewIntersectionAction(maskForm), new NewComplementAction(maskForm),
                new NewDifferenceAction(maskForm), new NewInvDifferenceAction(maskForm),
                new CopyAction(maskForm), new NullAction(maskForm),
                new EditAction(maskForm), new RemoveAction(maskForm),
                new ImportAction(maskToolView, maskForm), new ExportAction(maskToolView, maskForm),
                new TransferAction(maskForm), new NullAction(maskForm)
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
        return getMaskAction(NewBandMathAction.class);
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

    private static class NewBandMathAction extends BandMathAction {

        private NewBandMathAction(MaskForm maskForm) {
            super(maskForm, "BandMath24.png", "bandMathButton", "Creates a new mask based on a logical expression");
        }

        @Override
        String getCode(ActionEvent e) {
            Product product = getMaskForm().getProduct();
            ProductExpressionPane expressionPane = ProductExpressionPane.createBooleanExpressionPane(
                    new Product[]{product}, product, null);
            expressionPane.setEmptyExpressionAllowed(false);
            expressionPane.setCode("");
            if (expressionPane.showModalDialog(null, "New Logical Expression") == AbstractDialog.ID_OK) {
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

    private static class _NewVectorDataNodeAction extends MaskAction {
        private NewVectorDataNodeAction action;

        private _NewVectorDataNodeAction(MaskForm maskForm) {
            super(maskForm,
                  "Geometry24.png",
                  "newGeometry",
                  "Creates a new mask based on geometry (lines and polygons))");
            action = new NewVectorDataNodeAction();
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

    private static class NewIntersectionAction extends BandMathAction {

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

    private static class NewComplementAction extends BandMathAction {

        private NewComplementAction(MaskForm maskForm) {
            super(maskForm, "Complement24.png", "complementButton",
                  "Creates the complement of the union of the selected masks");
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
                Mask.RangeType type = new Mask.RangeType();

                final Mask mask = createNewMask(type);
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

    private static class NewDifferenceAction extends BandMathAction {

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

    private static class NewInvDifferenceAction extends BandMathAction {

        private NewInvDifferenceAction(MaskForm maskForm) {
            super(maskForm, "InvDifference24.png", "invDifferenceButton",
                  "Creates the difference of the selected masks (in bottom-up order)");
        }

        @Override
        String getCode(ActionEvent e) {
            Mask[] selectedMasks = getMaskForm().getSelectedMasks();
            final List<Mask> reverseList = new ArrayList<Mask>(Arrays.asList(selectedMasks));
            Collections.reverse(reverseList);
            selectedMasks =  reverseList.toArray(new Mask[selectedMasks.length]);
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

    private static class NewUnionAction extends BandMathAction {

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
            super(maskForm, "icons/Remove24.gif", "editButton", "Remove the selected mask.");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            // todo - ask user
            Mask[] selectedMasks = getMaskForm().getSelectedMasks();
            for (Mask selectedMask : selectedMasks) {
                getMaskForm().removeMask(selectedMask);
            }
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
            importBitmaskDefs();
        }

        @Override
        void updateState() {
            setEnabled(getMaskForm().isInManagementMode());
        }

        private void importBitmaskDefs() {
            final JFileChooser fileChooser = new BeamFileChooser();
            fileChooser.setDialogTitle("Import Masks from XML");
            final FileFilter beamFF = new BeamFileFilter("BITMASK_DEFINITION_FILE", ".bmd", "Bitmask definition files (*.bmd)");
            fileChooser.addChoosableFileFilter(beamFF);
            final FileFilter beamFFXml = new BeamFileFilter("BITMASK_DEFINITION_FILE_XML", ".bmdx", "Bitmask definition xml files (*.bmdx)");
            fileChooser.addChoosableFileFilter(beamFFXml);
            final FileFilter maskFF = new BeamFileFilter("XML", ".xml", "XML files (*.xml)");
            fileChooser.setFileFilter(maskFF);
            fileChooser.setCurrentDirectory(getDirectory());

            if (fileChooser.showOpenDialog(getMaskToolView().getPaneWindow()) == JFileChooser.APPROVE_OPTION) {
                final File file = fileChooser.getSelectedFile();
                if (file != null) {
                    setDirectory(file.getAbsoluteFile().getParentFile());
                    if (file.canRead()) {
                        if (beamFF.accept(file)) {
                            importBitmaskDef(file);
                        } else if (beamFFXml.accept(file)) {
                            importBitmaskDefs(file);
                        } else {
                            importMasks(file);
                        }

                    }
                }
            }
        }

        private void importBitmaskDef(File file) {
            final PropertyMap propertyMap = new PropertyMap();
            try {
                propertyMap.load(file); // Overwrite existing values
                String name = propertyMap.getPropertyString("bitmaskName", "bitmask");
                final String description = propertyMap.getPropertyString("bitmaskDesc", null);
                final String expr = propertyMap.getPropertyString("bitmaskExpr", "");
                final Color color = propertyMap.getPropertyColor("bitmaskColor", Color.yellow);
                final float transp = (float) propertyMap.getPropertyDouble("bitmaskTransparency", 0.5);
                final BitmaskDef bitmaskDef = new BitmaskDef(name, description, expr, color, transp);
                Product product = getMaskForm().getProduct();
                Mask mask = bitmaskDef.createMask(product.getSceneRasterWidth(), product.getSceneRasterHeight());
                if (mask != null) {
                    getMaskForm().getProduct().getMaskGroup().add(mask);
                }
//                    addOrOverwriteBitmaskDef(bitmaskDef);
            } catch (IOException e) {
                showErrorDialog("I/O Error.\nFailed to import bitmask definition.");        /*I18N*/
            }
        }

        private void importBitmaskDefs(File file) {
            try {
                try {
                    final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
                    final DocumentBuilder builder = factory.newDocumentBuilder();
                    final org.w3c.dom.Document w3cDocument = builder.parse(file);
                    final Document document = new DOMBuilder().build(w3cDocument);
                    final Element rootElement = document.getRootElement();
                    final List children = rootElement.getChildren(DimapProductConstants.TAG_BITMASK_DEFINITION);
                    Product product = getMaskForm().getProduct();
                    if (children != null) {
                        for (Object aChildren : children) {
                            final Element element = (Element) aChildren;
                            final BitmaskDef bitmaskDef = BitmaskDef.createBitmaskDef(element);
                            if (bitmaskDef != null) {
                                Mask mask = bitmaskDef.createMask(product.getSceneRasterWidth(), product.getSceneRasterHeight());
                                if (mask != null) {
                                    getMaskForm().getProduct().getMaskGroup().add(mask);
                                }
//                                addOrOverwriteBitmaskDef(bitmaskDef);
                            }
                        }
                    }
                } catch (FactoryConfigurationError error) {
                    throw new IOException(error.toString());
                } catch (ParserConfigurationException e) {
                    throw new IOException(e.toString());
                } catch (SAXException e) {
                    throw new IOException(e.toString());
                } catch (IOException e) {
                    throw new IOException(e.toString());
                }
            } catch (IOException e) {
                showErrorDialog("I/O Error.\nFailed to import bitmask definition.");        /*I18N*/
            }
        }

        private void importMasks(File file) {
            try {
                final SAXBuilder saxBuilder = new SAXBuilder();
                final Document document = saxBuilder.build(file);
                final Element rootElement = document.getRootElement();
                @SuppressWarnings({"unchecked"})
                final List<Element> children = rootElement.getChildren(DimapProductConstants.TAG_MASK);
                for (final Element child : children) {
                    final DimapPersistable persistable = DimapPersistence.getPersistable(child);
                    if (persistable != null) {
                        final Product product = getMaskForm().getProduct();
                        final Mask mask = (Mask) persistable.createObjectFromXml(child, product);
                        product.getMaskGroup().add(mask);
                    }
                }
            } catch (IOException e) {
                showErrorDialog(String.format("Failed to import mask(s): %s", e.getMessage()));
            } catch (JDOMException e) {
                showErrorDialog(String.format("Failed to import mask(s): %s", e.getMessage()));
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
            if (type instanceof Mask.BandMathType) {
                Product product = getMaskForm().getProduct();
                ProductExpressionPane expressionPane = ProductExpressionPane.createBooleanExpressionPane(
                        new Product[]{product}, product, null);
                expressionPane.setEmptyExpressionAllowed(false);
                expressionPane.setCode((String) selectedMaskConfig.getValue("expression"));
                if (expressionPane.showModalDialog(window, "Edit Band-Math Mask") == AbstractDialog.ID_OK) {
                    String code = expressionPane.getCode();
                    selectedMaskConfig.setValue("expression", code);
                    selectedMask.setDescription(code);
                }
            } else if (type instanceof Mask.RangeType) {
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
            } else if (type instanceof Mask.VectorDataType) {
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

    private abstract static class BandMathAction extends MaskAction {

        BandMathAction(MaskForm maskForm, String iconPath, String buttonName, String description) {
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
            final Mask mask = createNewMask(new Mask.BandMathType());
            final PropertyContainer imageConfig = mask.getImageConfig();
            imageConfig.setValue("expression", code);
            imageConfig.addPropertyChangeListener("expression", new PropertyChangeListener() {
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
            super(maskForm, "icons/MultiAssignProducts24.gif", "transferButton", "Transfer the selected mask(s) to other products.");
        }

        @Override
        void updateState() {
            setEnabled(getMaskForm().isInManagementMode() &&
                    getMaskForm().getSelectedRowCount() > 0 &&
                    VisatApp.getApp().getProductManager().getProducts().length > 1);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            final Product sourcProduct = getMaskForm().getProduct();
            Mask[] selectedMasks = getMaskForm().getSelectedMasks();
            Product[] allProducts = VisatApp.getApp().getProductManager().getProducts();
            final CopyMaskDialog dialog = new CopyMaskDialog(sourcProduct, allProducts, selectedMasks);
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

        private static void copyMaskPixel(Mask[] selectedMasks, Product sourcProduct, Product[] maskPixelTargetProducts) {
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
            int width = targetProduct.getSceneRasterWidth();
            int height = targetProduct.getSceneRasterHeight();
            Mask targetMask = new Mask(maskName, width, height, new Mask.BandMathType());
            BandMathType.setExpression(targetMask, bandName);
            targetMask.setDescription(mask.getDescription() + " (from " + mask.getProduct().getDisplayName() + ")");
            targetMask.setImageColor(mask.getImageColor());
            targetMask.setImageTransparency(mask.getImageTransparency());
            targetProduct.getMaskGroup().add(targetMask);
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
}
