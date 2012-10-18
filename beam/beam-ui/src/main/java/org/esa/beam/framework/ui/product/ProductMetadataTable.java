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
package org.esa.beam.framework.ui.product;

import com.jidesoft.grid.*;
import org.esa.beam.framework.datamodel.FlagCoding;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.ProductData;

import javax.swing.plaf.FontUIResource;
import javax.swing.plaf.metal.MetalLookAndFeel;
import javax.swing.table.TableColumn;
import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.List;

public class ProductMetadataTable extends TreeTable {

    public static final int NAME_COL_INDEX = 0;
    public static final int VALUE_COL_INDEX = 1;
    public static final int TYPE_COL_INDEX = 2;
    public static final int UNIT_COL_INDEX = 3;
    public static final int DESCR_COL_INDEX = 4;
    public static final int ID_COL_INDEX = 5;

    private static final String[] COLUMN_NAMES = {
            "Name", // 0
            "Value", // 1
            "Type", // 2
            "Unit", // 3
            "Description", // 4
            "#"
    };

    private static final int[] COLUMN_WIDTHS = {
            180, // 0
            180, // 1
            50, // 2
            40, // 3
            200,// 4
            40  // 5
    };

    private static final int _DEFAULT_FONT_SIZE = 11;
    private static final Color _numberValueColor = new Color(0, 0, 120);
    private static final Color _textValueColor = new Color(0, 120, 0);

    private static Font _fixedFont;
    private static Font _plainFont;
    private static Font _italicFont;

    MetadataElement rootElement;

    public ProductMetadataTable(MetadataElement rootElement) {
        this.rootElement = rootElement;
        initFonts();
        setModel(new MDTableModel(rootElement));
        getTableHeader().setReorderingAllowed(false);

        //ElementRefCellRenderer renderer = new ElementRefCellRenderer();
        //renderer.setBorder(new EmptyBorder(2, 3, 2, 3));
        //setDefaultRenderer(AttributeRef.class, renderer);
        for (int i = 0; i < COLUMN_WIDTHS.length; i++) {
            TableColumn column = getColumnModel().getColumn(i);
            column.setPreferredWidth(COLUMN_WIDTHS[i]);
        }
    }

    public MetadataElement getMetadataElement() {
        return rootElement;
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

//    public String getElementText(AttributeRef attributeRef, int column) {
//        String text = "";
//        boolean elemEnum = isNumericAttribute(attributeRef.getAttribute())
//                && attributeRef.getAttribute().getNumDataElems() > 1;
//        if (column == NAME_COL_INDEX) {
//            if (elemEnum) {
//                text = attributeRef.getAttribute().getName() + "." + (attributeRef.getElementIndex() + 1);
//            } else {
//                text = attributeRef.getAttribute().getName();
//            }
//        } else if (column == VALUE_COL_INDEX) {
//            if (isHexadecimal()) {
//                text = Integer.toHexString(attributeRef.getAttribute().getData().getElemInt());
//                if (text.length() % 2 == 0) {
//                    text = "0x" + text;
//                } else {
//                    text = "0x0" + text;
//                }
//            } else {
//                if (isNumericAttribute(attributeRef.getAttribute()) || elemEnum) {
//                    text = attributeRef.getAttribute().getData().getElemStringAt(attributeRef.getElementIndex());
//                } else {
//                    text = attributeRef.getAttribute().getData().getElemString();
//                }
//            }
//        } else if (column == TYPE_COL_INDEX) {
//            text = attributeRef.getAttribute().getData().getTypeString();
//        } else if (column == UNIT_COL_INDEX) {
//            text = attributeRef.getAttribute().getUnit() != null ? attributeRef.getAttribute().getUnit() : "";
//        } else if (column == DESCR_COL_INDEX) {
//            text = attributeRef.getAttribute().getDescription() != null ? attributeRef.getAttribute().getDescription() : "";
//        }
//        return text;
//    }

    /**
     * A specialized table model for a data product's meta-data annotation.
     */
    static class MDTableModel extends TreeTableModel {

        private final MetadataElement rootElement;

        public MDTableModel(MetadataElement rootElement) {
            this.rootElement = rootElement;
            setOriginalRows(createRowList(rootElement));
        }

        public static List<Row> createRowList(MetadataElement rootElement) {
            List<Row> rowList = new ArrayList<Row>(10);
            for (int i = 0; i < rootElement.getNumAttributes(); i++) {
                MetadataAttribute attribute = rootElement.getAttributeAt(i);
                if (!isNumericAttribute(attribute) || attribute.getData().isScalar()) {
                    rowList.add(new MDAttributeRow(attribute, -1, i));
                } else {
                    for (int j = 0; j < attribute.getNumDataElems(); j++) {
                        rowList.add(new MDAttributeRow(attribute, j, i));
                    }
                }
            }
            for (int i = 0; i < rootElement.getNumElements(); i++) {
                MetadataElement element = rootElement.getElementAt(i);
                rowList.add(new MDElementRow(element));
            }
            return rowList;
        }

        public MetadataElement getMetadataElement() {
            return rootElement;
        }

        @Override
        public String getColumnName(int col) {
            return COLUMN_NAMES[col];
        }

        public int getColumnCount() {
            return COLUMN_NAMES.length;
        }
    }

    static class MDElementRow extends DefaultExpandableRow {
        private MetadataElement element;

        MDElementRow(MetadataElement element) {
            this.element = element;
        }

        public MetadataElement getElement() {
            return element;
        }

        public Object getValueAt(int i) {
            if (i == NAME_COL_INDEX) {
                return element.getName();
            } else if (i == VALUE_COL_INDEX) {
                return "";
            } else if (i == TYPE_COL_INDEX) {
            } else if (i == UNIT_COL_INDEX) {
            } else if (i == DESCR_COL_INDEX) {
                return element.getDescription() != null ? element.getDescription() : "";
            }
            return "";
        }

        @Override
        public List getChildren() {
            List children = _children;
            if (children == null) {
                children = MDTableModel.createRowList(element);
                setChildren(children);
            }
            return children;
        }

        @Override
        public void setChildren(List children) {
            for (Object child : children) {
                ((Row) child).setParent(this);
            }
            super.setChildren(children);
        }
    }

    static class MDAttributeRow extends AbstractRow {
        MetadataAttribute attribute;
        int index;
        final int id;

        MDAttributeRow(MetadataAttribute attribute, int index, int id) {
            this.attribute = attribute;
            this.index = index;
            this.id = id;
        }

        public int getIndex() {
            return index;
        }

        public MetadataAttribute getAttribute() {
            return attribute;
        }

        public boolean isCellEditable(int column) {
            return !attribute.isReadOnly() && column == VALUE_COL_INDEX;
        }

        public void setValueAt(Object o, int column) {
            if(!attribute.isReadOnly() && column == VALUE_COL_INDEX) {
                final String value = (String) o;

                try {
                    if(attribute.getData() instanceof ProductData.ASCII) {
                        attribute.getData().setElems(value);
                    } else if(attribute.getData() instanceof ProductData.UTC) {
                        attribute.getData().setElems(ProductData.UTC.parse(value).getElems());
                    } else {
                        attribute.getData().setElemDouble(Double.parseDouble(value));
                    }
                } catch(Exception e) {
                    System.out.println(e.getMessage());
                    // do nothing and value will revert to original
                }
            }
        }

        public Object getValueAt(int column) {
            if(column == ID_COL_INDEX) {
                return id;   
            } else if (column == NAME_COL_INDEX) {
                if (index == -1) {
                    return attribute.getName();
                } else {
                    return attribute.getName() + '.' + index;
                }
            } else if (column == VALUE_COL_INDEX) {
                if (index == -1) {
                    return attribute.getData().getElemString();
                } else {
                    return attribute.getData().getElemStringAt(index);
                }
            } else if (column == TYPE_COL_INDEX) {
                return attribute.getData().getTypeString();
            } else if (column == UNIT_COL_INDEX) {
                return attribute.getUnit() != null ? attribute.getUnit() : "";
            } else if (column == DESCR_COL_INDEX) {
                return attribute.getDescription() != null ? attribute.getDescription() : "";
            }
            return "";
        }
    }

//    /**
//     * A specialized table cell renderer for a data product's meta-data annotation.
//     */
//    class ElementRefCellRenderer extends DefaultTableCellRenderer {
//
//        public Component getTableCellRendererComponent(JTable table,
//                                                       Object value,
//                                                       boolean isSelected,
//                                                       boolean hasFocus,
//                                                       int row,
//                                                       int column) {
//            AttributeRef attributeRef = (AttributeRef) value;
//
//            // Set color
//            //
//            if (isSelected) {
//                this.setBackground(getSelectionBackground());
//                this.setForeground(getSelectionForeground());
//            } else {
//                this.setBackground(Color.white);
//                if (column == VALUE_COL_INDEX) {
//                    if (isNumericAttribute(attributeRef.getAttribute())) {
//                        this.setForeground(_numberValueColor);
//                    } else {
//                        this.setForeground(_textValueColor);
//                    }
//                } else {
//                    this.setForeground(Color.black);
//                }
//            }
//
//            // Set text alignment
//            //
//            if (column == VALUE_COL_INDEX && isNumericAttribute(attributeRef.getAttribute())) {
//                setHorizontalAlignment(JLabel.RIGHT);
//            } else {
//                setHorizontalAlignment(JLabel.LEFT);
//            }
//
//            // Set font
//            //
//            if (column == NAME_COL_INDEX) {
//                this.setFont(_plainFont);
//            } else if (column == VALUE_COL_INDEX) {
//                this.setFont(_plainFont);
//            } else if (column == TYPE_COL_INDEX) {
//                this.setFont(_plainFont);
//            } else if (column == UNIT_COL_INDEX) {
//                this.setFont(_italicFont);
//            } else if (column == DESCR_COL_INDEX) {
//                this.setFont(_italicFont);
//            } else {
//                this.setFont(_plainFont);
//            }
//
//            final String text = getElementText(attributeRef, column);
//            setText(text);
//
//            return this;
//        }
//
//    }
}