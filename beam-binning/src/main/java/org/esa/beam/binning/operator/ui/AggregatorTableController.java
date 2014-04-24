package org.esa.beam.binning.operator.ui;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.swing.Grid;
import com.bc.ceres.swing.ListControlBar;
import org.apache.commons.lang.StringUtils;
import org.esa.beam.binning.AggregatorConfig;
import org.esa.beam.binning.AggregatorDescriptor;
import org.esa.beam.binning.aggregators.AggregatorAverage;
import org.esa.beam.binning.operator.VariableConfig;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.tool.ToolButtonFactory;

import javax.swing.AbstractButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * @author Norman Fomferra
 */
class AggregatorTableController extends ListControlBar.AbstractListController {

    static boolean isSourcePropertyName(String propertyName) {
        return propertyName.toLowerCase().contains("varname");
    }

    static class ACWrapper {

        AggregatorDescriptor aggregatorDescriptor;
        AggregatorConfig aggregatorConfig;

        ACWrapper() {
            this.aggregatorDescriptor = new AggregatorAverage.Descriptor();
            this.aggregatorConfig = aggregatorDescriptor.createConfig();
        }

    }

    final Grid grid;
    private final BinningFormModel binningFormModel;
    final List<ACWrapper> aggregatorConfigs;

    AggregatorTableController(Grid grid, BinningFormModel binningFormModel) {
        this(grid, binningFormModel, new ACWrapper[0]);

    }

    AggregatorTableController(Grid grid, BinningFormModel binningFormModel, ACWrapper... acs) {
        this.grid = grid;
        this.binningFormModel = binningFormModel;
        aggregatorConfigs = new ArrayList<>(Arrays.asList(acs));
        for (ACWrapper ac : acs) {
            addDataRow(ac);
        }
    }

    ACWrapper[] getAggregatorConfigs() {
        return aggregatorConfigs.toArray(new ACWrapper[aggregatorConfigs.size()]);
    }

    @Override
    public boolean addRow(int index) {
        return editAgg(new ACWrapper(), -1);
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

        ACWrapper ac1 = aggregatorConfigs.get(index - 1);
        ACWrapper ac2 = aggregatorConfigs.get(index);
        aggregatorConfigs.set(index - 1, ac2);
        aggregatorConfigs.set(index, ac1);

        return true;
    }

    @Override
    public boolean moveRowDown(int index) {
        grid.moveDataRowDown(index);

        ACWrapper ac1 = aggregatorConfigs.get(index);
        ACWrapper ac2 = aggregatorConfigs.get(index + 1);
        aggregatorConfigs.set(index, ac2);
        aggregatorConfigs.set(index + 1, ac1);

        return true;
    }

    private boolean editAgg(ACWrapper ac, int rowIndex) {
        Product contextProduct = binningFormModel.getContextProduct();
        if (contextProduct == null) {
            JOptionPane.showMessageDialog(grid, "Please select source products before adding aggregators.", "", JOptionPane.INFORMATION_MESSAGE);
            return false;
        }
        String[] varNames = getVariableNames(binningFormModel.getVariableConfigs());
        String[] bandNames = contextProduct.getBandNames();
        String[] tiePointGridNames = contextProduct.getTiePointGridNames();
        String[] maskNames = contextProduct.getMaskGroup().getNodeNames();
        String[] sourceNames = org.esa.beam.util.StringUtils.addArrays(varNames, bandNames);
        sourceNames = org.esa.beam.util.StringUtils.addArrays(sourceNames, tiePointGridNames);
        sourceNames = org.esa.beam.util.StringUtils.addArrays(sourceNames, maskNames);

        ModalDialog aggregatorDialog = new AggregatorConfigEditDialog(SwingUtilities.getWindowAncestor(grid), sourceNames, ac);
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

    private String[] getVariableNames(VariableConfig[] variableConfigs) {
        String[] varNames = new String[variableConfigs.length];
        for (int i = 0; i < variableConfigs.length; i++) {
            varNames[i] = variableConfigs[i].getName();
        }
        return varNames;
    }

    private void addDataRow(ACWrapper ac) {
        EmptyBorder emptyBorder = new EmptyBorder(2, 2, 2, 2);

        JLabel typeLabel = new JLabel(getTypeText(ac));
        //typeLabel.setBackground(grid.getBackground().darker());
        typeLabel.setBorder(emptyBorder);

        JLabel sourcesLabel = new JLabel(getSourcesText(ac));
        //sourcesLabel.setBackground(grid.getBackground().darker());
        sourcesLabel.setBorder(emptyBorder);

        JLabel targetsLabel = new JLabel(getTargetsText(ac));
        //targetsLabel.setBackground(grid.getBackground().darker());
        targetsLabel.setBorder(emptyBorder);

        JLabel parametersLabel = new JLabel(getParametersText(ac));
        //parametersLabel.setBackground(grid.getBackground().darker());
        parametersLabel.setBorder(emptyBorder);

        final AbstractButton editButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("/org/esa/beam/resources/images/icons/Edit16.gif"),
                                                                         false);
        editButton.setRolloverEnabled(true);
        editButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int rowIndex = grid.findDataRowIndex(editButton);
                editAgg(aggregatorConfigs.get(rowIndex), rowIndex);
            }
        });

        grid.addDataRow(
            /*1*/ typeLabel,
            /*2*/ sourcesLabel,
            /*3*/ targetsLabel,
            /*4*/ parametersLabel,
            /*5*/ editButton);

        aggregatorConfigs.add(ac);

    }

    private void updateDataRow(ACWrapper ac, int rowIndex) {
        JComponent[] components = grid.getDataRow(rowIndex);
        ((JLabel) components[0]).setText(getTypeText(ac));
        ((JLabel) components[1]).setText(getSourcesText(ac));
        ((JLabel) components[2]).setText(getTargetsText(ac));
        ((JLabel) components[3]).setText(getParametersText(ac));
    }

    private String getTypeText(ACWrapper ac) {
        return "<html><b>" + (ac.aggregatorConfig.getName()) + "</b>";
    }

    private String getSourcesText(ACWrapper ac) {
        String[] sourceVarNames = ac.aggregatorDescriptor.getSourceVarNames(ac.aggregatorConfig);
        return sourceVarNames.length != 0 ? "<html>" + StringUtils.join(sourceVarNames, "<br/>") : "";
    }

    private String getTargetsText(ACWrapper ac) {
        String[] targetVarNames = ac.aggregatorDescriptor.getTargetVarNames(ac.aggregatorConfig);
        return targetVarNames.length != 0 ? "<html>" + StringUtils.join(targetVarNames, "<br/>") : "";
    }

    private String getParametersText(ACWrapper ac) {
        PropertySet container = ac.aggregatorConfig.asPropertySet();
        StringBuilder sb = new StringBuilder();
        for (Property property : container.getProperties()) {
            if (!isSourcePropertyName(property.getName()) || property.getName().toLowerCase().equals("type")) {
                if (sb.length() > 0)  {
                    sb.append("<br/>");
                }
                sb.append(String.format("%s = %s", property.getName(), property.getValueAsText()));
            }
        }
        return "<html>" + sb.toString();
    }
}
