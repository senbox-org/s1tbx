package org.esa.beam.binning.operator.ui;

import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.swing.Grid;
import com.bc.ceres.swing.ListControlBar;
import com.bc.ceres.swing.binding.PropertyPane;
import org.apache.commons.lang.StringUtils;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.ParameterDescriptorFactory;
import org.esa.beam.framework.ui.ModalDialog;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Norman Fomferra
 */
class AggregatorTableController implements ListControlBar.ListController {

    static class AC {
        @Parameter(valueSet = {"AVG", "PERCENTILE", "ON_MAX_SET", "MIN_MAX", "COUNT"}, defaultValue = "AVG")
        String type;
        @Parameter
        String[] varNames;
        @Parameter
        String[] targetNames;
        @Parameter
        double weight;
        @Parameter
        int iterations;
    }

    final Grid grid;
    final List<AC> aggregatorConfigs;

    AggregatorTableController(Grid grid, AC... acs) {
        this.grid = grid;
        aggregatorConfigs = new ArrayList<>(Arrays.asList(acs));
        for (AC ac : acs) {
            addDataRow(ac);
        }
    }

    @Override
    public boolean addRow(int index) {
        AC ac = new AC();
        return editAgg(ac, -1);
    }

    @Override
    public boolean removeRows(int[] indices) {
        grid.removeDataRows(indices);
        for (int i = indices.length - 1; i >= 0; i--) {
            aggregatorConfigs.remove(indices[i]);
        }
        return true;
    }

    @Override
    public boolean moveRowUp(int index) {
        grid.moveDataRowUp(index);

        AC ac1 = aggregatorConfigs.get(index - 1);
        AC ac2 = aggregatorConfigs.get(index);
        aggregatorConfigs.set(index - 1, ac2);
        aggregatorConfigs.set(index, ac1);

        return true;
    }

    @Override
    public boolean moveRowDown(int index) {
        grid.moveDataRowDown(index);

        AC ac1 = aggregatorConfigs.get(index);
        AC ac2 = aggregatorConfigs.get(index + 1);
        aggregatorConfigs.set(index, ac2);
        aggregatorConfigs.set(index + 1, ac1);

        return true;
    }

    private boolean editAgg(AC ac, int rowIndex) {
        PropertyContainer propertyContainer = PropertyContainer.createObjectBacked(ac, new ParameterDescriptorFactory());
        PropertyPane propertyPane = new PropertyPane(propertyContainer);
        ModalDialog aggregatorDialog = new ModalDialog(SwingUtilities.getWindowAncestor(grid), "Aggregator", ModalDialog.ID_OK_CANCEL, null);
        aggregatorDialog.setContent(propertyPane.createPanel());
        int result = aggregatorDialog.show();
        if (result == ModalDialog.ID_OK) {
            if (rowIndex < 0) {
                addDataRow(ac);
            } else {
                updateDataRow(ac, rowIndex);
            }
            return true;
        }
        return false;
    }

    private void addDataRow(AC ac) {
        final JButton editButton = new JButton("...");
        editButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int rowIndex = grid.findDataRowIndex(editButton);
                editAgg(aggregatorConfigs.get(rowIndex), rowIndex);
            }
        });
        grid.addDataRow(
            /*1*/ new JLabel(getTypeText(ac)),
            /*2*/ new JLabel(getSourcesText(ac)),
            /*3*/ new JLabel(getTargetsText(ac)),
            /*4*/ new JLabel(getParametersText(ac)),
            /*5*/ editButton);
        aggregatorConfigs.add(ac);
    }

    private void updateDataRow(AC ac, int rowIndex) {
        JComponent[] components = grid.getDataRow(rowIndex);
        ((JLabel) components[0]).setText(getTypeText(ac));
        ((JLabel) components[1]).setText(getSourcesText(ac));
        ((JLabel) components[2]).setText(getTargetsText(ac));
        ((JLabel) components[3]).setText(getParametersText(ac));
    }

    private String getTypeText(AC ac) {
        return "<html><b>" + ac.type + "</b>";
    }

    private String getSourcesText(AC ac) {
        return "<html>" + StringUtils.join(ac.varNames, "<br/>");
    }

    private String getTargetsText(AC ac) {
        return "<html>" + StringUtils.join(ac.targetNames, "<br/>");
    }

    private String getParametersText(AC ac) {
        String parametersValue = "<html>";
        parametersValue += "weight=" + ac.weight;
        parametersValue += "<br/>";
        parametersValue += "iterations=" + ac.iterations;
        return parametersValue;
    }
}
