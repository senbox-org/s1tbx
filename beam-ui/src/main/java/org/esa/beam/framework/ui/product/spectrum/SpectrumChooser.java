package org.esa.beam.framework.ui.product.spectrum;

import com.jidesoft.grid.AutoFilterTableHeader;
import com.jidesoft.grid.HierarchicalTable;
import com.jidesoft.grid.HierarchicalTableComponentFactory;
import com.jidesoft.grid.HierarchicalTableModel;
import com.jidesoft.grid.JideTable;
import com.jidesoft.grid.SortableTable;
import com.jidesoft.grid.TableModelWrapperUtils;
import com.jidesoft.grid.TreeLikeHierarchicalPanel;
import com.jidesoft.grid.TristateCheckBoxCellEditor;
import com.jidesoft.swing.TristateCheckBox;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.swing.DefaultCellEditor;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.border.EmptyBorder;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.ui.DecimalTableCellRenderer;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.util.ArrayUtils;
import org.esa.beam.util.SystemUtils;
import org.esa.beam.util.io.FileUtils;

public class SpectrumChooser extends ModalDialog {

    private static final int spectrumSelectedIndex = 0;
    private static final int spectrumNameIndex = 1;
    private static final int spectrumUnitIndex = 2;
    private static final int spectrumStrokeIndex = 3;
    private static final int spectrumShapeIndex = 4;
    private static final int spectrumShapeSizeIndex = 5;

    private static final int bandSelectedIndex = 0;
    private static final int bandNameIndex = 1;
    private static final int bandDescriptionIndex = 2;
    private static final int bandWavelengthIndex = 3;
    private static final int bandBandwidthIndex = 4;
    private static final int bandUnitIndex = 5;

    private static HierarchicalTable spectraTable;
    private final DisplayableSpectrum[] originalSpectra;

    private DisplayableSpectrum[] spectra;
    private static SpectrumSelectionAdmin selectionAdmin;
    private static boolean selectionChangeLock;

    private final Map<Integer, SortableTable> rowToBandsTable;

    public SpectrumChooser(Window parent, DisplayableSpectrum[] originalSpectra, String helpID) {
        super(parent, "Available Spectra", ModalDialog.ID_OK_CANCEL, initLoadSaveConfigurationButtons(parent.getFocusOwner()),
                helpID);
        if (originalSpectra != null) {
            this.originalSpectra = originalSpectra;
            List<DisplayableSpectrum> spectraWithBands = new ArrayList<DisplayableSpectrum>();
            for (DisplayableSpectrum spectrum : originalSpectra) {
                if (spectrum.hasBands()) {
                    spectraWithBands.add(spectrum);
                }
            }
            this.spectra = spectraWithBands.toArray(new DisplayableSpectrum[spectraWithBands.size()]);
        } else {
            this.originalSpectra = new DisplayableSpectrum[0];
            this.spectra = new DisplayableSpectrum[0];
        }
        selectionAdmin = new SpectrumSelectionAdmin();
        selectionChangeLock = false;
        rowToBandsTable = new HashMap<Integer, SortableTable>();
        initUI();
    }

    @Override
    protected void onOther() {
        // do nothing. Most importantly, do not hide the dialog
    }
    
    private static JButton[] initLoadSaveConfigurationButtons(final Component parent) {
        JButton loadButton = new JButton("Load Spectra Configuration");
        loadButton.addActionListener(new LoadConfigurationActionListener(parent));
        JButton saveButton = new JButton("Save Spectra Configuration");
        saveButton.addActionListener(new SaveConfigurationActionListener(parent));
        return new JButton[]{loadButton, saveButton};
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private static File getSystemAuxdataDir() {
        File file = new File(SystemUtils.getApplicationDataDir(), "beam-ui" + File.separator +"auxdata" +
                File.separator + "spectra-sets");
        if (!file.exists()) {
            file.mkdir();
        }
        return file;
    }

    private void initUI() {
        final JPanel content = new JPanel(new BorderLayout());
        initSpectraTable();
        JScrollPane spectraScrollPane = new JScrollPane(spectraTable);
        final Dimension preferredSize = spectraTable.getPreferredSize();
        spectraScrollPane.setPreferredSize(new Dimension(Math.max(preferredSize.width + 20, 550),
                Math.max(preferredSize.height + 10, 200)));
        content.add(spectraScrollPane, BorderLayout.CENTER);
        setContent(content);
    }

    @SuppressWarnings("unchecked")
    private void initSpectraTable() {
        SpectrumTableModel spectrumTableModel = new SpectrumTableModel();
        spectraTable = new HierarchicalTable(spectrumTableModel);
        spectraTable.setComponentFactory(new SpectrumTableComponentFactory());
        AutoFilterTableHeader header = new AutoFilterTableHeader(spectraTable);
        spectraTable.setTableHeader(header);
        spectraTable.setRowHeight(21);

        final TableColumn selectionColumn = spectraTable.getColumnModel().getColumn(spectrumSelectedIndex);
        final TristateCheckBoxCellEditor tristateCheckBoxCellEditor = new TristateCheckBoxCellEditor();
        selectionColumn.setCellEditor(tristateCheckBoxCellEditor);
        selectionColumn.setCellRenderer(new TriStateRenderer());
        selectionColumn.setMinWidth(38);
        selectionColumn.setMaxWidth(38);

        final TableColumn nameColumn = spectraTable.getColumnModel().getColumn(spectrumNameIndex);
        nameColumn.setCellRenderer(new TextFieldRenderer());

        final TableColumn strokeColumn = spectraTable.getColumnModel().getColumn(spectrumStrokeIndex);
        JComboBox strokeComboBox = new JComboBox(SpectrumStrokeProvider.getStrokeIcons());
        ImageIconComboBoxRenderer strokeComboBoxRenderer = new ImageIconComboBoxRenderer(spectrumStrokeIndex);
        strokeComboBoxRenderer.setPreferredSize(new Dimension(200, 30));
        strokeComboBox.setRenderer(strokeComboBoxRenderer);
        strokeColumn.setCellEditor(new DefaultCellEditor(strokeComboBox));

        final TableColumn shapeColumn = spectraTable.getColumnModel().getColumn(spectrumShapeIndex);
        JComboBox shapeComboBox = new JComboBox(SpectrumShapeProvider.getShapeIcons());
        ImageIconComboBoxRenderer shapeComboBoxRenderer = new ImageIconComboBoxRenderer(spectrumShapeIndex);
        shapeComboBoxRenderer.setPreferredSize(new Dimension(200, 30));
        shapeComboBox.setRenderer(shapeComboBoxRenderer);
        shapeColumn.setCellEditor(new DefaultCellEditor(shapeComboBox));

        final TableColumn shapeSizeColumn = spectraTable.getColumnModel().getColumn(spectrumShapeSizeIndex);
        JComboBox shapeSizeComboBox = new JComboBox(SpectrumShapeProvider.getScaleGrades());
        shapeSizeColumn.setCellEditor(new DefaultCellEditor(shapeSizeComboBox));
    }

    private static SpectrumTableModel getSpectrumTableModel() {
        return (SpectrumTableModel) TableModelWrapperUtils.getActualTableModel(spectraTable.getModel());
    }

    public DisplayableSpectrum[] getSpectra() {
        return originalSpectra;
    }

    private class SpectrumTableModel extends DefaultTableModel implements HierarchicalTableModel {

        private final Class[] COLUMN_CLASSES = {
                Boolean.class,
                String.class,
                String.class,
                ImageIcon.class,
                ImageIcon.class,
                Integer.class
        };
        private final String[] bandColumns = new String[]{"", "Band name", "Band description", "Spectral wavelength (nm)", "Spectral bandwidth (nm)", "Unit"};

        private SpectrumTableModel() {
            super(new String[]{"", "Spectrum name", "Unit", "Line style", "Symbol", "Symbol size"}, 0);
            for (DisplayableSpectrum spectrum : spectra) {
                if (spectrum.hasBands()) {
                    addRow(spectrum);
                }
            }
        }

        @Override
        public Object getChildValueAt(final int row) {
            DisplayableSpectrum spectrum = spectra[row];
            if (rowToBandsTable.containsKey(row)) {
                return rowToBandsTable.get(row);
            }
            final Band[] spectralBands = spectrum.getSpectralBands();
            Object[][] spectrumData = new Object[spectralBands.length][bandColumns.length];
            for (int i = 0; i < spectralBands.length; i++) {
                Band spectralBand = spectralBands[i];
                final boolean selected = spectrum.isBandSelected(i) && spectrum.isSelected();
                spectrumData[i][bandSelectedIndex] = selected;
                spectrumData[i][bandNameIndex] = spectralBand.getName();
                spectrumData[i][bandDescriptionIndex] = spectralBand.getDescription();
                spectrumData[i][bandWavelengthIndex] = spectralBand.getSpectralWavelength();
                spectrumData[i][bandBandwidthIndex] = spectralBand.getSpectralBandwidth();
                spectrumData[i][bandUnitIndex] = spectralBand.getUnit();
            }
            final BandTableModel bandTableModel = new BandTableModel(spectrumData, bandColumns);
            bandTableModel.addTableModelListener(new TableModelListener() {
                @Override
                public void tableChanged(TableModelEvent e) {
                    e.getSource();
                    if (e.getColumn() == bandSelectedIndex) {
                        final DisplayableSpectrum spectrum = spectra[row];
                        final int bandRow = e.getFirstRow();
                        final Boolean selected = (Boolean) bandTableModel.getValueAt(bandRow, e.getColumn());
                        spectrum.setBandSelected(bandRow, selected);
                        if (!selectionChangeLock) {
                            selectionChangeLock = true;
                            selectionAdmin.setBandSelected(row, bandRow, selected);
                            selectionAdmin.updateSpectrumSelectionState(row, selectionAdmin.getState(row));
                            spectraTable.getModel().setValueAt(selectionAdmin.getState(row), row, spectrumSelectedIndex);
                            spectrum.setSelected(selectionAdmin.isSpectrumSelected(row));
                            selectionChangeLock = false;
                        }
                    }
                }
            });
            return bandTableModel;
        }

        private BandTableModel getBandTableModel(int row) {
            Object spectrumTableModelChild = getChildValueAt(row);
            if (spectrumTableModelChild instanceof BandTableModel) {
                return (BandTableModel) spectrumTableModelChild;
            } else {
                return (BandTableModel)
                        TableModelWrapperUtils.getActualTableModel(((JTable) spectrumTableModelChild).getModel());
            }
        }

        private void addRow(DisplayableSpectrum spectrum) {
            ImageIcon strokeIcon;
            if (spectrum.isDefaultSpectrum()) {
                strokeIcon = new ImageIcon();
            } else {
                strokeIcon = SpectrumStrokeProvider.getStrokeIcon(spectrum.getLineStyle());
            }
            final ImageIcon shapeIcon = SpectrumShapeProvider.getShapeIcon(spectrum.getSymbolIndex());
            selectionAdmin.evaluateSpectrumSelections(spectrum);
            super.addRow(new Object[]{selectionAdmin.getState(getRowCount()), spectrum.getName(), spectrum.getUnit(),
                    strokeIcon, shapeIcon, spectrum.getSymbolSize()});
        }

        @Override
        public boolean hasChild(int row) {
            return true;
        }

        @Override
        public boolean isHierarchical(int row) {
            return true;
        }

        @Override
        public boolean isExpandable(int row) {
            return true;
        }

        @Override
        public Class getColumnClass(int columnIndex) {
            return COLUMN_CLASSES[columnIndex];
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return !(column == spectrumStrokeIndex && spectra[row].isDefaultSpectrum()) && column != spectrumNameIndex
                    && column != spectrumUnitIndex;
        }

        @Override
        public void setValueAt(Object aValue, int row, int column) {
            if (column == spectrumSelectedIndex && !selectionChangeLock) {
                selectionChangeLock = true;
                selectionAdmin.updateSpectrumSelectionState(row, (Integer) aValue);
                aValue = selectionAdmin.getState(row);
                updateBandsTable(row);
                spectra[row].setSelected(selectionAdmin.isSpectrumSelected(row));
                fireTableCellUpdated(row, column);
                selectionChangeLock = false;
            } else if (column == spectrumStrokeIndex) {
                spectra[row].setLineStyle(SpectrumStrokeProvider.getStroke((ImageIcon) aValue));
            } else if (column == spectrumShapeIndex) {
                spectra[row].setSymbolIndex(SpectrumShapeProvider.getShape((ImageIcon) aValue));
            } else if (column == spectrumShapeSizeIndex) {
                spectra[row].setSymbolSize(Integer.parseInt(aValue.toString()));
            }
            super.setValueAt(aValue, row, column);
        }

        private void updateBandsTable(int row) {
            if (rowToBandsTable.containsKey(row)) {
                final SortableTable bandsTable = rowToBandsTable.get(row);
                final TableModel tableModel = bandsTable.getModel();
                for (int i = 0; i < tableModel.getRowCount(); i++) {
                    tableModel.setValueAt(selectionAdmin.isBandSelected(row, i), i, bandSelectedIndex);
                }
            } else {
                for (int i = 0; i < spectra[row].getSpectralBands().length; i++) {
                    spectra[row].setBandSelected(i, selectionAdmin.isBandSelected(row, i));
                }
            }
        }
    }

    private static class BandTableModel extends DefaultTableModel {

        private BandTableModel(Object[][] spectrumData, String[] bandColumns) {
            super(spectrumData, bandColumns);
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return column == bandSelectedIndex;
        }

    }

    private class SpectrumTableComponentFactory implements HierarchicalTableComponentFactory {

        @Override
        public Component createChildComponent(HierarchicalTable table, Object value, int row) {
            if (value == null) {
                return new JPanel();
            }
            TableModel model;
            if (value instanceof JideTable) {
                model = ((JideTable) value).getModel();
            } else {
                model = (TableModel) value;
            }
            SortableTable bandsTable = new SortableTable(model);
            AutoFilterTableHeader bandsHeader = new AutoFilterTableHeader(bandsTable);
            bandsTable.setTableHeader(bandsHeader);

            final TableColumn selectionColumn = bandsTable.getColumnModel().getColumn(bandSelectedIndex);
            final JCheckBox selectionCheckBox = new JCheckBox();
            selectionColumn.setCellEditor(new DefaultCellEditor(selectionCheckBox));
            selectionColumn.setMinWidth(20);
            selectionColumn.setMaxWidth(20);
            BooleanRenderer booleanRenderer = new BooleanRenderer();
            selectionColumn.setCellRenderer(booleanRenderer);

            final TableColumn wavelengthColumn = bandsTable.getColumnModel().getColumn(bandWavelengthIndex);
            wavelengthColumn.setCellRenderer(new DecimalTableCellRenderer(new DecimalFormat("###0.0##")));

            final TableColumn bandwidthColumn = bandsTable.getColumnModel().getColumn(bandBandwidthIndex);
            bandwidthColumn.setCellRenderer(new DecimalTableCellRenderer(new DecimalFormat("###0.0##")));

            rowToBandsTable.put(row, bandsTable);

            final JScrollPane jScrollPane = new SpectrumScrollPane(bandsTable);
            return new TreeLikeHierarchicalPanel(jScrollPane);
        }

        @Override
        public void destroyChildComponent(HierarchicalTable table, Component component, int row) {
            // do nothing
        }
    }

    private static class SpectrumScrollPane extends JScrollPane {

        public SpectrumScrollPane(JTable table) {
            super(table);
        }

        @Override
        public Dimension getPreferredSize() {
            getViewport().setPreferredSize(getViewport().getView().getPreferredSize());
            return super.getPreferredSize();
        }
    }

    private class ImageIconComboBoxRenderer extends JLabel implements ListCellRenderer {

        private final int columnIndex;

        private ImageIconComboBoxRenderer(int columnIndex) {
            setOpaque(true);
            setHorizontalAlignment(CENTER);
            setVerticalAlignment(CENTER);
            this.columnIndex = columnIndex;
        }

        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
            if (isSelected) {
                setBackground(list.getSelectionBackground());
                setForeground(list.getSelectionForeground());
            } else {
                setBackground(list.getBackground());
                setForeground(list.getForeground());
            }

            if (spectraTable.getColumnModel().getColumn(columnIndex).getWidth() < 125) {
                setHorizontalAlignment(LEFT);
            } else {
                setHorizontalAlignment(CENTER);
            }
            setIcon((ImageIcon) value);
            return this;
        }

    }

    private class BooleanRenderer extends JCheckBox implements TableCellRenderer {

        private BooleanRenderer() {
            this.setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (isSelected) {
                setBackground(table.getSelectionBackground());
                setForeground(table.getSelectionForeground());
            } else {
                setBackground(table.getBackground());
                setForeground(table.getForeground());
            }
            boolean selected = (Boolean) value;
            setSelected(selected);
            return this;
        }
    }

    private class TriStateRenderer extends TristateCheckBox implements TableCellRenderer {

        private TriStateRenderer() {
            this.setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (isSelected) {
                setBackground(table.getSelectionBackground());
                setForeground(table.getSelectionForeground());
            } else {
                setBackground(table.getBackground());
                setForeground(table.getForeground());
            }
            int state = (Integer) value;
            setState(state);
            return this;
        }
    }

    private class TextFieldRenderer extends JTextField implements TableCellRenderer {

        private TextFieldRenderer() {
            Font font = this.getFont();
            font = new Font(font.getName(), Font.BOLD, font.getSize());
            setFont(font);
            setBorder(new EmptyBorder(new Insets(0, 0, 0, 0)));
            setOpaque(true);
        }

        @Override
        public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
            if (isSelected) {
                setBackground(table.getSelectionBackground());
                setForeground(table.getSelectionForeground());
            } else {
                setBackground(table.getBackground());
                setForeground(table.getForeground());
            }
            setText(value.toString());
            return this;
        }
    }

    private static class LoadConfigurationActionListener implements ActionListener {

        private final Component parent;

        LoadConfigurationActionListener(Component parent) {
            this.parent = parent;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            File currentDirectory = getSystemAuxdataDir();
            JFileChooser fileChooser = new JFileChooser(currentDirectory);
            if (fileChooser.showOpenDialog(parent) == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                try {
                    BufferedReader reader = new BufferedReader(new FileReader(file));
                    List<String> bandNameList = new ArrayList<>();
                    String readBandName;
                    while ((readBandName = reader.readLine()) != null) {
                        bandNameList.add(readBandName);
                    }
                    reader.close();
                    String[] bandNames = bandNameList.toArray(new String[bandNameList.size()]);
                    for (int i = 0; i < spectraTable.getRowCount(); i++) {
                        SpectrumTableModel spectrumTableModel = getSpectrumTableModel();
                        spectrumTableModel.setValueAt(TristateCheckBox.STATE_UNSELECTED, i, spectrumSelectedIndex);
                        BandTableModel bandTableModel = spectrumTableModel.getBandTableModel(i);
                        for (int j = 0; j < bandTableModel.getRowCount(); j++) {
                            String bandName = bandTableModel.getValueAt(j, bandNameIndex).toString();
                            boolean selected = ArrayUtils.isMemberOf(bandName, bandNames);
                            bandTableModel.setValueAt(selected, j, bandSelectedIndex);
                        }
                    }
                } catch (IOException e1) {
                    JOptionPane.showMessageDialog(parent, "Could not load spectra configuration");
                }
            }
        }
    }

    private static class SaveConfigurationActionListener implements ActionListener {

        private final Component parent;

        SaveConfigurationActionListener(Component parent) {
            this.parent = parent;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            File currentDirectory = getSystemAuxdataDir();
            JFileChooser fileChooser = new JFileChooser(currentDirectory);
            File suggestedFile = new File(currentDirectory + File.separator + "spectra_config.txt");
            int fileCounter = 1;
            while(suggestedFile.exists()) {
                suggestedFile = new File("spectra_config_" + fileCounter + ".txt");
            }
            fileChooser.setSelectedFile(suggestedFile);
            if (fileChooser.showSaveDialog(parent) == JFileChooser.APPROVE_OPTION) {
                File file = fileChooser.getSelectedFile();
                try {
                    BufferedWriter writer = new BufferedWriter(new FileWriter(file));
                    SpectrumTableModel spectrumTableModel = getSpectrumTableModel();
                    for (int i = 0; i < spectrumTableModel.getRowCount(); i++) {
                        BandTableModel bandTableModel = spectrumTableModel.getBandTableModel(i);
                        for (int j = 0; j < bandTableModel.getRowCount(); j++) {
                            if ((boolean) bandTableModel.getValueAt(j, bandSelectedIndex)) {
                                writer.write(bandTableModel.getValueAt(j, bandNameIndex) + "\n");
                            }
                        }
                    }
                    writer.close();
                } catch (IOException e1) {
                    JOptionPane.showMessageDialog(parent, "Could not save spectra configuration");
                }
            }
        }
    }

}