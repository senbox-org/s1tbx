/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.core.Assert;
import com.bc.ceres.swing.binding.ComponentAdapter;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.tool.ToolButtonFactory;
import org.esa.beam.util.Debug;

import javax.swing.AbstractButton;
import javax.swing.BoxLayout;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListDataListener;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;

/**
 * Enables clients to create a component for choosing source products. The list of source products can arbitrarily be
 * composed of
 * <ul>
 * <li>currently opened products</li>
 * <li>single products anywhere in the file system</li>
 * <li>whole directories anywhere in the file system and</li>
 * <li>recursive directories anywhere in the file system</li>
 * </ul>
 * <p/>
 * The file paths the user chooses are stored as objects of type {@link java.io.File} within the property that is passed
 * into the constructor. Products that are chosen from the product tree can be retrieved via
 * {@link #getSourceProducts()}. So, clients of these must take care that the value in the given property is taken into
 * account as well as the return value of that method.
 *
 * The property that serves as target container for the source product paths must be of type
 * <code>String[].class</code>. Changes in the list are synchronised with the property. If the changes of the property
 * values outside this component shall be synchronised with the list, it is necessary that the property lies within a
 * property container.
 *
 * @author thomas
 */
public class SourceProductList extends ComponentAdapter {

    private final AppContext appContext;

    private String lastOpenInputDir;
    private String lastOpenedFormat;
    private JList inputPathsList;
    private InputListModel listModel;
    private boolean xAxis;
    private JComponent[] components;
    private final ArrayList<ListDataListener> listenersQueue;

    /**
     * Constructor.
     *
     * @param appContext                 The context of the app using this component.
     *
     */
    public SourceProductList(AppContext appContext) {
        this.appContext = appContext;
        this.lastOpenInputDir = "org.esa.beam.framework.ui.product.lastOpenInputDir";
        this.lastOpenedFormat = "org.esa.beam.framework.ui.product.lastOpenedFormat";
        this.xAxis = true;
        listenersQueue = new ArrayList<>();
    }

    /**
     * Creates an array of two JPanels. The first panel contains a list displaying the chosen products. The second panel
     * contains buttons for adding and removing products, laid out in vertical direction. Note that it makes only sense
     * to use both components.
     *
     * @return an array of two JPanels.
     */
    @Override
    public JComponent[] getComponents() {
        if (components == null) {
            components = createComponents();
        }
        return components;
    }

    /**
     * Creates an array of two JPanels. The first panel contains a list displaying the chosen products. The second panel
     * contains buttons for adding and removing products, laid out in configurable direction. Note that it makes only sense
     * to use both components.
     *
     * @return an array of two JPanels.
     */
    private JComponent[] createComponents() {
        JPanel listPanel = new JPanel(new BorderLayout());
        listModel = new InputListModel();
        for (ListDataListener listDataListener : listenersQueue) {
            listModel.addListDataListener(listDataListener);
        }
        inputPathsList = createInputPathsList(listModel);
        final JScrollPane scrollPane = new JScrollPane(inputPathsList);
        scrollPane.setPreferredSize(new Dimension(100, 50));
        listPanel.add(scrollPane, BorderLayout.CENTER);

        final JPanel addRemoveButtonPanel = new JPanel();
        int axis = this.xAxis ? BoxLayout.X_AXIS : BoxLayout.Y_AXIS;
        final BoxLayout buttonLayout = new BoxLayout(addRemoveButtonPanel, axis);
        addRemoveButtonPanel.setLayout(buttonLayout);
        addRemoveButtonPanel.add(createAddInputButton());
        addRemoveButtonPanel.add(createRemoveInputButton());

        JPanel[] panels = new JPanel[2];
        panels[0] = listPanel;
        panels[1] = addRemoveButtonPanel;

        return panels;
    }

    /**
     * Clears the list of source products.
     */
    public void clear() {
        listModel.clear();
    }

    /**
     * Allows clients to add single products.
     *
     * @param product A product to add.
     */
    public void addProduct(Product product) {
        if (product != null) {
            try {
                listModel.addElements(product);
            } catch (ValidationException ve) {
                Debug.trace(ve);
            }
        }
    }

    /**
     * Returns those source products that have been chosen from the product tree.
     *
     * @return An array of source products.
     */
    public Product[] getSourceProducts() {
        return listModel.getSourceProducts();
    }

    private JList<Object> createInputPathsList(InputListModel inputListModel) {
        JList<Object> list = new JList<>(inputListModel);
        list.setCellRenderer(new SourceProductListRenderer());
        list.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        return list;
    }

    private AbstractButton createAddInputButton() {
        final AbstractButton addButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/Plus24.gif"),
                                                                        false);
        addButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final JPopupMenu popup = new JPopupMenu("Add");
                final Rectangle buttonBounds = addButton.getBounds();
                popup.add(new AddProductAction(appContext, listModel));
                popup.add(new AddFileAction(appContext, listModel, lastOpenInputDir, lastOpenedFormat));
                popup.add(new AddDirectoryAction(appContext, listModel, false, lastOpenInputDir));
                popup.add(new AddDirectoryAction(appContext, listModel, true, lastOpenInputDir));
                popup.show(addButton, 1, buttonBounds.height + 1);
            }
        });
        return addButton;
    }

    private AbstractButton createRemoveInputButton() {
        final AbstractButton removeButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/Minus24.gif"),
                                                                           false);
        removeButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                listModel.removeElementsAt(inputPathsList.getSelectedIndices());
            }
        });
        return removeButton;
    }

    @Override
    public void bindComponents() {
        String propertyName = getBinding().getPropertyName();
        Property property = getBinding().getContext().getPropertySet().getProperty(propertyName);
        Assert.argument(property.getType().equals(String[].class), "property '" + propertyName +"' must be of type String[].class");
        listModel.setProperty(property);
    }

    @Override
    public void unbindComponents() {
        listModel.setProperty(null);
    }

    @Override
    public void adjustComponents() {

    }

    /**
     * Add a listener that is informed every time the list's contents change.
     * @param changeListener the listener to add
     */
    public void addChangeListener(ListDataListener changeListener) {
        if (listModel == null) {
            listenersQueue.add(changeListener);
        } else {
            listModel.addListDataListener(changeListener);
        }
    }

    /**
     * Remove a change listener
     * @param changeListener the listener to remove
     */
    public void removeChangeListener(ListDataListener changeListener) {
        if (listModel == null) {
            listenersQueue.remove(changeListener);
        } else {
            listModel.removeListDataListener(changeListener);
        }
    }

    /**
     * Setter for property name indicating the last directory the user has opened
     *
     * @param lastOpenedFormat property name indicating the last directory the user has opened
     */
    public void setLastOpenedFormat(String lastOpenedFormat) {
        this.lastOpenedFormat = lastOpenedFormat;
    }

    /**
     * Setter for property name indicating the last product format the user has opened
     * @param lastOpenInputDir property name indicating the last product format the user has opened
     */
    public void setLastOpenInputDir(String lastOpenInputDir) {
        this.lastOpenInputDir = lastOpenInputDir;
    }

    /**
     * Setter for xAxis property.
     *
     * @param xAxis <code>true</code> if the buttons on the second panel shall be laid out in horizontal direction
     */
    public void setXAxis(boolean xAxis) {
        this.xAxis = xAxis;
    }

    private static class SourceProductListRenderer extends DefaultListCellRenderer {

        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
                                                      boolean cellHasFocus) {
            JLabel label = (JLabel) super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            String text;
            if (value instanceof File) {
                text = ((File) value).getAbsolutePath();
            } else {
                text = ((ProductNode) value).getDisplayName();
            }

            label.setText(text);
            label.setToolTipText(text);
            return label;
        }
    }
}
