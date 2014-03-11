package org.esa.beam.framework.ui.product;

import com.jidesoft.swing.CheckBoxTree;
import javax.swing.tree.TreeNode;
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
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class GroupedRasterDataNodeChoosingStrategy implements RasterDataNodeChoosingStrategy {

    // @todo 3 nf/se - see ProductSubsetDialog for a similar declarations  (code smell!)
    private static final Font SMALL_PLAIN_FONT = new Font("SansSerif", Font.PLAIN, 10);
    private static final Font SMALL_ITALIC_FONT = SMALL_PLAIN_FONT.deriveFont(Font.ITALIC);

    private JCheckBox selectAllCheckBox;
    private JCheckBox selectNoneCheckBox;
    private boolean multipleProducts;
    private Product.AutoGrouping autoGrouping;
    private CheckBoxTree checkBoxTree;
    private final Map<Band, String> allBandsMap;
    private final Map<Band, String> selectedBandsMap;
    private final Map<TiePointGrid, String> allGridsMap;
    private final Map<TiePointGrid, String> selectedGridsMap;

    public GroupedRasterDataNodeChoosingStrategy(Band[] allBands, Band[] selectedBands, TiePointGrid[] allTiePointGrids,
                                                 TiePointGrid[] selectedTiePointGrids, Product.AutoGrouping autoGrouping, boolean multipleProducts) {
        allBandsMap = createBandMap(allBands);
        selectedBandsMap = createBandMap(selectedBands);
        allGridsMap = createTiepointGridMap(allTiePointGrids);
        selectedGridsMap = createTiepointGridMap(selectedTiePointGrids);
        this.autoGrouping = autoGrouping;
        this.multipleProducts = multipleProducts;
    }

    private Map<Band, String> createBandMap(Band[] bands) {
        final Map<Band, String> bandMap = new TreeMap<>(BandSorter.createComparator());
        if (bands != null) {
            for (Band band : bands) {
                bandMap.put(band, getDisplayDescription(band));
            }
        }
        return bandMap;
    }

    private Map<TiePointGrid, String> createTiepointGridMap(TiePointGrid[] grids) {
        final Map<TiePointGrid, String> gridMap = new TreeMap<>(new Comparator<TiePointGrid>() {
            @Override
            public int compare(TiePointGrid grid1, TiePointGrid grid2) {
                return grid1.getName().compareTo(grid2.getName());
            }
        });
        if (grids != null) {
            for (TiePointGrid grid : grids) {
                gridMap.put(grid, getDisplayDescription(grid));
            }
        }
        return gridMap;
    }

    private String getDisplayDescription(RasterDataNode rasterDataNode) {
        final String fullName = multipleProducts ? rasterDataNode.getDisplayName() : rasterDataNode.getName();
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
        List<Band> selectedBandList = new ArrayList<>();
        final TreePath[] selectionPaths = checkBoxTree.getCheckBoxTreeSelectionModel().getSelectionPaths();
        TreePath rootPath = new TreePath(checkBoxTree.getModel().getRoot());
        for (TreePath selectionPath : selectionPaths) {
            if (selectionPath.equals(rootPath)) {
                return allBandsMap.keySet().toArray(new Band[allBandsMap.size()]);
            }
            final DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) selectionPath.getLastPathComponent();
            if (selectedNode.isLeaf()) {
                RasterDataNode key = getKey(selectedNode.getUserObject().toString(), allBandsMap);
                if (key != null) {
                    selectedBandList.add((Band) key);
                }
            } else {
                for (int i = 0; i < selectedNode.getChildCount(); i++) {
                    final DefaultMutableTreeNode child = (DefaultMutableTreeNode) selectedNode.getChildAt(i);
                    RasterDataNode key = getKey(child.getUserObject().toString(), allBandsMap);
                    if (key != null) {
                        selectedBandList.add((Band) key);
                    }
                }
            }
        }
        return selectedBandList.toArray(new Band[selectedBandList.size()]);
    }

    private RasterDataNode getKey(String value, Map<? extends RasterDataNode, String> map) {
        for (Map.Entry<? extends RasterDataNode, String> entry : map.entrySet()) {
            if (entry.getValue().equals(value)) {
                return entry.getKey();
            }
        }
        return null;
    }

    @Override
    public TiePointGrid[] getSelectedTiePointGrids() {
        List<TiePointGrid> selectedGridList = new ArrayList<>();
        final TreePath[] selectionPaths = checkBoxTree.getCheckBoxTreeSelectionModel().getSelectionPaths();
        TreePath rootPath = new TreePath(checkBoxTree.getModel().getRoot());
        for (TreePath selectionPath : selectionPaths) {
            if (selectionPath.equals(rootPath)) {
                return allGridsMap.keySet().toArray(new TiePointGrid[allGridsMap.size()]);
            }
            final DefaultMutableTreeNode selectedNode = (DefaultMutableTreeNode) selectionPath.getLastPathComponent();
            if (selectedNode.isLeaf()) {
                RasterDataNode key = getKey(selectedNode.getUserObject().toString(), allGridsMap);
                if (key != null) {
                    selectedGridList.add((TiePointGrid) key);
                }
            } else {
                for (int i = 0; i < selectedNode.getChildCount(); i++) {
                    final DefaultMutableTreeNode child = (DefaultMutableTreeNode) selectedNode.getChildAt(i);
                    RasterDataNode key = getKey(child.getUserObject().toString(), allGridsMap);
                    if (key != null) {
                        selectedGridList.add((TiePointGrid) key);
                    }
                    selectedGridList.add((TiePointGrid) key);
                }
            }
        }
        return selectedGridList.toArray(new TiePointGrid[selectedGridList.size()]);
    }

    public JPanel createCheckersPane() {
        DefaultMutableTreeNode root = new DefaultMutableTreeNode();
        Map<String, Integer> groupNodeMap = initGrouping(root);
        List<TreePath> selectedPaths = new ArrayList<>();
        addBandCheckBoxes(root, selectedPaths, groupNodeMap);
        addTiePointGridCheckBoxes(root, selectedPaths, groupNodeMap);
        removeEmptyGroups(root, groupNodeMap);

        TreeModel treeModel = new DefaultTreeModel(root);

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
        renderer.setFont(SMALL_ITALIC_FONT);
        renderer.setLeafIcon(null);
        renderer.setOpenIcon(null);
        renderer.setClosedIcon(null);
        Color color = new Color(240, 240, 240);
        checkBoxTree.setBackground(color);
        renderer.setBackgroundSelectionColor(color);
        renderer.setBackgroundNonSelectionColor(color);
        renderer.setBorderSelectionColor(color);
        renderer.setTextSelectionColor(Color.BLACK);

        GridBagConstraints gbc2 = GridBagUtils.createConstraints("insets.left=4,anchor=WEST,fill=BOTH");
        final JPanel checkersPane = GridBagUtils.createPanel();
        GridBagUtils.addToPanel(checkersPane, checkBoxTree, gbc2, "weightx=1.0,weighty=1.0");
        return checkersPane;
    }

    private Map<String, Integer> initGrouping(DefaultMutableTreeNode root) {
        Map<String, Integer> groupNodeMap = new HashMap<>();
        if (autoGrouping != null) {
            for (String[] groupNames : autoGrouping) {
                final String groupName = groupNames[0];
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
        final Set<Map.Entry<Band, String>> allBands = allBandsMap.entrySet();
        for (Map.Entry<Band, String> bandEntry : allBands) {
            final Band band = bandEntry.getKey();
            if (autoGrouping != null) {
                final int bandIndex = autoGrouping.indexOf(band.getName());
                if (bandIndex >= 0) {
                    final String groupName = autoGrouping.get(bandIndex)[0];
                    final Integer index = groupNodeMap.get(groupName);
                    final DefaultMutableTreeNode groupNode = (DefaultMutableTreeNode) root.getChildAt(index);
                    final DefaultMutableTreeNode groupChild = new DefaultMutableTreeNode(bandEntry.getValue());
                    if (selectedBandsMap.containsValue(bandEntry.getValue())) {
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
        final Set<Map.Entry<TiePointGrid, String>> allGrids = allGridsMap.entrySet();
        for (Map.Entry<TiePointGrid, String> gridEntry : allGrids) {
            final TiePointGrid grid = gridEntry.getKey();
            if (autoGrouping != null) {
                final int gridIndex = autoGrouping.indexOf(grid.getName());
                if (gridIndex >= 0) {
                    final String groupName = autoGrouping.get(gridIndex)[0];
                    final Integer index = groupNodeMap.get(groupName);

                    final DefaultMutableTreeNode groupNode = (DefaultMutableTreeNode) root.getChildAt(index);
                    final DefaultMutableTreeNode groupChild = new DefaultMutableTreeNode(gridEntry.getValue());
                    if (selectedGridsMap.containsKey(grid)) {
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

    private void addToRoot(DefaultMutableTreeNode root, List<TreePath> selectedPaths, Map.Entry<Band, String> bandEntry, Band band) {
        final DefaultMutableTreeNode rootChild = new DefaultMutableTreeNode(bandEntry.getValue());
        if (selectedBandsMap.containsKey(band)) {
            selectedPaths.add(new TreePath(new Object[]{root, rootChild}));
        }
        root.add(rootChild);
    }

    private void addToRoot(DefaultMutableTreeNode root, List<TreePath> selectedPaths, Map.Entry<TiePointGrid, String> gridEntry, TiePointGrid grid) {
        final DefaultMutableTreeNode rootChild = new DefaultMutableTreeNode(gridEntry.getValue());
        if (selectedGridsMap.containsKey(grid)) {
            selectedPaths.add(new TreePath(new Object[]{root, rootChild}));
        }
        root.add(rootChild);
    }

    public void updateCheckBoxStates() {
        final TreePath[] selectionPaths = checkBoxTree.getCheckBoxTreeSelectionModel().getSelectionPaths();
        if (selectionPaths == null) {
            selectAllCheckBox.setSelected(false);
            selectAllCheckBox.setEnabled(true);
            selectAllCheckBox.updateUI();
            selectNoneCheckBox.setSelected(true);
            selectNoneCheckBox.setEnabled(false);
            selectNoneCheckBox.updateUI();
        } else {
            final TreePath rootPath = new TreePath(checkBoxTree.getModel().getRoot());
            boolean allSelected = false;
            for (TreePath selectionPath : selectionPaths) {
                if (selectionPath.equals(rootPath)) {
                    allSelected = true;
                }
                selectAllCheckBox.setSelected(allSelected);
                selectAllCheckBox.setEnabled(!allSelected);
                selectAllCheckBox.updateUI();
                selectNoneCheckBox.setSelected(false);
                selectNoneCheckBox.setEnabled(true);
                selectNoneCheckBox.updateUI();
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
        this.selectAllCheckBox = selectAllCheckBox;
        this.selectNoneCheckBox = selectNoneCheckBox;
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

    @Override
    public void selectRasterDataNodes(String[] nodeNames) {
        DefaultMutableTreeNode rootNode = (DefaultMutableTreeNode) checkBoxTree.getModel().getRoot();
        selectRasterDataNodes(rootNode, nodeNames);
    }

    private void selectRasterDataNodes(DefaultMutableTreeNode node, String[] nodeNames) {
        int childCount = node.getChildCount();
        if(childCount != 0) {
            for(int i = 0; i < childCount; i++) {
                selectRasterDataNodes((DefaultMutableTreeNode)node.getChildAt(i), nodeNames);
            }
        } else {
            for (String nodeName : nodeNames) {
                if (nodeName.equals(((String) node.getUserObject()).split(" ")[0].trim())) {
                    List<TreeNode> pathList = new ArrayList<>();
                    TreeNode currentNode = node;
                    while(currentNode != null) {
                        pathList.add(0, currentNode);
                        currentNode = currentNode.getParent();
                    }
                    TreePath path = new TreePath(pathList.toArray(new TreeNode[pathList.size()]));
                    checkBoxTree.getCheckBoxTreeSelectionModel().addSelectionPath(path);
                }
            }
        }
    }

}
