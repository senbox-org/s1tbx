package org.esa.nest.dat.toolviews.productlibrary;

import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.util.io.BeamFileChooser;
import org.esa.beam.visat.VisatApp;
import org.esa.nest.dat.toolviews.productlibrary.model.SortingDecorator;
import org.esa.nest.dat.utils.ProductOpener;
import org.esa.snap.dat.dialogs.BatchGraphDialog;
import org.esa.snap.db.ProductEntry;
import org.esa.snap.util.ClipboardUtils;
import org.esa.snap.util.DialogUtils;
import org.esa.snap.util.ResourceUtils;
import org.jlinda.nest.dat.dialogs.InSARMasterDialog;


import javax.swing.*;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.TableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.ArrayList;

/**
 * actions on product entry selections
 */
public class ProductLibraryActions {

    private static final ImageIcon selectAllIcon = UIUtils.loadImageIcon("/org/esa/nest/icons/select-all24.png", ProductLibraryToolView.class);
    private static final ImageIcon openIcon = UIUtils.loadImageIcon("/org/esa/nest/icons/open24.png", ProductLibraryToolView.class);
    private static final ImageIcon copyIcon = UIUtils.loadImageIcon("/org/esa/nest/icons/copy24.png", ProductLibraryToolView.class);
    private static final ImageIcon batchIcon = UIUtils.loadImageIcon("/org/esa/nest/icons/batch24.png", ProductLibraryToolView.class);
    private static final ImageIcon stackIcon = UIUtils.loadImageIcon("/org/esa/nest/icons/stack24.png", ProductLibraryToolView.class);

    private final JTable productEntryTable;
    private final ProductOpener openHandler;
    private JButton selectAllButton, openAllSelectedButton, copySelectedButton, batchProcessButton, stackButton;

    private JMenuItem copyToItem,  moveToItem, deleteItem;

    private File currentDirectory;
    private final java.util.List<ProductLibraryActionListener> listenerList = new ArrayList<>(1);

    public ProductLibraryActions(final JTable productEntryTable) {
        this.productEntryTable = productEntryTable;
        this.openHandler = new ProductOpener(VisatApp.getApp());
    }

    public JPanel createCommandPanel() {
        final JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.Y_AXIS));

        selectAllButton = DialogUtils.createButton("selectAllButton", "Select all", selectAllIcon, panel, false);
        selectAllButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                performSelectAllAction();
            }
        });

        openAllSelectedButton = DialogUtils.createButton("openAllSelectedButton", "Open selected", openIcon, panel, false);
        openAllSelectedButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                performOpenAction();
            }
        });

        copySelectedButton = DialogUtils.createButton("copySelectedButton", "Copy to clipboard", copyIcon, panel, false);
        copySelectedButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                performCopyAction();
            }
        });

        batchProcessButton = DialogUtils.createButton("batchProcessButton", "Batch", batchIcon, panel, false);
        batchProcessButton.setToolTipText("Right click to select a graph");
        batchProcessButton.setComponentPopupMenu(createGraphPopup());
        batchProcessButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                batchProcess(getSelectedProductEntries(), null);
            }
        });

        stackButton = DialogUtils.createButton("stackButton", "Stack overview", stackIcon, panel, false);
        stackButton.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                performStackOverviewAction();
            }
        });

        panel.add(selectAllButton);
        panel.add(openAllSelectedButton);
        panel.add(copySelectedButton);
        panel.add(batchProcessButton);
        //panel.add(new JSeparator(SwingConstants.HORIZONTAL));
        panel.add(stackButton);

        return panel;
    }

    private static void batchProcess(final ProductEntry[] productEntryList, final File graphFile) {
        final BatchGraphDialog batchDlg = new BatchGraphDialog(VisatApp.getApp(),
                "Batch Processing", "batchProcessing", false);
        batchDlg.setInputFiles(productEntryList);
        if (graphFile != null) {
            batchDlg.LoadGraphFile(graphFile);
        }
        batchDlg.show();
    }

    private void performStackOverviewAction() {

        final InSARMasterDialog dialog = new InSARMasterDialog();
        dialog.setInputProductList(getSelectedProductEntries());
        dialog.show();
    }

    public void selectionEnabled(final boolean enable) {
        openAllSelectedButton.setEnabled(enable);
        copySelectedButton.setEnabled(enable);
        batchProcessButton.setEnabled(enable);
        stackButton.setEnabled(enable && getNumberOfSelections() > 1);
    }

    private int getNumberOfSelections() {
        return productEntryTable.getSelectedRowCount();
    }

    public ProductEntry[] getSelectedProductEntries() {
        final int[] selectedRows = productEntryTable.getSelectedRows();
        final ProductEntry[] selectedEntries = new ProductEntry[selectedRows.length];
        for (int i = 0; i < selectedRows.length; i++) {
            final Object entry = productEntryTable.getValueAt(selectedRows[i], 0);
            if (entry instanceof ProductEntry) {
                selectedEntries[i] = (ProductEntry) entry;
            }
        }
        return selectedEntries;
    }

    public File[] getSelectedFiles() {
        final int[] selectedRows = productEntryTable.getSelectedRows();
        final File[] selectedFiles = new File[selectedRows.length];
        for (int i = 0; i < selectedRows.length; i++) {
            final Object entry = productEntryTable.getValueAt(selectedRows[i], 0);
            if (entry instanceof ProductEntry) {
                selectedFiles[i] = ((ProductEntry) entry).getFile();
            }
        }
        return selectedFiles;
    }

    private void performSelectAllAction() {
        productEntryTable.selectAll();
        notifySelectionChanged();
    }

    /**
     * Copy the selected file list to the clipboard
     */
    private void performCopyAction() {
        final File[] fileList = getSelectedFiles();
        if (fileList.length != 0)
            ClipboardUtils.copyToClipboard(fileList);
    }

    private void performCopyToAction() {
        final File targetFolder = promptForRepositoryBaseDir();
        if (targetFolder == null) return;

        final ProductEntry[] entries = getSelectedProductEntries();
        for (ProductEntry entry : entries) {
            try {
                ProductFileHandler.copyTo(entry, targetFolder);
            } catch (Exception e) {
                VisatApp.getApp().showErrorDialog("Unable to copy file " + entry.getFile().getAbsolutePath() +
                        '\n' + e.getMessage());
            }
        }
    }

    private void performMoveToAction() {
        final File targetFolder = promptForRepositoryBaseDir();
        if (targetFolder == null) return;

        final ProductEntry[] entries = getSelectedProductEntries();
        for (ProductEntry entry : entries) {
            try {
                ProductFileHandler.moveTo(entry, targetFolder);
            } catch (Exception e) {
                VisatApp.getApp().showErrorDialog("Unable to move file " + entry.getFile().getAbsolutePath() +
                        '\n' + e.getMessage());
            }
        }
        notifyDirectoryChanged();
    }

    private void performDeleteAction() {
        final ProductEntry[] entries = getSelectedProductEntries();
        for (ProductEntry entry : entries) {
            try {
                ProductFileHandler.delete(entry);

            } catch (Exception e) {
                VisatApp.getApp().showErrorDialog("Unable to delete file " + entry.getFile().getAbsolutePath() +
                        '\n' + e.getMessage());
            }
        }
        notifyDirectoryChanged();
    }

    public void performOpenAction() {
        if (openHandler != null) {
            openHandler.openProducts(getSelectedFiles());
        }
    }

    public File promptForRepositoryBaseDir() {
        final JFileChooser fileChooser = createDirectoryChooser();
        fileChooser.setCurrentDirectory(currentDirectory);
        final int response = fileChooser.showOpenDialog(VisatApp.getApp().getMainFrame());
        currentDirectory = fileChooser.getCurrentDirectory();
        File selectedDir = fileChooser.getSelectedFile();
        if (selectedDir != null && selectedDir.isFile())
            selectedDir = selectedDir.getParentFile();
        if (response == JFileChooser.APPROVE_OPTION) {
            return selectedDir;
        }
        return null;
    }

    private static JFileChooser createDirectoryChooser() {
        final JFileChooser fileChooser = new BeamFileChooser();
        fileChooser.setAcceptAllFileFilterUsed(false);
        fileChooser.setFileFilter(new FileFilter() {

            @Override
            public boolean accept(final File f) {
                return f.isDirectory();
            }

            @Override
            public String getDescription() {
                return "Directories"; /* I18N */
            }
        });
        fileChooser.setDialogTitle("Select Directory"); /* I18N */
        fileChooser.setMultiSelectionEnabled(false);
        fileChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        fileChooser.setApproveButtonText("Select"); /* I18N */
        fileChooser.setApproveButtonMnemonic('S');
        return fileChooser;
    }

    // Context Menu

    public JPopupMenu createEntryTablePopup() {
        final JPopupMenu popup = new JPopupMenu();
        final JMenuItem selectAllItem = new JMenuItem("Select All");
        selectAllItem.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                performSelectAllAction();
            }
        });
        popup.add(selectAllItem);

        final JMenuItem openSelectedItem = new JMenuItem("Open Selected");
        openSelectedItem.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                performOpenAction();
            }
        });
        popup.add(openSelectedItem);

        final JMenuItem copySelectedItem = new JMenuItem("Copy Selected");
        copySelectedItem.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                performCopyAction();
            }
        });
        popup.add(copySelectedItem);

        popup.addSeparator();

        copyToItem = new JMenuItem("Copy Selected Files To...");
        copyToItem.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                performCopyToAction();
            }
        });
        popup.add(copyToItem);

        moveToItem = new JMenuItem("Move Selected Files To...");
        moveToItem.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                performMoveToAction();
            }
        });
        popup.add(moveToItem);

        deleteItem = new JMenuItem("Delete Selected Files");
        deleteItem.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                final int status = VisatApp.getApp().showQuestionDialog("Are you sure you want to delete these products", "");
                if (status == JOptionPane.YES_OPTION)
                    performDeleteAction();
            }
        });
        popup.add(deleteItem);

        final JMenuItem exploreItem = new JMenuItem("Browse Folder");
        exploreItem.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                final Point pos = productEntryTable.getMousePosition();
                int row = 0;
                if (pos != null)
                    row = productEntryTable.rowAtPoint(pos);
                final Object entry = productEntryTable.getValueAt(row, 0);
                if (entry != null && entry instanceof ProductEntry) {
                    final ProductEntry prodEntry = (ProductEntry) entry;
                    try {
                        Desktop.getDesktop().open(prodEntry.getFile().getParentFile());
                    } catch (Exception ex) {
                        System.out.println(ex.getMessage());
                    }
                }
            }
        });
        popup.add(exploreItem);

        popup.addSeparator();

        final JMenu sortMenu = new JMenu("Sort By");
        popup.add(sortMenu);

        sortMenu.add(createSortItem("Product Name", SortingDecorator.SORT_BY.NAME));
        sortMenu.add(createSortItem("Product Type", SortingDecorator.SORT_BY.TYPE));
        sortMenu.add(createSortItem("Acquisition Date", SortingDecorator.SORT_BY.DATE));
        sortMenu.add(createSortItem("Mission", SortingDecorator.SORT_BY.MISSON));
        sortMenu.add(createSortItem("File Size", SortingDecorator.SORT_BY.FILESIZE));

        return popup;
    }

    private JMenuItem createSortItem(final String name, final SortingDecorator.SORT_BY sortBy) {
        final JMenuItem item = new JMenuItem(name);
        item.addActionListener(new ActionListener() {
            public void actionPerformed(final ActionEvent e) {
                final TableModel model = productEntryTable.getModel();
                if(model instanceof SortingDecorator) {
                    SortingDecorator sortedModel = (SortingDecorator) model;
                    sortedModel.sortBy(sortBy);
                }
            }
        });
        return item;
    }

    public JPopupMenu createGraphPopup() {
        final File graphPath = ResourceUtils.getGraphFolder("");

        final JPopupMenu popup = new JPopupMenu();
        if (graphPath.exists()) {
            createGraphMenu(popup, graphPath);
        }
        return popup;
    }

    private void createGraphMenu(final JPopupMenu menu, final File path) {
        final File[] filesList = path.listFiles();
        if (filesList == null || filesList.length == 0) return;

        for (final File file : filesList) {
            final String name = file.getName();
            if (file.isDirectory() && !file.isHidden() && !name.equalsIgnoreCase("internal")) {
                final JMenu subMenu = new JMenu(name);
                menu.add(subMenu);
                createGraphMenu(subMenu, file);
            } else if (name.toLowerCase().endsWith(".xml")) {
                final JMenuItem item = new JMenuItem(name.substring(0, name.indexOf(".xml")));
                item.addActionListener(new ActionListener() {

                    public void actionPerformed(final ActionEvent e) {
                        //todo
                       // if (batchProcessButton.isEnabled())
                       //     batchProcess(getSelectedProductEntries(), file);
                    }
                });
                menu.add(item);
            }
        }
    }

    private void createGraphMenu(final JMenu menu, final File path) {
        final File[] filesList = path.listFiles();
        if (filesList == null || filesList.length == 0) return;

        for (final File file : filesList) {
            final String name = file.getName();
            if (file.isDirectory() && !file.isHidden() && !name.equalsIgnoreCase("internal")) {
                final JMenu subMenu = new JMenu(name);
                menu.add(subMenu);
                createGraphMenu(subMenu, file);
            } else if (name.toLowerCase().endsWith(".xml")) {
                final JMenuItem item = new JMenuItem(name.substring(0, name.indexOf(".xml")));
                item.addActionListener(new ActionListener() {

                    public void actionPerformed(final ActionEvent e) {
                        //todo
                       // if (batchProcessButton.isEnabled())
                      //      batchProcess(getSelectedProductEntries(), file);
                    }
                });
                menu.add(item);
            }
        }
    }

    public void updateContextMenu(final ProductEntry[] selections) {
        boolean allValid = true;
        for (ProductEntry entry : selections) {
            if (!ProductFileHandler.canMove(entry)) {
                allValid = false;
                break;
            }
        }
        copyToItem.setEnabled(allValid);
        moveToItem.setEnabled(allValid);
        deleteItem.setEnabled(allValid);
    }

    public void addListener(final ProductLibraryActionListener listener) {
        if (!listenerList.contains(listener)) {
            listenerList.add(listener);
        }
    }

    private void notifyDirectoryChanged() {
        for(ProductLibraryActionListener listener : listenerList) {
            listener.notifyDirectoryChanged();
        }
    }

    private void notifySelectionChanged() {
        for(ProductLibraryActionListener listener : listenerList) {
            listener.notifySelectionChanged();
        }
    }

    public interface ProductLibraryActionListener {

        public void notifyDirectoryChanged();
        public void notifySelectionChanged();
    }
}
