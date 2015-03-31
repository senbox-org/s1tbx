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
package org.esa.nest.dat.toolviews.productlibrary;

import org.esa.beam.framework.help.HelpSys;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.application.support.AbstractToolView;
import org.esa.beam.framework.ui.tool.ToolButtonFactory;
import org.esa.beam.visat.VisatApp;
import org.esa.nest.dat.toolviews.productlibrary.model.*;
import org.esa.nest.dat.toolviews.productlibrary.timeline.TimelinePanel;
import org.esa.snap.dat.dialogs.CheckListDialog;
import org.esa.snap.db.DBQuery;
import org.esa.snap.db.ProductEntry;
import org.esa.snap.rcp.SnapApp;
import org.esa.snap.rcp.SnapDialogs;
import org.esa.snap.util.DialogUtils;
import org.esa.snap.util.FileFolderUtils;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.HashMap;
import java.util.Map;

public class ProductLibraryToolView extends AbstractToolView implements LabelBarProgressMonitor.ProgressBarListener,
        DatabaseQueryListener, ProductLibraryActions.ProductLibraryActionListener {

    private static final ImageIcon updateIcon = UIUtils.loadImageIcon("/org/esa/nest/icons/refresh24.png", ProductLibraryToolView.class);
    private static final ImageIcon updateRolloverIcon = ToolButtonFactory.createRolloverIcon(updateIcon);
    private static final ImageIcon stopIcon = UIUtils.loadImageIcon("icons/Stop24.gif");
    private static final ImageIcon stopRolloverIcon = ToolButtonFactory.createRolloverIcon(stopIcon);
    private static final ImageIcon addButtonIcon = UIUtils.loadImageIcon("icons/Plus24.gif");
    private static final ImageIcon removeButtonIcon = UIUtils.loadImageIcon("icons/Minus24.gif");
    private static final ImageIcon helpButtonIcon = UIUtils.loadImageIcon("icons/Help24.gif");

    private JPanel mainPanel;
    private JComboBox repositoryListCombo;
    private JTable productEntryTable  = new JTable();

    private JLabel statusLabel;
    private JPanel progressPanel;
    private JButton addToProjectButton;
    private JButton addButton;
    private JButton removeButton;
    private JButton updateButton;

    private LabelBarProgressMonitor progMon;
    private JProgressBar progressBar;
    private ProductLibraryConfig libConfig;
    private static final String helpId = "productLibrary";

    private WorldMapUI worldMapUI = null;
    private DatabasePane dbPane;
    private ProductLibraryActions productLibraryActions;

    public ProductLibraryToolView() {
    }

    @Override
    public void componentOpened() {
        dbPane.getDB();
    }

    public JComponent createControl() {

        libConfig = new ProductLibraryConfig(SnapApp.getDefault().getCompatiblePreferences());

        dbPane = new DatabasePane();
        dbPane.addListener(this);

        productLibraryActions = new ProductLibraryActions(productEntryTable, this);
        productLibraryActions.addListener(this);

        initUI();

        mainPanel.addComponentListener(new ComponentAdapter() {

            @Override
            public void componentHidden(final ComponentEvent e) {
                if (progMon != null) {
                    progMon.setCanceled(true);
                }
            }
        });
        applyConfig(libConfig);
        mainPanel.addComponentListener(new ComponentAdapter() {

            @Override
            public void componentMoved(final ComponentEvent e) {
                libConfig.setWindowBounds(e.getComponent().getBounds());
            }

            @Override
            public void componentResized(final ComponentEvent e) {
                libConfig.setWindowBounds(e.getComponent().getBounds());
            }
        });
        setUIComponentsEnabled(repositoryListCombo.getItemCount() > 1);

        return mainPanel;
    }

    private void initUI() {

        final JPanel northPanel = createHeaderPanel();
        final JPanel centrePanel = createCentrePanel();
        final JPanel southPanel = createStatusPanel();

        final DatabaseStatistics stats = new DatabaseStatistics(dbPane);
        final TimelinePanel timeLinePanel = new TimelinePanel(stats);
        dbPane.addListener(timeLinePanel);
        final JSplitPane splitPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT,
                centrePanel, timeLinePanel);
        splitPane.setOneTouchExpandable(true);
        splitPane.setResizeWeight(0.99);

        mainPanel = new JPanel(new BorderLayout(4, 4));
        mainPanel.add(northPanel, BorderLayout.NORTH);
        mainPanel.add(splitPane, BorderLayout.CENTER);
        mainPanel.add(southPanel, BorderLayout.SOUTH);
        mainPanel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
    }

    private JPanel createHeaderPanel() {
        final JPanel headerBar = new JPanel();
        headerBar.setLayout(new GridBagLayout());
        final GridBagConstraints gbc = new GridBagConstraints();
        gbc.fill = GridBagConstraints.HORIZONTAL;
        gbc.anchor = GridBagConstraints.NORTHWEST;

        updateButton = DialogUtils.createButton("updateButton", "Rescan folder", updateIcon, headerBar, DialogUtils.ButtonStyle.Icon);
        updateButton.setActionCommand(LabelBarProgressMonitor.updateCommand);
        updateButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                if (e.getActionCommand().equals("stop")) {
                    updateButton.setEnabled(false);
                    mainPanel.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
                    if(progMon != null) {
                        progMon.setCanceled(true);
                    }
                } else {
                    final RescanOptions dlg = new RescanOptions();
                    dlg.show();
                    rescanFolder(dlg.shouldDoRecusive(), dlg.shouldDoQuicklooks());
                }
            }
        });
        headerBar.add(updateButton, gbc);

        headerBar.add(new JLabel("Folder:")); /* I18N */
        gbc.weightx = 99;
        repositoryListCombo = new JComboBox();
        repositoryListCombo.setName(getClass().getName() + "repositoryListCombo");
        repositoryListCombo.addItemListener(new ItemListener() {
            public void itemStateChanged(ItemEvent event) {
                if (event.getStateChange() == ItemEvent.SELECTED) {
                    final Object selectedItem = repositoryListCombo.getSelectedItem();
                    if (selectedItem instanceof File) {
                        dbPane.setBaseDir((File) selectedItem);
                    } else {
                        dbPane.setBaseDir(null);
                    }
                }
            }
        });
        headerBar.add(repositoryListCombo, gbc);
        gbc.weightx = 0;

        addButton = DialogUtils.createButton("addButton", "Add folder", addButtonIcon, headerBar, DialogUtils.ButtonStyle.Icon);
        addButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                addRepository();
            }
        });
        headerBar.add(addButton, gbc);

        removeButton = DialogUtils.createButton("removeButton", "Remove folder", removeButtonIcon, headerBar, DialogUtils.ButtonStyle.Icon);
        removeButton.addActionListener(new ActionListener() {

            public void actionPerformed(final ActionEvent e) {
                removeRepository();
            }
        });
        headerBar.add(removeButton, gbc);

        final JButton helpButton = DialogUtils.createButton("helpButton", "Help", helpButtonIcon, headerBar, DialogUtils.ButtonStyle.Icon);
        HelpSys.enableHelpOnButton(helpButton, helpId);
        headerBar.add(helpButton, gbc);

        return headerBar;
    }

    private JPanel createStatusPanel() {
       /* addToProjectButton = new JButton();
        setComponentName(addToProjectButton, "addToProject");
        addToProjectButton.setText("Import to Project");
        addToProjectButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                Project.instance().ImportFileList(productLibraryActions.getSelectedFiles());
            }
        });

        final JPanel openPanel = new JPanel(new BorderLayout(4, 4));
        openPanel.add(addToProjectButton, BorderLayout.WEST);*/

        final JPanel southPanel = new JPanel(new BorderLayout(4, 4));
        statusLabel = new JLabel("");
        statusLabel.setMinimumSize(new Dimension(100, 10));
        southPanel.add(statusLabel, BorderLayout.CENTER);
        //southPanel.add(openPanel, BorderLayout.WEST);

        progressBar = new JProgressBar();
        progressBar.setName(getClass().getName() + "progressBar");
        progressBar.setStringPainted(true);
        progressPanel = new JPanel();
        progressPanel.setLayout(new BorderLayout());
        progressPanel.add(progressBar, BorderLayout.CENTER);
        progressPanel.setVisible(false);
        southPanel.add(progressPanel, BorderLayout.EAST);

        return southPanel;
    }

    private JPanel createCentrePanel() {

        final JPanel leftPanel = new JPanel(new BorderLayout());
        leftPanel.setMinimumSize(new Dimension(200, 577));
        leftPanel.add(dbPane, BorderLayout.NORTH);

        productEntryTable.setAutoResizeMode(JTable.AUTO_RESIZE_LAST_COLUMN);
        productEntryTable.setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
        productEntryTable.setComponentPopupMenu(productLibraryActions.createEntryTablePopup());
        productEntryTable.addMouseListener(new MouseAdapter() {

            @Override
            public void mouseClicked(final MouseEvent e) {
                final int clickCount = e.getClickCount();
                if (clickCount == 2) {
                    productLibraryActions.performOpenAction();
                } else if (clickCount == 1) {
                    notifySelectionChanged();
                }
            }
        });

        final JPanel commandPanel = productLibraryActions.createCommandPanel();

        final JScrollPane tablePane = new JScrollPane(productEntryTable);
        tablePane.setMinimumSize(new Dimension(400, 400));

        worldMapUI = new WorldMapUI();
        worldMapUI.addListener(this);

        final JSplitPane splitPaneV = new JSplitPane(JSplitPane.VERTICAL_SPLIT, tablePane, worldMapUI.getWorlMapPane());
        splitPaneV.setOneTouchExpandable(true);
        splitPaneV.setResizeWeight(0.8);

        final JPanel centrePanel = new JPanel(new BorderLayout());
        centrePanel.add(leftPanel, BorderLayout.WEST);
        centrePanel.add(splitPaneV, BorderLayout.CENTER);
        centrePanel.add(commandPanel, BorderLayout.EAST);

        return centrePanel;
    }

    private void applyConfig(final ProductLibraryConfig config) {
        final File[] baseDirList = config.getBaseDirs();
        repositoryListCombo.insertItemAt(DBQuery.ALL_FOLDERS, 0);
        for (File f : baseDirList) {
            repositoryListCombo.insertItemAt(f, repositoryListCombo.getItemCount());
        }
        if (baseDirList.length > 0)
            repositoryListCombo.setSelectedIndex(0);
    }

    private void addRepository() {
        final File baseDir = productLibraryActions.promptForRepositoryBaseDir();
        if (baseDir == null) {
            return;
        }

        final Map<String, Boolean> checkBoxMap = new HashMap<>(3);
        checkBoxMap.put("Generate quicklooks?", false);
        checkBoxMap.put("Search folder recursively?", true);

        final RescanOptions dlg = new RescanOptions();
        dlg.show();

        libConfig.addBaseDir(baseDir);
        final int index = repositoryListCombo.getItemCount();
        repositoryListCombo.insertItemAt(baseDir, index);
        setUIComponentsEnabled(repositoryListCombo.getItemCount() > 1);

        updateRepostitory(baseDir, dlg.shouldDoRecusive(), dlg.shouldDoQuicklooks());
    }

    LabelBarProgressMonitor createLabelBarProgressMonitor() {
        progMon = new LabelBarProgressMonitor(progressBar, statusLabel);
        progMon.addListener(this);
        return progMon;
    }

    private synchronized void updateRepostitory(final File baseDir, final boolean doRecursive, final boolean doQuicklooks) {
        if (baseDir == null) return;
        progMon = createLabelBarProgressMonitor();
        final DBScanner scanner = new DBScanner(dbPane.getDB(), baseDir, doRecursive, doQuicklooks, progMon);
        scanner.addListener(new MyDatabaseScannerListener());
        scanner.execute();
    }

    private synchronized void removeProducts(final File baseDir) {
        progMon = createLabelBarProgressMonitor();
        final DBRemover remover = new DBRemover(dbPane.getDB(), baseDir, progMon);
        remover.addListener(new MyDatabaseRemoverListener());
        remover.execute();
    }

    private void removeRepository() {

        final Object selectedItem = repositoryListCombo.getSelectedItem();
        final int index = repositoryListCombo.getSelectedIndex();
        if (index == 0) {
            final int status = VisatApp.getApp().showQuestionDialog("This will remove all folders and products from the database.\n" +
                    "Are you sure you wish to continue?", null);
            if (status == JOptionPane.NO_OPTION)
                return;
            while (repositoryListCombo.getItemCount() > 1) {
                final File baseDir = (File) repositoryListCombo.getItemAt(1);
                libConfig.removeBaseDir(baseDir);
                repositoryListCombo.removeItemAt(1);
            }
            removeProducts(null); // remove all

        } else if (selectedItem instanceof File) {
            final File baseDir = (File) selectedItem;
            final int status = VisatApp.getApp().showQuestionDialog("This will remove all products within " +
                    baseDir.getAbsolutePath() + " from the database\n" +
                    "Are you sure you wish to continue?", null);
            if (status == JOptionPane.NO_OPTION)
                return;
            libConfig.removeBaseDir(baseDir);
            repositoryListCombo.removeItemAt(index);
            removeProducts(baseDir);
        }
    }

    private void setUIComponentsEnabled(final boolean enable) {
        removeButton.setEnabled(enable);
        updateButton.setEnabled(enable);
        repositoryListCombo.setEnabled(enable);
    }

    private void toggleUpdateButton(final String command) {
        if (command.equals(LabelBarProgressMonitor.stopCommand)) {
            updateButton.setIcon(stopIcon);
            updateButton.setRolloverIcon(stopRolloverIcon);
            updateButton.setActionCommand(LabelBarProgressMonitor.stopCommand);
            addButton.setEnabled(false);
            removeButton.setEnabled(false);
        } else {
            updateButton.setIcon(updateIcon);
            updateButton.setRolloverIcon(updateRolloverIcon);
            updateButton.setActionCommand(LabelBarProgressMonitor.updateCommand);
            addButton.setEnabled(true);
            removeButton.setEnabled(true);
        }
    }

    public void UpdateUI() {
        dbPane.refresh();
        productEntryTable.updateUI();
    }

    public void findSlices(int dataTakeId) {
        dbPane.findSlices(dataTakeId);
    }

    private void rescanFolder(final boolean doRecursive, final boolean doQuicklooks) {
        if (repositoryListCombo.getSelectedIndex() != 0) {
            updateRepostitory((File) repositoryListCombo.getSelectedItem(), doRecursive, doQuicklooks);
        } else {
            final File[] baseDirList = libConfig.getBaseDirs();
            for (File f : baseDirList) {
                updateRepostitory(f, doRecursive, doQuicklooks);
            }
        }
    }

    private void updateStatusLabel() {
        String selectedText = "";
        final int selecteRows = productEntryTable.getSelectedRowCount();

        productLibraryActions.selectionEnabled(selecteRows > 0);
        if (selecteRows > 0)
            selectedText = ", " + selecteRows + " Selected";
        else
            dbPane.updateProductSelectionText(null);
        statusLabel.setText(productEntryTable.getRowCount() + " Products" + selectedText);
    }

    public void ShowRepository(final ProductEntry[] productEntryList) {
        final ProductEntryTableModel tableModel = new ProductEntryTableModel(productEntryList);
        productEntryTable.setModel(new SortingDecorator(tableModel, productEntryTable.getTableHeader()));
        productEntryTable.setColumnModel(tableModel.getColumnModel());
        updateStatusLabel();
        worldMapUI.setProductEntryList(productEntryList);
        worldMapUI.setSelectedProductEntryList(null);
    }

    public static void handleErrorList(final java.util.List<DBScanner.ErrorFile> errorList) {
        final StringBuilder str = new StringBuilder();
        int cnt = 1;
        for (DBScanner.ErrorFile err : errorList) {
            str.append(err.message);
            str.append("   ");
            str.append(err.file.getAbsolutePath());
            str.append('\n');
            if (cnt >= 20) {
                str.append("plus " + (errorList.size() - 20) + " other errors...\n");
                break;
            }
            ++cnt;
        }
        final String question = "\nWould you like to save the list to a text file?";
        if (VisatApp.getApp().showQuestionDialog("Product Errors",
                "The follow files have errors:\n" + str.toString() + question,
                null) == 0) {

            File file = FileFolderUtils.GetSaveFilePath("Save as...", "Text", "txt",
                                                        "ProductErrorList", "Products with errors");
            try {
                writeErrors(errorList, file);
            } catch (Exception e) {
                SnapDialogs.showError("Unable to save to " + file.getAbsolutePath());
                file = FileFolderUtils.GetSaveFilePath("Save as...", "Text", "txt",
                        "ProductErrorList", "Products with errors");
                try {
                    writeErrors(errorList, file);
                } catch (Exception ignore) {
                    //
                }
            }
            if (Desktop.isDesktopSupported() && file.exists()) {
                try {
                    Desktop.getDesktop().open(file);
                } catch (Exception e) {
                    // do nothing
                }
            }
        }
    }

    private static void writeErrors(final java.util.List<DBScanner.ErrorFile> errorList, final File file) throws Exception {
        if (file == null) return;

        PrintStream p = null; // declare a print stream object
        try {
            final FileOutputStream out = new FileOutputStream(file.getAbsolutePath());
            // Connect print stream to the output stream
            p = new PrintStream(out);

            for (DBScanner.ErrorFile err : errorList) {
                p.println(err.message + "   " + err.file.getAbsolutePath());
            }
        } finally {
            if (p != null)
                p.close();
        }
    }

    public void notifyStart() {
        progressPanel.setVisible(true);
        toggleUpdateButton(LabelBarProgressMonitor.stopCommand);
    }

    public void notifyDone() {
        progressPanel.setVisible(false);
        toggleUpdateButton(LabelBarProgressMonitor.updateCommand);
        updateButton.setEnabled(true);
        mainPanel.setCursor(Cursor.getDefaultCursor());
    }

    public void notifyNewEntryListAvailable() {
        ShowRepository(dbPane.getProductEntryList());
    }

    public void notifyNewMapSelectionAvailable() {
        dbPane.setSelectionRect(worldMapUI.getSelectionBox());
    }

    public void notifyDirectoryChanged() {
        rescanFolder(true, false);
        UpdateUI();
    }

    public void notifySelectionChanged() {
        updateStatusLabel();
        final ProductEntry[] selections = productLibraryActions.getSelectedProductEntries();
        productLibraryActions.selectionEnabled(selections.length > 0);

        productLibraryActions.updateContextMenu(selections);
        dbPane.updateProductSelectionText(selections);
        worldMapUI.setSelectedProductEntryList(selections);
    }

    private class MyDatabaseScannerListener implements DBScanner.DBScannerListener {

        public void notifyMSG(final DBScanner dbScanner, final MSG msg) {
            if (msg.equals(DBScanner.DBScannerListener.MSG.DONE)) {
                final java.util.List<DBScanner.ErrorFile> errorList = dbScanner.getErrorList();
                if (!errorList.isEmpty()) {
                    handleErrorList(errorList);
                }
            }
            UpdateUI();
        }
    }

    private class MyDatabaseRemoverListener implements DBRemover.DBRemoverListener {

        public void notifyMSG(final MSG msg) {
            if (msg.equals(DBRemover.DBRemoverListener.MSG.DONE)) {
                setUIComponentsEnabled(repositoryListCombo.getItemCount() > 1);
                UpdateUI();
            }
        }
    }

    private class RescanOptions extends CheckListDialog {
        RescanOptions() {
            super("Scan Folder Options");
        }

        @Override
        protected void initContent() {
            items.put("Generate quicklooks?", false);
            items.put("Search folder recursively?", true);

            super.initContent();
        }

        public boolean shouldDoRecusive() {
            return items.get("Search folder recursively?");
        }

        public boolean shouldDoQuicklooks() {
            return items.get("Generate quicklooks?");
        }
    }
}