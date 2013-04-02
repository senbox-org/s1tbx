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
package org.esa.beam.processor.binning.ui;

import com.bc.jexp.ParseException;
import org.esa.beam.dataio.dimap.DimapProductConstants;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.param.ParamChangeEvent;
import org.esa.beam.framework.param.ParamChangeListener;
import org.esa.beam.framework.param.ParamEditor;
import org.esa.beam.framework.param.ParamGroup;
import org.esa.beam.framework.param.ParamValidateException;
import org.esa.beam.framework.param.Parameter;
import org.esa.beam.framework.processor.ProcessorConstants;
import org.esa.beam.framework.processor.ProcessorException;
import org.esa.beam.framework.processor.ProductRef;
import org.esa.beam.framework.processor.Request;
import org.esa.beam.framework.processor.RequestElementFactoryException;
import org.esa.beam.framework.processor.ui.AbstractProcessorUI;
import org.esa.beam.framework.processor.ui.ProcessorApp;
import org.esa.beam.framework.ui.GridBagUtils;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.io.FileArrayEditor;
import org.esa.beam.framework.ui.product.BandChooser;
import org.esa.beam.framework.ui.product.ProductExpressionPane;
import org.esa.beam.framework.ui.tool.ToolButtonFactory;
import org.esa.beam.processor.binning.L3Constants;
import org.esa.beam.processor.binning.L3Processor;
import org.esa.beam.processor.binning.L3RequestElementFactory;
import org.esa.beam.processor.binning.algorithm.Algorithm;
import org.esa.beam.processor.binning.algorithm.AlgorithmCreator;
import org.esa.beam.processor.binning.algorithm.AlgorithmFactory;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.StringUtils;
import org.esa.beam.util.SystemUtils;

import javax.swing.AbstractButton;
import javax.swing.AbstractCellEditor;
import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.TableModelEvent;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellEditor;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Vector;
import java.util.logging.Logger;

@Deprecated
/**
 * @Deprecated since beam-binning 2.1.2 as part of the BEAM 4.11-release. Use module 'beam-binning2' instead.
 */
public abstract class L3UI extends AbstractProcessorUI {

    public static class Row {

        private final String bandName;
        private final String bitmaskExpression;
        private final String algorithmName;
        private final double weightCoefficient;

        public Row(String bandName, String bitmaskExpression, String algorithmName, double weightCoefficient) {
            this.bandName = bandName;
            this.bitmaskExpression = bitmaskExpression;
            this.algorithmName = algorithmName;
            this.weightCoefficient = weightCoefficient;
        }

        public String getBandName() {
            return bandName;
        }

        public String getBitmaskExpression() {
            return bitmaskExpression;
        }

        public String getAlgorithmName() {
            return algorithmName;
        }

        public double getWeightCoefficient() {
            return weightCoefficient;
        }
    }

    public class ProcessingParamsTable {

        private JTable table;
        private DefaultTableModel tableModel;

        public ProcessingParamsTable() {
            tableModel = new DefaultTableModel() {
                @Override
                public boolean isCellEditable(int row, int column) {
                    return column != 0;
                }
            };

            tableModel.setColumnIdentifiers(new String[]{
                    "Band",
                    "Valid expression",
                    "Aggregation",
                    "Weight"
            });

            table = new JTable(tableModel) {
                @Override
                public Class getColumnClass(int column) {
                    if (column == 3) {
                        return Double.class;
                    } else {
                        return String.class;
                    }
                }

                @Override
                public void tableChanged(TableModelEvent e) {
                    super.tableChanged(e);
                    updateEstimatedProductSize(ProcessingParamsTable.this);
                }
            };
            table.getTableHeader().setReorderingAllowed(false);

            table.getColumnModel().getColumn(0).setPreferredWidth(150);
            table.getColumnModel().getColumn(1).setPreferredWidth(450);
            table.getColumnModel().getColumn(2).setPreferredWidth(150);
            table.getColumnModel().getColumn(3).setPreferredWidth(150);

            table.getColumnModel().getColumn(1).setCellEditor(new ExprEditor(true));
            table.getColumnModel().getColumn(2).setCellEditor(
                    new DefaultCellEditor(new JComboBox(L3Constants.ALGORITHM_VALUE_SET)));
        }

        public JComponent getComponent() {
            final JScrollPane jScrollPane = new JScrollPane(table);
            jScrollPane.setPreferredSize(new Dimension(500, 200));
            return jScrollPane;
        }

        public String[] getBandNames() {
            final int numRows = table.getRowCount();
            final String[] bandNames = new String[numRows];
            for (int i = 0; i < bandNames.length; i++) {
                bandNames[i] = (String) table.getValueAt(i, 0);
            }
            return bandNames;
        }

        public void add(final String bandName, final String validExpression, String algorithmName,
                        double weightCoefficient) {
            if (algorithmName == null || !StringUtils.contains(L3Constants.ALGORITHM_VALUE_SET, algorithmName)) {
                algorithmName = L3Constants.ALGORITHM_DEFAULT_VALUE;
            }
            tableModel.addRow(new Object[]{bandName, validExpression, algorithmName, weightCoefficient});
        }

        public void remove(final String bandName) {
            final String[] bandNames = getBandNames();
            final int rowToRemove = StringUtils.indexOf(bandNames, bandName);
            tableModel.removeRow(rowToRemove);
        }

        public Row[] getRows() {
            final List dataList = tableModel.getDataVector();
            final Row[] rows = new Row[dataList.size()];
            for (int i = 0; i < dataList.size(); i++) {
                final List dataListRow = (List) dataList.get(i);
                rows[i] = new Row((String) dataListRow.get(0),
                                  (String) dataListRow.get(1),
                                  (String) dataListRow.get(2),
                                  (Double) dataListRow.get(3));
            }
            return rows;
        }

        public void clear() {
            final String[] bandNames = getBandNames();
            for (final String bandName : bandNames) {
                remove(bandName);
            }
        }

        public void removeAll() {
            tableModel.setRowCount(0);
        }
    }

    public class ExprEditor extends AbstractCellEditor implements TableCellEditor {

        private final JPanel editorComponent;
        private final JTextField textField;

        private int row;
        private int column;
        private JTable table;

        public ExprEditor(final boolean booleanExpected) {

            JButton button = new JButton("...");
            final Dimension preferredSize = button.getPreferredSize();
            preferredSize.setSize(25, preferredSize.getHeight());
            button.setPreferredSize(preferredSize);

            final ActionListener actionListener = new ActionListener() {
                public void actionPerformed(ActionEvent e) {
                    final String newCode = editExpression(textField.getText(), booleanExpected);
                    if (newCode != null) {
                        textField.setText(newCode);
                        fireEditingStopped();
/*
                        final int[] selectedRows = table.getSelectedRows();
                        for (final int selectedRow : selectedRows) {
                            if (selectedRow != row) {
                                table.setValueAt(value[0], selectedRow, column);
                            }
                        }
*/
                    } else {
                        fireEditingCanceled();
                    }
                }
            };
            button.addActionListener(actionListener);

            textField = new JTextField();

            editorComponent = new JPanel(new BorderLayout());
            editorComponent.add(textField);
            editorComponent.add(button, BorderLayout.EAST);
        }

        public Object getCellEditorValue() {
            return textField.getText();
        }

        public Component getTableCellEditorComponent(JTable table,
                                                     Object value,
                                                     boolean isSelected,
                                                     int row,
                                                     int column) {
            this.table = table;
            this.row = row;
            this.column = column;
            textField.setText((String) value);
            return editorComponent;
        }
    }

    protected static final String TAB_NAME_INPUT = "Input Products";
    protected static final String TAB_NAME_OUTPUT = "Output Parameters";
    protected static final String TAB_NAME_PROCESSING_PARAMETERS = "Processing Parameters";
    protected static final String TAB_NAME_BANDS = "Bands";

    protected L3Processor processor;
    protected Vector<Request> requests;
    protected L3RequestElementFactory reqElemFactory;
    protected ParamGroup paramGroup;
    protected File userDbDir;
    protected File userInputDir;
    protected Logger logger;

    protected static final float kilometersPerDegreee = 40000.f / 360.f;
    protected static final float oneMB = 1024 * 1024;
    protected static final float oneGB = oneMB * 1024;
    protected static final float oneTB = oneGB * 1024;
    protected JLabel expectedProductSizeLabel;
    private File requestFile;
    private AlgorithmCreator algoCreator;
    private JPanel compositePanel;

    /**
     * Constructs the object with given processor.
     */
    L3UI(L3Processor processor) {
        Guardian.assertNotNull("processor", processor);
        this.processor = processor;
        reqElemFactory = (L3RequestElementFactory) this.processor.getRequestElementFactory();
        requests = new Vector<Request>();
        logger = Logger.getLogger(L3Constants.LOGGER_NAME);
        algoCreator = new AlgorithmFactory();
    }

    /**
     * Retrieves the requests currently edited.
     */
    public Vector getRequests() throws ProcessorException {
        if (requests == null) {
            requests = new Vector<Request>();
        }
        requests.clear();
        collectRequestsFromUI(requests);
        ((Request) requests.get(0)).setFile(requestFile);
        return requests;
    }

    /**
     * Sets the given Request vector to be edited.
     */
    public void setRequests(Vector requests) throws ProcessorException {
        Guardian.assertNotNull("requests", requests);
        this.requests = requests;
        if (requests.size() > 0) {
            final Request request = (Request) requests.get(0);
            requestFile = request.getFile();
        }
        setRequests();
        updateUI();
    }

    /**
     * Create a set of new default requests.
     */
    public void setDefaultRequests() throws ProcessorException {
        requests = new Vector<Request>();
        requestFile = null;
        setDefaultRequestsImpl();
        updateUI();
    }

    /**
     * Sets the processor app for the UI
     */
    @Override
    public void setApp(ProcessorApp app) {
        super.setApp(app);
        ensureUserDirs();
        markIODirChanges(paramGroup);
    }

    ///////////////////////////////////////////////////////////////////////////
    /////// END OF PUBLIC
    ///////////////////////////////////////////////////////////////////////////

    protected abstract void collectRequestsFromUI(final List requests) throws ProcessorException;

    protected abstract void setRequests() throws ProcessorException;

    protected abstract void updateUI() throws ProcessorException;

    protected abstract void setDefaultRequestsImpl() throws ProcessorException;

    private void markIODirChanges(final ParamGroup paramGroup) {
        if (paramGroup != null) {
            getApp().markIODirChanges(paramGroup);
        }
    }

    /**
     * Adds a parameter with given name to the parameter panel at given line index
     *
     * @param parameterName the parameter name
     * @param gbc           the <code>GridBagConstraints</code> to be used
     * @param panel         whre to add the parameter
     */
    protected void addParameterToGridBagPanel(String parameterName, GridBagConstraints gbc, JPanel panel) {
        final Parameter parameter = paramGroup.getParameter(parameterName);
        if (parameter != null) {
            JLabel label = parameter.getEditor().getLabelComponent();
            JLabel unit = parameter.getEditor().getPhysUnitLabelComponent();

            GridBagConstraints myGbc = (GridBagConstraints) gbc.clone();

            myGbc.gridwidth = 1;
            myGbc.fill = GridBagConstraints.NONE;

            if (label != null) {
                myGbc.weightx = 0;
                panel.add(label, myGbc);
            } else {
                myGbc.gridwidth = 2;
            }

            myGbc.fill = gbc.fill;
            if (unit != null) {
                panel.add(parameter.getEditor().getComponent(), myGbc);
                myGbc.gridwidth = 1;
                myGbc.fill = GridBagConstraints.NONE;
                myGbc.weightx = gbc.weightx;
                panel.add(unit, myGbc);
            } else {
                myGbc.gridwidth++;
                myGbc.weightx = gbc.weightx;
                panel.add(parameter.getEditor().getComponent(), myGbc);
            }
        }
    }

    protected JPanel createLatLonPanel() {
        final GridBagConstraints latLonGbc = GridBagUtils.createConstraints(null);
        JPanel latMinPanel = GridBagUtils.createDefaultEmptyBorderPanel();
        addParameterToGridBagPanel(L3Constants.LAT_MIN_PARAMETER_NAME, latLonGbc, latMinPanel);
        JPanel latMaxPanel = GridBagUtils.createDefaultEmptyBorderPanel();
        addParameterToGridBagPanel(L3Constants.LAT_MAX_PARAMETER_NAME, latLonGbc, latMaxPanel);
        JPanel lonMinPanel = GridBagUtils.createDefaultEmptyBorderPanel();
        addParameterToGridBagPanel(L3Constants.LON_MIN_PARAMETER_NAME, latLonGbc, lonMinPanel);
        JPanel lonMaxPanel = GridBagUtils.createDefaultEmptyBorderPanel();
        addParameterToGridBagPanel(L3Constants.LON_MAX_PARAMETER_NAME, latLonGbc, lonMaxPanel);

        JPanel latlonPanel = new JPanel(new BorderLayout());
        latlonPanel.add(latMaxPanel, BorderLayout.NORTH);
        latlonPanel.add(lonMinPanel, BorderLayout.WEST);
        latlonPanel.add(lonMaxPanel, BorderLayout.EAST);
        latlonPanel.add(latMinPanel, BorderLayout.SOUTH);
        return latlonPanel;
    }

    /**
     * Retrieves the current user database directory
     */
    protected File getUserDbDir() {
        return userDbDir;
    }

    /**
     * Sets a new user database directory
     */
    protected void setUserDbDir(File newDir) {
        userDbDir = newDir;
        getApp().getPreferences().setPropertyString(L3Constants.USER_DB_DIR, userDbDir.toString());
    }

    /**
     * Retrieves the current user input directory
     */
    protected File getUserInputDir() {
        return userInputDir;
    }

    /**
     * Sets a new user input directory
     */
    protected void setUserInputDir(File newDir) {
        userInputDir = newDir;
        getApp().getPreferences().setPropertyString(L3Constants.USER_INPUT_DIR, userInputDir.toString());
    }

    /**
     * Reads the user directories from the preferences. If none are set yet, creates both preference values.
     */
    private void ensureUserDirs() {
        String dirString = getApp().getPreferences().getPropertyString(L3Constants.USER_DB_DIR);
        if (dirString == null) {
            userDbDir = SystemUtils.getUserHomeDir();
            getApp().getPreferences().setPropertyString(L3Constants.USER_DB_DIR, userDbDir.toString());
        } else {
            userDbDir = new File(dirString);
        }

        dirString = getApp().getPreferences().getPropertyString(L3Constants.USER_INPUT_DIR);
        if (dirString == null) {
            userInputDir = SystemUtils.getUserHomeDir();
            getApp().getPreferences().setPropertyString(L3Constants.USER_INPUT_DIR, userInputDir.toString());
        } else {
            userInputDir = new File(dirString);
        }
    }

    protected void updateEstimatedProductSize(final ProcessingParamsTable table) {
        final Row[] rows;
        if (table != null) {
            rows = table.getRows();
        } else {
            rows = new Row[0];
        }

        int numVariables = 0;
        for (final Row row : rows) {
            final Algorithm algorithm = algoCreator.getAlgorithm(row.getAlgorithmName());
            numVariables += algorithm.getNumberOfInterpretedVariables();
        }

        Parameter parameter = paramGroup.getParameter(L3Constants.LAT_MIN_PARAMETER_NAME);
        final float latMin = (Float) parameter.getValue();

        parameter = paramGroup.getParameter(L3Constants.LAT_MAX_PARAMETER_NAME);
        final float latMax = (Float) parameter.getValue();

        parameter = paramGroup.getParameter(L3Constants.LON_MIN_PARAMETER_NAME);
        final float lonMin = (Float) parameter.getValue();

        parameter = paramGroup.getParameter(L3Constants.LON_MAX_PARAMETER_NAME);
        final float lonMax = (Float) parameter.getValue();

        parameter = paramGroup.getParameter(L3Constants.GRID_CELL_SIZE_PARAM_NAME);
        final float cellSize = (Float) parameter.getValue();

        // the 4 corresponds to the size of a variable in bytes
        float size = 4 * ((latMax - latMin) * (lonMax - lonMin) * kilometersPerDegreee * kilometersPerDegreee * numVariables) / cellSize;

        String unit;
        if (size > oneTB) {
            size = size / oneTB;
            unit = "TB";
        } else if (size > oneGB) {
            size = size / oneGB;
            unit = "GB";
        } else {
            size = size / oneMB;
            unit = "MB";
        }
        expectedProductSizeLabel.setText(size + " " + unit);
    }

    protected boolean isOneShotUI() {
        return false;
    }

    protected JPanel createInputProductsPanel(FileArrayEditor inputProductEditor) {
        JPanel panel = GridBagUtils.createDefaultEmptyBorderPanel();
        final GridBagConstraints gbc = GridBagUtils.createConstraints(null);

        gbc.gridx = 0;
        gbc.gridy = -1;

        if (!isOneShotUI()) {
            Parameter param = paramGroup.getParameter(L3Constants.DATABASE_PARAM_NAME);

            gbc.gridy++;
            gbc.anchor = GridBagConstraints.SOUTHWEST;
            gbc.weighty = 0;
            panel.add(param.getEditor().getLabelComponent(), gbc);

            gbc.gridy++;
            gbc.anchor = GridBagConstraints.WEST;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.gridwidth = 3;
            gbc.weightx = 1;
            panel.add(param.getEditor().getComponent(), gbc);
        }

        gbc.gridy++;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.weighty = 1;
        gbc.weightx = 1;
        panel.add(inputProductEditor.getUI(), gbc);

        if (!isOneShotUI()) {
            final Parameter logPrefixParam = paramGroup.getParameter(ProcessorConstants.LOG_PREFIX_PARAM_NAME);

            gbc.gridy++;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.gridwidth = 3;
            gbc.anchor = GridBagConstraints.SOUTHWEST;
            panel.add(logPrefixParam.getEditor().getLabelComponent(), gbc);

            gbc.gridy++;
            gbc.anchor = GridBagConstraints.NORTHWEST;
            gbc.insets.top = 0;
            panel.add(logPrefixParam.getEditor().getComponent(), gbc);
        }
        return panel;
    }

    protected JPanel createOutParamsPane() {
        final JPanel panel = GridBagUtils.createDefaultEmptyBorderPanel();
        final GridBagConstraints gbc = GridBagUtils.createConstraints(null);

        gbc.gridwidth = 3;
        if (!isOneShotUI()) {
            final Parameter databaseParam = paramGroup.getParameter(L3Constants.DATABASE_PARAM_NAME);

            gbc.gridy++;
            gbc.anchor = GridBagConstraints.SOUTHWEST;
            gbc.weighty = 0;
            panel.add(databaseParam.getEditor().getLabelComponent(), gbc);

            gbc.gridy++;
            gbc.anchor = GridBagConstraints.WEST;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.weightx = 1;
            panel.add(databaseParam.getEditor().getComponent(), gbc);
        }


        final Parameter outFileParam = paramGroup.getParameter(L3Constants.OUTPUT_PRODUCT_PARAM_NAME);

        gbc.gridy++;
        gbc.anchor = GridBagConstraints.SOUTHWEST;
        gbc.insets.top = 7;
        gbc.weighty = 0;
        panel.add(outFileParam.getEditor().getLabelComponent(), gbc);

        gbc.gridy++;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.insets.top = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;
        panel.add(outFileParam.getEditor().getComponent(), gbc);


        final Parameter outFormatParam = paramGroup.getParameter(ProcessorConstants.OUTPUT_FORMAT_PARAM_NAME);

        gbc.gridy++;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.insets.top = 7;
        panel.add(outFormatParam.getEditor().getLabelComponent(), gbc);

        gbc.gridy++;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets.top = 0;
        panel.add(outFormatParam.getEditor().getComponent(), gbc);

        gbc.insets.top = 7;

        final Parameter tailoringParam = paramGroup.getParameter(L3Constants.TAILORING_PARAM_NAME);

        gbc.gridy++;
        gbc.gridwidth = 3;
        gbc.anchor = GridBagConstraints.WEST;
        panel.add(tailoringParam.getEditor().getComponent(), gbc);

        if (!isOneShotUI()) {
            gbc.gridy++;
            addParameterToGridBagPanel(L3Constants.DELETE_DB_PARAMETER_NAME, gbc, panel);
        }


        final Parameter logPrefixParam = paramGroup.getParameter(ProcessorConstants.LOG_PREFIX_PARAM_NAME);

        gbc.gridy++;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.gridwidth = 3;
        gbc.anchor = GridBagConstraints.SOUTHWEST;
        panel.add(logPrefixParam.getEditor().getLabelComponent(), gbc);

        gbc.gridy++;
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.insets.top = 0;
        panel.add(logPrefixParam.getEditor().getComponent(), gbc);

        final Parameter logToOutputParam = paramGroup.getParameter(ProcessorConstants.LOG_TO_OUTPUT_PARAM_NAME);

        gbc.gridy++;
        gbc.insets.top = 7;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weighty = 0.5;
        gbc.insets.bottom = 0;
        panel.add(logToOutputParam.getEditor().getComponent(), gbc);

        return panel;
    }

    // Must be overwritten in all the implemented UI classes which handles with processing parameters
    protected Product getExampleProduct(boolean forBandFilter) throws IOException {
        return null;
    }


    protected JPanel createBandsPanel(final ProcessingParamsTable table) throws ProcessorException {
        final JPanel panel = GridBagUtils.createDefaultEmptyBorderPanel();
        final GridBagConstraints gbc = GridBagUtils.createDefaultConstraints();


        gbc.gridy++;
        gbc.insets.bottom = 0;
        panel.add(new JLabel("Bands to be included:"), gbc);
        final JPanel bandsButtonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));

        final Component bandFileterButton = createBandFilterButton(table);
        bandsButtonsPanel.add(bandFileterButton);

        gbc.anchor = GridBagConstraints.EAST;
        panel.add(bandsButtonsPanel, gbc);

        gbc.gridy++;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.BOTH;
        gbc.insets.bottom = 3;
        gbc.insets.top = 0;
        gbc.gridwidth = 2;
        gbc.weightx = 1;
        gbc.weighty = 1;
        panel.add(table.getComponent(), gbc);

        return panel;
    }


    private String editExpression(String code, final boolean booleanExpected) {
        final Product product;
        try {
            product = getExampleProduct(false);
        } catch (IOException e) {
            return null;
        }
        if (product == null) {
            return null;
        }
        final ProductExpressionPane pep;
        if (booleanExpected) {
            pep = ProductExpressionPane.createBooleanExpressionPane(new Product[]{product}, product,
                                                                    getApp().getPreferences());
        } else {
            pep = ProductExpressionPane.createGeneralExpressionPane(new Product[]{product}, product,
                                                                    getApp().getPreferences());
        }
        pep.setCode(code);
        final int i = pep.showModalDialog(getApp().getMainFrame(), "Expression Editor");
        if (i == ModalDialog.ID_OK) {
            return pep.getCode();
        }
        return null;
    }

    protected JPanel createProcessingParametersPanel() throws ProcessorException {
        final JPanel panel = GridBagUtils.createDefaultEmptyBorderPanel();
        final GridBagConstraints gbc = GridBagUtils.createConstraints(null);

        gbc.anchor = GridBagConstraints.WEST;
        gbc.gridwidth = 2;

        if (!isOneShotUI()) {
            gbc.gridy++;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            Parameter dbParam = paramGroup.getParameter(L3Constants.DATABASE_PARAM_NAME);
            panel.add(dbParam.getEditor().getLabelComponent(), gbc);

            gbc.gridy++;
            panel.add(dbParam.getEditor().getComponent(), gbc);

            gbc.insets.top = 15;
        }

        gbc.gridy++;
        gbc.insets.top = 7;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;

        compositePanel = createCompositePanel();
        compositePanel.setBorder(BorderFactory.createTitledBorder("Compositing"));
        panel.add(compositePanel, gbc);


        gbc.gridy++;
        gbc.anchor = GridBagConstraints.CENTER;
        final JPanel latLonPanel = createLatLonPanel();
        latLonPanel.setBorder(BorderFactory.createTitledBorder("Geographic Boundary"));
        panel.add(latLonPanel, gbc);
        gbc.anchor = GridBagConstraints.NORTHWEST;
        gbc.weightx = 0;

        if (!isOneShotUI()) {
            final Parameter logPrefixParam = paramGroup.getParameter(ProcessorConstants.LOG_PREFIX_PARAM_NAME);

            gbc.gridy++;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.anchor = GridBagConstraints.SOUTHWEST;
            panel.add(logPrefixParam.getEditor().getLabelComponent(), gbc);

            gbc.gridy++;
            gbc.anchor = GridBagConstraints.NORTHWEST;
            gbc.insets.top = 0;
            panel.add(logPrefixParam.getEditor().getComponent(), gbc);
        }

        gbc.gridy++;
        gbc.insets.top = 15;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weighty = 10;
        gbc.anchor = GridBagConstraints.SOUTHEAST;
        gbc.weightx = 1;
        gbc.gridwidth = 1;

        final String text = "Expected L3 product size: ";
        JLabel expectedSizeLabel = new JLabel(text); /*I18N*/
        expectedSizeLabel.setFont(expectedSizeLabel.getFont().deriveFont(Font.ITALIC));
        panel.add(expectedSizeLabel, gbc);

        gbc.weightx = 0;
        expectedProductSizeLabel = new JLabel("0.00 MB");
        expectedProductSizeLabel.setFont(expectedSizeLabel.getFont());
        expectedProductSizeLabel.setName(text.substring(0, text.length() - 2));
        panel.add(expectedProductSizeLabel, gbc);


        updateEstimatedProductSize(null);
        return panel;
    }

    private JPanel createCompositePanel() {
        final JPanel panel = GridBagUtils.createDefaultEmptyBorderPanel();
        final GridBagConstraints gbc = GridBagUtils.createDefaultConstraints();

        gbc.insets.top = 7;
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 1;

        gbc.gridy++;
        addParameterToGridBagPanel(L3Constants.RESAMPLING_TYPE_PARAM_NAME, gbc, panel);

        gbc.gridy++;

        addParameterToGridBagPanel(L3Constants.GRID_CELL_SIZE_PARAM_NAME, gbc, panel);

        return panel;
    }

    private Component createBandFilterButton(final ProcessingParamsTable table) {
        final AbstractButton bandFilterButton = createButton("icons/Copy16.gif");
        bandFilterButton.setName("BandFilterButton");
        bandFilterButton.setToolTipText("Choose the bands to process"); /*I18N*/
        bandFilterButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                try {
                    final Product exampleProduct = getExampleProduct(true);
                    if (exampleProduct != null) {
                        final Band[] allBands = exampleProduct.getBands();
                        final String[] existingBandNames = table.getBandNames();
                        final List<Band> existingBandList = new ArrayList<Band>();
                        for (String existingBandName : existingBandNames) {
                            final Band band = exampleProduct.getBand(existingBandName);
                            if (band != null) {
                                existingBandList.add(band);
                            }
                        }
                        final Band[] existingBands = existingBandList.toArray(
                                new Band[existingBandList.size()]);
                        final BandChooser bandChooser = new BandChooser(getApp().getMainFrame(),
                                                                        "Band Chooser", "", /*I18N*/
                                                                        allBands,
                                                                        existingBands);
                        if (bandChooser.show() == ModalDialog.ID_OK) {
                            final Row[] rows = table.getRows();
                            final HashMap<String, Row> rowsMap = new HashMap<String, Row>();
                            for (final Row row : rows) {
                                rowsMap.put(row.getBandName(), row);
                            }
                            table.removeAll();

                            final Band[] selectedBands = bandChooser.getSelectedBands();

                            for (final Band selectedBand : selectedBands) {
                                final String bandName = selectedBand.getName();
                                if (rowsMap.containsKey(bandName)) {
                                    final Row row = rowsMap.get(bandName);
                                    table.add(bandName, row.getBitmaskExpression(), row.getAlgorithmName(),
                                            row.getWeightCoefficient());
                                } else {
                                    table.add(bandName, selectedBand.getValidMaskExpression(), L3Constants.ALGORITHM_VALUE_ARITHMETIC_MEAN, 1.0);
                                }
                            }
                        }
                    }
                } catch (IOException e1) {
                    getApp().showErrorDialog("Unable to load an example product to\n" +
                                             "select the bands which are to process.\n\n" +
                                             "An IOException occurred while opening the example Product.\n" +
                                             "Exception message:\n" +
                                             e1.getMessage());
                }
            }
        });
        return bandFilterButton;
    }

    private AbstractButton createButton(final String path) {
        return ToolButtonFactory.createButton(UIUtils.loadImageIcon(path), false);
    }

    protected void updateProcessingParams(final ProcessingParamsTable processingParamsTable,
                                          final Request initRequest) {
        processingParamsTable.clear();
        final Parameter[] allParameters = initRequest.getAllParameters();
        for (Parameter bandNameParam : allParameters) {
            final String name = bandNameParam.getName();
            if (name.startsWith(L3Constants.BAND_NAME_PARAMETER_NAME)) {
                final String suffix;
                if (name.contains(".")) {
                    suffix = name.substring(name.lastIndexOf("."));
                } else {
                    suffix = "";
                }
                final String bitmaskParamName = L3Constants.BITMASK_PARAMETER_NAME + suffix;
                final String algorithmParamName = L3Constants.ALGORITHM_PARAMETER_NAME + suffix;
                final String coefficientParamName = L3Constants.WEIGHT_COEFFICIENT_PARAMETER_NAME + suffix;

                final Parameter bitmaskParam = initRequest.getParameter(bitmaskParamName);
                final Parameter algorithmParam = initRequest.getParameter(algorithmParamName);
                final Parameter weightCoeffParam = initRequest.getParameter(coefficientParamName);

                final String bandName = bandNameParam.getValueAsText();
                final String bitmask;
                if (bitmaskParam == null) {
                    bitmask = null;
                } else {
                    bitmask = bitmaskParam.getValueAsText();
                }

                final String algoName;
                if (algorithmParam == null) {
                    algoName = L3Constants.ALGORITHM_DEFAULT_VALUE;
                    logger.info("Parameter '" + algorithmParamName + "' not found. Using the default value '" +
                            algoName + "' instead.");
                } else {
                    algoName = algorithmParam.getValueAsText();
                }

                final double coeffValue;
                if (weightCoeffParam == null) {
                    coeffValue = algoName.equalsIgnoreCase(L3Constants.ALGORITHM_VALUE_MAXIMUM_LIKELIHOOD) ?  0.5 : 1.0;
                    logger.info(
                            "Parameter '" + coefficientParamName + "' not found. Using the default value '"+
                                coeffValue    +"' instead.");
                } else {
                    coeffValue = ((Float) weightCoeffParam.getValue()).doubleValue();
                }

                processingParamsTable.add(bandName, bitmask, algoName, coeffValue);
            }
        }
    }

    protected boolean validateRequestType(Request request) {
        final String requestType = request.getType();
        if (!requestType.equals(L3Constants.REQUEST_TYPE)) {
            getApp().showInfoDialog("Illegal request type: '" + requestType + "'", null);
            return false;
        }
        return true;
    }


    protected boolean validateProcessingParameters(final Request request) {
        final Product exampleProduct;
        try {
            exampleProduct = getExampleProduct(false);
            if (exampleProduct == null) {
                return true;
            }
        } catch (IOException e) {
            getApp().showErrorDialog("Unable to open the product to validate the request.\n" +
                                     "An I/O exception occurred while trying to\n" +
                                     "open the product for request validation.");
            return false;
        }

        final Parameter[] allParameters = request.getAllParameters();
        int processingParamNumber = 0;
        for (final Parameter param : allParameters) {
            final String name = param.getName();
            if (name.startsWith(L3Constants.BAND_NAME_PARAMETER_NAME)) {
                processingParamNumber++;
                final String bandName = param.getValueAsText();
                if (bandName == null || bandName.trim().length() == 0) {
                    getApp().showInfoDialog("The band name of the processing parameter #" + processingParamNumber
                            + " is empty.", null);
                    return false;
                }
                final String postfix = name.substring(name.lastIndexOf("."));
                final Parameter bitmaskParam =
                        request.getParameter(L3Constants.BITMASK_PARAMETER_NAME + postfix);
                final String expression = bitmaskParam.getValueAsText();
                try {
                    exampleProduct.parseExpression(expression);
                } catch (ParseException e) {
                    String message = String.format("The valid expression of the band named '%s' is not applicable.", bandName);
                    getApp().showInfoDialog(message, null);
                    return false;
                }
            }
        }
        return true;
    }

    protected void collectProcessingParameters(final ProcessingParamsTable processingParamsTable,
                                               Request initRequest) throws RequestElementFactoryException {
        final Row[] rows = processingParamsTable.getRows();
        for (int i = 0; i < rows.length; i++) {
            final Row row = rows[i];
            final String postfix = "." + i;
            final String bandName = row.getBandName();

            String paramName;
            paramName = L3Constants.BAND_NAME_PARAMETER_NAME + postfix;
            initRequest.addParameter(
                    reqElemFactory.createParameter(paramName, bandName == null ? "" : bandName.trim()));

            paramName = L3Constants.BITMASK_PARAMETER_NAME + postfix;
            final String expression = row.getBitmaskExpression();
            initRequest.addParameter(
                    reqElemFactory.createParameter(paramName, expression == null ? "" : expression.trim()));

            paramName = L3Constants.ALGORITHM_PARAMETER_NAME + postfix;
            initRequest.addParameter(reqElemFactory.createParameter(paramName, row.getAlgorithmName()));

            paramName = L3Constants.WEIGHT_COEFFICIENT_PARAMETER_NAME + postfix;
            initRequest.addParameter(reqElemFactory.createParameter(paramName, "" + row.getWeightCoefficient()));
        }
    }

    /**
     * Sets the output file parameter to the value stored in the request. Updates the file format combo box with the
     * correct value
     */
    protected void setOutputFile(final Request request) {
        if (request.getNumOutputProducts() > 0) {
            final ProductRef outputProduct = request.getOutputProductAt(0);
            final File file = new File(outputProduct.getFilePath());
            final Parameter param = paramGroup.getParameter(L3Constants.OUTPUT_PRODUCT_PARAM_NAME);
            param.setValue(file, null);

            final Parameter outFormatParam = paramGroup.getParameter(ProcessorConstants.OUTPUT_FORMAT_PARAM_NAME);
            if (outputProduct.getFileFormat() == null) {
                // set default format - beam-dimap
                outFormatParam.setValue(DimapProductConstants.DIMAP_FORMAT_NAME, null);
            } else {
                outFormatParam.setValue(outputProduct.getFileFormat(), null);
            }
        }
    }

    protected ParamChangeListener createResamplingChangeListener() {
        return new ParamChangeListener() {
            public void parameterValueChanged(ParamChangeEvent event) {
                final Parameter changedParam = event.getParameter();
                if (changedParam.getName().equals(L3Constants.RESAMPLING_TYPE_PARAM_NAME)) {

                    final Parameter paramGCC = paramGroup.getParameter(L3Constants.GRID_CELL_SIZE_PARAM_NAME);
                    final ParamEditor paramEditorGCC = paramGCC.getEditor();
                    final JLabel labelGCC = paramEditorGCC.getLabelComponent();
                    final JComponent editorGCC = paramEditorGCC.getComponent();
                    final JLabel unitGCC = paramEditorGCC.getPhysUnitLabelComponent();

                    final Parameter paramCPD = paramGroup.getParameter(L3Constants.CELLS_PER_DEGREE_PARAM_NAME);
                    final ParamEditor paramEditorCPD = paramCPD.getEditor();
                    final JLabel labelCPD = paramEditorCPD.getLabelComponent();
                    final JComponent editorCPD = paramEditorCPD.getComponent();
                    final JLabel unitCPD = paramEditorCPD.getPhysUnitLabelComponent();


                    if (changedParam.getValueAsText().equals(L3Constants.RESAMPLING_TYPE_VALUE_BINNING)) {
                        replaceComponents(labelCPD, labelGCC, editorCPD, editorGCC, unitCPD, unitGCC);
                    } else {
                        replaceComponents(labelGCC, labelCPD, editorGCC, editorCPD, unitGCC, unitCPD);
                    }
                }
            }
        };
    }

    protected final boolean isFluxConserving() {
        final Parameter param = paramGroup.getParameter(L3Constants.RESAMPLING_TYPE_PARAM_NAME);
        if (param == null) {
            return false;
        }
        return L3Constants.RESAMPLING_TYPE_VALUE_FLUX_CONSERVING.equals(param.getValueAsText());
    }

    private void replaceComponents(final JLabel label, final JLabel newLabel,
                                   final JComponent editor, final JComponent newEditor,
                                   final JLabel unit, final JLabel newUnit) {
        final GridBagLayout gbl = (GridBagLayout) compositePanel.getLayout();

        replaceComponent(gbl, label, newLabel);
        replaceComponent(gbl, editor, newEditor);
        replaceComponent(gbl, unit, newUnit);

        compositePanel.revalidate();
        compositePanel.repaint();
    }

    private void replaceComponent(final GridBagLayout gbl, final Component comp, final Component newComp) {
        final GridBagConstraints gbc = gbl.getConstraints(comp);
        compositePanel.remove(comp);
        compositePanel.add(newComp, gbc);
    }

    protected void updateUIComponent(String paramName, Request request) throws ParamValidateException {
        final Parameter param = request.getParameter(paramName);
        final Parameter toUpdate = paramGroup.getParameter(paramName);
        if (param != null) {
            toUpdate.setValue(param.getValue());
        } else {
            toUpdate.setDefaultValue();
        }
    }
}
