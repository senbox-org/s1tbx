/*
 * $Id: ProductMetadataTable.java,v 1.1 2006/10/10 14:47:37 norman Exp $
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
package org.esa.beam.framework.ui.product;

import java.awt.Color;
import java.awt.Component;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JLabel;
import javax.swing.JTable;
import javax.swing.border.EmptyBorder;
import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;

import org.esa.beam.framework.datamodel.FlagCoding;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.ProductData;

//@todo 1 nf/nf - class documentation

public class ProductMetadataTable extends JTable {

    public static final int NAME_COL_INDEX = 0;
    public static final int VALUE_COL_INDEX = 1;
    public static final int TYPE_COL_INDEX = 2;
    public static final int UNIT_COL_INDEX = 3;
    public static final int DESCR_COL_INDEX = 4;

    private static final String[] _columnNames = {
        "Name", // 0
        "Value", // 1
        "Type", // 2
        "Unit", // 3
        "Description" // 4
    };

    private static final int[] _columnWidths = {
        180, // 0
        180, // 1
        50, // 2
        40, // 3
        200 // 4
    };

    private static final int _DEFAULT_FONT_SIZE = 11;
    private static final Color _numberValueColor = new Color(0, 0, 120);
    private static final Color _textValueColor = new Color(0, 120, 0);

    private static Font _fixedFont;
    private static Font _plainFont;
    private static Font _italicFont;

    public ProductMetadataTable(MetadataElement metadataGroup) {
        initFonts();
        setModel(new AnnotationTableModel(metadataGroup));
        getTableHeader().setReorderingAllowed(false);
        ElementRefCellRenderer renderer = new ElementRefCellRenderer();
        renderer.setBorder(new EmptyBorder(2, 3, 2, 3));
        setDefaultRenderer(ElementRef.class, renderer);
        for (int i = 0; i < _columnWidths.length; i++) {
            TableColumn column = getColumnModel().getColumn(i);
            column.setPreferredWidth(_columnWidths[i]);
        }
    }

    public MetadataElement getMetadataElement() {
        return ((AnnotationTableModel) getModel()).getMetadataElement();
    }

    private static int computeTotalRowCount(MetadataElement metadataGroup) {
        int rowCount = 0;
        for (int i = 0; i < metadataGroup.getNumAttributes(); i++) {
            MetadataAttribute attribute = metadataGroup.getAttributeAt(i);
            if (isNumericAttribute(attribute)) {
                rowCount += attribute.getNumDataElems();
            } else {
                rowCount++;
            }
        }
        return rowCount;
    }

    public boolean isHexadecimal() {
        return getMetadataElement() instanceof FlagCoding;
    }

    private static boolean isNumericAttribute(MetadataAttribute attribute) {
        return !(attribute.getData() instanceof ProductData.ASCII)
               && !(attribute.getData() instanceof ProductData.UTC);
    }

    private static void initFonts() {
        if (_fixedFont == null) {
            FontUIResource resource = MetalLookAndFeel.getUserTextFont();
            if (resource != null) {
                _fixedFont = new Font("Courier", Font.PLAIN, resource.getSize());
                _plainFont = resource.deriveFont(Font.PLAIN);
                _italicFont = resource.deriveFont(Font.ITALIC);
            } else {
                _fixedFont = new Font("Courier", Font.PLAIN, _DEFAULT_FONT_SIZE);
                _plainFont = new Font("SansSerif", Font.PLAIN, _DEFAULT_FONT_SIZE);
                _italicFont = new Font("SansSerif", Font.ITALIC, _DEFAULT_FONT_SIZE);
            }
        }
    }

    public String getElementText(ElementRef elementRef, int column) {
        String text = "";
        boolean elemEnum = isNumericAttribute(elementRef.getAttribute())
                           && elementRef.getAttribute().getNumDataElems() > 1;
        if (column == NAME_COL_INDEX) {
            if (elemEnum) {
                text = elementRef.getAttribute().getName() + "." + (elementRef.getElementIndex() + 1);
            } else {
                text = elementRef.getAttribute().getName();
            }
        } else if (column == VALUE_COL_INDEX) {
            if (isHexadecimal()) {
                text = Integer.toHexString(elementRef.getAttribute().getData().getElemInt());
                if (text.length() % 2 == 0) {
                    text = "0x" + text;
                } else {
                    text = "0x0" + text;
                }
            } else {
                if (isNumericAttribute(elementRef.getAttribute()) || elemEnum) {
                    text = elementRef.getAttribute().getData().getElemStringAt(elementRef.getElementIndex());
                } else {
                    text = elementRef.getAttribute().getData().getElemString();
                }
            }
        } else if (column == TYPE_COL_INDEX) {
            text = elementRef.getAttribute().getData().getTypeString();
        } else if (column == UNIT_COL_INDEX) {
            text = elementRef.getAttribute().getUnit() != null ? elementRef.getAttribute().getUnit() : "";
        } else if (column == DESCR_COL_INDEX) {
            text = elementRef.getAttribute().getDescription() != null ? elementRef.getAttribute().getDescription() : "";
        }
        return text;
    }

    /**
     * A specialized table model for a data product's meta-data annotation.
     */
    class AnnotationTableModel extends AbstractTableModel {

        private final MetadataElement _metadataElement;
        private final List _elementRefList;

        public AnnotationTableModel(MetadataElement metadataGroup) {
            _metadataElement = metadataGroup;
            int rowCount = computeTotalRowCount(metadataGroup);
            _elementRefList = new ArrayList(rowCount);
            for (int i = 0; i < metadataGroup.getNumAttributes(); i++) {
                MetadataAttribute attribute = metadataGroup.getAttributeAt(i);
                if (isNumericAttribute(attribute)) {
                    for (int j = 0; j < attribute.getNumDataElems(); j++) {
                        _elementRefList.add(new ElementRef(attribute, j));
                    }
                } else {
                    _elementRefList.add(new ElementRef(attribute, 0));
                }
            }
        }

        public MetadataElement getMetadataElement() {
            return _metadataElement;
        }

        public String getColumnName(int col) {
            return _columnNames[col];
        }

        public int getRowCount() {
            return _elementRefList.size();
        }

        public int getColumnCount() {
            return _columnNames.length;
        }

        public Class getColumnClass(int col) {
            return ElementRef.class;
        }

        public Object getValueAt(int row, int col) {
            return _elementRefList.get(row);
        }

        public boolean isCellEditable(int row, int col) {
            return false; // No!
        }

        public void setValueAt(Object value, int row, int col) {
            // Don't set value, read only
        }
    }

    /**
     * A specialized table cell renderer for a data product's meta-data annotation.
     */
    class ElementRefCellRenderer extends DefaultTableCellRenderer {

        public Component getTableCellRendererComponent(JTable table,
                                                       Object value,
                                                       boolean isSelected,
                                                       boolean hasFocus,
                                                       int row,
                                                       int column) {
            ElementRef elementRef = (ElementRef) value;

            // Set color
            //
            if (isSelected) {
                this.setBackground(getSelectionBackground());
                this.setForeground(getSelectionForeground());
            } else {
                this.setBackground(Color.white);
                if (column == VALUE_COL_INDEX) {
                    if (isNumericAttribute(elementRef.getAttribute())) {
                        this.setForeground(_numberValueColor);
                    } else {
                        this.setForeground(_textValueColor);
                    }
                } else {
                    this.setForeground(Color.black);
                }
            }

            // Set text alignment
            //
            if (column == VALUE_COL_INDEX && isNumericAttribute(elementRef.getAttribute())) {
                setHorizontalAlignment(JLabel.RIGHT);
            } else {
                setHorizontalAlignment(JLabel.LEFT);
            }

            // Set font
            //
            if (column == NAME_COL_INDEX) {
                this.setFont(_plainFont);
            } else if (column == VALUE_COL_INDEX) {
                this.setFont(_plainFont);
            } else if (column == TYPE_COL_INDEX) {
                this.setFont(_plainFont);
            } else if (column == UNIT_COL_INDEX) {
                this.setFont(_italicFont);
            } else if (column == DESCR_COL_INDEX) {
                this.setFont(_italicFont);
            } else {
                this.setFont(_plainFont);
            }

            final String text = getElementText(elementRef, column);
            setText(text);

            return this;
        }

    }

    public static class ElementRef {

        private MetadataAttribute _attribute;
        private int _elementIndex;

        ElementRef(MetadataAttribute attribute, int elementIndex) {
            _attribute = attribute;
            _elementIndex = elementIndex;
        }

        public MetadataAttribute getAttribute() {
            return _attribute;
        }

        public int getElementIndex() {
            return _elementIndex;
        }

        public String toString() {
            return _attribute.getName() + ".element[" + _elementIndex + "]";
        }
    }
}