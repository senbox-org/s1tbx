
package org.esa.s1tbx.fex.gpf.ui.decisiontree;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.graphbuilder.gpf.ui.BaseOperatorUI;
import org.esa.snap.graphbuilder.gpf.ui.UIValidation;
import org.esa.snap.ui.AppContext;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

/**
 * User interface for Decision Tree Operator
 */
public class DecisionTreeOpUI extends BaseOperatorUI {

    private Product targetProduct = null;
    private JComponent panel = null;
    private String errorText = "";

    private final EditNodePanel editPanel = new EditNodePanel();
    private final DecisionTreePanel treePanel = new DecisionTreePanel(editPanel);
    private final ControlPanel controlPanel = new ControlPanel(treePanel);

    @Override
    public JComponent CreateOpTab(String operatorName, Map<String, Object> parameterMap, AppContext appContext) {

        initializeOperatorUI(operatorName, parameterMap);
        panel = createUI();
        initParameters();

        editPanel.addListener(new EditNodeListener());

        return panel;
    }

    @Override
    public void initParameters() {

        final Object[] decisionTreeArray = (Object[]) paramMap.get("decisionTree");
        treePanel.createNewTree(decisionTreeArray);

        if (sourceProducts != null) {
            editPanel.setProducts(sourceProducts);
        }
    }

    @Override
    public UIValidation validateParameters() {
        return new UIValidation(UIValidation.State.OK, "");
    }

    @Override
    public void updateParameters() {

        paramMap.put("decisionTree", treePanel.getTreeAsArray());
    }

    private JComponent createUI() {

        final JPanel gridPanel = new JPanel(new BorderLayout());
        gridPanel.add(treePanel, BorderLayout.CENTER);
        gridPanel.add(controlPanel, BorderLayout.EAST);
        gridPanel.add(editPanel, BorderLayout.SOUTH);

        return gridPanel;
    }

    public class EditNodeListener implements EditNodePanel.EditNodeListener {
        public void notifyMSG() {
            treePanel.repaint();
        }
    }
}