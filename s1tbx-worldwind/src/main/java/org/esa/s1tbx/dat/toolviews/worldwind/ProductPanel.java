/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.s1tbx.dat.toolviews.worldwind;

import com.alee.extended.tree.WebCheckBoxTree;
import gov.nasa.worldwind.WorldWindow;
import gov.nasa.worldwind.awt.WorldWindowGLCanvas;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.border.CompoundBorder;
import javax.swing.border.TitledBorder;
import javax.swing.tree.DefaultMutableTreeNode;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;


class ProductPanel extends JPanel {
    private final ProductLayer productLayer;
    private JPanel layersPanel;
    private JPanel westPanel;
    private JScrollPane scrollPane;
    private Font defaultFont = null;
    //private JTree tree = null;
    private WebCheckBoxTree tree = null;

    public ProductPanel(WorldWindow wwd, ProductLayer prodLayer) {
        super(new BorderLayout());
        productLayer = prodLayer;
        this.makePanel(wwd, new Dimension(100, 400));
    }

    public ProductPanel(WorldWindow wwd, Dimension size, ProductLayer prodLayer) {
        super(new BorderLayout());
        productLayer = prodLayer;
        this.makePanel(wwd, size);
    }

    private void makePanel(WorldWindow wwd, Dimension size) {
        // Make and fill the panel holding the layer titles.
        this.layersPanel = new JPanel(new GridLayout(0, 1, 0, 4));
        this.layersPanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
        this.fill(wwd);

        // Must put the layer grid in a container to prevent scroll panel from stretching their vertical spacing.
        final JPanel dummyPanel = new JPanel(new BorderLayout());

        // CHANGED
        dummyPanel.add(this.layersPanel, BorderLayout.NORTH);

        // Put the name panel in a scroll bar.
        this.scrollPane = new JScrollPane(dummyPanel);
        this.scrollPane.setBorder(BorderFactory.createEmptyBorder(0, 0, 0, 0));
        if (size != null)
            this.scrollPane.setPreferredSize(size);

        // Add the scroll bar and name panel to a titled panel that will resize with the main window.
        // CHANGED

        westPanel = new JPanel(new GridLayout(0, 1, 0, 10));
        westPanel.setBorder(
                new CompoundBorder(BorderFactory.createEmptyBorder(9, 9, 9, 9), new TitledBorder("Products")));
        westPanel.setToolTipText("Products to Show");
        westPanel.add(scrollPane);
        this.add(westPanel, BorderLayout.CENTER);

        /*
        WorldWindow emptyWWD = this.createWorldWindow();
        ((Component) emptyWWD).setPreferredSize(size);

        // Create the default model as described in the current worldwind properties.
        Model m = (Model) WorldWind.createConfigurationComponent(AVKey.MODEL_CLASS_NAME);
        emptyWWD.setModel(m);
        this.add((Component) emptyWWD, BorderLayout.CENTER);
        */
    }

    protected WorldWindow createWorldWindow()
    {
        return new WorldWindowGLCanvas();
    }

    private void fill(WorldWindow wwd) {


        final String[] productNames = productLayer.getProductNames();
        for (String name : productNames) {
            final LayerAction action = new LayerAction(productLayer, wwd, name, productLayer.getOpacity(name) != 0);
            final JCheckBox jcb = new JCheckBox(action);

            // CHANGED
            System.out.println("fill: checkbox" + name);
            jcb.setSelected(action.selected);
            this.layersPanel.add(jcb);

            /*
            DefaultMutableTreeNode top =
                    new DefaultMutableTreeNode("The Java Series");
            createNodes(top);
            //Create a tree that allows one selection at a time.
            tree = new WebCheckBoxTree(top);
            this.layersPanel.add(tree);
            */

            if (defaultFont == null) {
                this.defaultFont = jcb.getFont();
            }
        }
    }

    public void update(WorldWindow wwd) {
        // Replace all the layer names in the layers panel with the names of the current layers.
        this.layersPanel.removeAll();
        this.fill(wwd);
        this.westPanel.revalidate();
        this.westPanel.repaint();
    }

    @Override
    public void setToolTipText(String string) {
        this.scrollPane.setToolTipText(string);
    }

    private static class LayerAction extends AbstractAction {
        final WorldWindow wwd;
        private final ProductLayer layer;
        private final boolean selected;
        private final String name;

        public LayerAction(ProductLayer layer, WorldWindow wwd, String name, boolean selected) {
            super(name);
            this.wwd = wwd;
            this.layer = layer;
            this.name = name;
            this.selected = selected;
            this.layer.setEnabled(this.selected);
        }

        public void actionPerformed(ActionEvent actionEvent) {
            // ADDED
            System.out.println("actionPerformed " + actionEvent);
            // Simply enable or disable the layer based on its toggle button.
            if (((JCheckBox) actionEvent.getSource()).isSelected())
                this.layer.setOpacity(name, this.layer.getOpacity());
            else
                this.layer.setOpacity(name, 0);

            wwd.redraw();
        }
    }



    private void createNodes(DefaultMutableTreeNode top) {
        DefaultMutableTreeNode category = null;
        DefaultMutableTreeNode book = null;

        category = new DefaultMutableTreeNode("Books for Java Programmers");
        top.add(category);

        //original Tutorial
        book = new DefaultMutableTreeNode(new BookInfo
                ("The Java Tutorial: A Short Course on the Basics",
                        "tutorial.html"));
        category.add(book);

        //Tutorial Continued
        book = new DefaultMutableTreeNode(new BookInfo
                ("The Java Tutorial Continued: The Rest of the JDK",
                        "tutorialcont.html"));
        category.add(book);

    }

    private class BookInfo {
        public String bookName;

        public BookInfo(String book, String filename) {
            bookName = book;
        }

        public String toString() {
            return bookName;
        }
    }

}
