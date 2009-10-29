/*
 * $Id: BitmaskOverlayToolView.java,v 1.1 2007/04/19 10:41:38 norman Exp $
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
package org.esa.beam.visat.toolviews.mask;

import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.Property;
import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.dataop.barithm.BandArithmetic;
import org.esa.beam.framework.ui.AbstractDialog;
import org.esa.beam.framework.ui.GridBagUtils;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.product.ProductExpressionPane;
import org.esa.beam.framework.ui.tool.ToolButtonFactory;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.net.URL;

class MaskManagerForm extends MaskForm {

    private final AbstractButton helpButton;

    private final MaskAction[] maskActions;

    public MaskManagerForm() {
        super(true);

        maskActions = new MaskAction[]{
                new NewBandMathAction(), new NewRangeAction(),
                new NewUnionAction(), new NewIntersectionAction(),
                new NewSubtractionAction(), new NewInversionAction(),
                new CopyAction(), new NullAction(),
                new EditAction(), new RemoveAction(),
                new ImportAction(), new ExportAction(),
                new MoveUpAction(), new MoveDownAction(),
        };

        helpButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/Help24.gif"), false);
        helpButton.setName("helpButton");

        updateState();
    }

    @Override
    public Action getDoubleClickAction() {
        return getMaskAction(EditAction.class);
    }

    @Override
    public AbstractButton getHelpButton() {
        return helpButton;
    }

    @Override
    public void updateState() {
        for (MaskAction maskAction : maskActions) {
            maskAction.updateState();
        }
    }

    @Override
    public JPanel createContentPanel() {

        JPanel buttonPanel = GridBagUtils.createPanel();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 0;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.5;

        gbc.insets.bottom = 0;
        gbc.gridwidth = 1;

        for (int i = 0; i < maskActions.length; i += 2) {
            buttonPanel.add(maskActions[i].createComponent(), gbc);
            buttonPanel.add(maskActions[i + 1].createComponent(), gbc);
            gbc.gridy++;
        }

        gbc.fill = GridBagConstraints.VERTICAL;
        gbc.weighty = 1.0;
        gbc.gridwidth = 2;
        buttonPanel.add(new JLabel(" "), gbc); // filler
        gbc.fill = GridBagConstraints.NONE;
        gbc.weighty = 0.0;
        gbc.gridx = 1;
        gbc.gridy++;
        gbc.gridwidth = 1;
        buttonPanel.add(helpButton, gbc);

        JPanel tablePanel = new JPanel(new BorderLayout(4, 4));
        tablePanel.add(new JScrollPane(getMaskTable()), BorderLayout.CENTER);

        JPanel contentPane1 = new JPanel(new BorderLayout(4, 4));
        contentPane1.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        contentPane1.add(BorderLayout.CENTER, tablePanel);
        contentPane1.add(BorderLayout.EAST, buttonPanel);

        updateState();

        return contentPane1;
    }

    private MaskAction getMaskAction(Class<?> type) {
        for (MaskAction maskAction : maskActions) {
            if (type.getName().equals(maskAction.getValue(Action.ACTION_COMMAND_KEY))) {
                return maskAction;
            }
        }
        return null;
    }

    public abstract class MaskAction extends AbstractAction {
        MaskAction(String iconPath, String buttonName, String description) {
            putValue(ACTION_COMMAND_KEY, getClass().getName());
            putValue(LARGE_ICON_KEY, loadIcon(iconPath));
            putValue(SHORT_DESCRIPTION, description);
            putValue("componentName", buttonName);
        }

        private ImageIcon loadIcon(String iconPath) {
            final ImageIcon icon;
            URL resource = MaskManagerForm.class.getResource(iconPath);
            if (resource != null) {
                icon = new ImageIcon(resource);
            } else {
                icon = UIUtils.loadImageIcon(iconPath);
            }
            return icon;
        }

        JComponent createComponent() {
            AbstractButton button = ToolButtonFactory.createButton(this, false);
            button.setName((String) getValue("componentName"));
            return button;
        }

        void updateState() {
        }
    }

    public abstract class BandMathAction extends MaskAction {
        BandMathAction(String iconPath, String buttonName, String description) {
            super(iconPath, buttonName, description);
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
            return createCodeFromSelection(op, getSelectedMasks(), selectionOffset);
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
            Mask.BandMathType type = new Mask.BandMathType();
            Mask mask = new Mask("M_" + Long.toHexString(System.nanoTime()),
                                 getProduct().getSceneRasterWidth(),
                                 getProduct().getSceneRasterHeight(),
                                 type);
            mask.getImageConfig().setValue("expression", code);
            mask.setDescription(code);
            addMask(mask);
        }

    }

    public class NewBandMathAction extends BandMathAction {
        public NewBandMathAction() {
            super("BandMath24.png", "bandMathButton", "Creates a new mask based on a band math expression");
        }

        @Override
        String getCode(ActionEvent e) {
            Product product = getProduct();
            ProductExpressionPane expressionPane = ProductExpressionPane.createBooleanExpressionPane(new Product[]{product}, product, null);
            expressionPane.setCode("");
            if (expressionPane.showModalDialog(null, "New Band-Math Mask") == AbstractDialog.ID_OK) {
                return expressionPane.getCode();
            } else {
                return null;
            }
        }

        @Override
        void updateState() {
            setEnabled(isInManagmentMode());
        }

    }

    public class NewRangeAction extends BandMathAction {
        public NewRangeAction() {
            super("Range24.png", "rangeButton",
                  "Creates a new mask based on a value range");
        }

        @Override
        String getCode(ActionEvent e) {
            // todo - implement
            return null;
        }

        @Override
        void updateState() {
            setEnabled(isInManagmentMode());
        }
    }

    public class NewUnionAction extends BandMathAction {
        public NewUnionAction() {
            super("Union24.png", "unionButton",
                  "Creates the union of the selected masks");
        }

        @Override
        String getCode(ActionEvent e) {
            return createCodeFromSelection("||");
        }

        @Override
        void updateState() {
            setEnabled(isInManagmentMode() && getSelectedRowCount() > 1);
        }
    }

    public class NewIntersectionAction extends BandMathAction {
        public NewIntersectionAction() {
            super("Intersect24.png", "intersectionButton",
                  "Creates the intersection of the selected masks");
        }

        @Override
        String getCode(ActionEvent e) {
            return createCodeFromSelection("&&");
        }

        @Override
        void updateState() {
            setEnabled(isInManagmentMode()
                    && getSelectedRowCount() > 1);
        }
    }

    public class NewSubtractionAction extends BandMathAction {
        public NewSubtractionAction() {
            super("Subtract24.png", "subtractButton",
                  "Creates the subtraction of the selected masks");
        }

        @Override
        String getCode(ActionEvent e) {
            Mask[] selectedMasks = getSelectedMasks();
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
            setEnabled(isInManagmentMode()
                    && getSelectedRowCount() > 1);
        }

    }

    public class NewInversionAction extends BandMathAction {
        public NewInversionAction() {
            super("Invert24.png", "intersectionButton",
                  "Creates the inversion of subtraction of the selected masks");
        }

        @Override
        String getCode(ActionEvent e) {
            Mask[] selectedMasks = getSelectedMasks();
            StringBuilder code = new StringBuilder();
            code.append("!");
            code.append(BandArithmetic.createExternalName(selectedMasks[0].getName()));
            if (selectedMasks.length > 1) {
                code.append(" || ");
                code.append(createCodeFromSelection("||", selectedMasks, 1));
            }
            return code.toString();
        }

        @Override
        void updateState() {
            setEnabled(isInManagmentMode()
                    && getSelectedRowCount() >= 1);
        }
    }

    public class CopyAction extends MaskAction {
        public CopyAction() {
            super("icons/Copy24.gif", "copyButton", "Copy the selected mask.");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            addNewMaskCopy();
        }

        @Override
        void updateState() {
            setEnabled(isInManagmentMode()
                    && getSelectedRowCount() == 1);
        }

        private void addNewMaskCopy() {
            Mask selectedMask = getSelectedMask();
            Mask mask = new Mask("Copy_of_" + selectedMask.getName(),
                                 selectedMask.getSceneRasterWidth(),
                                 selectedMask.getSceneRasterHeight(),
                                 selectedMask.getImageType());
            mask.setDescription(selectedMask.getDescription());
            PropertyContainer selectedConfig = selectedMask.getImageConfig();
            Property[] models = selectedConfig.getProperties();
            for (Property model : models) {
                mask.getImageConfig().setValue(model.getDescriptor().getName(),
                                               model.getValue());
            }
            addMask(mask);
        }
    }

    public class EditAction extends MaskAction {
        public EditAction() {
            super("icons/Edit24.gif", "editButton", "Edit the selected mask.");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            Mask selectedMask = getSelectedMask();
            Mask.ImageType type = selectedMask.getImageType();
            if (type instanceof Mask.BandMathType) {
                Product product = getProduct();
                ProductExpressionPane expressionPane = ProductExpressionPane.createBooleanExpressionPane(new Product[]{product}, product, null);
                expressionPane.setCode((String) selectedMask.getImageConfig().getValue("expression"));
                if (expressionPane.showModalDialog(null, "Edit Band-Math Mask") == AbstractDialog.ID_OK) {
                    String code = expressionPane.getCode();
                    selectedMask.getImageConfig().setValue("expression", code);
                }
            } else {
                // todo - implement for other types too
            }
        }

        @Override
        void updateState() {
            setEnabled(isInManagmentMode()
                    && getSelectedRowCount() == 1);
        }
    }

    public class RemoveAction extends MaskAction {
        public RemoveAction() {
            super("icons/Remove24.gif", "editButton", "Edit the selected mask.");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            // todo - ask user
            Mask[] selectedMasks = getSelectedMasks();
            for (Mask selectedMask : selectedMasks) {
                removeMask(selectedMask);
            }
        }

        @Override
        void updateState() {
            setEnabled(isInManagmentMode() && getSelectedRowCount() > 0);
        }
    }

    public class ImportAction extends MaskAction {
        public ImportAction() {
            super("icons/Import24.gif", "importButton", "Import masks from file.");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            // todo - implement
        }

        @Override
        void updateState() {
            setEnabled(isInManagmentMode());
        }
    }

    public class ExportAction extends MaskAction {
        public ExportAction() {
            super("icons/Export24.gif", "exportButton", "Export masks from file.");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            // todo - implement
        }

        @Override
        void updateState() {
            setEnabled(isInManagmentMode()
                    && getSelectedRowCount() > 0);
        }
    }

    public class MoveUpAction extends MaskAction {
        public MoveUpAction() {
            super("icons/Up24.gif", "moveUpButton", "Moves up the selected mask.");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            // todo - implement
        }

        @Override
        void updateState() {
            setEnabled(getSelectedRowCount() == 1
                    && getSelectedRow() > 0);
        }
    }

    public class MoveDownAction extends MaskAction {
        public MoveDownAction() {
            super("icons/Down24.gif", "moveDownButton", "Moves down the selected mask.");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            // todo - implement
        }

        @Override
        void updateState() {
            setEnabled(getSelectedRowCount() == 1
                    && getSelectedRow() < getRowCount() - 1);
        }
    }

    public class NullAction extends MaskAction {
        public NullAction() {
            super("", "", "");
        }

        @Override
        JComponent createComponent() {
            return new JPanel();
        }

        @Override
        public void actionPerformed(ActionEvent e) {
        }
    }

}