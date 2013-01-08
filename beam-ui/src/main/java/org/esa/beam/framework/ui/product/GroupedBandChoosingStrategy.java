package org.esa.beam.framework.ui.product;

import com.jidesoft.swing.CheckBoxTree;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.framework.ui.GridBagUtils;

import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import java.awt.Color;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class GroupedBandChoosingStrategy implements BandChoosingStrategy {

    // @todo 3 nf/se - see ProductSubsetDialog for a similar declarations  (code smell!)
    private static final Font _SMALL_PLAIN_FONT = new Font("SansSerif", Font.PLAIN, 10);
    private static final Font _SMALL_ITALIC_FONT = _SMALL_PLAIN_FONT.deriveFont(Font.ITALIC);

    private JCheckBox _selectAllCheckBox;
    private JCheckBox _selectNoneCheckBox;
    private boolean _multipleProducts;
    private Product.AutoGrouping autoGrouping;
    private CheckBoxTree checkBoxTree;
    private final Map<String, Band> allBandsMap;
    private final Map<String, Band> selectedBandsMap;
    private final Map<String, TiePointGrid> allGridsMap;
    private final Map<String, TiePointGrid> selectedGridsMap;

    public GroupedBandChoosingStrategy(Band[] allBands, Band[] selectedBands, TiePointGrid[] allTiePointGrids,
                                       TiePointGrid[] selectedTiePointGrids, Product.AutoGrouping autoGrouping, boolean multipleProducts) {
        allBandsMap = createBandMap(allBands);
        selectedBandsMap = createBandMap(selectedBands);
        allGridsMap = createTiepointGridMap(allTiePointGrids);
        selectedGridsMap = createTiepointGridMap(selectedTiePointGrids);
        this.autoGrouping = autoGrouping;
        this._multipleProducts = multipleProducts;
    }

    private Map<String, Band> createBandMap(Band[] bands) {
        final Map<String, Band> bandMap = new TreeMap<String, Band>();
        if (bands != null) {
            for (Band band : bands) {
                bandMap.put(getDisplayDescription(band), band);
            }
        }
        return bandMap;
    }

    private Map<String, TiePointGrid> createTiepointGridMap(TiePointGrid[] grids) {
        final Map<String, TiePointGrid> gridMap = new TreeMap<String, TiePointGrid>();
        if (grids != null) {
            for (TiePointGrid grid : grids) {
                gridMap.put(getDisplayDescription(grid), grid);
            }
        }
        return gridMap;
    }

    private String getDisplayDescription(RasterDataNode rasterDataNode) {
        final String fullName = _multipleProducts ? rasterDataNode.getDisplayName() : rasterDataNode.getName();
        final StringBuilder description = new StringBuilder();
        description.setLength(0);
        description.append(fullName);
        description.append(rasterDataNode.getDescription() == null ? "" : " (" + rasterDataNode.getDescription());
        if (rasterDataNode instanceof Band) {
            if (((Band) rasterDataNode).getSpectralWavelength() > 0.0) {
                description.append(" (");
                description.append(((Band) rasterDataNode).getSpectralWavelength());
                description.append(" nm)");
            }
        }
        description.append(")");
        return description.toString();
    }

    @Override
    public Band[] getSelectedBands() {
        List<Band> selectedBandList = new ArrayList<Band>();
        final TreePath[] selectionPaths = checkBoxTree.getCheckBoxTreeSelectionModel().getSelectionPaths();
        TreePath rootPath = new TreePath(checkBoxTree.getModel().getRoot());
        for (TreePath selectionPath : selectionPaths) {
            if (selectionPath.equals(rootPath)) {
                return allBandsMap.values().toArray(new Band[allBandsMap.size()]);
            }
            final DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) selectionPath.getLastPathComponent();
            if (selectedNode.isLeaf()) {
                selectedBandList.add(allBandsMap.get(selectedNode.getUserObject().toString()));
            } else {
                for (int i = 0; i < selectedNode.getChildCount(); i++) {
                    final DefaultMutableTreeNode child = (DefaultMutableTreeNode) selectedNode.getChildAt(i);
                    selectedBandList.add(allBandsMap.get(child.getUserObject().toString()));
                }
            }
        }
        return selectedBandList.toArray(new Band[selectedBandList.size()]);
    }

    @Override
    public TiePointGrid[] getSelectedTiePointGrids() {
        List<TiePointGrid> selectedGridList = new ArrayList<TiePointGrid>();
        final TreePath[] selectionPaths = checkBoxTree.getCheckBoxTreeSelectionModel().getSelectionPaths();
        TreePath rootPath = new TreePath(checkBoxTree.getModel().getRoot());
        for (TreePath selectionPath : selectionPaths) {
            if (selectionPath.equals(rootPath)) {
                return allGridsMap.values().toArray(new TiePointGrid[allGridsMap.size()]);
            }
            final DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) selectionPath.getLastPathComponent();
            if (selectedNode.isLeaf()) {
                selectedGridList.add(allGridsMap.get(selectedNode.getUserObject().toString()));
            } else {
                for (int i = 0; i < selectedNode.getChildCount(); i++) {
                    final DefaultMutableTreeNode child = (DefaultMutableTreeNode) selectedNode.getChildAt(i);
                    selectedGridList.add(allGridsMap.get(child.getUserObject().toString()));
                }
            }
        }
        return selectedGridList.toArray(new TiePointGrid[selectedGridList.size()]);
    }

    public JPanel createCheckersPane() {
        final JPanel checkersPane = GridBagUtils.createPanel();
        DefaultMutableTreeNode root = new DefaultMutableTreeNode();
        TreeModel treeModel = new DefaultTreeModel(root);
        Map<String, Integer> groupNodeMap = initGrouping(root);
        List<TreePath> selectedPaths = new ArrayList<TreePath>();
        addBandCheckBoxes(root, selectedPaths, groupNodeMap);
        addTiePointGridCheckBoxes(root, selectedPaths, groupNodeMap);
        removeEmptyGroups(root, groupNodeMap);
        checkBoxTree = new CheckBoxTree(treeModel);
        checkBoxTree.getCheckBoxTreeSelectionModel().setSelectionPaths(selectedPaths.toArray(new TreePath[selectedPaths.size()]));
        checkBoxTree.setRootVisible(false);
        checkBoxTree.setShowsRootHandles(true);
        checkBoxTree.getCheckBoxTreeSelectionModel().addTreeSelectionListener(new TreeSelectionListener() {
            @Override
            public void valueChanged(TreeSelectionEvent e) {
                updateCheckBoxStates();
            }
        });
        final DefaultTreeCellRenderer renderer = (DefaultTreeCellRenderer) checkBoxTree.getActualCellRenderer();
        renderer.setFont(_SMALL_ITALIC_FONT);
        renderer.setLeafIcon(null);
        renderer.setOpenIcon(null);
        renderer.setClosedIcon(null);
        Color color = new Color(240, 240, 240);
        checkBoxTree.setBackground(color);
        renderer.setBackgroundSelectionColor(color);
        renderer.setBackgroundNonSelectionColor(color);
        GridBagConstraints gbc2 = GridBagUtils.createConstraints("insets.left=4,anchor=WEST,fill=BOTH");
        GridBagUtils.addToPanel(checkersPane, checkBoxTree, gbc2, "weightx=1.0,weighty=1.0");
        return checkersPane;
    }

    private Map<String, Integer> initGrouping(DefaultMutableTreeNode root) {
        Map<String, Integer> groupNodeMap = new HashMap<String, Integer>();
        if (autoGrouping != null) {
            final Iterator<String[]> iterator = autoGrouping.iterator();
            while (iterator.hasNext()) {
                final String groupName = iterator.next()[0];
                if (!hasChild(root, groupName)) {
                    DefaultMutableTreeNode groupNode = new DefaultMutableTreeNode(groupName);
                    groupNodeMap.put(groupNode.getUserObject().toString(), root.getChildCount());
                    root.add(groupNode);
                }
            }
        }
        return groupNodeMap;
    }

    private void removeEmptyGroups(DefaultMutableTreeNode root, Map<String, Integer> groupNodeMap) {
        DefaultMutableTreeNode rootChild = (DefaultMutableTreeNode) root.getFirstChild();
        while (rootChild != null) {
            DefaultMutableTreeNode nextChild = rootChild.getNextSibling();
            if (rootChild.getChildCount() == 0 && groupNodeMap.containsKey(rootChild.getUserObject().toString())) {
                root.remove(rootChild);
            }
            rootChild = nextChild;
        }
    }

    private void addBandCheckBoxes(DefaultMutableTreeNode root, List<TreePath> selectedPaths,
                                   Map<String, Integer> groupNodeMap) {
        final Set<Map.Entry<String, Band>> allBands = allBandsMap.entrySet();
        for (Map.Entry<String, Band> bandEntry : allBands) {
            final Band band = bandEntry.getValue();
            if (autoGrouping != null) {
                final int bandIndex = autoGrouping.indexOf(band.getName());
                if (bandIndex >= 0) {
                    final String groupName = autoGrouping.get(bandIndex)[0];
                    final Integer index = groupNodeMap.get(groupName);
                    final DefaultMutableTreeNode groupNode = (DefaultMutableTreeNode) root.getChildAt(index);
                    final DefaultMutableTreeNode groupChild = new DefaultMutableTreeNode(bandEntry.getKey());
                    if (selectedBandsMap.containsValue(band)) {
                        selectedPaths.add(new TreePath(new Object[]{root, groupNode, groupChild}));
                    }
                    groupNode.add(groupChild);
                } else {
                    addToRoot(root, selectedPaths, bandEntry, band);
                }
            } else {
                addToRoot(root, selectedPaths, bandEntry, band);
            }
        }
    }

    private void addTiePointGridCheckBoxes(DefaultMutableTreeNode root, List<TreePath> selectedPaths,
                                           Map<String, Integer> groupNodeMap) {
        final Set<Map.Entry<String, TiePointGrid>> allGrids = allGridsMap.entrySet();
        for (Map.Entry<String, TiePointGrid> gridEntry : allGrids) {
            final TiePointGrid grid = gridEntry.getValue();
            if (autoGrouping != null) {
                final int gridIndex = autoGrouping.indexOf(grid.getName());
                if (gridIndex >= 0) {
                    final String groupName = autoGrouping.get(gridIndex)[0];
                    final Integer index = groupNodeMap.get(groupName);

                    final DefaultMutableTreeNode groupNode = (DefaultMutableTreeNode) root.getChildAt(index);
                    final DefaultMutableTreeNode groupChild = new DefaultMutableTreeNode(gridEntry.getKey());
                    if (selectedGridsMap.containsValue(grid)) {
                        selectedPaths.add(new TreePath(new Object[]{root, groupNode, groupChild}));
                    }
                    groupNode.add(groupChild);
                } else {
                    addToRoot(root, selectedPaths, gridEntry, grid);
                }
            } else {
                addToRoot(root, selectedPaths, gridEntry, grid);
            }
        }
    }

    private void addToRoot(DefaultMutableTreeNode root, List<TreePath> selectedPaths, Map.Entry<String, Band> bandEntry, Band band) {
        final DefaultMutableTreeNode rootChild = new DefaultMutableTreeNode(bandEntry.getKey());
        if (selectedBandsMap.containsValue(band)) {
            selectedPaths.add(new TreePath(new Object[]{root, rootChild}));
        }
        root.add(rootChild);
    }

    private void addToRoot(DefaultMutableTreeNode root, List<TreePath> selectedPaths, Map.Entry<String, TiePointGrid> gridEntry, TiePointGrid grid) {
        final DefaultMutableTreeNode rootChild = new DefaultMutableTreeNode(gridEntry.getKey());
        if (selectedGridsMap.containsValue(grid)) {
            selectedPaths.add(new TreePath(new Object[]{root, rootChild}));
        }
        root.add(rootChild);
    }

    public void updateCheckBoxStates() {
        final TreePath[] selectionPaths = checkBoxTree.getCheckBoxTreeSelectionModel().getSelectionPaths();
        if (selectionPaths == null) {
            _selectAllCheckBox.setSelected(false);
            _selectAllCheckBox.setEnabled(true);
            _selectAllCheckBox.updateUI();
            _selectNoneCheckBox.setSelected(true);
            _selectNoneCheckBox.setEnabled(false);
            _selectNoneCheckBox.updateUI();
        } else {
            final TreePath rootPath = new TreePath(checkBoxTree.getModel().getRoot());
            boolean allSelected = false;
            for (TreePath selectionPath : selectionPaths) {
                if (selectionPath.equals(rootPath)) {
                    allSelected = true;
                }
                _selectAllCheckBox.setSelected(allSelected);
                _selectAllCheckBox.setEnabled(!allSelected);
                _selectAllCheckBox.updateUI();
                _selectNoneCheckBox.setSelected(false);
                _selectNoneCheckBox.setEnabled(true);
                _selectNoneCheckBox.updateUI();
            }
        }
    }

    private boolean hasChild(DefaultMutableTreeNode node, String groupName) {
        for (int i = 0; i < node.getChildCount(); i++) {
            if (((DefaultMutableTreeNode) node.getChildAt(i)).getUserObject().equals(groupName)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public void setCheckBoxes(JCheckBox selectAllCheckBox, JCheckBox selectNoneCheckBox) {
        this._selectAllCheckBox = selectAllCheckBox;
        this._selectNoneCheckBox = selectNoneCheckBox;
        updateCheckBoxStates();
    }

    @Override
    public void selectAll() {
        checkBoxTree.getCheckBoxTreeSelectionModel().setSelectionPath(new TreePath(checkBoxTree.getModel().getRoot()));
    }

    @Override
    public void selectNone() {
        checkBoxTree.getCheckBoxTreeSelectionModel().clearSelection();
    }

    @Override
    public boolean atLeastOneBandSelected() {
        return checkBoxTree.getCheckBoxTreeSelectionModel().getSelectionPaths() != null;
    }

}
