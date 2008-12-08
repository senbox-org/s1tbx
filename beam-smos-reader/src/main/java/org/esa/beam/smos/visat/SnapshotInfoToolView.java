package org.esa.beam.smos.visat;

import com.bc.ceres.binio.CompoundData;
import com.bc.ceres.core.Assert;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.glevel.MultiLevelSource;
import org.esa.beam.dataio.smos.L1cScienceSmosFile;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.framework.ui.tool.ToolButtonFactory;
import org.esa.beam.glevel.BandImageMultiLevelSource;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.table.DefaultTableCellRenderer;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.image.RenderedImage;
import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.List;

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
    private AbstractAction locateSnapshotAction;
    private AbstractAction toggleSnapshotModeAction;

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

        toggleSnapshotModeAction = new ToggleSnapshotModeAction();
        AbstractButton snapshotModeButton = ToolButtonFactory.createButton(toggleSnapshotModeAction, true);

        locateSnapshotAction = new LocateSnapshotAction();
        AbstractButton locateSnapshotButton = ToolButtonFactory.createButton(locateSnapshotAction, true);

        JPanel panel2 = new JPanel(new FlowLayout(FlowLayout.RIGHT, 2,2));
        panel2.add(snapshotModeButton);
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
        locateSnapshotAction.setEnabled(enabled);
        toggleSnapshotModeAction.setEnabled(enabled);
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
        snapshotIndexLabel.setText((snapshotId - snapshotIdMin + 1) + "/" +(snapshotIdMax-snapshotIdMin + 1));
    }

    public void setSnapshotId(int snapshotId) {
        if (this.snapshotId != snapshotId) {
            setSnapshotIdNoUpdate(snapshotId);

            int snapshotIndex = smosFile.getSnapshotIndex(snapshotId);
            if (snapshotIndex != -1) {
                try {
                    updateTable(snapshotIndex);
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

    static class LocateSnapshotAction extends AbstractAction {
        LocateSnapshotAction() {
            putValue(Action.NAME, "Locate Snapshot");
        }

        public void actionPerformed(ActionEvent e) {

        }
    }

    class ToggleSnapshotModeAction extends AbstractAction {
        ToggleSnapshotModeAction() {
            putValue(Action.NAME, "Snapshot Mode");
        }

        public void actionPerformed(ActionEvent e) {
            
            ProductSceneView sceneView = getSelectedSmosView();
            ImageLayer imageLayer = sceneView.getBaseImageLayer();
            RenderedImage sourceImage = sceneView.getRaster().getSourceImage();
            if (sourceImage instanceof MultiLevelSource) {
                MultiLevelSource multiLevelSource = (MultiLevelSource) sourceImage;
                multiLevelSource.reset();
            }
            RenderedImage validMaskImage = sceneView.getRaster().getValidMaskImage();
            if (validMaskImage instanceof MultiLevelSource) {
                MultiLevelSource multiLevelSource = (MultiLevelSource) validMaskImage;
                multiLevelSource.reset();
            }
            imageLayer.regenerate();
        }
    }
}