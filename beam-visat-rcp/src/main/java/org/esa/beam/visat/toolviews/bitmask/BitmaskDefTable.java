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
package org.esa.beam.visat.toolviews.bitmask;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;

import javax.swing.BorderFactory;
import javax.swing.DefaultCellEditor;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.UIManager;
import javax.swing.border.Border;
import javax.swing.event.MouseInputAdapter;
import javax.swing.table.TableCellRenderer;
import javax.swing.table.TableModel;

import org.esa.beam.framework.datamodel.BitmaskDef;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.util.StringUtils;

/**
 * A table component used to display a list of bitmask definitions.
 * <p/>
 * <p><code>BitmaskDefTable</code> uses a <code>BitmaskDefTableModel</code> to manage the data it displays.
 *
 * @see org.esa.beam.visat.toolviews.bitmask.BitmaskDefTableModel
 */
public class BitmaskDefTable extends JTable {

    private BitmaskDefTableModel _model;
    private VisibleFlagHeaderRenderer _visibleFlagHeaderRenderer;

    public BitmaskDefTable() {
        this(true);
    }

    public BitmaskDefTable(boolean visibileFlagColumnEnabled) {
        this(new BitmaskDefTableModel(visibileFlagColumnEnabled));
    }

    public BitmaskDefTable(BitmaskDefTableModel model) {
        super(model);
        _model = model;
        setPreferredScrollableViewportSize(new Dimension(200, 150));
        setDefaultEditor(Boolean.class, new BooleanEditor());
        setDefaultRenderer(Color.class, new ColorRenderer(true));
        setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        getTableHeader().setReorderingAllowed(false);
        getTableHeader().setResizingAllowed(true);
        setAutoResizeMode(JTable.AUTO_RESIZE_OFF);
        configureColumnModel();
        ToolTipSetter toolTipSetter = new ToolTipSetter();
        addMouseListener(toolTipSetter);
        addMouseMotionListener(toolTipSetter);
    }

    public boolean isVisibleFlagColumnEnabled() {
        return _model.isVisibleFlagColumnEnabled();
    }

    public void setVisibleFlagColumnEnabled(boolean visibleFlagColumnEnabled) {
        _model.setVisibleFlagColumnEnabled(visibleFlagColumnEnabled);
        configureColumnModel();
    }

    public boolean getSelectedVisibleFlag() {
        int rowIndex = getSelectedRow();
        if (rowIndex >= 0) {
            return getVisibleFlagAt(rowIndex);
        }
        return false;
    }

    public boolean getVisibleFlagAt(int rowIndex) {
        return _model.getVisibleFlagAt(rowIndex);
    }

    public BitmaskDef getSelectedBitmaskDef() {
        int rowIndex = getSelectedRow();
        if (rowIndex >= 0) {
            return getBitmaskDefAt(rowIndex);
        }
        return null;
    }

    public BitmaskDef getBitmaskDefAt(int rowIndex) {
        return _model.getBitmaskDefAt(rowIndex);
    }

    public BitmaskDef[] getBitmaskDefs() {
        return _model.getBitmaskDefs();
    }

    public void setBitmaskDefAt(BitmaskDef bitmaskDef, int rowIndex) {
        _model.setBitmaskDefAt(bitmaskDef, rowIndex);
    }

    public int getRowIndex(String bitmaskName) {
        return _model.getRowIndex(bitmaskName);
    }

    public int getRowIndex(BitmaskDef bitmaskDef) {
        return _model.getRowIndex(bitmaskDef);
    }

    public void addRow(BitmaskDef bitmaskDef) {
        _model.addRow(bitmaskDef);
    }

    public void addRow(boolean visibleFlag, BitmaskDef bitmaskDef) {
        _model.addRow(visibleFlag, bitmaskDef);
    }

    public void insertRowAt(BitmaskDef bitmaskDef, int rowIndex) {
        _model.insertRowAt(bitmaskDef, rowIndex);
    }

    public void insertRowAt(boolean visibleFlag, BitmaskDef bitmaskDef, int rowIndex) {
        _model.insertRowAt(visibleFlag, bitmaskDef, rowIndex);
    }

    public void removeRowAt(int rowIndex) {
        _model.removeRowAt(rowIndex);
    }

    public void clear() {
        _model.clear();
    }

    /**
     * Sets the data model for this table to <code>model</code> and registers with it for listener notifications from
     * the new data model.
     *
     * @param model the new data source for this table
     *
     * @throws java.lang.IllegalArgumentException
     *          if <code>newModel</code> is <code>null</code>
     * @see #getModel
     */
    @Override
    public void setModel(TableModel model) {
        if (model instanceof BitmaskDefTableModel) {
            _model = (BitmaskDefTableModel) model;
            super.setModel(_model);
        } else {
            throw new IllegalArgumentException("illegal model type");
        }
    }

    private void configureColumnModel() {
        if (_model.isVisibleFlagColumnEnabled()) {
            if (_visibleFlagHeaderRenderer == null) {
                _visibleFlagHeaderRenderer = new VisibleFlagHeaderRenderer();
            }
            getColumnModel().getColumn(0).setHeaderRenderer(_visibleFlagHeaderRenderer);
            getColumnModel().getColumn(0).setPreferredWidth(24); // vis
            getColumnModel().getColumn(1).setPreferredWidth(100); // name
            getColumnModel().getColumn(2).setPreferredWidth(50); // color
            getColumnModel().getColumn(3).setPreferredWidth(40); // transparency
            getColumnModel().getColumn(4).setPreferredWidth(640); // description
        } else {
            getColumnModel().getColumn(0).setPreferredWidth(100); // name
            getColumnModel().getColumn(1).setPreferredWidth(50); // color
            getColumnModel().getColumn(2).setPreferredWidth(40); // transparency
            getColumnModel().getColumn(3).setPreferredWidth(640); // description
        }
    }

    private class ColorRenderer extends JLabel implements TableCellRenderer {

        Border _unselectedBorder = null;
        Border _selectedBorder = null;
        boolean _bordered = true;

        public ColorRenderer(boolean bordered) {
            super();
            _bordered = bordered;
            this.setOpaque(true); //MUST do this for background to show up.
        }

        @Override
        public Component getTableCellRendererComponent(JTable table,
                                                       Object color,
                                                       boolean selected,
                                                       boolean hasFocus,
                                                       int row,
                                                       int column) {
            this.setBackground((Color) color);
            if (_bordered) {
                if (selected) {
                    if (_selectedBorder == null) {
                        _selectedBorder = BorderFactory.createMatteBorder(2, 5, 2, 5,
                                                                          table.getSelectionBackground());
                    }
                    this.setBorder(_selectedBorder);
                } else {
                    if (_unselectedBorder == null) {
                        _unselectedBorder = BorderFactory.createMatteBorder(2, 5, 2, 5,
                                                                            table.getBackground());
                    }
                    this.setBorder(_unselectedBorder);
                }
            }
            return this;
        }
    }

    private class VisibleFlagHeaderRenderer extends JLabel implements TableCellRenderer {

        public VisibleFlagHeaderRenderer() {
            ImageIcon icon = UIUtils.loadImageIcon("icons/EyeIcon10.gif");
            this.setBorder(UIManager.getBorder("TableHeader.cellBorder"));
            this.setText(_model.getColumnName(0));
            if (icon != null) {
                Dimension d = this.getPreferredSize();
                this.setText(null);
                this.setIcon(icon);
                this.setHorizontalAlignment(JLabel.CENTER);
                this.setPreferredSize(d);
            }
        }

        @Override
        public Component getTableCellRendererComponent(JTable table,
                                                       Object value,
                                                       boolean isSelected,
                                                       boolean hasFocus,
                                                       int row,
                                                       int column) {
            return this;
        }
    }


    private class BooleanEditor extends DefaultCellEditor {

        public BooleanEditor() {
            super(new JCheckBox());
            final JCheckBox checkBox = (JCheckBox) getComponent();
            checkBox.setHorizontalAlignment(JCheckBox.CENTER);
            checkBox.addActionListener(new ActionListener() {

                @Override
                public void actionPerformed(ActionEvent e) {
                    final int rowIndex = getSelectedRow();
                    if (rowIndex >= 0) {
                        final boolean visibleFlag = checkBox.isSelected();
                        _model.setVisibleFlag(visibleFlag, rowIndex);
                    }
                }
            });
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
            int rowIndex = rowAtPoint(e.getPoint());
            if (rowIndex != _rowIndex) {
                _rowIndex = rowIndex;
                if (_rowIndex >= 0 && _rowIndex < getRowCount()) {
                    String toolTipText = getBitmaskDefAt(_rowIndex).getDescription();
                    if (StringUtils.isNullOrEmpty(toolTipText)) {
                        toolTipText = getBitmaskDefAt(_rowIndex).getExpr();
                    }
                    setToolTipText(toolTipText);
                }
            }
        }
    }
}
