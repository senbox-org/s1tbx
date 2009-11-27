package org.esa.beam.visat.toolviews.mask;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.jexp.impl.Tokenizer;
import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.dataop.barithm.BandArithmetic;
import org.esa.beam.framework.ui.AbstractDialog;
import org.esa.beam.framework.ui.product.ProductExpressionPane;
import org.esa.beam.util.StringUtils;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * @author Marco Peters
 * @version $ Revision $ Date $
 * @since BEAM 4.7
 */
class MaskFormActions {

    private MaskAction[] maskActions;

    MaskFormActions(MaskForm maskForm) {
        maskActions = new MaskAction[]{
                new NewBandMathAction(maskForm), new NewRangeAction(maskForm),
                new NewUnionAction(maskForm), new NewIntersectionAction(maskForm),
                new NewDifferenceAction(maskForm), new NewComplementAction(maskForm),
                new CopyAction(maskForm), new NullAction(maskForm),
                new EditAction(maskForm), new RemoveAction(maskForm),
                new ImportAction(maskForm), new ExportAction(maskForm),
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
            super(maskForm, "BandMath24.png", "bandMathButton", "Creates a new mask based on a band math expression");
        }

        @Override
        String getCode(ActionEvent e) {
            Product product = getMaskForm().getProduct();
            ProductExpressionPane expressionPane = ProductExpressionPane.createBooleanExpressionPane(
                    new Product[]{product}, product, null);
            expressionPane.setEmptyExpressionAllowed(false);
            expressionPane.setCode("");
            if (expressionPane.showModalDialog(null, "New Band-Math Mask") == AbstractDialog.ID_OK) {
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

            final RangeEditorDialog rangeEditorDialog = new RangeEditorDialog(model);
            if (rangeEditorDialog.show() == AbstractDialog.ID_OK) {
                Mask.RangeType type = new Mask.RangeType();

                final Mask mask = createNewMask(type);
                final String externalName = Tokenizer.createExternalName(model.getRasterName());
                mask.setDescription(model.getMinValue() + " < " + externalName + " < " + model.getMaxValue());

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
                  "Creates the difference of the selected masks");
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
            super(maskForm, "icons/Remove24.gif", "editButton", "Edit the selected mask.");
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

    private static class ImportAction extends MaskAction {

        private ImportAction(MaskForm maskForm) {
            super(maskForm, "icons/Import24.gif", "importButton", "Import masks from file.");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            // todo - implement
        }

        @Override
        void updateState() {
            setEnabled(getMaskForm().isInManagementMode());
        }
    }

    private static class ExportAction extends MaskAction {

        private ExportAction(MaskForm maskForm) {
            super(maskForm, "icons/Export24.gif", "exportButton", "Export masks from file.");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            // todo - implement
        }

        @Override
        void updateState() {
            setEnabled(getMaskForm().isInManagementMode()
                       && getMaskForm().getSelectedRowCount() > 0);
        }
    }

    private static class EditAction extends MaskAction {

        private EditAction(MaskForm maskForm) {
            super(maskForm, "icons/Edit24.gif", "editButton", "Edit the selected mask.");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Mask selectedMask = getMaskForm().getSelectedMask();
            PropertyContainer selectedMaskConfig = selectedMask.getImageConfig();
            Mask.ImageType type = selectedMask.getImageType();
            if (type instanceof Mask.BandMathType) {
                Product product = getMaskForm().getProduct();
                ProductExpressionPane expressionPane = ProductExpressionPane.createBooleanExpressionPane(
                        new Product[]{product}, product, null);
                expressionPane.setEmptyExpressionAllowed(false);
                expressionPane.setCode((String) selectedMaskConfig.getValue("expression"));
                if (expressionPane.showModalDialog(null, "Edit Band-Math Mask") == AbstractDialog.ID_OK) {
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
                final RangeEditorDialog rangeEditorDialog = new RangeEditorDialog(model);
                if (rangeEditorDialog.show() == AbstractDialog.ID_OK) {
                    final String description = String.format("%s < %s < %s",
                                                             model.getMinValue(), model.getRasterName(),
                                                             model.getMaxValue());
                    selectedMask.setDescription(description);
                    selectedMaskConfig.setValue(Mask.RangeType.PROPERTY_NAME_MINIMUM, model.getMinValue());
                    selectedMaskConfig.setValue(Mask.RangeType.PROPERTY_NAME_MAXIMUM, model.getMaxValue());
                    selectedMaskConfig.setValue(Mask.RangeType.PROPERTY_NAME_RASTER, model.getRasterName());
                }
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
            addNewMaskCopy();
        }

        @Override
        void updateState() {
            setEnabled(getMaskForm().isInManagementMode() && getMaskForm().getSelectedRowCount() == 1);
        }


        private void addNewMaskCopy() {
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
}
