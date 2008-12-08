package org.esa.beam.smos.visat;

import com.bc.ceres.binio.CompoundData;
import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.glevel.support.DefaultMultiLevelImage;
import org.esa.beam.dataio.smos.GridPointValueProvider;
import org.esa.beam.dataio.smos.L1cGridPointValueProvider;
import org.esa.beam.dataio.smos.L1cScienceSmosFile;
import org.esa.beam.dataio.smos.SmosMultiLevelSource;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.framework.ui.tool.ToolButtonFactory;

import javax.swing.AbstractButton;
import javax.swing.DefaultBoundedRangeModel;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JSpinner;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.image.RenderedImage;
import java.io.IOException;

public class SnapshotInfoToolView extends SmosToolView {

    public static final String ID = SnapshotInfoToolView.class.getName();

    private SpinnerNumberModel snapshotSpinnerModel;
    private JSpinner snapshotSpinner;
    private JSlider snapshotSlider;
    private DefaultBoundedRangeModel snapshotSliderModel;

    private int snapshotId;
    private int snapshotIdMin;
    private int snapshotIdMax;
    private JTable snapshotTable;
    private L1cScienceSmosFile smosFile;
    private SnapshotTableModel nullModel;
    private SpinnerChangeListener snapshotSpinnerListener;
    private SliderChangeListener snapshotSliderListener;
    private JTextField snapshotIndexLabel;
    private AbstractButton toggleSnapshotModeButton;
    private AbstractButton locateSnapshotButton;

    public SnapshotInfoToolView() {
        nullModel = new SnapshotTableModel(new Object[0][0]);
    }

    @Override
    protected JComponent createClientComponent(ProductSceneView smosView) {

        snapshotSpinnerListener = new SpinnerChangeListener();
        snapshotSpinnerModel = new SpinnerNumberModel();
        snapshotSpinnerModel.setStepSize(1);
        snapshotSpinner = new JSpinner(snapshotSpinnerModel);
        ((JSpinner.DefaultEditor) snapshotSpinner.getEditor()).getTextField().setColumns(8);

        snapshotSliderListener = new SliderChangeListener();
        snapshotSliderModel = new DefaultBoundedRangeModel();
        snapshotSlider = new JSlider(snapshotSliderModel);

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

        snapshotIndexLabel = new JTextField(10);
        snapshotIndexLabel.setEditable(false);

        JPanel panel1 = new JPanel(new BorderLayout(2, 2));
        panel1.add(snapshotSpinner, BorderLayout.WEST);
        panel1.add(snapshotSlider, BorderLayout.CENTER);
        panel1.add(snapshotIndexLabel, BorderLayout.EAST);

        toggleSnapshotModeButton = ToolButtonFactory.createButton(new ImageIcon(SnapshotInfoToolView.class.getResource("Snapshot24.png")), true);
        toggleSnapshotModeButton.addActionListener(new ToggleSnapshotModeAction());
        locateSnapshotButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/ZoomTool24.gif"), false);
        locateSnapshotButton.addActionListener(new LocateSnapshotAction());

        JPanel panel2 = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2, 2));
        panel2.add(toggleSnapshotModeButton);
        panel2.add(locateSnapshotButton);

        JPanel panel3 = new JPanel(new BorderLayout(2, 2));
        panel3.add(panel1, BorderLayout.NORTH);
        panel3.add(new JScrollPane(snapshotTable), BorderLayout.CENTER);
        panel3.add(panel2, BorderLayout.SOUTH);

        return panel3;
    }

    @Override
    protected void updateClientComponent(ProductSceneView smosView) {
        boolean enabled = smosView != null && getSelectedSmosFile() instanceof L1cScienceSmosFile;

        snapshotSpinner.removeChangeListener(snapshotSpinnerListener);
        snapshotSlider.removeChangeListener(snapshotSliderListener);
        if (enabled) {
            smosFile = (L1cScienceSmosFile) getSelectedSmosFile();
            setSnapshotIdRange(smosFile.getSnapshotIdMin(), smosFile.getSnapshotIdMax());
            setSnapshotId(smosFile.getSnapshotIdMin());
            snapshotSpinner.addChangeListener(snapshotSpinnerListener);
            snapshotSlider.addChangeListener(snapshotSliderListener);
        } else {
            smosFile = null;
        }

        snapshotSpinner.setEnabled(enabled);
        snapshotSlider.setEnabled(enabled);
        snapshotTable.setEnabled(enabled);
        toggleSnapshotModeButton.setEnabled(enabled);
        locateSnapshotButton.setEnabled(enabled);
    }

    public void setSnapshotIdRange(int min, int max) {
        if (snapshotIdMin != min || snapshotIdMax != max) {
            this.snapshotIdMin = min;
            this.snapshotIdMax = max;
            snapshotSpinnerModel.setMinimum(min);
            snapshotSpinnerModel.setMaximum(max);
            snapshotSliderModel.setMinimum(min);
            snapshotSliderModel.setMaximum(max);
            if (snapshotId < min) {
                setSnapshotIdNoUpdate(min);
            } else if (snapshotId > max) {
                setSnapshotIdNoUpdate(max);
            }
            updateLabel();
        }
    }

    private void updateLabel() {
        snapshotIndexLabel.setText((snapshotId - snapshotIdMin + 1) + "/" + (snapshotIdMax - snapshotIdMin + 1));
    }

    public void setSnapshotId(int snapshotId) {
        if (this.snapshotId != snapshotId) {
            setSnapshotIdNoUpdate(snapshotId);

            int snapshotIndex = smosFile.getSnapshotIndex(snapshotId);
            if (snapshotIndex != -1) {
                try {
                    updateTable(snapshotIndex);
                    if (toggleSnapshotModeButton.isSelected()) {
                        updateImage();
                    }
                } catch (IOException e) {
                    snapshotTable.setModel(nullModel);
                }
            } else {
                snapshotTable.setModel(nullModel);
            }
            updateLabel();
        }
    }

    private void setSnapshotIdNoUpdate(int snapshotId) {
        this.snapshotId = snapshotId;
        snapshotSpinnerModel.setValue(snapshotId);
        snapshotSliderModel.setValue(snapshotId);
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

    private void updateImage() {
        ProductSceneView sceneView = getSelectedSmosView();
        ImageLayer imageLayer = sceneView.getBaseImageLayer();
        RenderedImage sourceImage = sceneView.getRaster().getSourceImage();
        if (sourceImage instanceof DefaultMultiLevelImage) {
            DefaultMultiLevelImage defaultMultiLevelImage = (DefaultMultiLevelImage) sourceImage;
            if (defaultMultiLevelImage.getSource() instanceof SmosMultiLevelSource) {
                SmosMultiLevelSource smosMultiLevelSource = (SmosMultiLevelSource) defaultMultiLevelImage.getSource();
                GridPointValueProvider gridPointValueProvider = smosMultiLevelSource.getGridPointValueProvider();
                if (gridPointValueProvider instanceof L1cGridPointValueProvider) {
                    L1cGridPointValueProvider l1cGridPointValueProvider = (L1cGridPointValueProvider) gridPointValueProvider;
                    int id = toggleSnapshotModeButton.isSelected() ? snapshotId : -1;
                    if (l1cGridPointValueProvider.getSnapshotId() != id) {
                        l1cGridPointValueProvider.setSnapshotId(id);
                        smosMultiLevelSource.reset();
                        sceneView.getRaster().setValidMaskImage(null);
                        imageLayer.regenerate();
                    }
                }
            }
        }
    }

    private class SpinnerChangeListener implements ChangeListener {
        @Override
        public void stateChanged(ChangeEvent e) {
            setSnapshotId(snapshotSpinnerModel.getNumber().intValue());
        }
    }

    private class SliderChangeListener implements ChangeListener {
        @Override
        public void stateChanged(ChangeEvent e) {
            setSnapshotId(snapshotSliderModel.getValue());
        }
    }

    static class LocateSnapshotAction implements ActionListener {

        public void actionPerformed(ActionEvent e) {

        }
    }

    class ToggleSnapshotModeAction implements ActionListener {

        public void actionPerformed(ActionEvent e) {
            updateImage();
        }

    }
}