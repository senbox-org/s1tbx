package org.esa.beam.binning.operator.ui;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.swing.Grid;
import com.bc.ceres.swing.ListControlBar;
import org.apache.commons.lang.StringUtils;
import org.esa.beam.binning.AggregatorConfig;
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
import java.util.List;

/**
 * @author Norman Fomferra
 */
class AggregatorTableController extends ListControlBar.AbstractListController {

    static boolean isSourcePropertyName(String propertyName) {
        return propertyName.toLowerCase().contains("varname");
    }

    private final Grid grid;
    private final BinningFormModel binningFormModel;
    private final List<AggregatorItem> aggregatorItems;

    AggregatorTableController(Grid grid, BinningFormModel binningFormModel) {
        this.grid = grid;
        this.binningFormModel = binningFormModel;
        AggregatorConfig[] aggregatorConfigs = binningFormModel.getAggregatorConfigs();

        aggregatorItems = new ArrayList<>();
        for (AggregatorConfig aggregatorConfig : aggregatorConfigs) {
            addDataRow(new AggregatorItem(aggregatorConfig));
        }
    }

    AggregatorItem[] getAggregatorItems() {
        return aggregatorItems.toArray(new AggregatorItem[aggregatorItems.size()]);
    }

    @Override
    public boolean addRow(int index) {
        boolean ok = editAggregatorItem(new AggregatorItem(), -1);
        if (ok) {
            updateBinningFormModel();
        }
        return ok;
    }

    @Override
    public boolean removeRows(int[] indices) {
        grid.removeDataRows(indices);
        for (int i = indices.length - 1; i >= 0; i--) {
            aggregatorItems.remove(indices[i]);
        }
        updateBinningFormModel();
        return true;
    }

    @Override
    public boolean moveRowUp(int index) {
        grid.moveDataRowUp(index);

        AggregatorItem ac1 = aggregatorItems.get(index - 1);
        AggregatorItem ac2 = aggregatorItems.get(index);
        aggregatorItems.set(index - 1, ac2);
        aggregatorItems.set(index, ac1);

        updateBinningFormModel();

        return true;
    }

    @Override
    public boolean moveRowDown(int index) {
        grid.moveDataRowDown(index);

        AggregatorItem ac1 = aggregatorItems.get(index);
        AggregatorItem ac2 = aggregatorItems.get(index + 1);
        aggregatorItems.set(index, ac2);
        aggregatorItems.set(index + 1, ac1);

        updateBinningFormModel();

        return true;
    }

    private void updateBinningFormModel() {
        AggregatorConfig[] aggregatorConfigs = new AggregatorConfig[aggregatorItems.size()];
        for (int i = 0; i < aggregatorItems.size(); i++) {
            AggregatorItem aggregatorItem = aggregatorItems.get(i);
            aggregatorConfigs[i] = aggregatorItem.aggregatorConfig;
        }
        try {
            binningFormModel.setProperty(BinningFormModel.PROPERTY_KEY_AGGREGATOR_CONFIGS, aggregatorConfigs);
        } catch (ValidationException e) {
            JOptionPane.showMessageDialog(grid, e.getMessage(), "Aggregator Configuration", JOptionPane.ERROR_MESSAGE);
        }
    }

    private boolean editAggregatorItem(AggregatorItem aggregatorItem, int rowIndex) {
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

        ModalDialog aggregatorDialog = new AggregatorItemDialog(SwingUtilities.getWindowAncestor(grid), sourceNames, aggregatorItem);
        int result = aggregatorDialog.show();
        if (result == ModalDialog.ID_OK) {
            if (rowIndex < 0) {
                addDataRow(aggregatorItem);
            } else {
                updateDataRow(aggregatorItem, rowIndex);
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

    private void addDataRow(AggregatorItem ac) {
        EmptyBorder emptyBorder = new EmptyBorder(2, 2, 2, 2);

        JLabel typeLabel = new JLabel(getTypeText(ac));
        //typeLabel.setBackground(grid.getBackground().darker());
        typeLabel.setBorder(emptyBorder);

        JLabel sourceBandsLabel = new JLabel(getSourceBandsText(ac));
        //sourcesLabel.setBackground(grid.getBackground().darker());
        sourceBandsLabel.setBorder(emptyBorder);

        JLabel parametersLabel = new JLabel(getParametersText(ac));
        //parametersLabel.setBackground(grid.getBackground().darker());
        parametersLabel.setBorder(emptyBorder);

        JLabel targetBandsLabel = new JLabel(getTargetBandsText(ac));
        //targetsLabel.setBackground(grid.getBackground().darker());
        targetBandsLabel.setBorder(emptyBorder);

        final AbstractButton editButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("/org/esa/beam/resources/images/icons/Edit16.gif"),
                                                                         false);
        editButton.setRolloverEnabled(true);
        editButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                int rowIndex = grid.findDataRowIndex(editButton);
                editAggregatorItem(aggregatorItems.get(rowIndex), rowIndex);
            }
        });

        grid.addDataRow(
            /*1*/ typeLabel,
            /*2*/ sourceBandsLabel,
            /*3*/ parametersLabel,
            /*4*/ targetBandsLabel,
            /*5*/ editButton);

        aggregatorItems.add(ac);
    }

    private void updateDataRow(AggregatorItem ac, int rowIndex) {
        JComponent[] components = grid.getDataRow(rowIndex);
        ((JLabel) components[0]).setText(getTypeText(ac));
        ((JLabel) components[1]).setText(getSourceBandsText(ac));
        ((JLabel) components[2]).setText(getParametersText(ac));
        ((JLabel) components[3]).setText(getTargetBandsText(ac));
    }

    private String getTypeText(AggregatorItem ac) {
        return "<html><b>" + (ac.aggregatorConfig.getName()) + "</b>";
    }

    private String getSourceBandsText(AggregatorItem ac) {
        String[] sourceVarNames = ac.aggregatorDescriptor.getSourceVarNames(ac.aggregatorConfig);
        return sourceVarNames.length != 0 ? "<html>" + StringUtils.join(sourceVarNames, "<br/>") : "";
    }

    private String getTargetBandsText(AggregatorItem ac) {
        String[] targetVarNames = ac.aggregatorDescriptor.getTargetVarNames(ac.aggregatorConfig);
        return targetVarNames.length != 0 ? "<html>" + StringUtils.join(targetVarNames, "<br/>") : "";
    }

    private String getParametersText(AggregatorItem ac) {
        PropertySet container = ac.aggregatorConfig.asPropertySet();
        StringBuilder sb = new StringBuilder();
        for (Property property : container.getProperties()) {
            String propertyName = property.getName();
            if (!(isSourcePropertyName(propertyName) || propertyName.equals("type"))) {
                if (sb.length() > 0) {
                    sb.append("<br/>");
                }
                sb.append(String.format("%s = %s", propertyName, property.getValueAsText()));
            }
        }
        return "<html>" + sb.toString();
    }
}
