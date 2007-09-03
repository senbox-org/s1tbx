package org.esa.beam.visat.toolviews.pin;

import org.esa.beam.dataio.dimap.DimapProductConstants;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.help.HelpSys;
import org.esa.beam.framework.ui.GridBagUtils;
import org.esa.beam.framework.ui.ImageDisplay;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.PlacemarkDescriptor;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.application.PageComponent;
import org.esa.beam.framework.ui.application.support.AbstractToolView;
import org.esa.beam.framework.ui.application.support.PageComponentListenerAdapter;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.framework.ui.product.BandChooser;
import org.esa.beam.framework.ui.product.PinDescriptor;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.framework.ui.tool.ToolButtonFactory;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.PropertyMap;
import org.esa.beam.util.StringUtils;
import org.esa.beam.util.SystemUtils;
import org.esa.beam.util.XmlWriter;
import org.esa.beam.util.io.BeamFileChooser;
import org.esa.beam.util.io.BeamFileFilter;
import org.esa.beam.util.io.FileUtils;
import org.esa.beam.visat.VisatApp;
import org.jdom.Document;
import org.jdom.Element;
import org.jdom.input.DOMBuilder;
import org.xml.sax.SAXException;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.MouseInputAdapter;
import javax.swing.event.TableColumnModelEvent;
import javax.swing.event.TableColumnModelListener;
import javax.swing.table.TableColumnModel;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.FactoryConfigurationError;
import javax.xml.parsers.ParserConfigurationException;
import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.RandomAccessFile;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * A dialog used to manage the list of pins associated with a selected product.
 */
public class PlacemarkManagerToolView extends AbstractToolView {

    public static final String PROPERTY_KEY_IO_DIR = "pin.io.dir";
    public static final String NAME_COL_NAME = "Name";
    public static final String LABEL_COL_NAME = "Label";
    public static final String DESC_COL_NAME = "Desc";
    public static final String X_COL_NAME = "X";
    public static final String Y_COL_NAME = "Y";
    public static final String LON_COL_NAME = "Lon";
    public static final String LAT_COL_NAME = "Lat";

    private static final String FILE_EXTENSION_FLAT_OLD = ".pnf";
    private static final String FILE_EXTENSION_XML_OLD = ".pnx";

    private static final String FILE_EXTENSION_FLAT_TEXT = ".txt";
    private static final String FILE_EXTENSION_PLACEMARK = ".placemark";

    private final PlacemarkDescriptor placemarkDescriptor;

    private VisatApp visatApp;
    private final PropertyMap propertyMap;
    private final HashMap<Product, Band[]> productToSelectedBands;
    private final HashMap<Product, TiePointGrid[]> productToSelectedGrids;

    private AbstractButton newButton;
    private AbstractButton copyButton;
    private AbstractButton editButton;
    private AbstractButton removeButton;
    private AbstractButton importButton;
    private AbstractButton exportButton;
    private AbstractButton importNButton;
    private AbstractButton exportNButton;
    private Product product;
    private JTable placemarkTable;
    private PlacemarkListener placemarkListener;
    private AbstractButton filterButton;
    private AbstractButton exportTableButton;
    private AbstractButton zoomToPlacemarkButton;
    private BeamFileFilter pinXMLFileFilter;
    private BeamFileFilter pinFlatFileFilter;
    private BeamFileFilter pinPlacemarkFileFilter;
    private BeamFileFilter pinTextFileFilter;
    private Band[] selectedBands;
    private TiePointGrid[] selectedGrids;
    private static final int indexForName = 0;
    private static final int indexForLon = 1;
    private static final int indexForLat = 2;
    private static final int indexForDesc = 3;
    private static final int indexForLabel = 4;
    private boolean synchronizingPlacemarkSelectedState;

    public PlacemarkManagerToolView(PlacemarkDescriptor placemarkDescriptor) {
        this.placemarkDescriptor = placemarkDescriptor;
        this.visatApp = VisatApp.getApp();
        propertyMap = visatApp.getPreferences();
        productToSelectedBands = new HashMap<Product, Band[]>();
        productToSelectedGrids = new HashMap<Product, TiePointGrid[]>();
    }

    @Override
    public JComponent createControl() {
        placemarkTable = new JTable();
        placemarkTable.setName("placemarkTable");
        placemarkTable.setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        placemarkTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        placemarkTable.setRowSelectionAllowed(true);
        // IMPORTANT: We set ReorderingAllowed=false, because we export the
        // table model AS IS to a flat text file.
        placemarkTable.getTableHeader().setReorderingAllowed(false);

        final TableColumnModel columnModel = placemarkTable.getColumnModel();
        columnModel.addColumnModelListener(new ColumnModelListener());

        ToolTipSetter toolTipSetter = new ToolTipSetter();
        placemarkTable.addMouseMotionListener(toolTipSetter);
        placemarkTable.addMouseListener(toolTipSetter);
        placemarkTable.addMouseListener(new EditListener());
        placemarkTable.addMouseListener(new PopupListener());
        placemarkTable.addKeyListener(new KeySelectionListener());

        placemarkTable.setModel(new PinTableModel(placemarkDescriptor, product, selectedBands, selectedGrids));

        JScrollPane tableScrollPane = new JScrollPane(placemarkTable);
        JPanel mainPane = new JPanel(new BorderLayout(4, 4));
        mainPane.add(tableScrollPane, BorderLayout.CENTER);

        placemarkTable.getTableHeader().addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                final TableColumnModel columnModel = placemarkTable.getColumnModel();
                final int colIdx = columnModel.getColumnIndexAtX(e.getX());
                if (colIdx == 4) { // "Label" column
                    sortPlacemarks();
                }
            }
        });

        newButton = createButton("icons/New24.gif");
        newButton.setName("newButton");
        newButton.setToolTipText("Create and add new pin."); /*I18N*/
        newButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                newPin();
            }
        });

        copyButton = createButton("icons/Copy24.gif");
        copyButton.setName("copyButton");
        copyButton.setToolTipText("Copy an existing pin."); /*I18N*/
        copyButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                copyActivePin();
            }
        });

        editButton = createButton("icons/Edit24.gif");
        editButton.setName("editButton");
        editButton.setToolTipText("Edit selected pin."); /*I18N*/
        editButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                editActivePin();
            }
        });

        removeButton = createButton("icons/Remove24.gif");
        removeButton.setName("removeButton");
        removeButton.setToolTipText("Remove selected pins."); /*I18N*/
        removeButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                removeSelectedPins();
            }
        });

        importButton = createButton("icons/Import24.gif");
        importButton.setName("importButton");
        importButton.setToolTipText("Import the first pin from XML or text file."); /*I18N*/
        importButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                importPins(false);
                updateUIState();
            }
        });

        exportButton = createButton("icons/Export24.gif");
        exportButton.setName("exportButton");
        exportButton.setToolTipText("Export the selected pins to XML file."); /*I18N*/
        exportButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                exportSelectedPins();
                updateUIState();
            }
        });

        importNButton = createButton("icons/ImportN24.gif");
        importNButton.setName("importNButton");
        importNButton.setToolTipText("Import all pins from XML or text file."); /*I18N*/
        importNButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                importPins(true);
                updateUIState();
            }
        });

        exportNButton = createButton("icons/ExportN24.gif");
        exportNButton.setName("exportNButton");
        exportNButton.setToolTipText("Export all pins to XML file."); /*I18N*/
        exportNButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (product != null && getPlacemarkGroup().getNodeCount() > 0) {
                    ProductNodeGroup<Pin> pinGroup = getPlacemarkGroup();
                    Pin[] exportPins = pinGroup.toArray(new Pin[pinGroup.getNodeCount()]);
                    exportPins(exportPins);
                }
            }
        });

        filterButton = createButton("icons/Filter24.gif");
        filterButton.setName("filterButton");
        filterButton.setToolTipText("Filter pixel data to be displayed in table."); /*I18N*/
        filterButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                if (product != null) {
                    Band[] allBands = product.getBands();
                    TiePointGrid[] allGrids = product.getTiePointGrids();
                    BandChooser bandChooser = new BandChooser(getWindowAncestor(),
                                                              "Available Bands And Tie Point Grids",
                                                              getDescriptor().getHelpId(), false,
                                                              allBands, selectedBands, allGrids, selectedGrids);
                    if (bandChooser.show() == ModalDialog.ID_OK) {
                        selectedBands = bandChooser.getSelectedBands();
                        selectedGrids = bandChooser.getSelectedTiePointGrids();
                        productToSelectedBands.put(product, selectedBands);
                        productToSelectedGrids.put(product, selectedGrids);
                        placemarkTable.setModel(
                                new PinTableModel(placemarkDescriptor, product, selectedBands, selectedGrids));
                        updatePlacemarkList();
                    }
                }
                updateUIState();
            }
        });

        exportTableButton = createButton("icons/ExportTable.gif");
        exportTableButton.setName("exportTableButton");
        exportTableButton.setToolTipText("Export pixel data at pin to flat text file."); /*I18N*/
        exportTableButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                exportPinDataTable();
                updateUIState();
            }
        });

        zoomToPlacemarkButton = createButton("icons/ZoomTo24.gif");
        zoomToPlacemarkButton.setName("zoomToPinButton");
        zoomToPlacemarkButton.setToolTipText("Zoom to selected pin."); /*I18N*/
        zoomToPlacemarkButton.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                zoomToActivePin();
            }
        });

        AbstractButton helpButton = createButton("icons/Help24.gif");
        helpButton.setName("helpButton");

        final JPanel buttonPane = GridBagUtils.createPanel();
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.gridy = 1;
        gbc.anchor = GridBagConstraints.CENTER;
        gbc.fill = GridBagConstraints.NONE;
        gbc.weightx = 0.5;
        gbc.gridy++;
        buttonPane.add(newButton, gbc);
        buttonPane.add(copyButton, gbc);
        gbc.gridy++;
        buttonPane.add(editButton, gbc);
        buttonPane.add(removeButton, gbc);
        gbc.gridy++;
        buttonPane.add(importButton, gbc);
        buttonPane.add(exportButton, gbc);
        gbc.gridy++;
        buttonPane.add(importNButton, gbc);
        buttonPane.add(exportNButton, gbc);
        gbc.gridy++;
        buttonPane.add(filterButton, gbc);
        buttonPane.add(exportTableButton, gbc);
        gbc.gridy++;
        buttonPane.add(zoomToPlacemarkButton, gbc);
        gbc.gridy++;
        gbc.fill = GridBagConstraints.VERTICAL;
        gbc.weighty = 1.0;
        gbc.gridwidth = 2;
        buttonPane.add(new JLabel(" "), gbc); // filler
        gbc.fill = GridBagConstraints.NONE;
        gbc.weighty = 0.0;
        gbc.gridx = 1;
        gbc.gridy++;
        gbc.gridwidth = 1;
        buttonPane.add(helpButton, gbc);

        JPanel content = new JPanel(new BorderLayout(4, 4));
        content.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        content.add(BorderLayout.CENTER, mainPane);
        content.add(BorderLayout.EAST, buttonPane);

        content.setPreferredSize(new Dimension(370, 200));

        if (getDescriptor().getHelpId() != null) {
            HelpSys.enableHelpOnButton(helpButton, getDescriptor().getHelpId());
            HelpSys.enableHelpKey(getContentPane(), getDescriptor().getHelpId());
        }

        ProductSceneView view = visatApp.getSelectedProductSceneView();
        if (view != null) {
            setProduct(view.getProduct());
        }

        visatApp.addInternalFrameListener(new PlacemarkManagerIFL(visatApp));

        getContext().getPage().addPageComponentListener(new PageComponentListenerAdapter() {
            @Override
            public void componentOpened(PageComponent component) {
                ExecCommand command = (ExecCommand) visatApp.getCommandManager().getCommand(
                        placemarkDescriptor.getShowLayerCommandId());
                command.setSelected(true);
                command.execute();
            }
        });


        updateUIState();

        return content;
    }

    public void setProduct(Product product) {
        if (this.product == product) {
            return;
        }
        Product oldProduct = this.product;
        if (oldProduct != null) {
            oldProduct.removeProductNodeListener(placemarkListener);
        }
        this.product = product;
        selectedBands = productToSelectedBands.get(this.product);
        selectedGrids = productToSelectedGrids.get(this.product);
        if (this.product != null) {
            setTitle(getDescriptor().getTitle() + " - " + this.product.getProductRefString());
            if (placemarkListener == null) {
                placemarkListener = new PlacemarkManagerToolView.PlacemarkListener();
            }
            this.product.addProductNodeListener(placemarkListener);
        } else {
            setTitle(getDescriptor().getTitle());
        }

        placemarkTable.setModel(new PinTableModel(placemarkDescriptor, this.product, selectedBands, selectedGrids));
        updatePlacemarkList();
        updateUIState();
    }

    private void sortPlacemarks() {
        Guardian.assertNotNull("product", product);
        // todo - sort table here
        JOptionPane.showMessageDialog(getWindowAncestor(), "TODO: Implement sort!");
    }

    private ProductSceneView getSceneView() {
        final ProductSceneView selectedProductSceneView = visatApp.getSelectedProductSceneView();
        if (selectedProductSceneView == null && product != null) {
            final Band[] bands = product.getBands();
            for (Band band : bands) {
                final JInternalFrame internalFrame = visatApp.findInternalFrame(band);
                if (internalFrame != null) {
                    final Container contentPane = internalFrame.getContentPane();
                    if (contentPane instanceof ProductSceneView) {
                        return (ProductSceneView) contentPane;
                    }
                }
            }
            final TiePointGrid[] tiePointGrids = product.getTiePointGrids();
            for (TiePointGrid tiePointGrid : tiePointGrids) {
                final JInternalFrame internalFrame = visatApp.findInternalFrame(tiePointGrid);
                if (internalFrame != null) {
                    final Container contentPane = internalFrame.getContentPane();
                    if (contentPane instanceof ProductSceneView) {
                        return (ProductSceneView) contentPane;
                    }
                }
            }
        }
        return selectedProductSceneView;
    }

    private Pin getPinAt(final int selectedRow) {
        Pin pin = null;
        if (product != null) {
            if (selectedRow > -1 && selectedRow < getPlacemarkGroup().getNodeCount()) {
                pin = getPlacemarkGroup().get(selectedRow);
            }
        }
        return pin;
    }

    private static AbstractButton createButton(String path) {
        return ToolButtonFactory.createButton(UIUtils.loadImageIcon(path), false);
    }

    /////////////////////////////////////////////////////////////////////////////////////////////////
    // {{ Actions

    private void newPin() {
        Guardian.assertNotNull("product", product);
        String[] uniquePinNameAndLabel = PlacemarkNameFactory.createUniqueNameAndLabel(placemarkDescriptor, product);
        Pin newPin = new Pin(uniquePinNameAndLabel[0],
                             uniquePinNameAndLabel[1],
                             "",
                             new PixelPos(0, 0), null,
                             placemarkDescriptor.createDefaultSymbol());
        if (PinDialog.showEditPinDialog(getWindowAncestor(), product, newPin, placemarkDescriptor)) {
            makePinNameUnique(newPin);
            getPlacemarkGroup().add(newPin);
            PinTool.setPlacemarkSelected(getPlacemarkGroup(), newPin, true);
            updateUIState();
        }
    }

    private void copyActivePin() {
        Guardian.assertNotNull("product", product);
        Pin activePin = getPlacemarkGroup().getSelectedNode();
        Guardian.assertNotNull("activePin", activePin);
        Pin newPin = new Pin("copy_of_" + activePin.getName(),
                             activePin.getLabel(),
                             activePin.getDescription(),
                             activePin.getPixelPos(),
                             activePin.getGeoPos(),
                             new PinSymbol(activePin.getSymbol().getName(),
                                           activePin.getSymbol().getShape()));
        if (PinDialog.showEditPinDialog(getWindowAncestor(), product, newPin, placemarkDescriptor)) {
            makePinNameUnique(newPin);
            getPlacemarkGroup().add(newPin);
            PinTool.setPlacemarkSelected(getPlacemarkGroup(), newPin, true);
            updateUIState();
        }
    }

    private ProductNodeGroup<Pin> getPlacemarkGroup() {
        return placemarkDescriptor.getPlacemarkGroup(product);
    }

    private void editActivePin() {
        Guardian.assertNotNull("product", product);
        Pin activePin = getPlacemarkGroup().getSelectedNode();
        Guardian.assertNotNull("activePin", activePin);
        if (PinDialog.showEditPinDialog(getWindowAncestor(), product, activePin, placemarkDescriptor)) {
            makePinNameUnique(activePin);
            updateUIState();
        }
    }

    private void removeSelectedPins() {
        final Collection<Pin> pins = getPlacemarkGroup().getSelectedNodes();
        int i = JOptionPane.showConfirmDialog(getWindowAncestor(),
                                              "Do you really want to remove " + pins.size() + " selected pin(s)?\n" +
                                              "This action can not be undone.",
                                              getDescriptor().getTitle() + " - Remove Pins",
                                              JOptionPane.OK_CANCEL_OPTION);
        if (i == JOptionPane.OK_OPTION) {
            int selectedRow = placemarkTable.getSelectedRow();
            for (Pin pin : pins) {
                getPlacemarkGroup().remove(pin);
            }
            if (selectedRow >= getPlacemarkGroup().getNodeCount()) {
                selectedRow = getPlacemarkGroup().getNodeCount() - 1;
            }
            if (selectedRow >= 0) {
                placemarkTable.getSelectionModel().setSelectionInterval(selectedRow, selectedRow);
            }
            updateUIState();
        }
    }


    private void zoomToActivePin() {
        Guardian.assertNotNull("product", product);
        Pin activePin = getPlacemarkGroup().getSelectedNode();
        Guardian.assertNotNull("activePin", activePin);
        final ProductSceneView view = getSceneView();
        if (view != null) {
            final ImageDisplay imageDisplay = view.getImageDisplay();
            final PixelPos pos = activePin.getPixelPos();
            imageDisplay.zoom(pos.getX(), pos.getY(), imageDisplay.getViewModel().getViewScale());
            updateUIState();
        }
    }

    // }} Actions
    /////////////////////////////////////////////////////////////////////////////////////////////////

    private void makePinNameUnique(Pin newPin) {
        if (makePinNameUnique0(newPin)) {
            showWarningDialog("Pin has been renamed to '" + newPin.getName() + "',\n" +
                              "because a pin with the former name already exists.");
        }
    }

    private void updateUIState() {
        boolean productSelected = product != null;
        boolean hasPins = product != null && getPlacemarkGroup().getNodeCount() > 0;
        int numSelectedPins = 0;
        if (product != null) {
            synchronizePinSelectedState();
            numSelectedPins = getPlacemarkGroup().getSelectedNodes().size();
        }
        boolean hasSelectedPins = numSelectedPins > 0;
        boolean hasActivePin = numSelectedPins == 1;

        placemarkTable.setEnabled(productSelected);
        newButton.setEnabled(productSelected);
        copyButton.setEnabled(hasActivePin);
        editButton.setEnabled(hasActivePin);
        removeButton.setEnabled(hasSelectedPins);
        zoomToPlacemarkButton.setEnabled(hasActivePin);
        importButton.setEnabled(productSelected);
        exportButton.setEnabled(hasPins);
        importNButton.setEnabled(productSelected);
        exportNButton.setEnabled(hasPins);
        exportTableButton.setEnabled(hasPins);
        filterButton.setEnabled(productSelected);
    }

    private void synchronizePinSelectedState() {
        if (!synchronizingPlacemarkSelectedState) {
            try {
                synchronizingPlacemarkSelectedState = true;
                ProductNodeGroup<Pin> placemarkGroup = getPlacemarkGroup();
                Pin[] placemarks = placemarkGroup.toArray(new Pin[placemarkGroup.getNodeCount()]);
                for (int i = 0; i < placemarks.length; i++) {
                    placemarks[i].setSelected(placemarkTable.isRowSelected(i));
                }
//                for (int selectedIndex : selectedIndexes) {
//                    if(selectedIndex < placemarkGroup.getNodeCount()) {
//                        Pin placemark = placemarkGroup.get(selectedIndex);
//                        placemark.setSelected(true);
//                    }
//                }



//                int[] selectedIndexes = placemarkTable.getSelectedRows();
//                ProductNodeGroup<Pin> pinGroup = getPlacemarkGroup();
//                Pin[] placemarks = pinGroup.toArray(new Pin[pinGroup.getNodeCount()]);
//                for (Pin placemark : placemarks) {
//                    boolean selected = false;
//                    for (int selectedIndex : selectedIndexes) {
//                        int numPlacemarks = getPlacemarkGroup().getNodeCount();
//                        if (selectedIndex < numPlacemarks && placemark == getPlacemarkGroup().get(selectedIndex)) {
//                            selected = true;
//                            break;
//                        }
//                    }
//                    placemark.setSelected(selected);
//                }
            } finally {
                synchronizingPlacemarkSelectedState = false;
            }
        }
    }

    private void updatePlacemarkList() {
        if (!synchronizingPlacemarkSelectedState) {
            try {
                synchronizingPlacemarkSelectedState = true;
                if (product != null) {
                    ProductNodeGroup<Pin> placemarkGroup = getPlacemarkGroup();
                    Pin[] placemarks = placemarkGroup.toArray(new Pin[placemarkGroup.getNodeCount()]);
                    for (int i = 0; i < placemarks.length; i++) {
                        Pin placemark = placemarks[i];
                        if (placemark.isSelected()) {
                            placemarkTable.addRowSelectionInterval(i, i);
                        } else {
                            placemarkTable.removeRowSelectionInterval(i, i);
                        }
                    }
                }
                placemarkTable.revalidate();
                placemarkTable.repaint();
            } finally {
                synchronizingPlacemarkSelectedState = false;
            }
        }
    }

    private void importPins(boolean allPins) {
        Pin[] pins;
        try {
            pins = loadPinsFromFile();
        } catch (IOException e) {
            showErrorDialog("I/O error, failed to import pins:\n" + e.getMessage());    /*I18N*/
            return;
        }
        if (pins.length == 0) {
            return;
        }
        int numPinsOutOfBounds = 0;
        int numPinsRenamed = 0;
        int numInvalids = 0;
        for (Pin pin : pins) {
            if (makePinNameUnique0(pin)) {
                numPinsRenamed++;
            }

            pin.updatePixelPos(product.getGeoCoding());

            final PixelPos pixelPos;
            if (pin.getPixelPos() != null) {
                pixelPos = pin.getPixelPos();
            } else {
                pixelPos = new PixelPos();
                pixelPos.setInvalid();
            }

            if (!pixelPos.isValid()) {
                numInvalids++;
                continue;
            }

            if (product.containsPixel(pixelPos)) {
                getPlacemarkGroup().add(pin);
            } else {
                numPinsOutOfBounds++;
            }
            if (!allPins) {
                break; // import only the first one
            }
        }

        if (numInvalids > 0) {
            showWarningDialog("One or more pins have not been imported,\n" +
                              "because they can not be assigned to a product without a geo-coding."); /*I18N*/
        }
        if (numPinsRenamed > 0) {
            showWarningDialog("One or more pins have been renamed,\n" +
                              "because their former names are already existing."); /*I18N*/
        }
        if (numPinsOutOfBounds > 0) {
            if (numPinsOutOfBounds == pins.length) {
                showErrorDialog("No pins have been imported, because their pixel\n" +
                                "positions are outside the product's bounds."); /*I18N*/
            } else {
                showErrorDialog(numPinsOutOfBounds + " pins have not been imported, because their pixel\n" +
                                "positions are outside the product's bounds."); /*I18N*/
            }
        }
    }

    private boolean makePinNameUnique0(Pin pin) {
        if (getPlacemarkGroup().get(pin.getName()) == pin) {
            return false;
        }
        String name0 = pin.getName();
        String name = name0;
        int id = 1;
        while (getPlacemarkGroup().contains(name)) {
            name = name0 + "_" + id;
        }
        if (!name0.equals(name)) {
            pin.setName(name);
            return true;
        }
        return false;
    }

    private Pin[] loadPinsFromFile() throws IOException {
        final BeamFileChooser fileChooser = new BeamFileChooser();
        fileChooser.setDialogTitle("Import Pin"); /*I18N*/
        fileChooser.addChoosableFileFilter(getOrCreateFlatPinFileFilter());
        fileChooser.addChoosableFileFilter(getOrCreateXMLPinFileFilter());
        fileChooser.addChoosableFileFilter(getTextFileFilter());
        fileChooser.setFileFilter(getPlacemarkFileFilter());
        fileChooser.setCurrentDirectory(getIODir());
        int result = fileChooser.showOpenDialog(getWindowAncestor());
        Pin[] pins = new Pin[0];
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (file != null) {
                setIODir(file.getAbsoluteFile().getParentFile());
                pins = readPlacemarksFromFile(file);
            }
        }
        return pins;
    }

    private void exportSelectedPins() {
        Collection<Pin> selectedPins = getPlacemarkGroup().getSelectedNodes();
        exportPins(selectedPins.toArray(new Pin[selectedPins.size()]));
    }

    private void exportPins(Pin[] pins) {
        final BeamFileChooser fileChooser = new BeamFileChooser();
        fileChooser.setDialogTitle("Export Placemarks");   /*I18N*/
        fileChooser.addChoosableFileFilter(getTextFileFilter());
        fileChooser.setFileFilter(getPlacemarkFileFilter());
        final File ioDir = getIODir();
        fileChooser.setCurrentDirectory(ioDir);
        fileChooser.setSelectedFile(new File(ioDir, placemarkDescriptor.getRoleName()));
        int result = fileChooser.showSaveDialog(getWindowAncestor());
        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (file != null) {
                if (!visatApp.promptForOverwrite(file)) {
                    return;
                }
                setIODir(file.getAbsoluteFile().getParentFile());
                BeamFileFilter beamFileFilter = fileChooser.getBeamFileFilter();
                if(!StringUtils.contains(beamFileFilter.getExtensions(), FileUtils.getExtension(file)) ) {
                    file = FileUtils.ensureExtension(file, beamFileFilter.getDefaultExtension());
                }
                Writer writer = null;
                try {
                    if(beamFileFilter.getFormatName().equals(getPlacemarkFileFilter().getFormatName())) {
                        writePlacemarksFile(pins, file);
                    }else {
                        writer = new FileWriter(file);
                        writePlacemarkDataTableText(writer, pins);
                        writer.close();
                    }
                } catch (IOException e) {
                    showErrorDialog("I/O Error.\n   Failed to export placemarks.");    /*I18N*/
                }finally{
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

    private void exportPinDataTable() {
        final BeamFileChooser fileChooser = new BeamFileChooser();
        String roleLabel = placemarkDescriptor.getRoleLabel();
        roleLabel = roleLabel.substring(0, 1).toUpperCase() + roleLabel.substring(1);
        fileChooser.setDialogTitle("Export " + roleLabel + " Data Table");/*I18N*/
        fileChooser.setFileFilter(getTextFileFilter());
        final File ioDir = getIODir();
        fileChooser.setCurrentDirectory(ioDir);
        fileChooser.setSelectedFile(new File(ioDir, "Data"));
        int result = fileChooser.showSaveDialog(getWindowAncestor());

        if (result == JFileChooser.APPROVE_OPTION) {
            File file = fileChooser.getSelectedFile();
            if (file != null) {
                if (!visatApp.promptForOverwrite(file)) {
                    return;
                }
                setIODir(file.getAbsoluteFile().getParentFile());
                file = FileUtils.ensureExtension(file, FILE_EXTENSION_FLAT_TEXT);
                try {
                    final Writer writer = new FileWriter(file);
                    ProductNodeGroup<Pin> placemarkGroup = getPlacemarkGroup();
                    writePlacemarkDataTableText(writer, placemarkGroup.toArray(new Pin[placemarkGroup.getNodeCount()]));
                    writer.close();
                } catch (IOException e) {
                    showErrorDialog("I/O Error.\nFailed to export " +roleLabel + " data table."); /*I18N*/
                }
            }
        }
    }

    private void writePlacemarkDataTableText(final Writer writer, Pin[] placemarks) {
        final PrintWriter pw = new PrintWriter(writer);

        final int columnCountMin = PinTableModel.DEFAULT_COLUMN_NAMES.length;
        final int columnCount = placemarkTable.getColumnCount();

        // Write file header
        pw.println("# BEAM " + placemarkDescriptor.getRoleLabel() + " export table");
        pw.println("#");
        pw.println("# Product:\t" + product.getName());
        pw.println("# Created on:\t" + new Date());
        pw.println();

        // Write header columns
        pw.print(PlacemarkManagerToolView.NAME_COL_NAME + "\t");
        pw.print(PlacemarkManagerToolView.X_COL_NAME + "\t");
        pw.print(PlacemarkManagerToolView.Y_COL_NAME + "\t");
        pw.print(PlacemarkManagerToolView.LAT_COL_NAME + "\t");
        pw.print(PlacemarkManagerToolView.LON_COL_NAME + "\t");
        pw.print(PlacemarkManagerToolView.LABEL_COL_NAME + "\t");
        pw.print(PlacemarkManagerToolView.DESC_COL_NAME + "\t");
        for (int i = columnCountMin; i < columnCount; i++) {
            pw.print(placemarkTable.getColumnName(i) + "\t");
        }
        pw.println();

        for (int i = 0; i < placemarks.length; i++) {
            Pin placemark = placemarks[i];
            pw.print(placemark.getName() + "\t");
            writePixelPos(placemark, pw);
            writeGeoPos(placemark, pw);
            pw.print(placemark.getLabel() + "\t");
            pw.print(placemark.getDescription() + "\t");
            for (int col = columnCountMin; col < columnCount; col++) {
                final Object value = placemarkTable.getValueAt(i, col);
                pw.print(value.toString() + "\t");
            }
            pw.println();
        }
        pw.close();
    }

    private void writeGeoPos(Pin pin, PrintWriter pw) {
        if (pin.getGeoPos() != null) {
            pw.print(pin.getGeoPos().getLat() + "\t");
            pw.print(pin.getGeoPos().getLon() + "\t");
        } else {
            pw.print(Float.NaN + "\t");
            pw.print(Float.NaN + "\t");
        }
    }

    private void writePixelPos(Pin pin, PrintWriter pw) {
        if (pin.getPixelPos() != null) {
            pw.print(pin.getPixelPos().getX() + "\t");
            pw.print(pin.getPixelPos().getY() + "\t");
        } else {
            pw.print(Float.NaN + "\t");
            pw.print(Float.NaN + "\t");
        }
    }

    private static void writePlacemarksFile(Pin[] pins, File outputFile) throws IOException {
        assert pins != null;
        assert outputFile != null;

        XmlWriter writer = new XmlWriter(outputFile);
        final String[] tags = XmlWriter.createTags(0, "Placemarks");
        writer.println(tags[0]);
        for (Pin pin : pins) {
            if (pin != null) {
                pin.writeXML(writer, 1);
            }
        }
        writer.println(tags[1]);
        writer.close();
    }

    private Pin[] readPlacemarksFromFile(File inputFile) throws IOException {
        assert inputFile != null;
        final DataInputStream dataInputStream = new DataInputStream(new FileInputStream(inputFile));
        byte[] magicBytes;
        try {
            magicBytes = new byte[5];
            dataInputStream.read(magicBytes);  // todo - BAD PRACTICE HERE!!!
        } finally {
            dataInputStream.close();
        }
        if (XmlWriter.XML_HEADER_LINE.startsWith(new String(magicBytes))) {
            return readPlacemarksFromXMLFile(inputFile);
        } else {
            return readPlacemarksFromFlatFile(inputFile);
        }
    }

    private Pin[] readPlacemarksFromFlatFile(File inputFile) throws IOException {
        assert inputFile != null;
        int[] columnIndexes = null;
        int biggestIndex = 0;
        ArrayList<Pin> pins = new ArrayList<Pin>();

        final RandomAccessFile file = new RandomAccessFile(inputFile, "r");
        int row = 0;
        while (true) {
            String line = file.readLine();
            if (line == null) {
                break;
            }
            line = line.trim(); // cut \n and \r from the end of the line
            if (line.equals("") || line.startsWith("#")) {
                continue;
            }
            String[] strings = StringUtils.toStringArray(line, "\t");
            if (columnIndexes == null) {
                int nameIndex = StringUtils.indexOf(strings, PlacemarkManagerToolView.NAME_COL_NAME);
                int lonIndex = StringUtils.indexOf(strings, PlacemarkManagerToolView.LON_COL_NAME);
                int latIndex = StringUtils.indexOf(strings, PlacemarkManagerToolView.LAT_COL_NAME);
                int descIndex = StringUtils.indexOf(strings, PlacemarkManagerToolView.DESC_COL_NAME);
                int labelIndex = StringUtils.indexOf(strings, PlacemarkManagerToolView.LABEL_COL_NAME);
                if (nameIndex == -1 || lonIndex == -1 || latIndex == -1) {
                    throw new IOException("Invalid placemark file format:\n" +
                                          "at least the columns 'Name', 'Lon' and 'Lat' must be given.");
                }
                biggestIndex = biggestIndex > nameIndex ? biggestIndex : nameIndex;
                biggestIndex = biggestIndex > lonIndex ? biggestIndex : lonIndex;
                biggestIndex = biggestIndex > latIndex ? biggestIndex : latIndex;
                columnIndexes = new int[5];
                columnIndexes[PlacemarkManagerToolView.indexForName] = nameIndex;
                columnIndexes[PlacemarkManagerToolView.indexForLon] = lonIndex;
                columnIndexes[PlacemarkManagerToolView.indexForLat] = latIndex;
                columnIndexes[PlacemarkManagerToolView.indexForDesc] = descIndex;
                columnIndexes[PlacemarkManagerToolView.indexForLabel] = labelIndex;
            } else {
                row++;
                if (strings.length > biggestIndex) {
                    String name = strings[columnIndexes[PlacemarkManagerToolView.indexForName]];
                    float lon;
                    try {
                        lon = Float.parseFloat(strings[columnIndexes[PlacemarkManagerToolView.indexForLon]]);
                    } catch (NumberFormatException e) {
                        throw new IOException("Invalid placemark file format:\n" +
                                              "data row " + row + ": value for 'Lon' is invalid");      /*I18N*/
                    }
                    float lat;
                    try {
                        lat = Float.parseFloat(strings[columnIndexes[PlacemarkManagerToolView.indexForLat]]);
                    } catch (NumberFormatException e) {
                        throw new IOException("Invalid placemark file format:\n" +
                                              "data row " + row + ": value for 'Lat' is invalid");      /*I18N*/
                    }
                    String desc = null;
                    if (columnIndexes[PlacemarkManagerToolView.indexForDesc] >= 0 && strings.length > columnIndexes[PlacemarkManagerToolView.indexForDesc])
                    {
                        desc = strings[columnIndexes[PlacemarkManagerToolView.indexForDesc]];
                    }
                    String label = name;
                    if (columnIndexes[PlacemarkManagerToolView.indexForLabel] >= 0 && strings.length > columnIndexes[PlacemarkManagerToolView.indexForLabel])
                    {
                        label = strings[columnIndexes[PlacemarkManagerToolView.indexForLabel]];
                    }
                    Pin pin = new Pin(name, label, "", null, new GeoPos(lat, lon),
                                      placemarkDescriptor.createDefaultSymbol());
                    if (desc != null) {
                        pin.setDescription(desc);
                    }
                    pins.add(pin);
                } else {
                    throw new IOException("Invalid placemark file format:\n" +
                                          "data row " + row + ": values for 'Name', 'Lon' and 'Lat' must be given.");   /*I18N*/
                }
            }
        }
        file.close();

        return pins.toArray(new Pin[pins.size()]);
    }

    private Pin[] readPlacemarksFromXMLFile(File inputFile) throws IOException {
        assert inputFile != null;
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            org.w3c.dom.Document w3cDocument = builder.parse(inputFile);
            Document document = new DOMBuilder().build(w3cDocument);
            final Element rootElement = document.getRootElement();
            List children = rootElement.getChildren(DimapProductConstants.TAG_PLACEMARK);
            if(children.size() == 0) {
                // support for old pin XML format (.pnx)
                children = rootElement.getChildren(DimapProductConstants.TAG_PIN);
            }
            final ArrayList<Pin> pins = new ArrayList<Pin>(children.size());
            for (Object child : children) {
                final Element element = (Element) child;
                try {
                    pins.add(Pin.createPlacemark(element, placemarkDescriptor.createDefaultSymbol()));
                } catch (IllegalArgumentException e) {
                    // todo - ?
                }
            }
            return pins.toArray(new Pin[pins.size()]);
        } catch (FactoryConfigurationError error) {
            throw new IOException(error.toString());
        } catch (ParserConfigurationException e) {
            throw new IOException(e.toString(), e);
        } catch (SAXException e) {
            throw new IOException(e.toString(), e);
        } catch (IOException e) {
            throw new IOException(e.toString(), e);
        }
    }

    private BeamFileFilter getOrCreateFlatPinFileFilter() {
        if (pinFlatFileFilter == null) {
            pinFlatFileFilter = new BeamFileFilter("PIN_FLAT_FILE", FILE_EXTENSION_FLAT_OLD,
                                                   "Pin files - flat text format");
        }
        return pinFlatFileFilter;
    }

    private BeamFileFilter getOrCreateXMLPinFileFilter() {
        if (pinXMLFileFilter == null) {
            pinXMLFileFilter = new BeamFileFilter("PIN_XML_FILE", FILE_EXTENSION_XML_OLD,
                                                  "Pin files - XML format");
        }
        return pinXMLFileFilter;
    }

    private BeamFileFilter getTextFileFilter() {
        if (pinTextFileFilter == null) {
            pinTextFileFilter = new BeamFileFilter("PLACEMARK_TEXT_FILE", FILE_EXTENSION_FLAT_TEXT,
                                                   "Placemark files - flat text format");
        }
        return pinTextFileFilter;
    }

    private BeamFileFilter getPlacemarkFileFilter() {
        if (pinPlacemarkFileFilter == null) {
            pinPlacemarkFileFilter = new BeamFileFilter("PLACEMARK_XML_FILE", FILE_EXTENSION_PLACEMARK,
                                                        "Placemark files - XML format");
        }
        return pinPlacemarkFileFilter;
    }

    private void setIODir(File dir) {
        if (propertyMap != null && dir != null) {
            propertyMap.setPropertyString(PROPERTY_KEY_IO_DIR, dir.getPath());
        }
    }

    private File getIODir() {
        File dir = SystemUtils.getUserHomeDir();
        if (propertyMap != null) {
            dir = new File(propertyMap.getPropertyString(PROPERTY_KEY_IO_DIR, dir.getPath()));
        }
        return dir;
    }

    private void showErrorDialog(String message) {
        JOptionPane.showMessageDialog(this.getContentPane(),
                                      message,
                                      getDescriptor().getTitle() + " - Error",
                                      JOptionPane.ERROR_MESSAGE);
    }

    private void showWarningDialog(String message) {
        JOptionPane.showMessageDialog(this.getContentPane(),
                                      message,
                                      getDescriptor().getTitle() + " - Warning",
                                      JOptionPane.WARNING_MESSAGE);
    }

    private class PlacemarkListener implements ProductNodeListener {

        public void nodeChanged(ProductNodeEvent event) {
            maybeRevalidatePlacemarkList(event);
        }

        public void nodeDataChanged(ProductNodeEvent event) {
            maybeRevalidatePlacemarkList(event);
        }

        public void nodeAdded(ProductNodeEvent event) {
            maybeRevalidatePlacemarkList(event);
        }

        public void nodeRemoved(ProductNodeEvent event) {
            maybeRevalidatePlacemarkList(event);
        }

        private void maybeRevalidatePlacemarkList(ProductNodeEvent event) {
            ProductNode sourceNode = event.getSourceNode();
            if (sourceNode instanceof Pin) {
                updatePlacemarkList();
                updateUIState();
            }
        }
    }

    private class ToolTipSetter extends MouseInputAdapter {

        private int _rowIndex;

        public ToolTipSetter() {
            _rowIndex = -1;
        }

        @Override
        public void mouseExited(MouseEvent e) {
            _rowIndex = -1;
        }

        @Override
        public void mouseMoved(MouseEvent e) {
            int rowIndex = placemarkTable.rowAtPoint(e.getPoint());
            if (rowIndex != _rowIndex) {
                _rowIndex = rowIndex;
                if (_rowIndex >= 0 && _rowIndex < placemarkTable.getRowCount()) {
                    GeoPos geoPos = getPinAt(_rowIndex).getGeoPos();
                    if (geoPos != null) {
                        placemarkTable.setToolTipText(geoPos.getLonString() + " / " + geoPos.getLatString());
                    }
                }
            }
        }

    }

    private static class ColumnModelListener implements TableColumnModelListener {

        public void columnAdded(TableColumnModelEvent e) {
            int minWidth;
            final int index = e.getToIndex();
            switch (index) {
            case 0:
            case 1:
                minWidth = 40;
                break;
            default:
                minWidth = 80;
            }
            TableColumnModel columnModel = (TableColumnModel) e.getSource();
            columnModel.getColumn(index).setPreferredWidth(minWidth);
        }

        public void columnRemoved(TableColumnModelEvent e) {
        }

        public void columnMoved(TableColumnModelEvent e) {
        }

        public void columnMarginChanged(ChangeEvent e) {
        }

        public void columnSelectionChanged(ListSelectionEvent e) {
        }
    }

    private class EditListener extends MouseAdapter {

        @Override
        public void mouseClicked(MouseEvent e) {
            action(e);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            action(e);
        }

        private void action(MouseEvent e) {
            updateUIState();
            if (e.getClickCount() == 2) {
                editActivePin();
            }
        }
    }

    private class PopupListener extends MouseAdapter {

        @Override
        public void mousePressed(MouseEvent e) {
            action(e);
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            action(e);
        }

        @Override
        public void mouseClicked(MouseEvent e) {
            action(e);
        }

        private void action(MouseEvent e) {
            if (e.isPopupTrigger()) {
                final JPopupMenu popupMenu = new JPopupMenu();
                final JMenuItem menuItem = new JMenuItem("Copy data to clipboard");
                menuItem.addActionListener(new ActionListener() {

                    public void actionPerformed(ActionEvent actionEvent) {
                        final StringWriter stringWriter = new StringWriter();
                        ProductNodeGroup<Pin> placemarkGroup = getPlacemarkGroup();
                        writePlacemarkDataTableText(stringWriter, placemarkGroup.toArray(new Pin[placemarkGroup.getNodeCount()]));
                        String text = stringWriter.toString();
                        text = text.replaceAll("\r\n", "\n");
                        text = text.replaceAll("\r", "\n");
                        SystemUtils.copyToClipboard(text);
                    }
                });
                popupMenu.add(menuItem);
                final Point point = SwingUtilities.convertPoint(e.getComponent(), e.getPoint(), placemarkTable);
                popupMenu.show(placemarkTable, point.x, point.y);
            }
        }
    }

    /** @deprecated in 4.1, {@link PinDialog#showEditPinDialog(Window,Product,Pin,PlacemarkDescriptor)} instead */
    public static boolean showEditPinDialog(Window parent, Product product, Pin pin) {
        return PinDialog.showEditPinDialog(parent, product, pin, PinDescriptor.INSTANCE);
    }

    /**
     * Returns the geographical position which is equivalent to the given pixel position. Returns <code>null</code> if
     * the given pixel position is outside of the given product or the geocoding cannot get geographical positions.
     *
     * @param product  must be given and must contain a geocoding.
     * @param pixelPos must be given.
     *
     * @return the geographical position which is equivalent to the given pixel position or <code>null</code> if the
     *         given pixel position is outside of the given product or the geocoding cannot get geographical positions.
     */
    static GeoPos getGeoPos(Product product, PixelPos pixelPos) {
        Guardian.assertNotNull("product", product);
        Guardian.assertNotNull("pixelPos", pixelPos);
        final GeoCoding geoCoding = product.getGeoCoding();
        Guardian.assertNotNull("geoCoding", geoCoding);
        if (product.containsPixel(pixelPos)) {
            return geoCoding.getGeoPos(pixelPos, null);
        }
        return null;
    }

    private class PlacemarkManagerIFL extends InternalFrameAdapter {

        private final VisatApp visatApp;

        public PlacemarkManagerIFL(VisatApp visatApp) {
            this.visatApp = visatApp;
        }

        @Override
        public void internalFrameOpened(InternalFrameEvent e) {
            updatePlacemarkManager();
        }

        @Override
        public void internalFrameActivated(InternalFrameEvent e) {
            updatePlacemarkManager();
        }

        @Override
        public void internalFrameClosing(InternalFrameEvent e) {
            updatePlacemarkManager();
        }

        @Override
        public void internalFrameClosed(InternalFrameEvent e) {
            updatePlacemarkManager();
        }

        private void updatePlacemarkManager() {
            final ProductSceneView view = visatApp.getSelectedProductSceneView();
            setProduct(view != null ? view.getProduct() : null);
        }
    }

    private class KeySelectionListener extends KeyAdapter {

        @Override
        public void keyReleased(KeyEvent e) {
                updateUIState();
        }
    }
}
