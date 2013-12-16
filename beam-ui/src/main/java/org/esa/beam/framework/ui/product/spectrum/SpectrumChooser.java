package org.esa.beam.framework.ui.product.spectrum;

import com.jidesoft.grid.AutoFilterTableHeader;
import com.jidesoft.grid.HierarchicalTable;
import com.jidesoft.grid.HierarchicalTableComponentFactory;
import com.jidesoft.grid.HierarchicalTableModel;
import com.jidesoft.grid.JideTable;
import com.jidesoft.grid.SortableTable;
import com.jidesoft.grid.TreeLikeHierarchicalPanel;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.ui.DecimalTableCellRenderer;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.util.ArrayUtils;

import javax.swing.DefaultCellEditor;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListCellRenderer;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableColumn;
import javax.swing.table.TableModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SpectrumChooser extends ModalDialog {

    private static final int spectrumSelectedIndex = 0;
    private static final int spectrumNameIndex = 1;
    private static final int spectrumStrokeIndex = 2;
    private static final int spectrumShapeIndex = 3;

    private static final int bandSelectedIndex = 0;
    private static final int bandNameIndex = 1;
    private static final int bandDescriptionIndex = 2;
    private static final int bandWavelengthIndex = 3;
    private static final int bandBandwidthIndex = 4;

    private static HierarchicalTable spectraTable;

    private List<DisplayableSpectrum> allSpectra;

    private final Map<Integer, SortableTable> rowToBandsTable;

    public SpectrumChooser(Window parent, List<DisplayableSpectrum> allSpectra, String helpID) {
        super(parent, "Available Spectra", ModalDialog.ID_OK_CANCEL, helpID);
        if (allSpectra != null) {
            this.allSpectra = allSpectra;
        } else {
            this.allSpectra = new ArrayList<DisplayableSpectrum>();
        }
        rowToBandsTable = new HashMap<Integer, SortableTable>();
        initUI();
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
        final JCheckBox selectionCheckBox = new JCheckBox();
        selectionColumn.setCellEditor(new DefaultCellEditor(selectionCheckBox));
        BooleanRenderer booleanRenderer = new BooleanRenderer();
        selectionColumn.setCellRenderer(booleanRenderer);
        selectionColumn.setMinWidth(38);
        selectionColumn.setMaxWidth(38);

        final TableColumn strokeColumn = spectraTable.getColumnModel().getColumn(spectrumStrokeIndex);
        JComboBox strokeComboBox = new JComboBox(SpectrumConstants.strokeIcons);
        ImageIconComboBoxRenderer strokeComboBoxRenderer = new ImageIconComboBoxRenderer();
        strokeComboBoxRenderer.setPreferredSize(new Dimension(200, 30));
        strokeComboBox.setRenderer(strokeComboBoxRenderer);
        strokeColumn.setCellEditor(new DefaultCellEditor(strokeComboBox));

        final TableColumn shapeColumn = spectraTable.getColumnModel().getColumn(spectrumShapeIndex);
        JComboBox shapeComboBox = new JComboBox(SpectrumConstants.shapeIcons);
        ImageIconComboBoxRenderer shapeComboBoxRenderer = new ImageIconComboBoxRenderer();
        shapeComboBoxRenderer.setPreferredSize(new Dimension(200, 30));
        shapeComboBox.setRenderer(shapeComboBoxRenderer);
        shapeColumn.setCellEditor(new DefaultCellEditor(shapeComboBox));
    }

    public List<DisplayableSpectrum> getSpectra() {
        return allSpectra;
    }

    class SpectrumTableModel extends DefaultTableModel implements HierarchicalTableModel {

        private final Class[] COLUMN_CLASSES = {
                Boolean.class,
                String.class,
                ImageIcon.class,
                ImageIcon.class,
        };
        private final String[] bandColumns = new String[]{"", "Band name", "Band description", "Spectral wavelength (nm)", "Spectral bandwidth (nm)"};

        public SpectrumTableModel() {
            super(new String[]{"", "Spectrum name", "Line style", "Symbol"}, 0);
            for (DisplayableSpectrum spectrum : allSpectra) {
                addRow(spectrum);
            }
        }

        @Override
        public Object getChildValueAt(final int row) {
            DisplayableSpectrum spectrum = allSpectra.get(row);
            if (rowToBandsTable.containsKey(row)) {
                return rowToBandsTable.get(row);
            }
            final Band[] spectralBands = spectrum.getSpectralBands();
            Object[][] spectrumData = new Object[spectralBands.length][bandColumns.length];
            for (int i = 0; i < spectralBands.length; i++) {
                Band spectralBand = spectralBands[i];
                spectrumData[i][bandSelectedIndex] = spectrum.isBandSelected(i);
                spectrumData[i][bandNameIndex] = spectralBand.getName();
                spectrumData[i][bandDescriptionIndex] = spectralBand.getDescription();
                spectrumData[i][bandWavelengthIndex] = spectralBand.getSpectralWavelength();
                spectrumData[i][bandBandwidthIndex] = spectralBand.getSpectralBandwidth();
            }
            final BandTableModel bandTableModel = new BandTableModel(spectrumData, bandColumns);
            bandTableModel.addTableModelListener(new TableModelListener() {
                @Override
                public void tableChanged(TableModelEvent e) {
                    e.getSource();
                    if (e.getColumn() == bandSelectedIndex) {
                        final DisplayableSpectrum spectrum = allSpectra.get(row);
                        spectrum.setBandSelected(e.getFirstRow(), (Boolean) bandTableModel.getValueAt(e.getFirstRow(), e.getColumn()));
                    }
                }
            });
            return bandTableModel;
        }

        private void addRow(DisplayableSpectrum spectrum) {
            if (spectrum.getLineStyle() == null) {
                spectrum.setLineStyle(SpectrumConstants.strokes[getRowCount() % SpectrumConstants.strokes.length]);
            }
            final ImageIcon strokeIcon = SpectrumConstants.strokeIcons[ArrayUtils.getElementIndex(spectrum.getLineStyle(), SpectrumConstants.strokes)];
            if (spectrum.getSymbol() == null) {
                spectrum.setSymbol(SpectrumConstants.shapes[getRowCount() % SpectrumConstants.shapes.length]);
            }
            final ImageIcon shapeIcon = SpectrumConstants.shapeIcons[ArrayUtils.getElementIndex(spectrum.getSymbol(), SpectrumConstants.shapes)];
            super.addRow(new Object[]{spectrum.isSelected(), spectrum.getName(), strokeIcon, shapeIcon});
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
            return column != spectrumNameIndex;
        }

        @Override
        public void setValueAt(Object aValue, int row, int column) {
            if (column == spectrumSelectedIndex) {
                if (rowToBandsTable.containsKey(row)) {
                    final SortableTable bandsTable = rowToBandsTable.get(row);
                    bandsTable.setEnabled((Boolean) aValue);
                }
                allSpectra.get(row).setSelected((Boolean) aValue);
                fireTableCellUpdated(row, column);
            } else if (column == spectrumStrokeIndex) {
                allSpectra.get(row).setLineStyle(SpectrumConstants.strokes[ArrayUtils.getElementIndex(aValue, SpectrumConstants.strokeIcons)]);
                fireTableCellUpdated(row, column);
            } else if (column == spectrumShapeIndex) {
                allSpectra.get(row).setSymbol(SpectrumConstants.shapes[ArrayUtils.getElementIndex(aValue, SpectrumConstants.shapeIcons)]);
                fireTableCellUpdated(row, column);
            }
            super.setValueAt(aValue, row, column);
        }
    }

    static class BandTableModel extends DefaultTableModel {

        public BandTableModel(Object[][] spectrumData, String[] bandColumns) {
            super(spectrumData, bandColumns);
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return column == bandSelectedIndex;
        }

        @Override
        public void setValueAt(Object aValue, int row, int column) {
            if (column == bandSelectedIndex) {
                this.getRowCount();
            }
            fireTableCellUpdated(row, column);
            super.setValueAt(aValue, row, column);
        }
    }

    class SpectrumTableComponentFactory implements HierarchicalTableComponentFactory {

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
            bandsTable.setEnabled((Boolean) spectraTable.getValueAt(row, spectrumSelectedIndex));
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

    class ImageIconComboBoxRenderer extends JLabel implements ListCellRenderer {

        public ImageIconComboBoxRenderer() {
            setOpaque(true);
            setHorizontalAlignment(CENTER);
            setVerticalAlignment(CENTER);
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
            setIcon((ImageIcon) value);
            return this;
        }

    }

    class BooleanRenderer extends JCheckBox implements TableCellRenderer {

        public BooleanRenderer() {
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

}
