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
package org.esa.beam.visat.toolviews.mask;

import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.RasterDataNode;

import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.JPanel;
import javax.swing.JTable;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

abstract class MaskForm {

    private final MaskTable maskTable;

    protected MaskForm(boolean maskManagmentMode, ListSelectionListener selectionListener) {
        maskTable = new MaskTable(maskManagmentMode);
        maskTable.getSelectionModel().addListSelectionListener(selectionListener);
        maskTable.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                updateState();
            }
        });
        maskTable.getModel().addTableModelListener(new TableModelListener() {
            @Override
            public void tableChanged(TableModelEvent e) {
                updateState();
            }
        });
        maskTable.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(final MouseEvent e) {
                if (e.getClickCount() == 2) {
                    Action action = getDoubleClickAction();
                    if (action.isEnabled()) {
                        action.actionPerformed(new ActionEvent(e.getSource(), e.getID(), null));
                    }
                }
            }
        });

    }

    public JTable getMaskTable() {
        return maskTable;
    }

    public Action getDoubleClickAction() {
        return null;
    }

    public AbstractButton getHelpButton() {
        return null;
    }

    public void updateState() {
    }

    public abstract JPanel createContentPanel();

    public Product getProduct() {
        return maskTable.getProduct();
    }

    public RasterDataNode getRaster() {
        return maskTable.getModel().getVisibleBand();
    }

    public Mask getSelectedMask() {
        return maskTable.getSelectedMask();
    }

    public Mask[] getSelectedMasks() {
        return maskTable.getSelectedMasks();
    }

    public Mask getMask(int rowIndex) {
        return maskTable.getMask(rowIndex);
    }

    public void addMask(Mask mask) {
        maskTable.addMask(mask);
    }

    public void insertMask(Mask mask, int index) {
        maskTable.insertMask(mask, index);
    }

    public void removeMask(Mask mask) {
        maskTable.removeMask(mask);
    }

    public boolean isInManagementMode() {
        return maskTable.isInManagmentMode();
    }

    public int getSelectedRowCount() {
        return maskTable.getSelectedRowCount();
    }

    public int getSelectedRow() {
        return maskTable.getSelectedRow();
    }

    public void setSelectedRow(int rowIndex) {
        maskTable.getSelectionModel().setSelectionInterval(rowIndex, rowIndex);
    }

    public int getRowCount() {
        return maskTable.getRowCount();
    }

    void reconfigureMaskTable(Product product, RasterDataNode visibleBand) {
        maskTable.setProduct(product, visibleBand);
    }

    void clearMaskTable() {
        maskTable.clear();
    }

}