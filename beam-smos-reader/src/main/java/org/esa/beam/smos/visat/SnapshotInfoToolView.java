package org.esa.beam.smos.visat;

import com.bc.ceres.binio.CompoundData;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.glevel.support.DefaultMultiLevelImage;
import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import org.esa.beam.dataio.smos.*;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.framework.ui.tool.ToolButtonFactory;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.Rectangle2D;
import java.awt.image.RenderedImage;
import java.io.IOException;

public class SnapshotInfoToolView extends SmosToolView {

    public static final String ID = SnapshotInfoToolView.class.getName();

    private SnapshotSelector snapshotSelector;

    private JTable snapshotTable;
    private L1cScienceSmosFile smosFile;
    private SnapshotTableModel nullModel;
    private SpinnerChangeListener snapshotSpinnerListener;
    private SliderChangeListener snapshotSliderListener;
    private AbstractButton snapshotModeButton;
    private AbstractButton locateSnapshotButton;
    private SSSL sssl;

    public SnapshotInfoToolView() {
        nullModel = new SnapshotTableModel(new Object[0][0]);
    }

    @Override
    public void componentOpened() {
        super.componentOpened();
        sssl = new SSSL();
        SmosBox.getInstance().getSnapshotSelectionService().addSnapshotIdChangeListener(sssl);
        realizeSnapshotIdChange(getSelectedSmosProduct());
    }

    @Override
    public void componentClosed() {
        super.componentClosed();
        SmosBox.getInstance().getSnapshotSelectionService().removeSnapshotIdChangeListener(sssl);
    }

    @Override
    protected JComponent createClientComponent(ProductSceneView smosView) {

        snapshotSpinnerListener = new SpinnerChangeListener();
        // todo - use snapshot selector combo (rq-20090114)
        snapshotSelector = new SnapshotSelector();

        snapshotSliderListener = new SliderChangeListener();

        snapshotTable = new JTable(nullModel);
        snapshotTable.getColumnModel().getColumn(1).setCellRenderer(new DefaultTableCellRenderer() {
            @Override
            public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
                super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
                if (value instanceof Number) {
                    setHorizontalAlignment(RIGHT);
                }
                return this;
            }
        });

        JPanel panel1 = new JPanel(new BorderLayout(2, 2));
        panel1.add(snapshotSelector.getSpinner(), BorderLayout.WEST);
        panel1.add(snapshotSelector.getSlider(), BorderLayout.CENTER);
        panel1.add(snapshotSelector.getSliderTextField(), BorderLayout.EAST);

        snapshotModeButton = ToolButtonFactory.createButton(
                new ImageIcon(SnapshotInfoToolView.class.getResource("Snapshot24.png")), true);
        snapshotModeButton.addActionListener(new ToggleSnapshotModeAction());
        snapshotModeButton.setToolTipText("Toggle snapshot mode on/off");

        locateSnapshotButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/ZoomTool24.gif"), false);
        locateSnapshotButton.addActionListener(new LocateSnapshotAction());
        locateSnapshotButton.setToolTipText("Locate selected snapshot in view");

        JPanel panel2 = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 2));
        panel2.add(snapshotModeButton);
        panel2.add(locateSnapshotButton);

        JPanel panel3 = new JPanel(new BorderLayout(2, 2));
        panel3.add(panel1, BorderLayout.NORTH);
        panel3.add(new JScrollPane(snapshotTable), BorderLayout.CENTER);
        panel3.add(panel2, BorderLayout.SOUTH);
        panel3.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        return panel3;
    }

    @Override
    protected void updateClientComponent(ProductSceneView smosView) {
        boolean enabled = smosView != null && getSelectedSmosFile() instanceof L1cScienceSmosFile;

        snapshotSelector.getSpinner().removeChangeListener(snapshotSpinnerListener);
        snapshotSelector.getSlider().removeChangeListener(snapshotSliderListener);
        if (enabled) {
            smosFile = (L1cScienceSmosFile) getSelectedSmosFile();
            snapshotSelector.setModel(new SnapshotSelectorModel(smosFile.getSnapshotIds()));
            realizeSnapshotIdChange(getSelectedSmosProduct());
            snapshotSelector.getSpinner().addChangeListener(snapshotSpinnerListener);
            snapshotSelector.getSlider().addChangeListener(snapshotSliderListener);
        } else {
            smosFile = null;
        }

        snapshotSelector.getSpinner().setEnabled(enabled);
        snapshotSelector.getSlider().setEnabled(enabled);
        snapshotTable.setEnabled(enabled);
        snapshotModeButton.setEnabled(enabled);
        snapshotModeButton.setSelected(getSnapshotIdFromView() != -1);
        locateSnapshotButton.setEnabled(enabled && snapshotModeButton.isSelected());
    }

    int getSelectedSnapshotId() {
        Product selectedSmosProduct = getSelectedSmosProduct();
        if (selectedSmosProduct == null) {
            return -1;
        }
        return SmosBox.getInstance().getSnapshotSelectionService().getSelectedSnapshotId(selectedSmosProduct);
    }

    public void realizeSnapshotIdChange(Product product) {
        if (product == getSelectedSmosProduct()) {
            int snapshotId = getSelectedSnapshotId();
            if (snapshotId != -1) {
                snapshotSelector.getModel().getSpinnerModel().setValue(snapshotId);
                int snapshotIndex = smosFile.getSnapshotIndex(snapshotId);
                if (snapshotIndex != -1) {
                    try {
                        updateTable(snapshotIndex);
                        setSnapshotIdOfView();
                    } catch (IOException e) {
                        snapshotTable.setModel(nullModel);
                    }
                } else {
                    snapshotTable.setModel(nullModel);
                }
            }
        }
    }

    private void updateTable(int snapshotIndex) throws IOException {
        CompoundData data = smosFile.getSnapshotData(snapshotIndex);
        int n = data.getMemberCount();
        Object[][] tableData = new Object[n][2];
        for (int i = 0; i < n; i++) {
            tableData[i][0] = data.getCompoundType().getMemberName(i);
            if (data.getCompoundType().getMemberType(i).isSimpleType()) {
                tableData[i][1] = GridPointBtDataset.getNumbericMember(data, i);
            } else {
                tableData[i][1] = data.getCompoundType().getMemberType(i).getName();
            }
        }
        snapshotTable.setModel(new SnapshotTableModel(tableData));
    }

    private void setSnapshotIdOfView() {
        ProductSceneView sceneView = getSelectedSmosView();
        ImageLayer imageLayer = sceneView.getBaseImageLayer();
        RenderedImage sourceImage = sceneView.getRaster().getSourceImage();
        if (sourceImage instanceof DefaultMultiLevelImage) {
            DefaultMultiLevelImage defaultMultiLevelImage = (DefaultMultiLevelImage) sourceImage;
            if (defaultMultiLevelImage.getSource() instanceof SmosMultiLevelSource) {
                SmosMultiLevelSource smosMultiLevelSource = (SmosMultiLevelSource) defaultMultiLevelImage.getSource();
                GridPointValueProvider gridPointValueProvider = smosMultiLevelSource.getValueProvider();
                if (gridPointValueProvider instanceof L1cFieldValueProvider) {
                    L1cFieldValueProvider l1cFieldValueProvider = (L1cFieldValueProvider) gridPointValueProvider;
                    int id = snapshotModeButton.isSelected() ? getSelectedSnapshotId() : -1;
                    if (l1cFieldValueProvider.getSnapshotId() != id) {
                        l1cFieldValueProvider.setSnapshotId(id);
                        smosMultiLevelSource.reset();
                        sceneView.getRaster().setValidMaskImage(null);
                        sceneView.getRaster().setGeophysicalImage(null);
                        imageLayer.regenerate();
                    }
                }
            }
        }
    }

    private int getSnapshotIdFromView() {
        ProductSceneView sceneView = getSelectedSmosView();
        RenderedImage sourceImage = sceneView.getRaster().getSourceImage();
        if (sourceImage instanceof DefaultMultiLevelImage) {
            DefaultMultiLevelImage defaultMultiLevelImage = (DefaultMultiLevelImage) sourceImage;
            if (defaultMultiLevelImage.getSource() instanceof SmosMultiLevelSource) {
                SmosMultiLevelSource smosMultiLevelSource = (SmosMultiLevelSource) defaultMultiLevelImage.getSource();
                GridPointValueProvider gridPointValueProvider = smosMultiLevelSource.getValueProvider();
                if (gridPointValueProvider instanceof L1cFieldValueProvider) {
                    L1cFieldValueProvider l1cFieldValueProvider = (L1cFieldValueProvider) gridPointValueProvider;
                    return l1cFieldValueProvider.getSnapshotId();
                }
            }
        }
        return -1;
    }

    private void locateSnapshotIdOfView() {
        SmosFile file = getSelectedSmosFile();
        if (file instanceof L1cScienceSmosFile) {
            final L1cScienceSmosFile l1cScienceSmosFile = (L1cScienceSmosFile) file;

            ProgressMonitorSwingWorker<Rectangle2D, Object> pmsw = new ProgressMonitorSwingWorker<Rectangle2D, Object>(
                    locateSnapshotButton, "Computing snapshot region") {
                @Override
                protected Rectangle2D doInBackground(ProgressMonitor pm) throws Exception {
                    return l1cScienceSmosFile.computeSnapshotRegion(getSelectedSnapshotId(), pm);
                }

                @Override
                protected void done() {
                    try {
                        Rectangle2D region = get();
                        if (region != null) {
                            getSelectedSmosView().getLayerCanvas().getViewport().zoom(region);
                        } else {
                            JOptionPane.showMessageDialog(locateSnapshotButton,
                                                          "No snapshot found with ID=" + getSelectedSnapshotId());
                        }
                    } catch (Exception e) {
                        JOptionPane.showMessageDialog(locateSnapshotButton, "Error:\n" + e.getMessage());
                        e.printStackTrace();
                    }
                }
            };

            pmsw.execute();
        }
    }


    boolean adjustingSpinner = false;

    private class SpinnerChangeListener implements ChangeListener {
        @Override
        public void stateChanged(ChangeEvent e) {
            adjustingSpinner = true;
            if (getSelectedSmosProduct() != null) {
                final Integer snapshotId = (Integer) snapshotSelector.getSpinner().getModel().getValue();
                SmosBox.getInstance().getSnapshotSelectionService().setSelectedSnapshotId(getSelectedSmosProduct(),
                                                                                          snapshotId);
            }
            adjustingSpinner = false;
        }
    }

    private class SliderChangeListener implements ChangeListener {
        @Override
        public void stateChanged(ChangeEvent e) {
            if (!adjustingSpinner) {
                if (getSelectedSmosProduct() != null) {
                    final Integer snapshotId = smosFile.getSnapshotIds()[snapshotSelector.getModel().getSliderModel().getValue()];
                    SmosBox.getInstance().getSnapshotSelectionService().setSelectedSnapshotId(getSelectedSmosProduct(),
                                                                                              snapshotId);
                }
            }
        }
    }

    class ToggleSnapshotModeAction implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent event) {
            if (getSelectedSnapshotId() != -1) {
                if (snapshotModeButton.isSelected()) {
                    locateSnapshotIdOfView();
                    locateSnapshotButton.setEnabled(true);
                } else {
                    locateSnapshotButton.setEnabled(false);
                }
                setSnapshotIdOfView();
            }
        }
    }

    class LocateSnapshotAction implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent event) {
            if (getSelectedSnapshotId() != -1) {
                locateSnapshotIdOfView();
            }
        }
    }

    class SSSL implements SnapshotSelectionService.SelectionListener {

        @Override
        public void handleSnapshotIdChanged(Product product, int oldId, int newId) {
            realizeSnapshotIdChange(product);
        }
    }
}