package org.esa.beam.opendap.ui;

import com.bc.ceres.core.ProgressBarProgressMonitor;
import com.jidesoft.combobox.AbstractComboBox;
import com.jidesoft.combobox.FolderChooserComboBox;
import com.jidesoft.combobox.PopupPanel;
import com.jidesoft.status.LabelStatusBarItem;
import com.jidesoft.status.ProgressStatusBarItem;
import com.jidesoft.status.StatusBar;
import com.jidesoft.swing.FolderChooser;
import com.jidesoft.swing.JideBoxLayout;
import com.jidesoft.swing.JideScrollPane;
import com.jidesoft.swing.SimpleScrollPane;
import org.esa.beam.framework.help.HelpSys;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.GridBagUtils;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.tool.ToolButtonFactory;
import org.esa.beam.opendap.datamodel.OpendapLeaf;
import org.esa.beam.opendap.utils.OpendapUtils;
import org.esa.beam.util.PropertyMap;
import org.esa.beam.util.StringUtils;
import org.esa.beam.util.logging.BeamLogManager;
import org.esa.beam.visat.VisatApp;
import thredds.catalog.InvCatalog;
import thredds.catalog.InvCatalogFactory;
import thredds.catalog.InvDataset;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.border.EmptyBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class OpendapAccessPanel extends JPanel {

    private static final String PROPERTY_KEY_SERVER_URLS = "opendap.server.urls";
    private final static int DDS_AREA_INDEX = 0;
    private final static int DAS_AREA_INDEX = 1;

    private JComboBox urlField;
    private AbstractButton refreshButton;
    private CatalogTree catalogTree;

    private JTabbedPane metaInfoArea;
    private JCheckBox useDatasetNameFilter;

    private FilterComponent datasetNameFilter;
    private JCheckBox useTimeRangeFilter;

    private FilterComponent timeRangeFilter;
    private JCheckBox useRegionFilter;

    private FilterComponent regionFilter;
    private JCheckBox useVariableFilter;

    private VariableFilter variableFilter;

    private JCheckBox openInVisat;
    private StatusBar statusBar;

    private double currentDataSize = 0.0;
    private final PropertyMap propertyMap;
    private final String helpId;
    private FolderChooserComboBox folderChooserComboBox;
    private JProgressBar progressBar;
    private JLabel preMessageLabel;
    private JLabel postMessageLabel;
    private Map<Integer, JTextArea> textAreas;
    private JButton downloadButton;
    private AppContext appContext;
    private JButton cancelButton;

    public OpendapAccessPanel(AppContext appContext, String helpId) {
        super();
        this.propertyMap = appContext.getPreferences();
        this.helpId = helpId;
        this.appContext = appContext;
        initComponents();
        initContentPane();
    }

    private void initComponents() {
        urlField = new JComboBox();
        urlField.setEditable(true);
        urlField.getEditor().getEditorComponent().addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                if (e.getKeyChar() == KeyEvent.VK_ENTER) {
                    refreshButton.doClick();
                }
            }
        });
        updateUrlField();
        refreshButton = ToolButtonFactory.createButton(
                UIUtils.loadImageIcon("/org/esa/beam/opendap/images/icons/ViewRefresh22.png", OpendapAccessPanel.class),
                false);
        refreshButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final boolean usingUrl = refresh();
                if (usingUrl) {
                    final String urls = propertyMap.getPropertyString(PROPERTY_KEY_SERVER_URLS);
                    final String currentUrl = urlField.getSelectedItem().toString();
                    if (!urls.contains(currentUrl)) {
                        propertyMap.setPropertyString(PROPERTY_KEY_SERVER_URLS, urls + "\n" + currentUrl);
                        updateUrlField();
                    }
                }
            }
        });
        metaInfoArea = new JTabbedPane(JTabbedPane.TOP, JTabbedPane.SCROLL_TAB_LAYOUT);
        JTextArea ddsArea = new JTextArea(10, 40);
        JTextArea dasArea = new JTextArea(10, 40);

        ddsArea.setEditable(false);
        dasArea.setEditable(false);

        textAreas = new HashMap<Integer, JTextArea>();
        textAreas.put(DAS_AREA_INDEX, dasArea);
        textAreas.put(DDS_AREA_INDEX, ddsArea);

        metaInfoArea.addTab("DDS", new JScrollPane(ddsArea));
        metaInfoArea.addTab("DAS", new JScrollPane(dasArea));
        metaInfoArea.setToolTipTextAt(DDS_AREA_INDEX, "Dataset Descriptor Structure: description of dataset variables");
        metaInfoArea.setToolTipTextAt(DAS_AREA_INDEX, "Dataset Attribute Structure: description of dataset attributes");

        metaInfoArea.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                if (catalogTree.getSelectedLeaf() != null) {
                    setMetadataText(metaInfoArea.getSelectedIndex(), catalogTree.getSelectedLeaf());
                }
            }
        });

        catalogTree = new CatalogTree(new CatalogTree.LeafSelectionListener() {

            @Override
            public void dapLeafSelected(OpendapLeaf leaf) {
                setMetadataText(metaInfoArea.getSelectedIndex(), leaf);
            }

            @Override
            public void fileLeafSelected(OpendapLeaf leaf) {
                setMetadataText(metaInfoArea.getSelectedIndex(), leaf);
            }

            @Override
            public void leafSelectionChanged(boolean isSelected, OpendapLeaf dapObject) {
                int dataSize = dapObject.getFileSize();
                currentDataSize += isSelected ? dataSize : -dataSize;
                if (currentDataSize <= 0) {
                    updateStatusBar("Ready.");
                    downloadButton.setEnabled(false);
                } else {
                    downloadButton.setEnabled(true);
                    double dataSizeInMB = currentDataSize / 1024.0;
                    updateStatusBar("Total size of currently selected files: " + OpendapUtils.format(dataSizeInMB) + " MB");
                }
            }
        }, appContext);
        useDatasetNameFilter = new JCheckBox("Use dataset name filter");
        useTimeRangeFilter = new JCheckBox("Use time range filter");
        useRegionFilter = new JCheckBox("Use region filter");
        useVariableFilter = new JCheckBox("Use variable name filter");

        DefaultFilterChangeListener filterChangeListener = new DefaultFilterChangeListener();
        datasetNameFilter = new DatasetNameFilter(useDatasetNameFilter);
        datasetNameFilter.addFilterChangeListener(filterChangeListener);
        timeRangeFilter = new TimeRangeFilter(useTimeRangeFilter);
        timeRangeFilter.addFilterChangeListener(filterChangeListener);
        regionFilter = new RegionFilter(useRegionFilter);
        regionFilter.addFilterChangeListener(filterChangeListener);
        variableFilter = new VariableFilter(useVariableFilter, catalogTree);
        variableFilter.addFilterChangeListener(filterChangeListener);

        catalogTree.addCatalogTreeListener(new CatalogTree.CatalogTreeListener() {
            @Override
            public void leafAdded(OpendapLeaf leaf, boolean hasNestedDatasets) {
                if (hasNestedDatasets) {
                    return;
                }
                if (leaf.getDataset().getGeospatialCoverage() != null) {
                    useRegionFilter.setEnabled(true);
                }
                filterLeaf(leaf);
            }

            @Override
            public void catalogElementsInsertionFinished() {
            }
        });

        openInVisat = new JCheckBox("Open in VISAT");
        statusBar = new StatusBar();
        final LabelStatusBarItem message = new LabelStatusBarItem("label");
        message.setText("Ready.");
        message.setAlignment(JLabel.LEFT);
        statusBar.add(message, JideBoxLayout.FLEXIBLE);

        preMessageLabel = new JLabel();
        postMessageLabel = new JLabel();
        final LabelStatusBarItem preMessage = new LabelStatusBarItem() {
            @Override
            protected JLabel createLabel() {
                return preMessageLabel;
            }
        };
        final LabelStatusBarItem postMessage = new LabelStatusBarItem() {
            @Override
            protected JLabel createLabel() {
                return postMessageLabel;
            }
        };

        statusBar.add(preMessage, JideBoxLayout.FIX);

        ProgressStatusBarItem progressBarItem = new ProgressStatusBarItem();
        progressBarItem.setProgress(0);
        progressBar = progressBarItem.getProgressBar();

        statusBar.add(progressBarItem, JideBoxLayout.FIX);

        statusBar.add(postMessage, JideBoxLayout.FIX);

        useRegionFilter.setEnabled(false);
    }


    private void setMetadataText(int componentIndex, OpendapLeaf leaf) {
        String text = null;
        try {
            if (leaf.isDapAccess()) {
                if (metaInfoArea.getSelectedIndex() == DDS_AREA_INDEX) {
                    text = OpendapUtils.getResponse(leaf.getDdsUri());
                } else if (metaInfoArea.getSelectedIndex() == DAS_AREA_INDEX) {
                    text = OpendapUtils.getResponse(leaf.getDasUri());
                }
            } else if (leaf.isFileAccess()) {
                if (metaInfoArea.getSelectedIndex() == DDS_AREA_INDEX) {
                    text = "No DDS information for file '" + leaf.getName() + "'.";
                } else if (metaInfoArea.getSelectedIndex() == DAS_AREA_INDEX) {
                    text = "No DAS information for file '" + leaf.getName() + "'.";
                }
            }
        } catch (IOException e) {
            BeamLogManager.getSystemLogger().warning("Unable to retrieve meta information for file '" + leaf.getName() + "'.");
        }

        setResponseText(componentIndex, text);
    }

    private void setResponseText(int componentIndex, String response) {
        JTextArea textArea = textAreas.get(componentIndex);
        if (response.length() > 100000) {
            StringBuilder responseBuilder = new StringBuilder(response.substring(0, 10000));
            responseBuilder.append("\n" + "Cut remaining file content");
            response = responseBuilder.toString();
        }
        textArea.setText(response);
        textArea.setCaretPosition(0);
    }

    private void updateStatusBar(String message) {
        LabelStatusBarItem messageItem = (LabelStatusBarItem) statusBar.getItemByName("label");
        messageItem.setText(message);
    }

    private void filterLeaf(OpendapLeaf leaf) {
        if (
                (!useDatasetNameFilter.isSelected() || datasetNameFilter.accept(leaf)) &&
                        (!useTimeRangeFilter.isSelected() || timeRangeFilter.accept(leaf)) &&
                        (!useRegionFilter.isSelected() || regionFilter.accept(leaf)) &&
                        (!useVariableFilter.isSelected() || variableFilter.accept(leaf))) {
            catalogTree.setLeafVisible(leaf, true);
        } else {
            catalogTree.setLeafVisible(leaf, false);
        }
    }

    private void updateUrlField() {
        final String urlsProperty = propertyMap.getPropertyString(PROPERTY_KEY_SERVER_URLS);
        final String[] urls = urlsProperty.split("\n");
        for (String url : urls) {
            if (StringUtils.isNotNullAndNotEmpty(url) && !contains(urlField, url)) {
                urlField.addItem(url);
            }
        }
    }

    private boolean contains(JComboBox urlField, String url) {
        for (int i = 0; i < urlField.getItemCount(); i++) {
            if (urlField.getItemAt(i).toString().equals(url)) {
                return true;
            }
        }
        return false;
    }

    private void initContentPane() {
        GridBagLayout layout = new GridBagLayout();
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets.right = 5;
        final JPanel urlPanel = new JPanel(layout);
        urlPanel.add(new JLabel("Root URL:"), gbc);
        gbc.gridx = 1;
        gbc.weightx = 1.0;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        urlPanel.add(urlField, gbc);
        gbc.gridx = 2;
        gbc.weightx = 0.0;
        gbc.fill = GridBagConstraints.NONE;
        urlPanel.add(refreshButton, gbc);
        gbc.gridx = 3;
        gbc.insets.right = 0;
        final AbstractButton helpButton = ToolButtonFactory.createButton(
                UIUtils.loadImageIcon("/org/esa/beam/opendap/images/icons/Help22.png", OpendapAccessPanel.class),
                false);
        HelpSys.enableHelpOnButton(helpButton, helpId);
        urlPanel.add(helpButton, gbc);

        final JPanel variableInfo = new JPanel(new BorderLayout(5, 5));
        variableInfo.setBorder(new EmptyBorder(10, 0, 0, 0));
        variableInfo.add(metaInfoArea, BorderLayout.CENTER);

        final JScrollPane openDapTree = new JScrollPane(catalogTree.getComponent());
        final JSplitPane centerLeftPane = new JSplitPane(JSplitPane.VERTICAL_SPLIT, openDapTree, variableInfo);
        centerLeftPane.setResizeWeight(1);
        centerLeftPane.setContinuousLayout(true);

        final JPanel filterPanel = new JPanel(new GridBagLayout());
        final JComponent datasetNameFilterUI = datasetNameFilter.getUI();
        final JComponent timeRangeFilterUI = timeRangeFilter.getUI();
        final JComponent regionFilterUI = regionFilter.getUI();
        final JComponent variableFilterUI = variableFilter.getUI();
        GridBagUtils.addToPanel(filterPanel, new TitledPanel(useDatasetNameFilter, datasetNameFilterUI, true, true), gbc, "gridx=0,gridy=0,anchor=NORTHWEST,weightx=1,weighty=0,,fill=BOTH");
        GridBagUtils.addToPanel(filterPanel, new TitledPanel(useTimeRangeFilter, timeRangeFilterUI, true, true), gbc, "gridy=1");
        GridBagUtils.addToPanel(filterPanel, new TitledPanel(useRegionFilter, regionFilterUI, true, true), gbc, "gridy=2");
        GridBagUtils.addToPanel(filterPanel, new TitledPanel(useVariableFilter, variableFilterUI, true, true), gbc, "gridy=3");
        GridBagUtils.addToPanel(filterPanel, new JLabel(), gbc, "gridy=4,weighty=1");
        filterPanel.setPreferredSize(new Dimension(460, 800));
        filterPanel.setMinimumSize(new Dimension(460, 120));
        filterPanel.setMaximumSize(new Dimension(460, 800));

        final JPanel downloadButtonPanel = new JPanel(new BorderLayout(8, 5));
        downloadButtonPanel.setBorder(new EmptyBorder(8, 8, 8, 8));
        cancelButton = new JButton("Cancel");
        final DownloadProgressBarProgressMonitor pm = new DownloadProgressBarProgressMonitor(progressBar, preMessageLabel, postMessageLabel, cancelButton);
        progressBar.setVisible(false);
        folderChooserComboBox = new FolderChooserComboBox() {
            @Override
            public PopupPanel createPopupComponent() {
                final PopupPanel popupComponent = super.createPopupComponent();
                final JScrollPane content = (JScrollPane) popupComponent.getComponents()[0];
                final JComponent upperPane = (JComponent) content.getComponents()[0];
                FolderChooser folderChooser = (FolderChooser) upperPane.getComponents()[0];
                folderChooser.setRecentListVisible(false);
                popupComponent.setTitle("Choose download directory");
                return popupComponent;
            }
        };
        downloadButton = new JButton("Download");
        downloadButton.setEnabled(false);
        final DownloadAction downloadAction = createDownloadAction(pm);
        downloadButton.addActionListener(downloadAction);
        folderChooserComboBox.setEditable(true);
        if (VisatApp.getApp() != null) {
            downloadButtonPanel.add(openInVisat, BorderLayout.NORTH);
        }
        downloadButtonPanel.add(folderChooserComboBox);
        JPanel buttonPanel = new JPanel(new BorderLayout());
        buttonPanel.add(downloadButton, BorderLayout.WEST);
        cancelButton.setEnabled(false);
        downloadButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                cancelButton.setEnabled(true);
            }
        });
        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                downloadAction.cancel();
                cancelButton.setEnabled(false);
            }
        });
        buttonPanel.add(cancelButton, BorderLayout.EAST);
        downloadButtonPanel.add(buttonPanel, BorderLayout.EAST);

        JPanel centerRightPane = new JPanel(new BorderLayout());
        final SimpleScrollPane simpleScrollPane = new SimpleScrollPane(filterPanel, JideScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED,
                                                           JideScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
        simpleScrollPane.setBorder(BorderFactory.createEmptyBorder());
        centerRightPane.add(simpleScrollPane, BorderLayout.CENTER);
        centerRightPane.add(downloadButtonPanel, BorderLayout.SOUTH);

        final JSplitPane centerPanel = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, centerLeftPane, centerRightPane);
        centerPanel.setResizeWeight(1);
        centerPanel.setContinuousLayout(true);

        this.setLayout(new BorderLayout(15, 15));
        this.setBorder(new EmptyBorder(8, 8, 8, 8));
        this.add(urlPanel, BorderLayout.NORTH);
        this.add(centerPanel, BorderLayout.CENTER);
        this.add(statusBar, BorderLayout.SOUTH);
    }

    private DownloadAction createDownloadAction(DownloadProgressBarProgressMonitor pm) {
        return new DownloadAction(pm, new ParameterProviderImpl(), new DownloadAction.DownloadHandler() {

            @Override
            public void handleException(Exception e) {
                appContext.handleError("Unable to perform download. Reason: " + e.getMessage(), e);
            }

            @Override
            public void handleDownloadFinished(File downloadedFiles) {
                if (openInVisat.isSelected()) {
                    VisatApp.getApp().openProduct(downloadedFiles);
                }
            }
        });
    }

    private File fetchTargetDirectory() {
        final JFileChooser chooser = new JFileChooser();
        chooser.setDialogType(JFileChooser.OPEN_DIALOG);
        chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        chooser.setDialogTitle("Select Target Directory");
        final int i = chooser.showDialog(null, "Save to directory");
        if (i == JFileChooser.APPROVE_OPTION) {
            return chooser.getSelectedFile();
        }
        return null;
    }

    private boolean refresh() {
        String url;
        if (urlField.getSelectedItem() == null) {
            url = urlField.getEditor().getItem().toString();
        } else {
            url = urlField.getSelectedItem().toString();
        }
        url = checkCatalogURLString(url);
        final InvCatalogFactory factory = InvCatalogFactory.getDefaultFactory(true);
        final InvCatalog catalog = factory.readXML(url);
        final List<InvDataset> datasets = catalog.getDatasets();

        if (datasets.size() == 0) {
            JOptionPane.showMessageDialog(this, "Cannnot find THREDDS catalog service xml at '" + url + "'");
            return false;
        }
        urlField.setSelectedItem(url);
        catalogTree.setNewRootDatasets(datasets);
        variableFilter.stopFiltering();
        return true;
    }

    private String checkCatalogURLString(String url) {
        if (url.endsWith("catalog.xml")) {
            return url;
        } else if (url.endsWith("catalog.html")) {
            return url.substring(0, url.lastIndexOf("h")).concat("xml");
        } else if (url.endsWith("/")) {
            return url.concat("catalog.xml");
        } else {
            return url.concat("/catalog.xml");
        }
    }

    private class DefaultFilterChangeListener implements FilterChangeListener {

        @Override
        public void filterChanged() {
            final OpendapLeaf[] leaves = catalogTree.getLeaves();
            for (OpendapLeaf leaf : leaves) {
                filterLeaf(leaf);
            }
        }
    }

    public static class DownloadProgressBarProgressMonitor extends ProgressBarProgressMonitor implements
            LabelledProgressBarPM {

        private final JProgressBar progressBar;
        private final JLabel preMessageLabel;
        private JLabel postMessageLabel;
        private int totalWork;
        private int currentWork;
        private long startTime;
        private JButton cancelButton;

        public DownloadProgressBarProgressMonitor(JProgressBar progressBar, JLabel preMessageLabel, JLabel postMessageLabel, JButton cancelButton) {
            super(progressBar, preMessageLabel);
            this.progressBar = progressBar;
            this.preMessageLabel = preMessageLabel;
            this.postMessageLabel = postMessageLabel;
            this.cancelButton = cancelButton;
        }

        @Override
        public void setPreMessage(String preMessageText) {
            setTaskName(preMessageText);
        }

        @Override
        public void setPostMessage(String postMessageText) {
            postMessageLabel.setText(postMessageText);
        }

        @Override
        public void setTooltip(String tooltip) {
            preMessageLabel.setToolTipText(tooltip);
            postMessageLabel.setToolTipText(tooltip);
            progressBar.setToolTipText(tooltip);
        }

        @Override
        public void beginTask(String name, int totalWork) {
            super.beginTask(name, totalWork);
            this.totalWork = totalWork;
            this.currentWork = 0;
            progressBar.setValue(0);
        }

        @Override
        public void worked(int work) {
            super.worked(work);
            currentWork += work;
        }

        @Override
        protected void setDescription(String description) {
        }

        @Override
        protected void setVisibility(boolean visible) {
            // once the progress bar has been made visible, it shall always be visible
            progressBar.setVisible(true);
        }

        @Override
        protected void setRunning() {
        }

        @Override
        protected void finish() {
            cancelButton.setEnabled(false);
        }

        @Override
        public int getTotalWork() {
            return totalWork;
        }

        @Override
        public int getCurrentWork() {
            return currentWork;
        }

        public void updateTask(int additionalWork) {
            totalWork += additionalWork;
            progressBar.setMaximum(totalWork);
            progressBar.updateUI();
        }

        public void resetStartTime() {
            GregorianCalendar gc = new GregorianCalendar();
            startTime = gc.getTimeInMillis();
        }

        public long getStartTime() {
            return startTime;
        }
    }

    private class ParameterProviderImpl implements DownloadAction.ParameterProvider {

        Map<String, Boolean> dapURIs = new HashMap<String, Boolean>();
        List<String> fileURIs = new ArrayList<String>();

        @Override
        public Map<String, Boolean> getDapURIs() {
            if (dapURIs.isEmpty() && fileURIs.isEmpty()) {
                collectURIs();
            }
            return new HashMap<String, Boolean>(dapURIs);
        }

        @Override
        public List<String> getFileURIs() {
            if (dapURIs.isEmpty() && fileURIs.isEmpty()) {
                collectURIs();
            }
            return new ArrayList<String>(fileURIs);
        }

        private void collectURIs() {
            final TreePath[] selectionPaths = ((JTree) catalogTree.getComponent()).getSelectionModel().getSelectionPaths();
            if (selectionPaths == null || selectionPaths.length <= 0) {
                return;
            }

            for (TreePath selectionPath : selectionPaths) {
                final DefaultMutableTreeNode treeNode = (DefaultMutableTreeNode) selectionPath.getLastPathComponent();
                if (CatalogTree.isDapNode(treeNode) || CatalogTree.isFileNode(treeNode)) {
                    final OpendapLeaf leaf = (OpendapLeaf) treeNode.getUserObject();
                    if (leaf.isDapAccess()) {
                        dapURIs.put(leaf.getDapUri(), leaf.getFileSize() >= 2 * 1024 * 1024);
                    } else if (leaf.isFileAccess()) {
                        fileURIs.add(leaf.getFileUri());
                    }
                }
            }
        }

        @Override
        public void reset() {
            dapURIs.clear();
            fileURIs.clear();
        }

        @Override
        public double getDatasizeInKb() {
            return currentDataSize;
        }

        @Override
        public File getTargetDirectory() {
            final File targetDirectory;
            AbstractComboBox.DefaultTextFieldEditorComponent textField = (AbstractComboBox.DefaultTextFieldEditorComponent) folderChooserComboBox.getEditor();
            if (textField.getText() == null || textField.getText().equals("")) {
                targetDirectory = fetchTargetDirectory();
                if (targetDirectory != null) {
                    textField.setText(targetDirectory.toString());
                }
            } else {
                targetDirectory = new File(textField.getText());
            }
            return targetDirectory;
        }
    }

}