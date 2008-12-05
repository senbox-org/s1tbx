package org.esa.beam.smos.visat;

import com.bc.ceres.binio.CompoundData;
import org.esa.beam.dataio.smos.SmosFile;
import org.esa.beam.framework.ui.product.ProductSceneView;

import javax.swing.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.BorderLayout;
import java.io.IOException;

public class SmosSnapshotImageToolView extends SmosToolView {

    public static final String ID = SmosSnapshotImageToolView.class.getName();
    private SpinnerNumberModel snapshotSpinnerModel;
    private JSpinner snapshotSpinner;
    private JSlider snapshotSlider;
    private DefaultBoundedRangeModel snapshotSliderModel;

    int snapshotId;
    int snapshotIdMin;
    int snapshotIdMax;
    private JTable snapshotTable;
    private SmosFile smosFile;
    private SnapshotTableModel nullModel;
    private SmosSnapshotImageToolView.SpinnerChangeListener snapshotSpinnerListener;
    private SmosSnapshotImageToolView.SliderChangeListener snapshotSliderListener;

    public SmosSnapshotImageToolView() {
        nullModel = new SnapshotTableModel(new Object[0][0]);
    }

    @Override
    protected JComponent createSmosComponent(ProductSceneView smosView) {

        snapshotSpinnerListener = new SpinnerChangeListener();
        snapshotSpinnerModel = new SpinnerNumberModel();
        snapshotSpinnerModel.setStepSize(1);
        snapshotSpinner = new JSpinner(snapshotSpinnerModel);
        ((JSpinner.DefaultEditor) snapshotSpinner.getEditor()).getTextField().setColumns(8);

        snapshotSliderListener = new SliderChangeListener();
        snapshotSliderModel = new DefaultBoundedRangeModel();
        snapshotSlider = new JSlider(snapshotSliderModel);

        snapshotTable = new JTable(nullModel);

        JPanel panel1 = new JPanel(new BorderLayout(2, 2));
        panel1.add(snapshotSpinner, BorderLayout.WEST);
        panel1.add(snapshotSlider, BorderLayout.CENTER);

        JPanel panel2 = new JPanel(new BorderLayout(2, 2));
        panel2.add(panel1, BorderLayout.NORTH);
        panel2.add(new JScrollPane(snapshotTable), BorderLayout.CENTER);

        return panel2;
    }

    @Override
    protected void updateSmosComponent(ProductSceneView oldView, ProductSceneView newView) {


        snapshotSpinner.removeChangeListener(snapshotSpinnerListener);
        snapshotSlider.removeChangeListener(snapshotSliderListener);
        if (newView != null && getSmosProductReader().getSmosFile().getSnapshotList() != null) {
            smosFile = getSmosProductReader().getSmosFile();
            setSnapshotIdRange(smosFile.getSnapshotIdMin(), smosFile.getSnapshotIdMax());
            setSnapshotId(smosFile.getSnapshotIdMin());
            snapshotSpinner.addChangeListener(snapshotSpinnerListener);
            snapshotSlider.addChangeListener(snapshotSliderListener);
        } else {
            smosFile = null;
        }

        snapshotSpinner.setEnabled(smosFile != null);
        snapshotSlider.setEnabled(smosFile != null);
        snapshotTable.setEnabled(smosFile != null);
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
        }
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
                tableData[i][1] = GridPointDataset.getNumbericMember(data, i);
            } else {
                tableData[i][1] = data.getCompoundType().getMemberType(i).getName();
            }
        }
        snapshotTable.setModel(new SnapshotTableModel(tableData));
    }

    private class SpinnerChangeListener implements ChangeListener {
        public void stateChanged(ChangeEvent e) {
            setSnapshotId(snapshotSpinnerModel.getNumber().intValue());
        }
    }

    private class SliderChangeListener implements ChangeListener {
        public void stateChanged(ChangeEvent e) {
            setSnapshotId(snapshotSliderModel.getValue());
        }
    }
}