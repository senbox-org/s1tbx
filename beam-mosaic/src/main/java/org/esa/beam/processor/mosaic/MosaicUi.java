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
package org.esa.beam.processor.mosaic;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.swing.progress.DialogProgressMonitor;
import com.bc.jexp.ParseException;
import com.bc.jexp.Parser;
import com.bc.jexp.Term;
import org.esa.beam.dataio.dimap.DimapProductConstants;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.MapGeoCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.dataop.maptransf.IdentityTransformDescriptor;
import org.esa.beam.framework.dataop.maptransf.MapInfo;
import org.esa.beam.framework.dataop.maptransf.MapProjection;
import org.esa.beam.framework.dataop.maptransf.MapProjectionRegistry;
import org.esa.beam.framework.dataop.maptransf.MapTransform;
import org.esa.beam.framework.dataop.maptransf.MapTransformUI;
import org.esa.beam.framework.dataop.maptransf.UTM;
import org.esa.beam.framework.param.ParamChangeEvent;
import org.esa.beam.framework.param.ParamChangeListener;
import org.esa.beam.framework.param.ParamEditor;
import org.esa.beam.framework.param.ParamGroup;
import org.esa.beam.framework.param.ParamValidateException;
import org.esa.beam.framework.param.Parameter;
import org.esa.beam.framework.processor.Processor;
import org.esa.beam.framework.processor.ProcessorConstants;
import org.esa.beam.framework.processor.ProcessorException;
import org.esa.beam.framework.processor.ProcessorUtils;
import org.esa.beam.framework.processor.ProductRef;
import org.esa.beam.framework.processor.Request;
import org.esa.beam.framework.processor.RequestElementFactoryException;
import org.esa.beam.framework.processor.RequestValidator;
import org.esa.beam.framework.processor.ui.AbstractProcessorUI;
import org.esa.beam.framework.processor.ui.ProcessorApp;
import org.esa.beam.framework.ui.GridBagUtils;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.ProjectionParamsDialog;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.WorldMapWindow;
import org.esa.beam.framework.ui.io.FileArrayEditor;
import org.esa.beam.framework.ui.product.BandChooser;
import org.esa.beam.framework.ui.product.ProductExpressionPane;
import org.esa.beam.framework.ui.tool.ToolButtonFactory;
import org.esa.beam.util.ArrayUtils;
import org.esa.beam.util.BeamConstants;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.MouseEventFilterFactory;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.StringUtils;
import org.esa.beam.util.io.BeamFileChooser;

import javax.swing.AbstractButton;
import javax.swing.AbstractCellEditor;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.WindowConstants;
import javax.swing.border.Border;
import javax.swing.border.EmptyBorder;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.JTableHeader;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableColumnModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dialog;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.geom.Rectangle2D;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;

/**
 * @deprecated since BEAM 4.7, replaced by GPF operator 'Mosaic'
 */
@Deprecated
public class MosaicUi extends AbstractProcessorUI {

    public static final int STANDARD_INSETS_TOP = 3;
    public static final int LARGE_INSETS_TOP = STANDARD_INSETS_TOP + 15;
    private static final String _defaultNumberText = "####";
    private static final String _defaultLatLonText = "##°";
    private static final int _PREFERRED_TABLE_WIDTH = 500;

    private MosaicRequestElementFactory _reqElemFac;
    private MapProjection[] _projections;
    private Parameter _paramProjectionName;
    private Parameter _paramOutputProduct;
    private Parameter _paramWestLon;
    private Parameter _paramNorthLat;
    private Parameter _paramEastLon;
    private Parameter _paramSouthLat;
    private Parameter _paramPixelSizeX;
    private Parameter _paramPixelSizeY;
    private WorldMapWindow _worldMapWindow;
    private JButton _buttonProjectionParams;
    private FileArrayEditor _inputProductEditor;
    private Product _exampleProduct;
    private File _requestFile;
    private Parameter _paramUpdateMode;
    private JTable _conditionsTable;
    private JTable _variablesTable;
    private AbstractButton _newVariableButton;
    private AbstractButton _removeVariableButton;
    private AbstractButton _moveVariableUpButton;
    private AbstractButton _moveVariableDownButton;
    private AbstractButton _variableFilterButton;
    private AbstractButton _newConditionsButton;
    private AbstractButton _removeConditionButton;
    private AbstractButton _moveConditionUpButton;
    private AbstractButton _moveConditionDownButton;
    private Parameter _paramConditionsOperator;
    private GeoPos[][] _inputProductBoundaries;
    private JCheckBox _displayInputProductsCheck;
    private Parameter _logToOutputParameter;
    private Parameter _logPrefixParameter;
    private TableColumn _outputColumn;
    private JLabel _labelWidthInfo;
    private JLabel _labelHeightInfo;
    private JLabel _labelCenterLatInfo;
    private JLabel _labelCenterLonInfo;
    private Parameter _paramOrthorectify;
    private Parameter _paramElevation;
    public static final String LABEL_NAME_CENTER_LAT = "Center latitude";
    public static final String LABEL_NAME_SCENE_HEIGHT = "Scene height";
    public static final String LABEL_NAME_SCENE_WIDTH = "Scene width";
    public static final String LABEL_NAME_CENTER_LON = "Center longitude";
    private static final String DEFAULT_MAP_PROJECTION_NAME = IdentityTransformDescriptor.NAME;

    public MosaicUi() {
        _reqElemFac = MosaicRequestElementFactory.getInstance();
    }

    /**
     * Retrieves the base component for the processor specific user interface classes. This can be any Java Swing
     * containertype.
     */
    @Override
    public JComponent getGuiComponent() {
        return createTabbedPane();
    }

    /**
     * Retrieves the list of requests currently edited.
     */
    @Override
    public Vector getRequests() throws ProcessorException {
        final Vector<Request> requests = new Vector<Request>();
        requests.add(getRequest());
        return requests;
    }

    /**
     * Sets a new Request to be edited.
     */
    @Override
    public void setRequests(Vector requests) throws ProcessorException {
        Guardian.assertNotNull("requests", requests);
        if (!requests.isEmpty()) {
            for (int i = 0; i < requests.size(); i++) {
                Request request = (Request) requests.elementAt(i);
                if (request == null) {
                    continue;
                }
                if (MosaicConstants.REQUEST_TYPE.equalsIgnoreCase(request.getType())) {
                    setRequest(request);
                    break;
                }
            }
        } else {
            setDefaultRequests();
        }
    }

    /**
     * Create a set of new default requests.
     */
    @Override
    public void setDefaultRequests() throws ProcessorException {
        disposeExampleProduct();
        setDefaultRequest();
    }

    /**
     * Sets the processor app for the UI
     */
    @Override
    public void setApp(ProcessorApp app) {
        super.setApp(app);
        // This shall remove the default output validator, but as a side effect it removes
        // also other validators. Currently there is only the default output validator. (mp - 06.11.2007)
        removeAllRequestValidators(app);

        app.addRequestValidator(new RequestValidator() {
            @Override
            public boolean validateRequest(Processor processor, Request request) {
                if (!MosaicConstants.REQUEST_TYPE.equals(request.getType())) {
                    return true;
                }
                if (_paramUpdateMode != null && (Boolean) _paramUpdateMode.getValue()) {
                    ((MosaicProcessor) processor).setProgressBarDepth(2);
                } else {
                    ((MosaicProcessor) processor).setProgressBarDepth(3);
                }
                return validateProjectionParameters()
                       && validateProcessingParameters()
                       && validateOutputProduct()
                       && validateInputProducts()
                       && validateProductSize();
            }
        });
        final ParamGroup paramGroup = new ParamGroup();
        paramGroup.addParameter(_paramOutputProduct);
        app.markIODirChanges(paramGroup);

    }

    private void removeAllRequestValidators(ProcessorApp app) {
        RequestValidator[] requestValidators = app.getRequestValidators();
        for (RequestValidator validator : requestValidators) {
            app.removeRequestValidator(validator);
        }
    }

    private JPanel createTabbedPane() {
        initParameters();
        final JPanel panel = new JPanel(new BorderLayout());
        final JTabbedPane tabbedPane = new JTabbedPane();
        addTabs(tabbedPane);
        panel.add(tabbedPane, BorderLayout.NORTH);
        panel.add(new JLabel("  "), BorderLayout.CENTER);
        panel.add(_paramUpdateMode.getEditor().getEditorComponent(), BorderLayout.SOUTH);
        return panel;
    }


    private void addTabs(final JTabbedPane panel) {
        panel.add("I/O Parameters", createIOPane()); /*I18N*/
        panel.addTab("Product Definition", createProductPanel()); /*I18N*/
        panel.addTab("Processing Parameters", createProcessingPanel());  /*I18N*/
    }

    private Component createIOPane() {
        final FileArrayEditor.EditorParent parent = new FileArrayEditor.EditorParent() {
            @Override
            public File getUserInputDir() {
                return getInputProductDir();
            }

            @Override
            public void setUserInputDir(File newDir) {
                setInputProductDir(newDir);
            }
        };
        _inputProductEditor = new FileArrayEditor(parent, "Input products");
        final FileArrayEditor.FileArrayEditorListener listener = new FileArrayEditor.FileArrayEditorListener() {
            @Override
            public void updatedList(final File[] files) {
                if (files == null || files.length == 0) {
                    _inputProductBoundaries = null;
                    if (_worldMapWindow != null) {
                        _worldMapWindow.setPathesToDisplay(null);
                    }
                    _paramUpdateMode.setDefaultValue();
                    _paramUpdateMode.setUIEnabled(false);
                    return;
                }
                _paramUpdateMode.setUIEnabled(true);
                final int filesSize = files.length;
                for (int i = 0; i < filesSize; i++) {
                    if (setExampleProduct(files[i])) {
                        break;
                    }
                }

                if (isWorldMapWindowVisible()) {
                    scanInputProducts(getApp().getMainFrame());
                }
            }
        };
        _inputProductEditor.setListener(listener);


        JPanel panel = GridBagUtils.createPanel();
        final GridBagConstraints gbc = GridBagUtils.createDefaultConstraints();
        gbc.gridy = 1;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets.top = LARGE_INSETS_TOP;

        gbc.gridy++;
        final ParamEditor editor = _paramOutputProduct.getEditor();
        panel.add(editor.getLabelComponent(), gbc);
        gbc.gridy++;
        gbc.insets.top = STANDARD_INSETS_TOP;
        panel.add(editor.getComponent(), gbc);

        gbc.gridy++;
        gbc.insets.top = LARGE_INSETS_TOP;
        final JPanel ui = (JPanel) _inputProductEditor.getUI();
        ui.setBorder(null);
        panel.add(ui, gbc);

        // logging
        // -------
        gbc.gridy++;
        GridBagUtils.addToPanel(panel, _logPrefixParameter.getEditor().getLabelComponent(), gbc);
        gbc.gridy++;
        gbc.insets.top = 0;
        GridBagUtils.addToPanel(panel, _logPrefixParameter.getEditor().getComponent(), gbc);

        gbc.gridy++;
        gbc.insets.top = STANDARD_INSETS_TOP;
        GridBagUtils.addToPanel(panel, _logToOutputParameter.getEditor().getComponent(), gbc);


        gbc.gridy++;
        gbc.weighty = 999;
        panel.add(new JLabel(""), gbc);

        return panel;
    }

    private void scanInputProducts(final Component parentComponent) {
        final List files = _inputProductEditor.getFiles();
        if (files == null) {
            return;
        }
        final int filesSize = files.size();
        final List<String> messages = new ArrayList<String>(2);
        final List<GeoPos[]> geoBoundaries = new ArrayList<GeoPos[]>(files.size());
        SwingWorker worker = new SwingWorker() {
            ProgressMonitor pm = new DialogProgressMonitor(parentComponent, "Scanning Input Products",
                                                           Dialog.ModalityType.APPLICATION_MODAL);

            @Override
            protected Object doInBackground() throws Exception {
                try {
                    pm.beginTask("Scanning input products", filesSize);
                    for (int i = 0; i < filesSize; i++) {
                        if (pm.isCanceled()) {
                            break;
                        }
                        pm.setSubTaskName(String.format("Product %d of %d", (i + 1), filesSize));
                        final File file = (File) files.get(i);
                        final Product product = loadProduct(file, messages);
                        try {
                            if (product != null) {
                                final int step = Math.max(16,
                                                          (product.getSceneRasterWidth() +
                                                           product.getSceneRasterHeight()) / 250);
                                final GeoPos[] geoBoundary = ProductUtils.createGeoBoundary(product, step);
                                geoBoundaries.add(geoBoundary);
                            }
                        } finally {
                            if (product != null) {
                                product.dispose();
                            }
                        }
                        pm.worked(1);
                    }
                } finally {
                    pm.done();
                }
                return null;
            }

            @Override
            protected void done() {
                if (pm.isCanceled()) {
                    _displayInputProductsCheck.setSelected(false);
                } else {
                    if (messages.size() > 0) {
                        String warningMessage = "There where errors during scanning of input products."; /*I18N*/
                        String[] warningDetails = messages.toArray(new String[messages.size()]);
                        getApp().showWarningsDialog(warningMessage, warningDetails);
                    }
                    _inputProductBoundaries = geoBoundaries.toArray(new GeoPos[geoBoundaries.size()][]);
                    if (isWorldMapWindowVisible() && _displayInputProductsCheck.isSelected()) {
                        _worldMapWindow.setPathesToDisplay(_inputProductBoundaries);
                    }
                }
            }
        };

        worker.execute();
    }


    private void setInputProductDir(final File currentDirectory) {
        final String inputProductDirKey = ProcessorApp.INPUT_PRODUCT_DIR_PREFERENCES_KEY;
        getApp().getPreferences().setPropertyString(inputProductDirKey,
                                                    currentDirectory.getAbsolutePath());
    }

    private File getInputProductDir() {
        final String inputProductDirKey = ProcessorApp.INPUT_PRODUCT_DIR_PREFERENCES_KEY;
        final String path = getApp().getPreferences().getPropertyString(inputProductDirKey);
        final File inputProductDir;
        if (path != null) {
            inputProductDir = new File(path);
        } else {
            inputProductDir = null;
        }
        return inputProductDir;
    }

    private boolean setExampleProduct(File selectedFile) {
        _exampleProduct = null;
        _exampleProduct = loadProduct(selectedFile, null);
        return _exampleProduct != null;
    }

    private JPanel createProcessingPanel() {
        GridBagConstraints gbc;

        final JPanel panel = GridBagUtils.createPanel();
        gbc = GridBagUtils.createDefaultConstraints();
        gbc.gridy = 0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.weightx = 1;

        gbc.insets.top = LARGE_INSETS_TOP;
        gbc.gridy++;
        gbc.weighty = 40;
        panel.add(createVariablesPanel(), gbc);

        gbc.gridy++;
        gbc.insets.top = 40;
        gbc.weighty = 20;
        panel.add(createConditionsPanel(), gbc);

        return panel;
    }

    private Component createConditionsPanel() {
        final String labelName = "Valid pixel conditions";
        final JPanel panel = GridBagUtils.createPanel();
        panel.setName(labelName);
        final GridBagConstraints gbc = GridBagUtils.createDefaultConstraints();
        gbc.gridy = 0;
        gbc.weightx = 1;

        gbc.gridy++;
        gbc.insets.bottom = 0;
        panel.add(new JLabel(labelName + ":"), gbc); /*I18N*/
        gbc.anchor = GridBagConstraints.EAST;
        final JPanel conditionButtonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        conditionButtonsPanel.setName(labelName);

        final Component newConditionButton = createNewConditionButton();
        newConditionButton.setName(labelName);
        conditionButtonsPanel.add(newConditionButton);

        final Component removeConditionButton = createRemoveConditionButton();
        removeConditionButton.setName(labelName);
        conditionButtonsPanel.add(removeConditionButton);

        final Component moveConditionUpButton = createMoveConditionUpButton();
        moveConditionUpButton.setName(labelName);
        conditionButtonsPanel.add(moveConditionUpButton);

        final Component moveConditionDownButton = createMoveConditionDownButton();
        moveConditionDownButton.setName(labelName);
        conditionButtonsPanel.add(moveConditionDownButton);

        panel.add(conditionButtonsPanel, gbc);

        gbc.gridy++;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.BOTH;
        panel.add(createConditionsTable(labelName), gbc);

        gbc.gridy++;
        gbc.fill = GridBagConstraints.NONE;
        gbc.anchor = GridBagConstraints.WEST;
        final JPanel operatorPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        final ParamEditor editor = _paramConditionsOperator.getEditor();
        operatorPanel.add(editor.getLabelComponent());
        operatorPanel.add(editor.getEditorComponent());
        panel.add(operatorPanel, gbc);

        return panel;
    }

    private Component createVariablesPanel() {
        final String labelName = "Output variables";  /*I18N*/

        final JPanel panel = GridBagUtils.createPanel();
        panel.setName(labelName);
        final GridBagConstraints gbc = GridBagUtils.createDefaultConstraints();
        gbc.gridy = 0;
        gbc.weightx = 1;

        gbc.gridy++;
        gbc.insets.bottom = 0;
        panel.add(new JLabel(labelName + ":"), gbc);
        final JPanel variableButtonsPanel = new JPanel(new FlowLayout(FlowLayout.LEFT, 5, 0));
        variableButtonsPanel.setName(labelName);

        final Component bandFileterButton = createBandFilterButton();
        bandFileterButton.setName(labelName);
        variableButtonsPanel.add(bandFileterButton);

        final Component newVariableButton = createNewVariableButton();
        newVariableButton.setName(labelName);
        variableButtonsPanel.add(newVariableButton);

        final Component removeVariableButton = createRemoveVariableButton();
        removeVariableButton.setName(labelName);
        variableButtonsPanel.add(removeVariableButton);

        final Component moveVariableUpButton = createMoveVariableUpButton();
        moveVariableUpButton.setName(labelName);
        variableButtonsPanel.add(moveVariableUpButton);

        final Component moveVariableDownButton = createMoveVariableDownButton();
        moveVariableDownButton.setName(labelName);
        variableButtonsPanel.add(moveVariableDownButton);

        gbc.anchor = GridBagConstraints.EAST;
        panel.add(variableButtonsPanel, gbc);
        gbc.gridy++;
        gbc.gridwidth = 2;
        gbc.fill = GridBagConstraints.BOTH;
        final JScrollPane valuesTable = createValuesTable(labelName);
        panel.add(valuesTable, gbc);

        return panel;
    }

    private JScrollPane createValuesTable(final String labelName) {
        final DefaultTableModel dataModel = new DefaultTableModel();
        dataModel.setColumnIdentifiers(new String[]{"Name", "Data Source Expression"}); /*I18N*/

        _variablesTable = new JTable();
        _variablesTable.setName(labelName);
        _variablesTable.setRowSelectionAllowed(true);
        _variablesTable.setModel(dataModel);
        _variablesTable.addMouseListener(createExpressionEditorMouseListener(_variablesTable, false));

        final JTableHeader tableHeader = _variablesTable.getTableHeader();
        tableHeader.setName(labelName);
        tableHeader.setReorderingAllowed(false);
        tableHeader.setResizingAllowed(true);

        final TableColumnModel columnModel = _variablesTable.getColumnModel();
        columnModel.setColumnSelectionAllowed(false);

        final TableColumn nameColumn = columnModel.getColumn(0);
        nameColumn.setPreferredWidth(100);
        nameColumn.setCellRenderer(new TCR());

        final TableColumn expressionColumn = columnModel.getColumn(1);
        expressionColumn.setPreferredWidth(400);
        expressionColumn.setCellRenderer(new TCR());
        expressionColumn.setCellEditor(new ExprEditor(false));

        final JScrollPane scrollPane = new JScrollPane(_variablesTable);
        scrollPane.setName(labelName);
        scrollPane.setPreferredSize(new Dimension(_PREFERRED_TABLE_WIDTH, 150));

        return scrollPane;
    }

    private JScrollPane createConditionsTable(final String labelName) {
        final DefaultTableModel dataModel = new DefaultTableModel();
        dataModel.setColumnIdentifiers(new String[]{"Name", "Test Expression", "Outp."}); /*I18N*/

        _conditionsTable = new JTable() {
            private static final long serialVersionUID = 1L;

            @Override
            public Class getColumnClass(int column) {
                if (column == 2) {
                    return Boolean.class;
                } else {
                    return super.getColumnClass(column);
                }
            }
        };
        _conditionsTable.setName(labelName);
        _conditionsTable.setRowSelectionAllowed(true);
        _conditionsTable.setModel(dataModel);
        _conditionsTable.addMouseListener(createExpressionEditorMouseListener(_conditionsTable, true));

        final JTableHeader tableHeader = _conditionsTable.getTableHeader();
        tableHeader.setName(labelName);
        tableHeader.setReorderingAllowed(false);
        tableHeader.setResizingAllowed(true);

        final TableColumnModel columnModel = _conditionsTable.getColumnModel();
        columnModel.setColumnSelectionAllowed(false);

        final TableColumn nameColumn = columnModel.getColumn(0);
        nameColumn.setPreferredWidth(100);
        nameColumn.setCellRenderer(new TCR());

        final TableColumn expressionColumn = columnModel.getColumn(1);
        expressionColumn.setPreferredWidth(360);
        expressionColumn.setCellRenderer(new TCR());
        expressionColumn.setCellEditor(new ExprEditor(true));

        final TableColumn outColumn = columnModel.getColumn(2);
        outColumn.setPreferredWidth(40);

        final JScrollPane pane = new JScrollPane(_conditionsTable);
        pane.setName(labelName);
        pane.setPreferredSize(new Dimension(_PREFERRED_TABLE_WIDTH, 80));

        return pane;
    }

    private MouseListener createExpressionEditorMouseListener(final JTable table, final boolean booleanExpected) {
        final MouseAdapter mouseListener = new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    final int column = table.getSelectedColumn();
                    if (column == 1) {
                        table.removeEditor();
                        final int row = table.getSelectedRow();
                        final String[] value = new String[]{(String) table.getValueAt(row, column)};
                        final int i = editExpression(value, booleanExpected);
                        if (ModalDialog.ID_OK == i) {
                            table.setValueAt(value[0], row, column);
                        }
                    }
                }
            }
        };
        return MouseEventFilterFactory.createFilter(mouseListener);
    }

    private Component createNewVariableButton() {
        _newVariableButton = createButton("icons/Plus24.gif", "newVariable");
        _newVariableButton.setToolTipText("Add new processing variable"); /*I18N*/
        _newVariableButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                final int rows = _variablesTable.getRowCount();
                addRow(_variablesTable, new Object[]{"variable_" + rows, ""}); /*I18N*/
            }
        });
        return _newVariableButton;
    }

    private Component createRemoveVariableButton() {
        _removeVariableButton = createButton("icons/Minus24.gif", "removeVariable");
        _removeVariableButton.setToolTipText("Remove selected rows."); /*I18N*/
        _removeVariableButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                removeRows(_variablesTable, _variablesTable.getSelectedRows());
            }
        });
        return _removeVariableButton;
    }

    private Component createMoveVariableUpButton() {
        _moveVariableUpButton = createButton("icons/MoveUp24.gif", "moveVariableUp");
        _moveVariableUpButton.setToolTipText("Move up selected rows."); /*I18N*/
        _moveVariableUpButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                moveRowsUp(_variablesTable, _variablesTable.getSelectedRows());
            }
        });
        return _moveVariableUpButton;
    }

    private Component createMoveVariableDownButton() {
        _moveVariableDownButton = createButton("icons/MoveDown24.gif", "moveVariableDown");
        _moveVariableDownButton.setToolTipText("Move down selected rows."); /*I18N*/
        _moveVariableDownButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                moveRowsDown(_variablesTable, _variablesTable.getSelectedRows());
            }
        });
        return _moveVariableDownButton;
    }

    private Component createBandFilterButton() {
        _variableFilterButton = createButton("icons/Copy16.gif", "bandButton");
        _variableFilterButton.setToolTipText("Choose the bands to process"); /*I18N*/
        _variableFilterButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final Product exampleProduct = getExampleProduct();
                if (exampleProduct != null) {
                    final String[] availableBandNames = exampleProduct.getBandNames();
                    final Band[] allBands = exampleProduct.getBands();
                    final List dataVector = ((DefaultTableModel) _variablesTable.getModel()).getDataVector();
                    final List<Band> existingBands = new ArrayList<Band>(dataVector.size());
                    for (Object aDataVector : dataVector) {
                        List row = (List) aDataVector;
                        final String name = (String) row.get(0);
                        final String expression = (String) row.get(1);
                        if (name == null || expression == null
                            || !StringUtils.contains(availableBandNames, name.trim())
                            || !name.trim().equals(expression.trim())) {
                            continue;
                        }
                        existingBands.add(exampleProduct.getBand(name.trim()));
                    }
                    final BandChooser bandChooser = new BandChooser(getApp().getMainFrame(), "Band Chooser", "",
                                                                    allBands, /*I18N*/
                                                                    existingBands.toArray(
                                                                            new Band[existingBands.size()]));
                    if (bandChooser.show() == ModalDialog.ID_OK) {
                        final Band[] selectedBands = bandChooser.getSelectedBands();
                        for (Band selectedBand : selectedBands) {
                            if (!existingBands.contains(selectedBand)) {
                                final String name = selectedBand.getName();
                                addRow(_variablesTable, new Object[]{name, name});
                            } else {
                                existingBands.remove(selectedBand);
                            }
                        }
                        final int[] rowsToRemove = new int[0];
                        final List newDataVector = ((DefaultTableModel) _variablesTable.getModel()).getDataVector();
                        for (Band existingBand : existingBands) {
                            String bandName = existingBand.getName();
                            final int rowIndex = getBandRow(newDataVector, bandName);
                            if (rowIndex > -1) {
                                ArrayUtils.addToArray(rowsToRemove, rowIndex);
                            }
                        }
                        removeRows(_variablesTable, rowsToRemove);
                    }
                }
            }
        });
        return _variableFilterButton;
    }

    private Component createNewConditionButton() {
        _newConditionsButton = createButton("icons/Plus24.gif", "newCondition");
        _newConditionsButton.setToolTipText("Add new processing condition"); /*I18N*/
        _newConditionsButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                final int rows = _conditionsTable.getRowCount();
                addRow(_conditionsTable, new Object[]{"condition_" + rows, "", false}); /*I18N*/
            }
        });
        return _newConditionsButton;
    }

    private Component createRemoveConditionButton() {
        _removeConditionButton = createButton("icons/Minus24.gif", "removeCondition");
        _removeConditionButton.setToolTipText("Remove selected rows."); /*I18N*/
        _removeConditionButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                removeRows(_conditionsTable, _conditionsTable.getSelectedRows());
            }
        });
        return _removeConditionButton;
    }

    private Component createMoveConditionUpButton() {
        _moveConditionUpButton = createButton("icons/MoveUp24.gif", "moveConditionUp");
        _moveConditionUpButton.setToolTipText("Move up selected rows."); /*I18N*/
        _moveConditionUpButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                moveRowsUp(_conditionsTable, _conditionsTable.getSelectedRows());
            }
        });
        return _moveConditionUpButton;
    }

    private Component createMoveConditionDownButton() {
        _moveConditionDownButton = createButton("icons/MoveDown24.gif", "moveConditionDown");
        _moveConditionDownButton.setToolTipText("Move down selected rows."); /*I18N*/
        _moveConditionDownButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                moveRowsDown(_conditionsTable, _conditionsTable.getSelectedRows());
            }
        });
        return _moveConditionDownButton;
    }

    private static void addRow(final JTable table, final Object[] rowData) {
        table.removeEditor();
        ((DefaultTableModel) table.getModel()).addRow(rowData);
        final int row = table.getRowCount() - 1;
        final int numCols = table.getColumnModel().getColumnCount();
        for (int i = 0; i < Math.min(numCols, rowData.length); i++) {
            Object o = rowData[i];
            table.setValueAt(o, row, i);
        }
        selectRows(table, row, row);
    }

    private static void moveRowsUp(final JTable table, final int[] rows) {
        for (int row1 : rows) {
            if (row1 == 0) {
                return;
            }
        }
        table.removeEditor();
        for (int row : rows) {
            ((DefaultTableModel) table.getModel()).moveRow(row, row, row - 1);
        }
        selectRows(table, rows);
    }

    private static void moveRowsDown(final JTable table, final int[] rows) {
        final int maxRow = table.getRowCount() - 1;
        for (int row1 : rows) {
            if (row1 == maxRow) {
                return;
            }
        }
        table.removeEditor();
        for (int i = rows.length - 1; i > -1; i--) {
            int row = rows[i];
            ((DefaultTableModel) table.getModel()).moveRow(row, row, row + 1);
            rows[i]++;
        }
        selectRows(table, rows);
    }

    private static void selectRows(JTable table, int min, int max) {
        final int numRows = max + 1 - min;
        if (numRows <= 0) {
            return;
        }
        selectRows(table, prepareRows(numRows, min));
    }

    private static void selectRows(final JTable table, final int[] rows) {
        final ListSelectionModel selectionModel = table.getSelectionModel();
        selectionModel.clearSelection();
        for (int row : rows) {
            selectionModel.addSelectionInterval(row, row);
        }
    }

    private static void removeRows(JTable table, int min, int max) {
        final int numRows = max + 1 - min;
        if (numRows <= 0) {
            return;
        }
        removeRows(table, prepareRows(numRows, min));
    }

    private static void removeRows(final JTable table, final int[] rows) {
        table.removeEditor();
        for (int i = rows.length - 1; i > -1; i--) {
            int row = rows[i];
            ((DefaultTableModel) table.getModel()).removeRow(row);
        }
    }

    private static int[] prepareRows(final int numRows, int min) {
        final int[] rows = new int[numRows];
        for (int i = 0; i < rows.length; i++) {
            rows[i] = min + i;
        }
        return rows;
    }

    private static int getBandRow(List newDataVector, String bandName) {
        for (int i = 0; i < newDataVector.size(); i++) {
            List row = (List) newDataVector.get(i);
            if (bandName.equals(row.get(0)) && bandName.equals(row.get(1))) {
                return i;
            }
        }
        return -1;
    }

    private static AbstractButton createButton(final String path, String name) {
        AbstractButton button = ToolButtonFactory.createButton(UIUtils.loadImageIcon(path), false);
        button.setName(name);
        return button;
    }

    private void initParameters() {
        _labelWidthInfo = new JLabel(_defaultNumberText);
        _labelHeightInfo = new JLabel(_defaultNumberText);
        _labelCenterLatInfo = new JLabel(_defaultLatLonText);
        _labelCenterLonInfo = new JLabel(_defaultLatLonText);

        try {
            _paramOutputProduct = _reqElemFac.createDefaultOutputProductParameter();

            try {
                _logToOutputParameter = _reqElemFac.createLogToOutputParameter("false");
            } catch (ParamValidateException ignored) {
                // ignore
            }
            _logPrefixParameter = _reqElemFac.createDefaultLogPatternParameter(MosaicConstants.DEFAULT_LOG_PREFIX);

            _paramUpdateMode = _reqElemFac.createParameter(MosaicConstants.PARAM_NAME_UPDATE_MODE, null);
            _paramWestLon = _reqElemFac.createParameter(MosaicConstants.PARAM_NAME_WEST_LON, null);
            _paramEastLon = _reqElemFac.createParameter(MosaicConstants.PARAM_NAME_EAST_LON, null);
            _paramNorthLat = _reqElemFac.createParameter(MosaicConstants.PARAM_NAME_NORTH_LAT, null);
            _paramSouthLat = _reqElemFac.createParameter(MosaicConstants.PARAM_NAME_SOUTH_LAT, null);
            _paramPixelSizeX = _reqElemFac.createParameter(MosaicConstants.PARAM_NAME_PIXEL_SIZE_X, null);
            _paramPixelSizeY = _reqElemFac.createParameter(MosaicConstants.PARAM_NAME_PIXEL_SIZE_Y, null);
            _paramConditionsOperator = _reqElemFac.createParameter(MosaicConstants.PARAM_NAME_CONDITION_OPERATOR, null);
            _paramOrthorectify = _reqElemFac.createParameter(MosaicConstants.PARAM_NAME_ORTHORECTIFY_INPUT_PRODUCTS,
                                                             null);
            _paramElevation = _reqElemFac.createParameter(
                    MosaicConstants.PARAM_NAME_ELEVATION_MODEL_FOR_ORTHORECTIFICATION, null);
        } catch (RequestElementFactoryException e) {
            throw new IllegalStateException("Unable to initialize parameters for processor UI.", e);
        }

        _projections = MapProjectionRegistry.getProjections();
        final int length = _projections.length;
        String[] _projectionNames = new String[length];
        Dimension maxMapUnitPreferredSize = new Dimension(0, 0);
        for (int i = 0; i < _projections.length; i++) {
            _projections[i] = (MapProjection) _projections[i].clone();
            _projectionNames[i] = _projections[i].getName();
            final JLabel label = new JLabel(" " + _projections[i].getMapUnit());
            final Dimension preferredSize = label.getPreferredSize();
            if (preferredSize.width > maxMapUnitPreferredSize.width) {
                maxMapUnitPreferredSize.width = preferredSize.width;
            }
            if (preferredSize.height > maxMapUnitPreferredSize.height) {
                maxMapUnitPreferredSize.height = preferredSize.height;
            }
        }

        Arrays.sort(_projectionNames);
        _paramProjectionName = new Parameter(MosaicConstants.PARAM_NAME_PROJECTION_NAME, DEFAULT_MAP_PROJECTION_NAME);
        _paramProjectionName.getProperties().setValueSet(_projectionNames);
        _paramProjectionName.getProperties().setValueSetBound(true);
        _paramProjectionName.getProperties().setDefaultValue(DEFAULT_MAP_PROJECTION_NAME);
        _paramProjectionName.getProperties().setLabel(MosaicConstants.PARAM_LABEL_PROJECTION_NAME);

        addParamListeners();
    }

    private void addParamListeners() {
        _paramProjectionName.addParamChangeListener(new ParamChangeListener() {
            @Override
            public void parameterValueChanged(ParamChangeEvent event) {
                updateButtonProjectionParamsEnabled();
                updatePixelSize();
                computeOutputProduct();
            }
        });

        final ParamChangeListener cornerCoordinateListener = new ParamChangeListener() {
            @Override
            public void parameterValueChanged(ParamChangeEvent event) {
                computeOutputProduct();
            }
        };

        _paramWestLon.addParamChangeListener(cornerCoordinateListener);
        _paramEastLon.addParamChangeListener(cornerCoordinateListener);
        _paramNorthLat.addParamChangeListener(cornerCoordinateListener);
        _paramSouthLat.addParamChangeListener(cornerCoordinateListener);
        _paramPixelSizeX.addParamChangeListener(cornerCoordinateListener);
        _paramPixelSizeY.addParamChangeListener(cornerCoordinateListener);

        final ParamChangeListener changeSouthMax = new ParamChangeListener() {
            @Override
            public void parameterValueChanged(ParamChangeEvent event) {
                _paramSouthLat.getProperties().setMaxValue((Number) _paramNorthLat.getValue());
            }
        };
        changeSouthMax.parameterValueChanged(null); // this call is necessary to initialize the south latitude parameter
        _paramNorthLat.addParamChangeListener(changeSouthMax);

        final ParamChangeListener changeNorthMin = new ParamChangeListener() {
            @Override
            public void parameterValueChanged(ParamChangeEvent event) {
                _paramNorthLat.getProperties().setMinValue((Number) _paramSouthLat.getValue());
            }
        };
        changeNorthMin.parameterValueChanged(null); // this call is necessary to initialize the north latitude parameter
        _paramSouthLat.addParamChangeListener(changeNorthMin);


        _paramOutputProduct.addParamChangeListener(new ParamChangeListener() {
            @Override
            public void parameterValueChanged(ParamChangeEvent event) {
                updateProductDefinitionAndProcessingParameters();
            }
        });

        _paramUpdateMode.addParamChangeListener(new ParamChangeListener() {
            @Override
            public void parameterValueChanged(ParamChangeEvent event) {
                updateProductDefinitionAndProcessingParameters();

                final boolean enable = !isUpdateMode();
                _variablesTable.setEnabled(enable);
                _conditionsTable.setEnabled(enable);

                _newVariableButton.setEnabled(enable);
                _removeVariableButton.setEnabled(enable);
                _moveVariableUpButton.setEnabled(enable);
                _moveVariableDownButton.setEnabled(enable);
                _variableFilterButton.setEnabled(enable);

                _newConditionsButton.setEnabled(enable);
                _removeConditionButton.setEnabled(enable);
                _moveConditionUpButton.setEnabled(enable);
                _moveConditionDownButton.setEnabled(enable);
                _paramConditionsOperator.setUIEnabled(enable);

                _paramPixelSizeX.setUIEnabled(enable);
                _paramPixelSizeY.setUIEnabled(enable);
                _paramNorthLat.setUIEnabled(enable);
                _paramWestLon.setUIEnabled(enable);
                _paramEastLon.setUIEnabled(enable);
                _paramSouthLat.setUIEnabled(enable);
                _paramProjectionName.setUIEnabled(enable);
                updateButtonProjectionParamsEnabled();

                _paramOrthorectify.setUIEnabled(enable);
                updateElevationEnabled();
            }
        });
        _paramOrthorectify.addParamChangeListener(new ParamChangeListener() {
            @Override
            public void parameterValueChanged(ParamChangeEvent event) {
                updateElevationEnabled();
            }
        });
        _paramConditionsOperator.addParamChangeListener(new ParamChangeListener() {
            @Override
            public void parameterValueChanged(ParamChangeEvent event) {
                final String and = MosaicConstants.PARAM_VALUESET_CONDITIONS_OPERATOR[1];
                final boolean isAnd = _paramConditionsOperator.getValueAsText().equals(and);
                final TableColumnModel columnModel = _conditionsTable.getColumnModel();
                if (isAnd) {
                    _outputColumn = columnModel.getColumn(2);
                    columnModel.removeColumn(_outputColumn);
                } else {
                    columnModel.addColumn(_outputColumn);
                }
            }
        });
    }

    private void updateElevationEnabled() {
        _paramElevation.setUIEnabled(!isUpdateMode() && MosaicUtils.isTrue(_paramOrthorectify));
    }

    private void updateProductDefinitionAndProcessingParameters() {
        if (isUpdateMode()) {

            final Product outputProduct = loadOutputProduct();
            if (outputProduct != null) {
                try {
                    final Parameter[] parameters = MosaicUtils.extractProcessingParameters(outputProduct);
                    setProductDefinitionAndProcessingParameters(parameters);
                } catch (ProcessorException e) {
                    getApp().showErrorDialog("The given output product cannot be used in update mode."); /*I18N*/
                } finally {
                    outputProduct.dispose();
                }
            }
        }
    }

    private void updateButtonProjectionParamsEnabled() {
        _buttonProjectionParams.setEnabled(!isUpdateMode() && getProjection().hasMapTransformUI());
    }

    private JPanel createProductPanel() {
        JPanel panel = GridBagUtils.createPanel();
        final GridBagConstraints gbc = GridBagUtils.createDefaultConstraints();
        gbc.gridy = 1;
        gbc.weightx = 1;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.insets.top = LARGE_INSETS_TOP;

        gbc.gridy++;
        gbc.insets.top = LARGE_INSETS_TOP;
        panel.add(createProjectionPanel(), gbc);

        gbc.gridy++;
        gbc.insets.top = LARGE_INSETS_TOP;
        panel.add(createOutputParameterPanel(), gbc);

//        final Dimension preferredSize = cornerPanel.getPreferredSize();
//        preferredSize.width = Math.max(preferredSize.width, 500);
//        cornerPanel.setPreferredSize(preferredSize);

        gbc.gridy++;
        gbc.insets.top = LARGE_INSETS_TOP;
        panel.add(createInfoPanel(), gbc);

        gbc.gridy++;
        gbc.weighty = 999;
        panel.add(new JLabel(""), gbc);

        return panel;
    }

    private void updateOutputProductInformation(Product outputProduct) {
        if (outputProduct != null) {
            final MapInfo mapInfo = ((MapGeoCoding) outputProduct.getGeoCoding()).getMapInfo();
            final GeoPos centerGeoPos = ProductUtils.getCenterGeoPos(outputProduct);
            _labelWidthInfo.setText(String.valueOf(mapInfo.getSceneWidth()));
            _labelHeightInfo.setText(String.valueOf(mapInfo.getSceneHeight()));
            _labelCenterLonInfo.setText(centerGeoPos.getLonString());
            _labelCenterLatInfo.setText(centerGeoPos.getLatString());
        } else {
            _labelWidthInfo.setText(_defaultNumberText);
            _labelHeightInfo.setText(_defaultNumberText);
            _labelCenterLonInfo.setText(_defaultLatLonText);
            _labelCenterLatInfo.setText(_defaultLatLonText);
        }
    }

    private JPanel createInfoPanel() {
        final JPanel infoPanel = GridBagUtils.createDefaultEmptyBorderPanel();
        infoPanel.setBorder(UIUtils.createGroupBorder("Output Product Information"));
        final GridBagConstraints gbc = GridBagUtils.createDefaultConstraints();
        final int standardLeft = gbc.insets.left;
        gbc.weightx = 1;

        gbc.gridy++;
        infoPanel.add(new JLabel(LABEL_NAME_SCENE_WIDTH + ":"), gbc);
        infoPanel.add(_labelWidthInfo, gbc);
        _labelWidthInfo.setName(LABEL_NAME_SCENE_WIDTH);
        infoPanel.add(new JLabel("pixel"), gbc);
        gbc.insets.left = 40;
        infoPanel.add(new JLabel(LABEL_NAME_CENTER_LON + ":"), gbc);
        gbc.insets.left = standardLeft;
        gbc.weightx = 2000;
        infoPanel.add(_labelCenterLonInfo, gbc);
        _labelCenterLonInfo.setName(LABEL_NAME_CENTER_LON);
        gbc.weightx = 1;

        gbc.gridy++;
        infoPanel.add(new JLabel(LABEL_NAME_SCENE_HEIGHT + ":"), gbc);
        infoPanel.add(_labelHeightInfo, gbc);
        _labelHeightInfo.setName(LABEL_NAME_SCENE_HEIGHT);
        infoPanel.add(new JLabel("pixel"), gbc);
        gbc.insets.left = 40;
        infoPanel.add(new JLabel(LABEL_NAME_CENTER_LAT + ":"), gbc);
        gbc.insets.left = standardLeft;
        gbc.weightx = 2000;
        infoPanel.add(_labelCenterLatInfo, gbc);
        _labelCenterLatInfo.setName(LABEL_NAME_CENTER_LAT);

        return infoPanel;
    }

    private JPanel createOutputParameterPanel() {
        final JLabel labelEasting = new JLabel("Easting");
        final JLabel labelEastingFrom = new JLabel("from:");
        final JLabel labelEastingTo = new JLabel("to:");
        final JLabel labelEastingDegree = new JLabel(" degree");

        final JLabel labelNorthing = new JLabel("Northing");
        final JLabel labelNorthingFrom = new JLabel("from:");
        final JLabel labelNorthingTo = new JLabel("to:");
        final JLabel labelNorthingDegree = new JLabel(" degree");

        final PropertyChangeListener listener = new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                final boolean enabled = (Boolean) evt.getNewValue();
                labelEasting.setEnabled(enabled);
                labelEastingFrom.setEnabled(enabled);
                labelEastingTo.setEnabled(enabled);
                labelEastingDegree.setEnabled(enabled);
                labelNorthing.setEnabled(enabled);
                labelNorthingFrom.setEnabled(enabled);
                labelNorthingTo.setEnabled(enabled);
                labelNorthingDegree.setEnabled(enabled);
            }
        };
        _paramWestLon.getEditor().getEditorComponent().addPropertyChangeListener("enabled",listener);


        JPanel panel = GridBagUtils.createPanel();
        panel.setBorder(UIUtils.createGroupBorder("Output Parameters")); /*I18N*/
        final GridBagConstraints gbc = GridBagUtils.createDefaultConstraints();
        gbc.gridy = 1;
        gbc.insets.top = STANDARD_INSETS_TOP;

        gbc.gridy++;
        panel.add(labelEasting, gbc); /*I18N*/
        panel.add(labelEastingFrom, gbc); /*I18N*/
        panel.add(UIUtils.createSpinner(_paramWestLon, (double) 1, "#0.00#"), gbc);
        panel.add(labelEastingTo, gbc); /*I18N*/
        panel.add(UIUtils.createSpinner(_paramEastLon, (double) 1, "#0.00#"), gbc);
        gbc.weightx = 40;
        panel.add(labelEastingDegree, gbc);  /*I18N*/
        gbc.weightx = 1;

        gbc.gridy++;
        panel.add(labelNorthing, gbc); /*I18N*/
        panel.add(labelNorthingFrom, gbc); /*I18N*/
        panel.add(UIUtils.createSpinner(_paramSouthLat, (double) 1, "#0.00#"), gbc);
        panel.add(labelNorthingTo, gbc); /*I18N*/
        panel.add(UIUtils.createSpinner(_paramNorthLat, (double) 1, "#0.00#"), gbc);
        gbc.weightx = 40;
        panel.add(labelNorthingDegree, gbc); /*I18N*/
        gbc.weightx = 1;

        gbc.gridy++;
        gbc.gridwidth = 6;
        gbc.insets.top = LARGE_INSETS_TOP;
        panel.add(createPixelSizePanel(), gbc);

        gbc.gridy++;
        gbc.gridwidth = 6;
        gbc.insets.top = LARGE_INSETS_TOP;
        panel.add(createPreviewPanel(), gbc);

        return panel;
    }

    private JPanel createPreviewPanel() {
        _displayInputProductsCheck = new JCheckBox("Display input products"); /*I18N*/
        _displayInputProductsCheck.setOpaque(false);
        final ActionListener scanActionListener = new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (isWorldMapWindowVisible()) {
                    if (_displayInputProductsCheck.isSelected()) {
                        scanInputProducts(_worldMapWindow);
                    } else {
                        _worldMapWindow.setPathesToDisplay(null);
                    }
                }
            }
        };
        _displayInputProductsCheck.addActionListener(scanActionListener);

        final JButton previewButton = new JButton("Open Location Preview..."); /*I18N*/
        previewButton.setEnabled(true);
        previewButton.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                previewButton.setEnabled(false);
                if (_worldMapWindow == null) {
                    _worldMapWindow = new WorldMapWindow(getApp().getMainFrame(), "Mosaic Processor - Location Preview",
                                                         "mosaic"); /*I18N*/
                    _worldMapWindow.pack();
                    _worldMapWindow.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);
                    _worldMapWindow.addWindowListener(new WindowAdapter() {
                        @Override
                        public void windowClosed(WindowEvent e) {
                            previewButton.setEnabled(true);
                            _worldMapWindow = null;
                        }
                    });
                    _worldMapWindow.addComponentListener(new ComponentAdapter() {
                        @Override
                        public void componentResized(ComponentEvent e) {
                            _worldMapWindow.pack();
                        }
                    });
                    UIUtils.centerComponent(_worldMapWindow);
                }
                _worldMapWindow.setVisible(true);
                scanActionListener.actionPerformed(null);
                computeOutputProduct();
            }
        });

        final JPanel previewPanel = new JPanel(new BorderLayout(10, 0));
        previewPanel.add(previewButton, BorderLayout.WEST);
        previewPanel.add(_displayInputProductsCheck, BorderLayout.CENTER);
        return previewPanel;
    }

    private JPanel createPixelSizePanel() {
        final JPanel panel = GridBagUtils.createPanel();
        final GridBagConstraints gbc = GridBagUtils.createDefaultConstraints();

        gbc.gridy++;
        final ParamEditor editorX = _paramPixelSizeX.getEditor();
        panel.add(editorX.getLabelComponent(), gbc);
        panel.add(editorX.getComponent(), gbc);
        panel.add(editorX.getPhysUnitLabelComponent(), gbc);

        // Pixel size Y
        gbc.gridy++;
        final ParamEditor editorY = _paramPixelSizeY.getEditor();
        panel.add(editorY.getLabelComponent(), gbc);
        panel.add(editorY.getComponent(), gbc);
        panel.add(editorY.getPhysUnitLabelComponent(), gbc);

        updatePixelSize();

        return panel;
    }

    private JPanel createProjectionPanel() {
        _buttonProjectionParams = new JButton("Projection Parameters..."); /*I18N*/
        _buttonProjectionParams.setEnabled(false);
        _buttonProjectionParams.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                showProjectionParameterDialog();
            }
        });

        final JPanel panel = GridBagUtils.createPanel();
        panel.setBorder(UIUtils.createGroupBorder("Projection")); /*I18N*/
        final GridBagConstraints gbc = GridBagUtils.createDefaultConstraints();
        gbc.gridy = 1;
        gbc.weightx = 1;
        gbc.insets.top = STANDARD_INSETS_TOP;
        gbc.gridwidth = 1;

        gbc.gridy++;
        panel.add(_paramProjectionName.getEditor().getLabelComponent(), gbc);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.weightx = 2000;
        panel.add(_paramProjectionName.getEditor().getComponent(), gbc);

        gbc.gridy++;
        gbc.weightx = 0;
        gbc.fill = GridBagConstraints.NONE;
        panel.add(new JPanel(), gbc);
        panel.add(_buttonProjectionParams, gbc);
        updateButtonProjectionParamsEnabled();

        gbc.gridy++;
        gbc.gridwidth = 2;
        panel.add(_paramOrthorectify.getEditor().getComponent(), gbc);

        gbc.gridy++;
        gbc.gridwidth = 1;
        panel.add(_paramElevation.getEditor().getLabelComponent(), gbc);
        panel.add(_paramElevation.getEditor().getComponent(), gbc);
        updateElevationEnabled();

        return panel;
    }

    private void showProjectionParameterDialog() {
        MapProjection projection = getProjection();
        final MapTransformUI transformUI = projection.getMapTransformUI();
        final ProjectionParamsDialog dialog = new ProjectionParamsDialog(getApp().getMainFrame(), transformUI);
        if (dialog.show() == ModalDialog.ID_OK) {
            projection.setMapTransform(transformUI.createTransform());
            computeOutputProduct();
        }
    }

    private void updatePixelSize() {
        updateGridCellSizeDefaultValue();
        updatePixelSizeUnit();
    }

    private void updatePixelSizeUnit() {
        final String mapUnit = getProjection().getMapUnit();
        final String mapUnitWithLeadingSapce = " " + mapUnit;

        _paramPixelSizeX.getProperties().setPhysicalUnit(mapUnit);
        _paramPixelSizeY.getProperties().setPhysicalUnit(mapUnit);
        _paramPixelSizeX.getEditor().getPhysUnitLabelComponent().setText(mapUnitWithLeadingSapce);
        _paramPixelSizeY.getEditor().getPhysUnitLabelComponent().setText(mapUnitWithLeadingSapce);
    }

    private void updateGridCellSizeDefaultValue() {
        final MapProjection projection = getProjection();
        final String mapUnit = projection.getMapUnit();
        if ("degree".equalsIgnoreCase(mapUnit)) { /*I18N*/
            _paramPixelSizeX.setValue(0.05f, null);
            _paramPixelSizeY.setValue(0.05f, null);
        } else if ("meter".equalsIgnoreCase(mapUnit)) { /*I18N*/
            _paramPixelSizeX.setValue((float) 1000, null);
            _paramPixelSizeY.setValue((float) 1000, null);
        } else {
            _paramPixelSizeX.setDefaultValue();
            _paramPixelSizeY.setDefaultValue();
        }
    }

    private void computeOutputProduct() {
        Product product = computeProjectedOutputProduct();
        updateOutputProductInformation(product);
        if (isWorldMapWindowVisible()) {
            final Product selectedProduct = _worldMapWindow.getSelectedProduct();
            if (selectedProduct != null) {
                selectedProduct.dispose();
            }
            _worldMapWindow.setSelectedProduct(product);
        }
    }

    private Product computeProjectedOutputProduct() {
        MapProjection projection = getProjection();
        if (projection.getName().startsWith("UTM ")) {
            projection = checkForValidUTMProjection(projection);
        }
        return computeOutputProduct(projection);
    }

    private boolean isWorldMapWindowVisible() {
        return _worldMapWindow != null && _worldMapWindow.isVisible();
    }

    private Product computeOutputProduct(MapProjection projection) {
        if (projection != null) {
            final float pixelSizeX = getFloatValue(_paramPixelSizeX);
            final float pixelSizeY = getFloatValue(_paramPixelSizeY);

            final Rectangle2D outputRect = MosaicUtils.createOutputProductBoundaries(projection, getUpperLeft(),
                                                                                     getLowerRight(), pixelSizeX,
                                                                                     pixelSizeY);
            return MosaicUtils.createGeocodedProduct(outputRect, "temp", projection, pixelSizeX, pixelSizeY);
        }
        return null;
    }

    private MapProjection checkForValidUTMProjection(MapProjection projection) {
        final String projectionName = projection.getName();
        if (projectionName.equals(UTM.AUTO_PROJECTION_NAME)) {
            final GeoPos centerPos = getCenterCoordinate();
            projection = UTM.getSuitableProjection(centerPos);
        } else if (projectionName.startsWith("UTM Zone ")) {
            int zone = MosaicUtils.extractUTMZoneNumber(projectionName);
            int[] validZones = computeValid_UTM_Zones();
            if (!isValidUtmZone(zone, validZones)) {
                _paramProjectionName.setValueAsText(UTM.AUTO_PROJECTION_NAME, null);
                if (_worldMapWindow != null) {
                    _worldMapWindow.setSelectedProduct(null);
                }
                final StringBuffer sb = new StringBuffer();
                for (int i = 0; i < validZones.length; i++) {
                    if (i > 0) {
                        sb.append(", ");
                    }
                    sb.append(validZones[i]);
                }
                getApp().showErrorDialog("The selected projection '" + projectionName + "' " +
                                         "cannot be used with the given corner coordinates.\n" +
                                         "Select the projection named '" + UTM.AUTO_PROJECTION_NAME + "' or an\n" +
                                         "UTM projection with one of the zone numbers\n" + sb); /*I18N*/
                projection = null;
            }
        }
        return projection;
    }

    private static boolean isValidUtmZone(int zone, int[] validZones) {
        for (int validZone : validZones) {
            if (validZone == zone) {
                return true;
            }
        }
        return false;
    }

    private int[] computeValid_UTM_Zones() {
        final MapProjection suitableProjection = UTM.getSuitableProjection(getCenterCoordinate());
        final int zoneNumber = MosaicUtils.extractUTMZoneNumber(suitableProjection.getName());
        final int[] validZones = new int[7];
        for (int i = 0; i < validZones.length; i++) {
            validZones[i] = zoneNumber - 3 + i;
            if (validZones[i] < 0) {
                validZones[i] += 60;
            } else if (validZones[i] > 59) {
                validZones[i] -= 60;
            }
        }
        return validZones;
    }

    private GeoPos getUpperLeft() {
        return new GeoPos(getFloatValue(_paramNorthLat), getFloatValue(_paramWestLon));
    }

    private GeoPos getLowerRight() {
        return new GeoPos(getFloatValue(_paramSouthLat), getFloatValue(_paramEastLon));
    }

    private GeoPos getCenterCoordinate() {
        return MosaicUtils.getCenterCoordinate(getUpperLeft(), getLowerRight());
    }

    private static float getFloatValue(final Parameter param) {
        return (Float) param.getValue();
    }

    private MapProjection getProjection() {
        final String projectionName = _paramProjectionName.getValueAsText();
        for (MapProjection projection : _projections) {
            if (projection.getName().equals(projectionName)) {
                return projection;
            }
        }
        throw new IllegalStateException("Unknown projection name 'projectionName'.");
    }

    private void disposeExampleProduct() {
        if (_exampleProduct != null) {
            _exampleProduct.dispose();
            _exampleProduct = null;
        }
    }

    private Request getRequest() throws RequestElementFactoryException {
        final Request request = new Request();
        request.setFile(_requestFile);
        request.setType(MosaicConstants.REQUEST_TYPE);

        final String outputFile = _paramOutputProduct.getValueAsText();
        request.addOutputProduct(ProcessorUtils.createProductRef(outputFile, DimapProductConstants.DIMAP_FORMAT_NAME));

        final List files = _inputProductEditor.getFiles();
        for (Object file : files) {
            request.addInputProduct(new ProductRef((File) file));
        }

        if (MosaicUtils.isTrue(_paramUpdateMode)) { // update mode
            request.addParameter(
                    _reqElemFac.createParameter(_paramUpdateMode.getName(), _paramUpdateMode.getValueAsText()));
        } else {
            // corner coordinates
            request.addParameter(_reqElemFac.createParameter(_paramWestLon.getName(), _paramWestLon.getValueAsText()));
            request.addParameter(
                    _reqElemFac.createParameter(_paramNorthLat.getName(), _paramNorthLat.getValueAsText()));
            request.addParameter(_reqElemFac.createParameter(_paramEastLon.getName(), _paramEastLon.getValueAsText()));
            request.addParameter(
                    _reqElemFac.createParameter(_paramSouthLat.getName(), _paramSouthLat.getValueAsText()));

            // projection parameters
            final MapProjection projection = checkForValidUTMProjection(getProjection());
            request.addParameter(
                    _reqElemFac.createParameter(MosaicConstants.PARAM_NAME_PROJECTION_NAME, projection.getName()));
            final double[] parameterValues = projection.getMapTransform().getParameterValues();
            final StringBuffer paramValues = new StringBuffer();
            for (int i = 0; i < parameterValues.length; i++) {
                if (i > 0) {
                    paramValues.append(",");
                }
                paramValues.append(parameterValues[i]);
            }
            request.addParameter(_reqElemFac.createParameter(MosaicConstants.PARAM_NAME_PROJECTION_PARAMETERS,
                                                             paramValues.toString()));
            request.addParameter(
                    _reqElemFac.createParameter(_paramPixelSizeX.getName(), _paramPixelSizeX.getValueAsText()));
            request.addParameter(
                    _reqElemFac.createParameter(_paramPixelSizeY.getName(), _paramPixelSizeY.getValueAsText()));
            request.addParameter(
                    _reqElemFac.createParameter(MosaicConstants.PARAM_NAME_NO_DATA_VALUE, null));

            request.addParameter(
                    _reqElemFac.createParameter(_paramOrthorectify.getName(), _paramOrthorectify.getValueAsText()));
            request.addParameter(
                    _reqElemFac.createParameter(_paramElevation.getName(), _paramElevation.getValueAsText()));

            // processing parameters
            final List variablesData = ((DefaultTableModel) _variablesTable.getModel()).getDataVector();
            for (Object aVariablesData : variablesData) {
                final List row = (List) aVariablesData;
                final String name = (String) row.get(0);
                final String expression = (String) row.get(1);
                request.addParameter(
                        _reqElemFac.createParameter(name + MosaicConstants.PARAM_SUFFIX_EXPRESSION, expression));
            }
            final String operator = _paramConditionsOperator.getValueAsText();
            request.addParameter(_reqElemFac.createParameter(_paramConditionsOperator.getName(), operator));
            final List conditionsData = ((DefaultTableModel) _conditionsTable.getModel()).getDataVector();
            for (Object aConditionsData : conditionsData) {
                final List row = (List) aConditionsData;
                final String name = (String) row.get(0);
                final String expression = (String) row.get(1);
                final Boolean isOutput;
                final String or = MosaicConstants.PARAM_VALUESET_CONDITIONS_OPERATOR[0];
                if (or.equals(operator)) {
                    isOutput = (Boolean) row.get(2);
                } else {
                    isOutput = Boolean.FALSE;
                }
                request.addParameter(
                        _reqElemFac.createParameter(name + MosaicConstants.PARAM_SUFFIX_EXPRESSION, expression));
                request.addParameter(
                        _reqElemFac.createParameter(name + MosaicConstants.PARAM_SUFFIX_CONDITION, "true"));
                request.addParameter(
                        _reqElemFac.createParameter(name + MosaicConstants.PARAM_SUFFIX_OUTPUT, isOutput.toString()));
            }
        }
        request.addParameter(_logToOutputParameter);
        request.addParameter(_logPrefixParameter);

        return request;
    }

    private void setRequest(Request request) {
        _requestFile = request.getFile();
        final ProductRef outputProductAt = request.getOutputProductAt(0);
        if (outputProductAt != null) {
            _paramOutputProduct.setValueAsText(outputProductAt.getFilePath(), null);
        }

        final int numInputProducts = request.getNumInputProducts();
        final List<File> inputFiles = new ArrayList<File>(numInputProducts);
        for (int i = 0; i < numInputProducts; i++) {
            inputFiles.add(new File(request.getInputProductAt(i).getFilePath()));
        }
        _inputProductEditor.setFiles(inputFiles);

        setUpdateModeValue(request);
        if (!isUpdateMode()) {
            setProductDefinitionAndProcessingParameters(request.getAllParameters());
        }
        updateLogParameter(request);
    }

    private void updateLogParameter(Request request) {
        final Parameter prefixParam = request.getParameter(ProcessorConstants.LOG_PREFIX_PARAM_NAME);
        if (prefixParam != null) {
            _logPrefixParameter.setValue(prefixParam.getValue(), null);
        }

        final Parameter logOutputParam = request.getParameter(ProcessorConstants.LOG_TO_OUTPUT_PARAM_NAME);
        if (logOutputParam != null) {
            _logToOutputParameter.setValue(logOutputParam.getValue(), null);
        }
    }

    private void setUpdateModeValue(Request request) {
        _paramUpdateMode.setDefaultValue();
        final Parameter paramUpdateMode = request.getParameter(_paramUpdateMode.getName());
        if (paramUpdateMode != null) {
            _paramUpdateMode.setValueAsText(paramUpdateMode.getValueAsText(), null);
        }
    }

    private void setProductDefinitionAndProcessingParameters(Parameter[] parameters) {
        setProjectionValues(parameters);
        setCornerCoordinateValues(parameters);
        setProcessingValues(parameters);
    }

    private void setCornerCoordinateValues(Parameter[] parameters) {
        final Parameter paramWestLon = MosaicUtils.askForParameter(parameters, _paramWestLon.getName());
        if (paramWestLon != null) {
            _paramWestLon.setValueAsText(paramWestLon.getValueAsText(), null);
        }
        final Parameter paramEastLon = MosaicUtils.askForParameter(parameters, _paramEastLon.getName());
        if (paramEastLon != null) {
            _paramEastLon.setValueAsText(paramEastLon.getValueAsText(), null);
        }
        _paramNorthLat.setValueAsText("90", null);
        _paramSouthLat.setValueAsText("-90", null);
        final Parameter paramNorthLat = MosaicUtils.askForParameter(parameters, _paramNorthLat.getName());
        if (paramNorthLat != null) {
            _paramNorthLat.setValueAsText(paramNorthLat.getValueAsText(), null);
        }
        final Parameter paramSouthLat = MosaicUtils.askForParameter(parameters, _paramSouthLat.getName());
        if (paramSouthLat != null) {
            _paramSouthLat.setValueAsText(paramSouthLat.getValueAsText(), null);
        }
    }

    private void setProjectionValues(Parameter[] parameters) {
        final Parameter projectionName = MosaicUtils.askForParameter(parameters,
                                                                     MosaicConstants.PARAM_NAME_PROJECTION_NAME);
        if (projectionName != null) {
            _paramProjectionName.setValueAsText(projectionName.getValueAsText(), null);
        }
        final Parameter paramPixelSizeX = MosaicUtils.askForParameter(parameters, _paramPixelSizeX.getName());
        if (paramPixelSizeX != null) {
            _paramPixelSizeX.setValueAsText(paramPixelSizeX.getValueAsText(), null);
        }
        final Parameter paramPixelSizeY = MosaicUtils.askForParameter(parameters, _paramPixelSizeY.getName());
        if (paramPixelSizeY != null) {
            _paramPixelSizeY.setValueAsText(paramPixelSizeY.getValueAsText(), null);
        }
        final Parameter paramProjectionValues = MosaicUtils.askForParameter(parameters,
                                                                            MosaicConstants.PARAM_NAME_PROJECTION_PARAMETERS);
        if (paramProjectionValues != null) {
            final double[] doubles = StringUtils.toDoubleArray(paramProjectionValues.getValueAsText(), ",");
            final MapProjection projection = getProjection();
            final MapTransform transform = projection.getMapTransform().getDescriptor().createTransform(doubles);
            projection.setMapTransform(transform);
        }

        final Parameter orthorect = MosaicUtils.askForParameter(parameters,
                                                                MosaicConstants.PARAM_NAME_ORTHORECTIFY_INPUT_PRODUCTS);
        if (orthorect != null) {
            _paramOrthorectify.setValueAsText(orthorect.getValueAsText(), null);
        }

        final Parameter elevation = MosaicUtils.askForParameter(parameters,
                                                                MosaicConstants.PARAM_NAME_ELEVATION_MODEL_FOR_ORTHORECTIFICATION);
        if (elevation != null) {
            _paramElevation.setValueAsText(elevation.getValueAsText(), null);
        }
    }

    private void setProcessingValues(Parameter[] parameters) {
        emptyTables();

        final DefaultTableModel variableModel = (DefaultTableModel) _variablesTable.getModel();
        final DefaultTableModel conditionsModel = (DefaultTableModel) _conditionsTable.getModel();

        final List variables = MosaicUtils.extractVariables(parameters);
        for (Object variable1 : variables) {
            MosaicUtils.MosaicIoChannel channel = (MosaicUtils.MosaicIoChannel) variable1;
            final MosaicUtils.MosaicVariable variable = channel.getVariable();
            final String name = variable.getName();
            final String expression = variable.getExpression();
            if (variable.isCondition()) {
                conditionsModel.addRow(new Object[]{name, expression, variable.isOutput()});
            } else {
                variableModel.addRow(new Object[]{name, expression});
            }
        }
        final Parameter conditionsOperator = MosaicUtils.askForParameter(parameters,
                                                                         MosaicConstants.PARAM_NAME_CONDITION_OPERATOR);
        if (conditionsOperator != null) {
            _paramConditionsOperator.setValueAsText(conditionsOperator.getValueAsText(), null);
        } else {
            _paramConditionsOperator.setValueAsText(MosaicConstants.PARAM_DEFAULT_VALUE_CONDITION_OPERATOR, null);
        }
    }

    private void setDefaultRequest() {
        _requestFile = null;
        _inputProductEditor.setFiles(new ArrayList(1));
        final File outputProductFile = (File) _paramOutputProduct.getValue();
        if (outputProductFile != null && outputProductFile.getParentFile() != null) {
            final File parentFile = outputProductFile.getParentFile();
            _paramOutputProduct.setValue(new File(parentFile, MosaicConstants.DEFAULT_OUTPUT_PRODUCT_NAME), null);
        } else {
            _paramOutputProduct.setDefaultValue();
        }

        _paramProjectionName.setDefaultValue();

        _paramWestLon.setDefaultValue();
        _paramEastLon.setDefaultValue();

        final float northLatDefault = (Float) _paramNorthLat.getProperties().getDefaultValue();
        final float southLatCurrent = (Float) _paramSouthLat.getValue();
        if (northLatDefault < southLatCurrent) {
            _paramSouthLat.setDefaultValue();
            _paramNorthLat.setDefaultValue();
        } else {
            _paramNorthLat.setDefaultValue();
            _paramSouthLat.setDefaultValue();
        }

        updateGridCellSizeDefaultValue();

        emptyTables();

        _paramUpdateMode.setDefaultValue();
        _paramUpdateMode.setUIEnabled(false);
        _paramConditionsOperator.setDefaultValue();

        _paramOrthorectify.setDefaultValue();
        _paramElevation.setDefaultValue();
    }

    private void emptyTables() {
        _variablesTable.removeEditor();
        _conditionsTable.removeEditor();
        removeRows(_variablesTable, 0, _variablesTable.getRowCount() - 1);
        removeRows(_conditionsTable, 0, _conditionsTable.getRowCount() - 1);
    }

    private boolean validateOutputProduct() {
        final File file = (File) _paramOutputProduct.getValue();
        if (file == null || file.isDirectory()) {
            getApp().showErrorDialog("Please enter a valid product name."); /*I18N*/
            return false;
        }
        if (isUpdateMode()) {
            if (!file.isFile()) {
                getApp().showErrorDialog("The selected output product does not exist.\n" +
                                         "Please select an existing product."); /*I18N*/
                return false;
            }
        }
        if (file.isFile()) {
            if (!file.canWrite()) {
                getApp().showErrorDialog("The selected output product file is read-only.\n" +
                                         "Please change the output product file path or remove the write protection."); /*I18N*/
                return false;
            }

        }
        if (!isUpdateMode()) {
            if (file.isFile()) {
                if (file.canWrite()) {
                    final int result = getApp().showQuestionDialog("The selected output product already exists.\n" +
                                                                   "Do you want to overwrite it?", null); /*I18N*/
                    return result == JOptionPane.YES_OPTION;
                }
            }
            boolean valid = true;
            try {
                valid = file.createNewFile();
                if (valid) {
                    valid = file.delete();
                }
            } catch (IOException ignored) {
                valid = false;
            } finally {
                if (!valid) {
                    getApp().showErrorDialog("The output product file could not be created.\n" +
                                             "Please check the available disk space and/or write permissions for the output directory."); /*I18N*/
                }
            }
            return valid;
        }
        return true;
    }

    private boolean isUpdateMode() {
        return (Boolean) _paramUpdateMode.getValue();
    }


    private static boolean validateProductSize() {
//        final int numPixels = _outputProductWidth * _outputProductHeight;
//        final int numVariables = _variablesTable.getRowCount();
//        final int floatBandsSizes = numVariables * numPixels * 4;
//        _labelWidthInfo.getText();
//        _app.showWarningDialog();
        return true;
    }

    private boolean validateInputProducts() {
        final List files = _inputProductEditor.getFiles();
        if (files.isEmpty()) {
            getApp().showErrorDialog("No input products selected.\n" +
                                     "Please choose at least one input product."); /*I18N*/
            return false;
        }
        return true;
    }

    private boolean validateProjectionParameters() {
        final MapProjection projection = checkForValidUTMProjection(getProjection());
        return projection != null;
    }

    private boolean validateProcessingParameters() {
        final Product exampleProduct = getExampleProduct();
        if (exampleProduct == null) {
            return false;
        }
        final Parser parser = exampleProduct.createBandArithmeticParser();

        if (!isTableValid(_variablesTable, parser, "processing variable", false) ||
            !isTableValid(_conditionsTable, parser, "test condition", true)) {
            return false;
        }
        return areNamesUnique();
    }

    private boolean areNamesUnique() {
        for (int i = 0; i < _variablesTable.getRowCount(); i++) {
            String variableName = (String) _variablesTable.getModel().getValueAt(i, 0);
            for (int j = 0; j < _conditionsTable.getRowCount(); j++) {
                String conditionName = (String) _conditionsTable.getModel().getValueAt(j, 0);
                if (variableName.equals(conditionName)) {
                    String message = MessageFormat.format(
                            "The name of the condition ''{0}'' \nis already in use by a variable.", /*I18N*/
                            conditionName);
                    getApp().showWarningDialog(message);
                    return false;
                }
            }
        }
        return true;
    }

    private boolean isTableValid(final JTable table, final Parser parser, final String s1,
                                 final boolean checkForConditions) {
        final String reservedBandname = MosaicConstants.BANDNAME_COUNT;
        final List tableData = ((DefaultTableModel) table.getModel()).getDataVector();
        if (!checkForConditions && tableData.isEmpty()) {
            getApp().showWarningDialog("No processing variables are defined.\n" +
                                       "At least one processing variable must be defined.");
            return false;
        }
        final ArrayList<String> names = new ArrayList<String>(tableData.size());

        for (int i = 0; i < tableData.size(); i++) {
            List row = (List) tableData.get(i);
            final String n0 = (String) row.get(0);
            final String name;
            if (n0 == null) {
                name = n0;
            } else {
                name = n0.trim();
            }
            if (name == null || name.length() == 0) {
                final String msg = String.format("No name given for the\n%s at row %d.", s1, (i + 1));
                getApp().showWarningDialog(msg);
                return false;
            }
            if (name.equals(reservedBandname)) {
                final String msg = String.format(
                        "The given name '%s'\nfor the %s at row %d\nis reserved and cannot be used.",
                        reservedBandname, s1, (i + 1));
                getApp().showWarningDialog(msg);
                return false;
            }
            if (names.contains(name)) {
                final String msg = String.format("The name '%s'\nfor the %s at row %d\nis already in use.",
                                                 name, s1, (i + 1));
                getApp().showWarningDialog(msg);
                return false;
            }
            names.add(name);
            final String expression = (String) row.get(1);
            if (expression == null || expression.trim().length() == 0) {
                final String msg = String.format("The expression for the %s\nnamed '%s'\nis empty.", s1, name);
                getApp().showWarningDialog(msg);
                return false;
            }
            try {
                final Term term = parser.parse(expression);
                if (checkForConditions && !term.isB()) {
                    final String msg = String.format("The expression for the %s\nnamed '%s'\nis not boolean.", s1,
                                                     name);
                    getApp().showWarningDialog(msg);
                    return false;
                }
            } catch (ParseException ignored) {
                final String message = String.format("The expression for the %s\nnamed '%s'\nis invalid.", s1, name);
                getApp().showWarningDialog(message);
                return false;
            }
        }
        return true;
    }

    private Product getExampleProduct() {
        if (_exampleProduct == null) {
            loadExampleProduct();
        }
        return _exampleProduct;
    }

    private void loadExampleProduct() {
        getApp().showInfoDialog("At least one input product must be given in order to edit the value.", null); /*I18N*/

        final String currentDirectoryPath = getApp().getPreferences().getPropertyString(
                ProcessorApp.INPUT_PRODUCT_DIR_PREFERENCES_KEY);

        BeamFileChooser beamFileChooser = MosaicUtils.createBeamFileChooser(JFileChooser.FILES_ONLY,
                                                                            false,
                                                                            currentDirectoryPath,
                                                                            BeamConstants.ENVISAT_FORMAT_NAME);

        if (beamFileChooser.showOpenDialog(getApp().getMainFrame()) == JFileChooser.APPROVE_OPTION) {
            final File selectedFile = beamFileChooser.getSelectedFile();
            setExampleProduct(selectedFile);
        }
    }

    private Product loadOutputProduct() {
        return loadProduct(new File(_paramOutputProduct.getValueAsText()), null);
    }

    private Product loadProduct(final File productFile, List<String> errorMessages) {
        Product product = null;
        try {
            if (productFile != null) {
                product = ProductIO.readProduct(productFile);
            }
        } catch (IOException ignored) {
            final String message = String.format("Unable to open file '%s'", productFile.getPath());
            if (errorMessages == null) {
                getApp().showErrorDialog(message);
            } else {
                errorMessages.add(message);
            }
        }
        return product;
    }

    private int editExpression(String[] value, final boolean booleanExpected) {
        final Product product = getExampleProduct();
        final ProductExpressionPane pep;
        if (booleanExpected) {
            pep = ProductExpressionPane.createBooleanExpressionPane(new Product[]{product}, product,
                                                                    getApp().getPreferences());
        } else {
            pep = ProductExpressionPane.createGeneralExpressionPane(new Product[]{product}, product,
                                                                    getApp().getPreferences());
        }
        pep.setCode(value[0]);
        final int i = pep.showModalDialog(getApp().getMainFrame(), value[0]);
        if (i == ModalDialog.ID_OK) {
            value[0] = pep.getCode();
        }
        return i;
    }

    private static class TCR extends JLabel implements TableCellRenderer {

        private static final Border noFocusBorder = new EmptyBorder(1, 1, 1, 1);

        /**
         * Creates a <code>JLabel</code> instance with no image and with an empty string for the title. The label is
         * centered vertically in its display area. The label's contents, once set, will be displayed on the leading
         * edge of the label's display area.
         */
        private TCR() {
            setOpaque(true);
            setBorder(noFocusBorder);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            final boolean enabled = table.isEnabled();
            setText((String) value);


            if (isSelected) {
                super.setForeground(table.getSelectionForeground());
                super.setBackground(table.getSelectionBackground());
            } else if (!enabled) {
                super.setForeground(UIManager.getColor("TextField.inactiveForeground"));
                super.setBackground(table.getBackground());
            } else {
                super.setForeground(table.getForeground());
                super.setBackground(table.getBackground());
            }

            setFont(table.getFont());

            if (hasFocus) {
                setBorder(UIManager.getBorder("Table.focusCellHighlightBorder"));
                if (table.isCellEditable(row, column)) {
                    super.setForeground(UIManager.getColor("Table.focusCellForeground"));
                    super.setBackground(UIManager.getColor("Table.focusCellBackground"));
                }
            } else {
                setBorder(noFocusBorder);
            }

            setValue(value);

            return this;
        }

        private void setValue(Object value) {
            setText(value == null ? "" : value.toString());
        }
    }

    private class ExprEditor extends AbstractCellEditor implements TableCellEditor {

        private final JButton button;
        private String[] value;

        private ExprEditor(final boolean booleanExpected) {
            button = new JButton("...");
            final Dimension preferredSize = button.getPreferredSize();
            preferredSize.setSize(25, preferredSize.getHeight());
            button.setPreferredSize(preferredSize);
            value = new String[1];
            final ActionListener actionListener = new ActionListener() {
                @Override
                public void actionPerformed(ActionEvent e) {
                    final int i = editExpression(value, booleanExpected);
                    if (i == ModalDialog.ID_OK) {
                        fireEditingStopped();
                    } else {
                        fireEditingCanceled();
                    }
                }
            };
            button.addActionListener(actionListener);
        }

        /**
         * Returns the value contained in the editor.
         *
         * @return the value contained in the editor
         */
        @Override
        public Object getCellEditorValue() {
            return value[0];
        }

        /**
         * Sets an initial <code>value</code> for the editor.  This will cause the editor to <code>stopEditing</code>
         * and lose any partially edited value if the editor is editing when this method is called. <p>
         * <p/>
         * Returns the component that should be added to the client's <code>Component</code> hierarchy.  Once installed
         * in the client's hierarchy this component will then be able to draw and receive user input.
         *
         * @param table      the <code>JTable</code> that is asking the editor to edit; can be <code>null</code>
         * @param value      the value of the cell to be edited; it is up to the specific editor to interpret and draw the
         *                   value.  For example, if value is the string "true", it could be rendered as a string or it could be rendered
         *                   as a check box that is checked.  <code>null</code> is a valid value
         * @param isSelected true if the cell is to be rendered with highlighting
         * @param row        the row of the cell being edited
         * @param column     the column of the cell being edited
         *
         * @return the component for editing
         */
        @Override
        public Component getTableCellEditorComponent(JTable table, Object value, boolean isSelected, int row,
                                                     int column) {
            final JPanel renderPanel = new JPanel(new BorderLayout());
            final DefaultTableCellRenderer defaultRenderer = new DefaultTableCellRenderer();
            final Component label = defaultRenderer.getTableCellRendererComponent(table, value, isSelected,
                                                                                  false, row, column);
            renderPanel.add(label);
            renderPanel.add(button, BorderLayout.EAST);
            this.value[0] = (String) value;
            return renderPanel;
        }
    }
}
