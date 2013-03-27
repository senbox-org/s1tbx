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

package org.esa.beam.timeseries.ui.manager;

import com.bc.ceres.swing.TableLayout;
import com.jidesoft.swing.TitledSeparator;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.help.HelpSys;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.application.PageComponentDescriptor;
import org.esa.beam.framework.ui.command.Command;
import org.esa.beam.framework.ui.tool.ToolButtonFactory;
import org.esa.beam.timeseries.core.TimeSeriesMapper;
import org.esa.beam.timeseries.core.timeseries.datamodel.AbstractTimeSeries;
import org.esa.beam.timeseries.core.timeseries.datamodel.AxisMapping;
import org.esa.beam.timeseries.core.timeseries.datamodel.ProductLocation;
import org.esa.beam.timeseries.core.timeseries.datamodel.ProductLocationType;
import org.esa.beam.timeseries.core.timeseries.datamodel.TimeSeriesChangeEvent;
import org.esa.beam.timeseries.core.timeseries.datamodel.TimeSeriesListener;
import org.esa.beam.timeseries.ui.DefaultProductLocationsPaneModel;
import org.esa.beam.timeseries.ui.DefaultVariableSelectionPaneModel;
import org.esa.beam.timeseries.ui.ProductLocationsPane;
import org.esa.beam.timeseries.ui.ProductLocationsPaneModel;
import org.esa.beam.timeseries.ui.Variable;
import org.esa.beam.timeseries.ui.VariableSelectionPane;
import org.esa.beam.timeseries.ui.VariableSelectionPaneModel;
import org.esa.beam.timeseries.ui.assistant.TimeSeriesAssistantAction;
import org.esa.beam.ui.NamesAssociationDialog;
import org.esa.beam.util.Debug;
import org.esa.beam.visat.VisatApp;

import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.AbstractListModel;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JInternalFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.beans.PropertyVetoException;
import java.io.File;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

class TimeSeriesManagerForm {

    private final PageComponentDescriptor descriptor;
    private final SimpleDateFormat dateFormat;
    private final JComponent control;
    private final FrameClosingTimeSeriesListener frameClosingTimeSeriesListener;
    private JLabel nameField;
    private JLabel crsField;
    private JLabel startField;
    private JLabel endField;
    private JLabel dimensionField;
    private VariableSelectionPane eoVariablePane;
    private VariableSelectionPane insituVariablePane;
    private ProductLocationsPane locationsPane;
    private AbstractButton loadInsituButton;
    private AbstractButton timeSpanButton;
    private AbstractButton viewButton;
    private AbstractTimeSeries currentTimeSeries;
    private AbstractButton editNamesAssociationButton;

    TimeSeriesManagerForm(PageComponentDescriptor descriptor) {
        this.descriptor = descriptor;
        dateFormat = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss", Locale.ENGLISH);
        control = createControl();
        frameClosingTimeSeriesListener = new FrameClosingTimeSeriesListener();
    }

    private JComponent createControl() {
        final TableLayout layout = new TableLayout(4);
        layout.setTableAnchor(TableLayout.Anchor.NORTHWEST);
        layout.setTablePadding(4, 4);
        layout.setTableFill(TableLayout.Fill.BOTH);
        layout.setColumnFill(3, TableLayout.Fill.BOTH);
        layout.setRowWeightY(0, 1.0);
        layout.setCellWeightY(0, 3, 1.0);
        layout.setColumnWeightX(0, 1.0);
        layout.setColumnWeightX(1, 2.0);
        layout.setColumnWeightX(2, 2.0);
        layout.setColumnWeightX(3, 0.0);
        layout.setRowWeightY(1, 2.0);
        layout.setCellFill(0, 0, TableLayout.Fill.HORIZONTAL);
        layout.setCellRowspan(0, 3, 2);
        layout.setCellColspan(1, 0, 3);

        eoVariablePane = new VariableSelectionPane();
        insituVariablePane = new VariableSelectionPane();

        JPanel infoPanel = createInfoPanel();
        JPanel variablePanel = createVariablePanel("Variables", eoVariablePane);
        JPanel insituVariablePanel = createVariablePanel("In-situ variables", insituVariablePane);
        JPanel buttonPanel = createButtonPanel();
        JPanel productsPanel = createProductsPanel();

        final JPanel control = new JPanel(layout);
        control.add(infoPanel);
        control.add(variablePanel);
        control.add(insituVariablePanel);
        control.add(buttonPanel);
        control.add(productsPanel);

        return control;
    }

    public JComponent getControl() {
        return control;
    }

    public void updateFormControl(Product product) {
        currentTimeSeries = TimeSeriesMapper.getInstance().getTimeSeries(product);
        if (currentTimeSeries != null) {
            currentTimeSeries.addTimeSeriesListener(frameClosingTimeSeriesListener);
        }

        loadInsituButton.setAction(new LoadInsituAction(currentTimeSeries));
        timeSpanButton.setAction(new EditTimeSpanAction(currentTimeSeries));
        updateInfoPanel(currentTimeSeries);
        updateButtonPanel(currentTimeSeries);
        updateVariablePanel(currentTimeSeries);
        updateProductsPanel(currentTimeSeries);
    }


    private JPanel createInfoPanel() {
        final TableLayout layout = new TableLayout(2);
        layout.setTableAnchor(TableLayout.Anchor.NORTHWEST);
        layout.setTablePadding(4, 4);
        layout.setColumnWeightX(0, 0.1);
        layout.setColumnWeightX(1, 1.0);
        layout.setTableWeightY(0.0);
        layout.setTableFill(TableLayout.Fill.HORIZONTAL);
        layout.setCellColspan(0, 0, 2);

        final JLabel nameLabel = new JLabel("Name:");
        nameField = new JLabel();
        final JLabel crsLabel = new JLabel("CRS:");
        crsField = new JLabel();
        final JLabel startLabel = new JLabel("Start time:");
        startField = new JLabel();
        final JLabel endLabel = new JLabel("End time:");
        endField = new JLabel();
        final JLabel dimensionLabel = new JLabel("Dimension:");
        dimensionField = new JLabel("Dimension:");

        final JPanel panel = new JPanel(layout);
        panel.add(new TitledSeparator("Information"));
        panel.add(nameLabel);
        panel.add(nameField);
        panel.add(crsLabel);
        panel.add(crsField);
        panel.add(startLabel);
        panel.add(startField);
        panel.add(endLabel);
        panel.add(endField);
        panel.add(dimensionLabel);
        panel.add(dimensionField);
        return panel;
    }

    private void updateInfoPanel(AbstractTimeSeries timeSeries) {
        if (timeSeries == null) {
            nameField.setVisible(false);
            crsField.setVisible(false);
            startField.setVisible(false);
            endField.setVisible(false);
            dimensionField.setVisible(false);
            return;
        }
        final Product tsProduct = timeSeries.getTsProduct();

        nameField.setText(tsProduct.getDisplayName());
        crsField.setText(tsProduct.getGeoCoding().getMapCRS().getName().getCode());
        final String startTime = dateFormat.format(tsProduct.getStartTime().getAsDate());
        startField.setText(startTime);
        String endTime = dateFormat.format(tsProduct.getEndTime().getAsDate());
        endField.setText(endTime);
        final String dimensionString = tsProduct.getSceneRasterWidth() + " x " + tsProduct.getSceneRasterHeight();
        dimensionField.setText(dimensionString);
    }

    private JPanel createButtonPanel() {
        final Command newTSCommand = VisatApp.getApp().getCommandManager().getCommand(TimeSeriesAssistantAction.ID);
        final AbstractButton newButton = ToolButtonFactory.createButton(newTSCommand.getAction(), false);

        final Action loadInsituAction = new LoadInsituAction(currentTimeSeries);
        loadInsituButton = ToolButtonFactory.createButton(loadInsituAction, false);

        editNamesAssociationButton = ToolButtonFactory.createButton(new EditNameAssociationAction(), false);

        final EditTimeSpanAction editTimeSpanAction = new EditTimeSpanAction(currentTimeSeries);
        timeSpanButton = ToolButtonFactory.createButton(editTimeSpanAction, false);

        final Action viewTimeSeriesButtonAction = new ViewTimeSeriesButtonAction();
        viewButton = ToolButtonFactory.createButton(viewTimeSeriesButtonAction, false);

        AbstractButton helpButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/Help22.png"), false);
        helpButton.setToolTipText("Help");

        final TableLayout layout = new TableLayout(1);
        layout.setTableAnchor(TableLayout.Anchor.NORTHWEST);
        layout.setTableFill(TableLayout.Fill.HORIZONTAL);
        layout.setTableWeightX(1.0);
        layout.setTableWeightY(0.0);
        final JPanel panel = new JPanel(layout);
        panel.add(newButton);
        panel.add(loadInsituButton);
        panel.add(editNamesAssociationButton);
        panel.add(timeSpanButton);
        panel.add(viewButton);
        panel.add(layout.createVerticalSpacer());
        panel.add(helpButton);

        if (descriptor.getHelpId() != null) {
            HelpSys.enableHelpOnButton(helpButton, descriptor.getHelpId());
            HelpSys.enableHelpKey(panel, descriptor.getHelpId());
        }

        return panel;
    }

    private void showTimeSeriesView(String variableName) {
        final List<Band> bandList = currentTimeSeries.getBandsForVariable(variableName);
        final VisatApp app = VisatApp.getApp();
        for (Band band : bandList) {
            final JInternalFrame internalFrame = app.findInternalFrame(band);
            if (internalFrame != null) {
                return;
            }
        }
        if (!bandList.isEmpty()) {
            app.openProductSceneView(bandList.get(0));
        }
    }

    private void updateButtonPanel(AbstractTimeSeries timeSeries) {
        boolean enabled = timeSeries != null;
        viewButton.setEnabled(enabled);
        loadInsituButton.setEnabled(enabled);
        editNamesAssociationButton.setEnabled(enabled);
    }

    private JPanel createVariablePanel(String title, VariableSelectionPane variablePane) {
        final TableLayout layout = new TableLayout(1);
        layout.setTableAnchor(TableLayout.Anchor.NORTHWEST);
        layout.setTableFill(TableLayout.Fill.HORIZONTAL);
        layout.setTableWeightX(1.0);
        layout.setRowWeightY(0, 0.0);
        layout.setRowWeightY(1, 1.0);
        layout.setRowFill(1, TableLayout.Fill.BOTH);

        final JPanel panel = new JPanel(layout);
        panel.add(new TitledSeparator(title));
        variablePane.setPreferredSize(new Dimension(150, 80));
        panel.add(variablePane);
        return panel;
    }

    private void updateVariablePanel(AbstractTimeSeries timeSeries) {
        final VariableSelectionPaneModel model;
        if (timeSeries != null) {
            model = new TimeSeriesEoVariableSelectionPaneModel(timeSeries);
        } else {
            model = new DefaultVariableSelectionPaneModel();
        }
        eoVariablePane.setModel(model);
    }

    private void updateInsituVariablePanel() {
        final VariableSelectionPaneModel model;
        if (currentTimeSeries != null) {
            model = new TimeSeriesInsituVariableSelectionPaneModel(currentTimeSeries);
        } else {
            model = new DefaultVariableSelectionPaneModel();
        }
        insituVariablePane.setModel(model);
    }

    private JPanel createProductsPanel() {
        final TableLayout layout = new TableLayout(1);
        layout.setTableAnchor(TableLayout.Anchor.NORTHWEST);
        layout.setTableFill(TableLayout.Fill.HORIZONTAL);
        layout.setTableWeightX(1.0);
        layout.setRowWeightY(0, 0.0);
        layout.setRowWeightY(1, 1.0);
        layout.setRowFill(1, TableLayout.Fill.BOTH);

        final JPanel panel = new JPanel(layout);
        panel.add(new TitledSeparator("Product Sources"));
        locationsPane = new ProductLocationsPane();
        locationsPane.setPreferredSize(new Dimension(150, 80));
        panel.add(locationsPane);
        return panel;
    }

    private void updateProductsPanel(AbstractTimeSeries timeSeries) {
        final ProductLocationsPaneModel locationsModel;
        if (timeSeries != null) {
            locationsModel = new TimeSeriesProductLocationsPaneModel(timeSeries);
        } else {
            locationsModel = new DefaultProductLocationsPaneModel();
        }
        locationsPane.setModel(locationsModel, (timeSeries != null));
    }

    private static class TimeSeriesEoVariableSelectionPaneModel extends AbstractListModel
            implements VariableSelectionPaneModel {

        private final AbstractTimeSeries timeSeries;

        private TimeSeriesEoVariableSelectionPaneModel(AbstractTimeSeries timeSeries) {
            this.timeSeries = timeSeries;
        }

        @Override
        public int getSize() {
            return timeSeries.getEoVariables().size();
        }

        @Override
        public Variable getElementAt(int index) {
            final String varName = timeSeries.getEoVariables().get(index);
            return new Variable(varName, timeSeries.isEoVariableSelected(varName));
        }

        @Override
        public void set(Variable... variables) {
        }

        @Override
        public void add(Variable... variables) {
        }

        @Override
        public void setSelectedVariableAt(int index, boolean selected) {
            final String varName = timeSeries.getEoVariables().get(index);
            if (timeSeries.isEoVariableSelected(varName) != selected) {
                if (!selected) {
                    closeAssociatedViews(varName);
                }
                timeSeries.setEoVariableSelected(varName, selected);
                fireContentsChanged(this, index, index);
            }
        }

        private void closeAssociatedViews(String varName) {
            final List<Band> bands = timeSeries.getBandsForVariable(varName);
            for (Band band : bands) {
                final JInternalFrame[] internalFrames = VisatApp.getApp().findInternalFrames(band);
                for (final JInternalFrame internalFrame : internalFrames) {
                    try {
                        internalFrame.setClosed(true);
                    } catch (PropertyVetoException e) {
                        Debug.trace(e);
                    }
                }
            }

        }

        @Override
        public List<String> getSelectedVariableNames() {
            final List<String> allVars = timeSeries.getEoVariables();
            final List<String> selectedVars = new ArrayList<String>(allVars.size());
            for (String varName : allVars) {
                if (timeSeries.isEoVariableSelected(varName)) {
                    selectedVars.add(varName);
                }
            }
            return selectedVars;
        }
    }

    private static class TimeSeriesInsituVariableSelectionPaneModel extends AbstractListModel
            implements VariableSelectionPaneModel {

        private final AbstractTimeSeries timeSeries;

        private TimeSeriesInsituVariableSelectionPaneModel(AbstractTimeSeries timeSeries) {
            this.timeSeries = timeSeries;
        }

        @Override
        public int getSize() {
            return timeSeries.getInsituSource().getParameterNames().length;
        }

        @Override
        public Variable getElementAt(int index) {
            final String variableName = timeSeries.getInsituSource().getParameterNames()[index];
            return new Variable(variableName, timeSeries.isInsituVariableSelected(variableName));
        }

        @Override
        public void set(Variable... variables) {
        }

        @Override
        public void add(Variable... variables) {
        }

        @Override
        public void setSelectedVariableAt(int index, boolean selected) {
            String variableName = timeSeries.getInsituSource().getParameterNames()[index];
            if (timeSeries.isInsituVariableSelected(variableName) != selected) {
                timeSeries.setInsituVariableSelected(variableName, selected);
                fireContentsChanged(this, index, index);
            }
        }

        @Override
        public List<String> getSelectedVariableNames() {
            final String[] allVars = timeSeries.getInsituSource().getParameterNames();
            final List<String> selectedVars = new ArrayList<String>(allVars.length);
            for (String varName : allVars) {
                if (timeSeries.isInsituVariableSelected(varName)) {
                    selectedVars.add(varName);
                }
            }
            return selectedVars;
        }
    }

    private static class TimeSeriesProductLocationsPaneModel extends AbstractListModel
            implements ProductLocationsPaneModel {

        private final AbstractTimeSeries timeSeries;

        private TimeSeriesProductLocationsPaneModel(AbstractTimeSeries timeSeries) {
            this.timeSeries = timeSeries;
        }

        @Override
        public int getSize() {
            return timeSeries.getProductLocations().size();
        }

        @Override
        public ProductLocation getElementAt(int index) {
            return timeSeries.getProductLocations().get(index);
        }

        @Override
        public List<ProductLocation> getProductLocations() {
            return timeSeries.getProductLocations();
        }

        @Override
        public void addFiles(File... files) {
            final int startIndex = timeSeries.getProductLocations().size();
            for (File file : files) {
                timeSeries.addProductLocation(new ProductLocation(ProductLocationType.FILE, file.getAbsolutePath()));
            }
            final int stopIndex = timeSeries.getProductLocations().size() - 1;
            fireIntervalAdded(this, startIndex, stopIndex);
        }

        @Override
        public void addDirectory(File directory, boolean recursive) {
            timeSeries.addProductLocation(
                    new ProductLocation(recursive ? ProductLocationType.DIRECTORY_REC : ProductLocationType.DIRECTORY,
                                        directory.getAbsolutePath()));
            final int index = timeSeries.getProductLocations().size() - 1;
            fireIntervalAdded(this, index, index);
        }

        @Override
        public void remove(int... indices) {
            final List<ProductLocation> locationList = timeSeries.getProductLocations();
            final List<ProductLocation> toRemove = new ArrayList<ProductLocation>();
            for (int index : indices) {
                toRemove.add(locationList.get(index));
            }
            for (ProductLocation location : toRemove) {
                closeAssociatedViews(location);
                timeSeries.removeProductLocation(location);
            }
            if (!toRemove.isEmpty()) {
                fireContentsChanged(this, indices[0], indices[indices.length - 1]);
            }
        }

        private void closeAssociatedViews(ProductLocation location) {
            final List<Band> bands = timeSeries.getBandsForProductLocation(location);
            for (Band band : bands) {
                final JInternalFrame[] internalFrames = VisatApp.getApp().findInternalFrames(band);
                for (final JInternalFrame internalFrame : internalFrames) {
                    try {
                        internalFrame.setClosed(true);
                    } catch (PropertyVetoException e) {
                        Debug.trace(e);
                    }
                }
            }

        }
    }

    private class EditNameAssociationAction extends AbstractAction {

        private EditNameAssociationAction() {
            URL viewIconImageURL = UIUtils.getImageURL("/org/esa/beam/timeseries/ui/icons/timeseries-combvar24.png", TimeSeriesManagerForm.class);
            putValue(LARGE_ICON_KEY, new ImageIcon(viewIconImageURL));
            putValue(SHORT_DESCRIPTION, "Edit names association");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            final NamesAssociationDialog.AssociationModel associationModel = getAssociationModel();
            final NamesAssociationDialog.NameProvider nameProvider = getNameProvider();
            final String helpId = "associations";
            NamesAssociationDialog.show(associationModel, nameProvider, helpId);
        }
    }

    private class ViewTimeSeriesButtonAction extends AbstractAction {

        private ViewTimeSeriesButtonAction() {
            URL viewIconImageURL = UIUtils.getImageURL("/org/esa/beam/timeseries/ui/icons/timeseries-view24.png", TimeSeriesManagerForm.class);
            putValue(LARGE_ICON_KEY, new ImageIcon(viewIconImageURL));
            putValue(SHORT_DESCRIPTION, "View Time Series");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            final VariableSelectionPaneModel variableModel = eoVariablePane.getModel();
            final List<String> variableNames = variableModel.getSelectedVariableNames();
            if (!variableNames.isEmpty() && currentTimeSeries != null) {
                if (variableNames.size() == 1) {
                    showTimeSeriesView(variableNames.get(0));

                } else {
                    JPopupMenu viewPopup = new JPopupMenu("View variable");
                    for (String varName : variableNames) {
                        viewPopup.add(new ViewTimeSeriesAction(varName));
                    }
                    final Rectangle buttonBounds = viewButton.getBounds();
                    viewPopup.show(viewButton, 1, buttonBounds.height + 1);
                }
            }
        }
    }

    private class ViewTimeSeriesAction extends AbstractAction {

        private final String variableName;

        private ViewTimeSeriesAction(String variableName) {
            super("View " + variableName);
            this.variableName = variableName;
        }
        @Override
        public void actionPerformed(ActionEvent e) {
            showTimeSeriesView(variableName);
        }
    }

    private NamesAssociationDialog.AssociationModel getAssociationModel() {
        final AxisMapping axisMapping = currentTimeSeries.getAxisMapping();
        return new NamesAssociationDialog.AssociationModel() {
            @Override
            public List<String> getRightListNames(String alias) {
                return axisMapping.getInsituNames(alias);
            }

            @Override
            public List<String> getCenterListNames(String alias) {
                return axisMapping.getRasterNames(alias);
            }

            @Override
            public void addFromCenterList(String alias, String name) {
                axisMapping.addRasterName(alias, name);
            }

            @Override
            public void addFromRightList(String alias, String name) {
                axisMapping.addInsituName(alias, name);
            }

            @Override
            public void removeAlias(String alias) {
                axisMapping.removeAlias(alias);
            }

            @Override
            public void addAlias(String alias) {
                axisMapping.addAlias(alias);
            }

            @Override
            public void removeFromRightList(String alias, String name) {
                axisMapping.removeInsituName(alias, name);
            }

            @Override
            public void removeFromCenterList(String alias, String name) {
                axisMapping.removeRasterName(alias, name);
            }

            @Override
            public Set<String> getAliasNames() {
                return axisMapping.getAliasNames();
            }

            @Override
            public void replaceAlias(String beforeName, String changedName) {
                axisMapping.replaceAlias(beforeName, changedName);
            }
        };
    }

    private NamesAssociationDialog.NameProvider getNameProvider() {
        final String windowTitle = "Names Association";
        final String aliasHeaderName = "Association name:";
        final String centerHeaderName = "Names from time series:";
        final String rightHeaderName = "Names from insitu file:";
        return new NamesAssociationDialog.NameProvider(windowTitle, aliasHeaderName, centerHeaderName, rightHeaderName) {

            @Override
            public String[] getCenterNames() {
                final List<String> eoVariables = currentTimeSeries.getEoVariables();
                return eoVariables.toArray(new String[eoVariables.size()]);
            }

            @Override
            public String[] getRightNames() {
                if (currentTimeSeries.getInsituSource() != null) {
                    return currentTimeSeries.getInsituSource().getParameterNames();
                } else {
                    return new String[0];
                }
            }
        };
    }

    private class FrameClosingTimeSeriesListener extends TimeSeriesListener {
        @Override
        public void timeSeriesChanged(TimeSeriesChangeEvent event) {

            if (event.getType() == TimeSeriesChangeEvent.BAND_TO_BE_REMOVED) {
                Band band = (Band) event.getValue();
                VisatApp app = VisatApp.getApp();
                JInternalFrame internalFrame = app.findInternalFrame(band);
                if (internalFrame != null) {
                    internalFrame.dispose();
                }
            } else if (event.getType() == TimeSeriesChangeEvent.INSITU_SOURCE_CHANGED) {
                updateInsituVariablePanel();
            }
        }

    }
}
