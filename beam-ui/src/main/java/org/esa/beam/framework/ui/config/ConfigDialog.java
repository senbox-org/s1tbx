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
package org.esa.beam.framework.ui.config;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Window;

import javax.swing.BorderFactory;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.ToolTipManager;
import javax.swing.border.BevelBorder;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeModel;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

import org.esa.beam.framework.param.ParamExceptionHandler;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.util.PropertyMap;

/**
 * Instances of the <code>ConfigDialog</code> class are modal dialogs composed of one or more, possibly nested
 * configuration pages of the type <code>ConfigPage</code>. <code>ConfigDialog</code> uses a tree view to let the user
 * switch between particular pages.
 *
 * @author Norman Fomferra
 * @author Sabine Embacher
 * @version $Revision$  $Date$
 * @see ConfigPage
 */
public class ConfigDialog extends ModalDialog {

    private static final String _DEFAULT_TITLE_BASE = "Configuration"; /*I18N*/

    private String _titleBase;
    private JTree _tree;
    private DefaultMutableTreeNode _rootNode;
    private DefaultTreeModel _treeModel;
    private JPanel _pagePane;
    private CardLayout _pagePaneLM;
    private ConfigPage _currentPage;

    public ConfigDialog() {
        this(null, null);
    }

    public ConfigDialog(Window owner, String helpID) {
        super(owner, _DEFAULT_TITLE_BASE, ModalDialog.ID_OK_CANCEL | (helpID != null ? ModalDialog.ID_HELP : 0),
              helpID);
        createUI();
    }

    public PropertyMap getConfigParamValues(PropertyMap propertyMap) {
        for (int i = 0; i < getNumRootPages(); i++) {
            ConfigPage page = getRootPageAt(i);
            propertyMap = getConfigParamValues(page, propertyMap);
        }
        return propertyMap;
    }

    public void setConfigParamValues(PropertyMap propertyMap, ParamExceptionHandler errorHandler) {
        for (int i = 0; i < getNumRootPages(); i++) {
            ConfigPage page = getRootPageAt(i);
            setConfigParamValues(page, propertyMap, errorHandler);
        }
    }

    public void expandAllPages() {
        final TreeModel model = _tree.getModel();
        final Object root = model.getRoot();
        final TreePath treePath = new TreePath(root);
        _tree.expandPath(treePath);
        final int rowCount = _tree.getRowCount();
        for (int i = rowCount-1; i >= 0; --i) {
            _tree.expandRow(i);
        }
    }

    public String getTitleBase() {
        return _titleBase;
    }

    public void setTitleBase(String titleBase) {
        _titleBase = titleBase;
    }

    public int getNumRootPages() {
        return _rootNode.getChildCount();
    }

    public ConfigPage getRootPageAt(int index) {
        return valueToPage(_rootNode.getChildAt(index));
    }

    public void addRootPage(ConfigPage page) {
        DefaultMutableTreeNode node = createNode(page);
        _rootNode.add(node);
        _treeModel.reload();
        if (getNumRootPages() == 1) {
            setCurrentPage(page);
        }
    }

    public void removeRootPage(ConfigPage page) {
        for (int i = 0; i < getNumRootPages(); i++) {
            ConfigPage pageOld = getRootPageAt(i);
            if (pageOld == page) {
                removeRootPageAt(i);
                return;
            }
        }
    }

    public void removeRootPageAt(int index) {
        _rootNode.remove(index);
        if (getNumRootPages() > 0) {
            setCurrentPage(getRootPageAt(0));
        } else {
            setCurrentPage(null);
        }
    }

    public ConfigPage getCurrentPage() {
        return _currentPage;
    }

    public void setCurrentPage(ConfigPage currentPage) {
        if (_currentPage == currentPage || !verifyUserInput()) {
            return;
        }
        _currentPage = currentPage;
        updatePageTitle();
        updatePagePane();
    }

    @Override
    protected void onOK() {
        for (int i = 0; i < getNumRootPages(); i++) {
            ConfigPage page = getRootPageAt(i);
            onOK(page);
        }
        super.onOK();
    }

    @Override
    public int show() {
        for (int i = 0; i < getNumRootPages(); i++) {
            ConfigPage page = getRootPageAt(i);
            updatePageUI(page);
        }
        return super.show();
    }

    @Override
    protected boolean verifyUserInput() {
        if (_currentPage != null) {
            return _currentPage.verifyUserInput();
        }
        return true;
    }

    private DefaultMutableTreeNode createNode(ConfigPage page) {
        installPageUIComponent(page);
        DefaultMutableTreeNode node = new DefaultMutableTreeNode(page);
        ConfigPage[] subPages = page.getSubPages();
        if (subPages != null) {
            for (int i = 0; i < subPages.length; i++) {
                node.add(createNode(subPages[i]));
            }
        }
        return node;
    }

    private void installPageUIComponent(ConfigPage page) {
        Component component = page.getPageUI();
        if (component == null) {
            throw new IllegalArgumentException("page without UI component added");
        }
        _pagePane.add(component, page.getKey());
    }

    private void registerTreeCellRenderer() {
        _tree.setCellRenderer(new DefaultTreeCellRenderer() {

            @Override
            public Component getTreeCellRendererComponent(JTree tree,
                                                          Object value,
                                                          boolean sel,
                                                          boolean expanded,
                                                          boolean leaf,
                                                          int row,
                                                          boolean hasFocus) {
                super.getTreeCellRendererComponent(tree, value, sel, expanded, leaf, row, hasFocus);
                ConfigPage page = valueToPage(value);
                if (page != null) {
                    setText(page.getTitle());
                    setIcon(page.getIcon());
                    setToolTipText(page.getTitle());
                }
                return this;
            }
        });
    }

    private void registerTreeSelectionListener() {
        _tree.addTreeSelectionListener(new TreeSelectionListener() {

            public void valueChanged(TreeSelectionEvent e) {
                ConfigPage page = valueToPage(_tree.getLastSelectedPathComponent());
                if (page != null) {
                    setCurrentPage(page);
                }
            }

        });
    }

    private ConfigPage valueToPage(Object value) {
        return nodeToPage(valueToNode(value));
    }

    private DefaultMutableTreeNode valueToNode(Object value) {
        if (value instanceof DefaultMutableTreeNode) {
            return (DefaultMutableTreeNode) value;
        }
        return null;
    }

    private ConfigPage nodeToPage(DefaultMutableTreeNode node) {
        if (node != null && node.getUserObject() instanceof ConfigPage) {
            return (ConfigPage) node.getUserObject();
        }
        return null;
    }

    private void createUI() {

        _titleBase = _DEFAULT_TITLE_BASE; /*I18N*/

        //getJDialog().setResizable(false);

        _rootNode = new DefaultMutableTreeNode(_titleBase);
        _treeModel = new DefaultTreeModel(_rootNode);
        _tree = new JTree(_treeModel);
        _tree.setRootVisible(false);
        _tree.setShowsRootHandles(true);
        _tree.setEditable(false);
        _tree.putClientProperty("JTree.lineStyle", "Angled");
        _tree.getSelectionModel().setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

        registerTreeCellRenderer();
        registerTreeSelectionListener();

        //Enable tool tips for the tree.
        ToolTipManager.sharedInstance().registerComponent(_tree);

        final JScrollPane treeScrollPane = new JScrollPane(_tree);
        treeScrollPane.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
        treeScrollPane.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
        treeScrollPane.setPreferredSize(new Dimension(180, 280));

        _pagePaneLM = new CardLayout();
        _pagePane = new JPanel(_pagePaneLM);

        final JPanel content = new JPanel(new BorderLayout(3, 3));
        content.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createBevelBorder(BevelBorder.RAISED),
                                                             BorderFactory.createEmptyBorder(3, 3, 3, 3)));

        content.add(treeScrollPane, BorderLayout.WEST);
        content.add(_pagePane);

        setContent(content);
    }

    private void updatePageTitle() {
        String title = getTitleBase();
        if (_currentPage != null) {
            title = title.concat(" - ".concat(_currentPage.getTitle()));
        }
        getJDialog().setTitle(title);
    }

    private void updatePagePane() {
        if (_currentPage != null) {
            _pagePaneLM.show(_pagePane, _currentPage.getKey());
        }
    }

    private PropertyMap getConfigParamValues(ConfigPage page, PropertyMap propertyMap) {
        propertyMap = page.getConfigParamValues(propertyMap);
        ConfigPage[] subPages = page.getSubPages();
        if (subPages != null) {
            for (int i = 0; i < subPages.length; i++) {
                ConfigPage subPage = subPages[i];
                propertyMap = getConfigParamValues(subPage, propertyMap);
            }
        }
        return propertyMap;
    }

    private void setConfigParamValues(ConfigPage page, PropertyMap propertyMap, ParamExceptionHandler errorHandler) {
        page.setConfigParamValues(propertyMap, errorHandler);
        ConfigPage[] subPages = page.getSubPages();
        if (subPages != null) {
            for (int i = 0; i < subPages.length; i++) {
                ConfigPage subPage = subPages[i];
                setConfigParamValues(subPage, propertyMap, errorHandler);
            }
        }
    }

    private void onOK(ConfigPage page) {
        ConfigPage[] subPages = page.getSubPages();
        if (subPages != null) {
            for (int i = 0; i < subPages.length; i++) {
                onOK(subPages[i]);
            }
        }
        page.onOK();
    }

    private void updatePageUI(ConfigPage page) {
        ConfigPage[] subPages = page.getSubPages();
        if (subPages != null) {
            for (int i = 0; i < subPages.length; i++) {
                updatePageUI(subPages[i]);
            }
        }
        page.updatePageUI();
    }
}
