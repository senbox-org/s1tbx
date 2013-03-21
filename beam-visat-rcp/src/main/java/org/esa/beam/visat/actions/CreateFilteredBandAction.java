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
package org.esa.beam.visat.actions;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.ConvolutionFilterBand;
import org.esa.beam.framework.datamodel.FilterBand;
import org.esa.beam.framework.datamodel.GeneralFilterBand;
import org.esa.beam.framework.datamodel.Kernel;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.command.CommandEvent;
import org.esa.beam.framework.ui.command.ExecCommand;
import org.esa.beam.visat.VisatApp;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Font;
import java.text.MessageFormat;

// todo - allow for user-defined kernels
// todo - add kernel editor
// todo - import/export kernels
// todo - make filtered bands 'real' product components and store in DIMAP

/**
 * Installs commands into VISAT which lets a user attach a {@link org.esa.beam.framework.datamodel.PixelGeoCoding} based on pixels rather than tie points to the current product.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 */
public class CreateFilteredBandAction extends ExecCommand {

    private static final String TITLE = "Create Filtered Band"; /*I18N*/


    @Override
    public void actionPerformed(CommandEvent event) {
        applyImageKernel();
    }

    @Override
    public void updateState(CommandEvent event) {
        final ProductNode node = VisatApp.getApp().getSelectedProductNode();
        event.getCommand().setEnabled(node instanceof Band);
    }

    Filter[] LINE_DETECTION_FILTERS = {
            new KernelFilter("Horizontal Edges", new Kernel(3, 3, new double[]{
                    -1, -1, -1,
                    +2, +2, +2,
                    -1, -1, -1
            })),
            new KernelFilter("Vertical Edges", new Kernel(3, 3, new double[]{
                    -1, +2, -1,
                    -1, +2, -1,
                    -1, +2, -1
            })),
            new KernelFilter("Left Diagonal Edges", new Kernel(3, 3, new double[]{
                    +2, -1, -1,
                    -1, +2, -1,
                    -1, -1, +2
            })),
            new KernelFilter("Right Diagonal Edges", new Kernel(3, 3, new double[]{
                    -1, -1, +2,
                    -1, +2, -1,
                    +2, -1, -1
            })),

            new KernelFilter("Compass Edge Detector", new Kernel(3, 3, new double[]{
                    -1, +1, +1,
                    -1, -2, +1,
                    -1, +1, +1,
            })),

            new KernelFilter("Diagonal Compass Edge Detector", new Kernel(3, 3, new double[]{
                    +1, +1, +1,
                    -1, -2, +1,
                    -1, -1, +1,
            })),

            new KernelFilter("Roberts Cross North-West", new Kernel(2, 2, new double[]{
                    +1, 0,
                    0, -1,
            })),

            new KernelFilter("Roberts Cross North-East", new Kernel(2, 2, new double[]{
                    0, +1,
                    -1, 0,
            })),
    };
    Filter[] GRADIENT_DETECTION_FILTERS = {
            new KernelFilter("Sobel North", new Kernel(3, 3, new double[]{
                    -1, -2, -1,
                    +0, +0, +0,
                    +1, +2, +1,
            })),
            new KernelFilter("Sobel South", new Kernel(3, 3, new double[]{
                    +1, +2, +1,
                    +0, +0, +0,
                    -1, -2, -1,
            })),
            new KernelFilter("Sobel West", new Kernel(3, 3, new double[]{
                    -1, 0, +1,
                    -2, 0, +2,
                    -1, 0, +1,
            })),
            new KernelFilter("Sobel East", new Kernel(3, 3, new double[]{
                    +1, 0, -1,
                    +2, 0, -2,
                    +1, 0, -1,
            })),
            new KernelFilter("Sobel North East", new Kernel(3, 3, new double[]{
                    +0, -1, -2,
                    +1, +0, -1,
                    +2, +1, -0,
            })),
    };
    Filter[] SMOOTHING_FILTERS = {
            new KernelFilter("Arithmetic 3x3 Mean", new Kernel(3, 3, 1.0 / 9.0, new double[]{
                    +1, +1, +1,
                    +1, +1, +1,
                    +1, +1, +1,
            })),

            new KernelFilter("Arithmetic 4x4 Mean", new Kernel(4, 4, 1.0 / 16.0, new double[]{
                    +1, +1, +1, +1,
                    +1, +1, +1, +1,
                    +1, +1, +1, +1,
                    +1, +1, +1, +1,
            })),

            new KernelFilter("Arithmetic 5x5 Mean", new Kernel(5, 5, 1.0 / 25.0, new double[]{
                    +1, +1, +1, +1, +1,
                    +1, +1, +1, +1, +1,
                    +1, +1, +1, +1, +1,
                    +1, +1, +1, +1, +1,
                    +1, +1, +1, +1, +1,
            })),

            new KernelFilter("Low-Pass 3x3", new Kernel(3, 3, 1.0 / 16.0, new double[]{
                    +1, +2, +1,
                    +2, +4, +2,
                    +1, +2, +1,
            })),
            new KernelFilter("Low-Pass 5x5", new Kernel(5, 5, 1.0 / 60.0, new double[]{
                    +1, +1, +1, +1, +1,
                    +1, +4, +4, +4, +1,
                    +1, +4, 12, +4, +1,
                    +1, +4, +4, +4, +1,
                    +1, +1, +1, +1, +1,
            })),
    };
    Filter[] SHARPENING_FILTERS = {
            new KernelFilter("High-Pass 3x3 #1", new Kernel(3, 3, new double[]{
                    -1, -1, -1,
                    -1, +9, -1,
                    -1, -1, -1
            })),


            new KernelFilter("High-Pass 3x3 #2", new Kernel(3, 3, new double[]{
                    +0, -1, +0,
                    -1, +5, -1,
                    +0, -1, +0
            })),

            new KernelFilter("High-Pass 5x5", new Kernel(5, 5, new double[]{
                    +0, -1, -1, -1, +0,
                    -1, +2, -4, +2, -1,
                    -1, -4, 13, -4, -1,
                    -1, +2, -4, +2, -1,
                    +0, -1, -1, -1, +0,
            })),

    };
    Filter[] LAPLACIAN_FILTERS = {
            new KernelFilter("Laplace 3x3", new Kernel(3, 3, new double[]{
                    +0, -1, +0,
                    -1, +4, -1,
                    +0, -1, +0,
            })),
            new KernelFilter("Laplace 5x5", new Kernel(5, 5, new double[]{
                    +1, +1, +1, +1, +1,
                    +1, +1, +1, +1, +1,
                    +1, +1, 24, +1, +1,
                    +1, +1, +1, +1, +1,
                    +1, +1, +1, +1, +1,
            })),
    };

    Filter[] NON_LINEAR_FILTERS = {
            new GeneralFilter("Minimum 3x3", 3, 3, GeneralFilterBand.MIN),
            new GeneralFilter("Minimum 5x5", 5, 5, GeneralFilterBand.MIN),
            new GeneralFilter("Maximum 3x3", 3, 3, GeneralFilterBand.MAX),
            new GeneralFilter("Maximum 5x5", 5, 5, GeneralFilterBand.MAX),
            new GeneralFilter("Mean 3x3", 3, 3, GeneralFilterBand.MEAN),
            new GeneralFilter("Mean 5x5", 5, 5, GeneralFilterBand.MEAN),
            new GeneralFilter("Median 3x3", 3, 3, GeneralFilterBand.MEDIAN),
            new GeneralFilter("Median 5x5", 5, 5, GeneralFilterBand.MEDIAN),
// TODO(mp - 08.10.2008) - removed till the JAI operator is implemented
//            new GeneralFilter("Standard Deviation 3x3", 3, 3, GeneralFilterBand.STDDEV),
//            new GeneralFilter("Standard Deviation 5x5", 5, 5, GeneralFilterBand.STDDEV),
//            new GeneralFilter("Root-Mean-Square 3x3", 3, 3, GeneralFilterBand.RMS),
//            new GeneralFilter("Root-Mean-Square 5x5", 5, 5, GeneralFilterBand.RMS),
    };

    private void applyImageKernel() {
        final DialogData dialogData = promptForFilter();
        if (dialogData == null) {
            return;
        }
        final FilterBand filterBand = createFilterBand(dialogData.getFilter(), dialogData.getBandName());
        VisatApp visatApp = VisatApp.getApp();
        if (visatApp.getPreferences().getPropertyBool(VisatApp.PROPERTY_KEY_AUTO_SHOW_NEW_BANDS, true)) {
            visatApp.openProductSceneView(filterBand);
        }
    }

    private static FilterBand createFilterBand(Filter filter, String bandName) {
        final RasterDataNode raster = (RasterDataNode) VisatApp.getApp().getSelectedProductNode();

        final FilterBand filterBand;
        if (filter instanceof KernelFilter) {
            final KernelFilter kernelFilter = (KernelFilter) filter;
            filterBand = new ConvolutionFilterBand(bandName, raster, kernelFilter.kernel);
        } else {
            final GeneralFilter generalFilter = (GeneralFilter) filter;
            filterBand = new GeneralFilterBand(bandName, raster, generalFilter.width, generalFilter.operator);
        }
        final String descr = MessageFormat.format("Filter ''{0}'' applied to ''{1}''",
                                                  filter.toString(),
                                                  raster.getName());
        filterBand.setDescription(descr);
        raster.getProduct().addBand(filterBand);
        filterBand.fireProductNodeDataChanged();
        return filterBand;
    }


    private DialogData promptForFilter() {
        final JTree tree = createTree();
        tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
        final JScrollPane treeView = new JScrollPane(tree);

        final JPanel contentPane = new JPanel(new BorderLayout(4, 4));
        contentPane.add(new JLabel("Filters:"), BorderLayout.NORTH);
        contentPane.add(treeView, BorderLayout.CENTER);

        final ProductNode selectedNode = VisatApp.getApp().getSelectedProductNode();
        final Product product = selectedNode.getProduct();
        final JPanel namePanel = new JPanel(new BorderLayout(4, 4));
        namePanel.add(new JLabel("Band name:"), BorderLayout.WEST);     /*I18N*/
        final JTextField nameField = new JTextField("filtered_" + selectedNode.getName());
        namePanel.add(nameField, BorderLayout.CENTER);
        contentPane.add(namePanel, BorderLayout.SOUTH);

        final ModalDialog dialog = new CreateFilteredBandDialog(nameField, product, tree);
        dialog.setContent(contentPane);
        if (dialog.show() == ModalDialog.ID_OK) {
            return new DialogData(nameField.getText(), getSelectedFilter(tree));
        }
        return null;
    }

    private static class DialogData {

        private final Filter filter;
        private final String bandName;

        public DialogData(String bandName, Filter filter) {
            this.bandName = bandName;
            this.filter = filter;
        }

        public String getBandName() {
            return bandName;
        }

        public Filter getFilter() {
            return filter;
        }
    }

    private JTree createTree() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode("@root");
        root.add(createNodes("Detect Lines", LINE_DETECTION_FILTERS));
        root.add(createNodes("Detect Gradients (Emboss)", GRADIENT_DETECTION_FILTERS));
        root.add(createNodes("Smooth and Blurr", SMOOTHING_FILTERS));
        root.add(createNodes("Sharpen", SHARPENING_FILTERS));
        root.add(createNodes("Enhance Discontinuities", LAPLACIAN_FILTERS));
        root.add(createNodes("Non-Linear Filters", NON_LINEAR_FILTERS));
        final JTree tree = new JTree(root);
        tree.setRootVisible(false);
        tree.setShowsRootHandles(true);
        tree.setCellRenderer(new MyDefaultTreeCellRenderer());
        tree.putClientProperty("JTree.lineStyle", "Angled");
        expandAll(tree);
        return tree;
    }

    private static Filter getSelectedFilter(final JTree tree) {
        final TreePath selectionPath = tree.getSelectionPath();
        if (selectionPath == null) {
            return null;
        }
        final Object[] path = selectionPath.getPath();
        if (path != null && path.length > 0) {
            final DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) path[path.length - 1];
            if (treeNode.getUserObject() instanceof Filter) {
                return (Filter) treeNode.getUserObject();

            }
        }
        return null;
    }


    private static DefaultMutableTreeNode createNodes(String categoryName, Filter[] filters) {

        DefaultMutableTreeNode category = new DefaultMutableTreeNode(categoryName);

        for (Filter filter : filters) {
            DefaultMutableTreeNode item = new DefaultMutableTreeNode(filter);
            category.add(item);
        }

        return category;
    }


    private static void expandAll(JTree tree) {
        DefaultMutableTreeNode actNode = (DefaultMutableTreeNode) tree.getModel().getRoot();
        while (actNode != null) {
            if (!actNode.isLeaf()) {
                final TreePath actPath = new TreePath(actNode.getPath());
                tree.expandRow(tree.getRowForPath(actPath));
            }
            actNode = actNode.getNextNode();
        }
    }

    private static class MyDefaultTreeCellRenderer extends DefaultTreeCellRenderer {

        private Font _plainFont;
        private Font _boldFont;

        @Override
        public Component getTreeCellRendererComponent(JTree tree, Object value, boolean sel, boolean expanded,
                                                      boolean leaf, int row, boolean hasFocus) {
            final JLabel c = (JLabel) super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row,
                                                                         hasFocus);
            if (_plainFont == null) {
                _plainFont = c.getFont().deriveFont(Font.PLAIN);
                _boldFont = c.getFont().deriveFont(Font.BOLD);
            }
            c.setFont(leaf ? _plainFont : _boldFont);
            c.setIcon(null);
            return c;
        }
    }

    private static abstract class Filter {

        private String name;

        public Filter(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return name;
        }

        @Override
        public abstract boolean equals(Object obj);
    }

    private static class KernelFilter extends Filter {

        private Kernel kernel;

        public KernelFilter(String name, Kernel kernel) {
            super(name);
            this.kernel = kernel;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            } else if (obj instanceof KernelFilter) {
                KernelFilter other = (KernelFilter) obj;
                return toString().equals(other.toString()) && kernel.equals(other.kernel);
            }
            return false;
        }
    }

    private static class GeneralFilter extends Filter {

        int width;
        int height;
        GeneralFilterBand.Operator operator;

        public GeneralFilter(String name, int width, int height, GeneralFilterBand.Operator operator) {
            super(name);
            this.width = width;
            this.height = height;
            this.operator = operator;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == this) {
                return true;
            } else if (obj instanceof GeneralFilter) {
                GeneralFilter other = (GeneralFilter) obj;
                return toString().equals(other.toString()) && operator == other.operator;
            }
            return false;
        }
    }

    class CreateFilteredBandDialog extends ModalDialog {

        private final JTextField nameField;
        private final Product product;
        private final JTree tree;

        public CreateFilteredBandDialog(JTextField nameField, Product product, JTree tree) {
            super(VisatApp.getApp().getMainFrame(), CreateFilteredBandAction.TITLE, ModalDialog.ID_OK_CANCEL_HELP,
                  CreateFilteredBandAction.this.getHelpId());
            this.nameField = nameField;
            this.product = product;
            this.tree = tree;
        }

        @Override
        protected boolean verifyUserInput() {
            String message = null;
            final String bandName = nameField.getText().trim();
            if (bandName.equals("")) {
                message = "Please enter a name for the new filtered band."; /*I18N*/
            } else if (!ProductNode.isValidNodeName(bandName)) {
                message = MessageFormat.format("The band name ''{0}'' appears not to be valid.\n" +
                                               "Please choose a different band name.", bandName); /*I18N*/
            } else if (product.containsBand(bandName)) {
                message = MessageFormat.format("The selected product already contains a band named ''{0}''.\n" +
                                               "Please choose a different band name.", bandName); /*I18N*/
            } else if (getSelectedFilter(tree) == null) {
                message = "Please select a filter.";    /*I18N*/
            }
            if (message != null) {
                VisatApp.getApp().showErrorDialog(TITLE, message);
                return false;
            }
            return true;
        }
    }
}
