package org.esa.s1tbx.sar.gpf.ui;

import org.esa.snap.graphbuilder.gpf.ui.BaseOperatorUI;
import org.esa.snap.graphbuilder.gpf.ui.UIValidation;
import org.esa.snap.graphbuilder.rcp.utils.DialogUtils;
import org.esa.snap.ui.AppContext;

import javax.swing.*;
import java.awt.*;
import java.util.Map;

// UI for the PyRate Export.
public class PyRateExportUI extends BaseOperatorUI {


    /*
        UI components for PyRATE
     */

    private final JLabel processingLocationLabel = new JLabel("The output folder to which the input to PyRate will be written");
    private final JFileChooser processingLocationFileChooser = new JFileChooser();

    private final JLabel snaphuInstallLocationLabel = new JLabel("Install location, or location of existing binary for SNAPHU");
    private final JFileChooser snaphuInstallLocationFileChooser = new JFileChooser();

    private final JButton displaySnaphuExportParamsBtn = new JButton("Show SNAPHU params");

    private final JLabel numProcessorsLbl = new JLabel("Number of Processors (Set to 1 for serial)");
    private final JTextField numProcessorsTxtField = new JTextField("4");

    private final JCheckBox displayVerboseSNAPHUChkBox = new JCheckBox("Display verbose output of SNAPHU in this tool");



    /*
        UI components for the expandable SNAPHU Export default parameters.
     */
    private final JFrame snaphuExportDefaultParams = new JFrame("SNAPHU utility parameters");

    private final JLabel snaphuStatCostMethodLbl = new JLabel("Statistical-cost method");
    private final JComboBox snaphuStatCostMethodComboBox = new JComboBox(new String[]{"TOPO", "DEFO", "SMOOTH", "NOSTATCOST"});

    private final JLabel snaphuInitialMethodLbl = new JLabel("Initial method");
    private final JComboBox snaphuInitialMethodComboBox = new JComboBox(new String[]{"MST", "MCF"});

    private final JLabel snaphuNumTileRowsLbl = new JLabel("Number of Tile Rows");
    private final JTextField snaphuNumTileRowsTxtField = new JTextField("10");

    private final JLabel snaphuNumTileColsLbl = new JLabel("Number of Tile Columns");
    private final JTextField snaphuNumTileColsTxtField = new JTextField("10");

    private final JLabel snaphuRowOverlapLbl = new JLabel("Row overlap");
    private final JTextField snaphuRowOverlapTxtField = new JTextField("0");

    private final JLabel snaphuColumnOverlapLbl = new JLabel("Column overlap");
    private final JTextField snaphuColumnOverlapTxtField = new JTextField("0");

    private final JLabel snaphuTileCostThresholdLbl = new JLabel("Tile Cost Threshold");
    private final JTextField snaphuTileCostThresholdTxtField = new JTextField("600");

    /*
        End of UI components for SNAPHU Unwrap
     */








    @Override
    public JComponent CreateOpTab(String operatorName, Map<String, Object> parameterMap, AppContext appContext) {

        initializeOperatorUI(operatorName, parameterMap);
        final JComponent panel = createPanel();
        initParameters();
        return null;
    }

    @Override
    public void initParameters() {
        snaphuStatCostMethodComboBox.setSelectedIndex(1);
        snaphuInitialMethodComboBox.setSelectedIndex(1);

    }

    @Override
    public UIValidation validateParameters() {
        return null;
    }

    @Override
    public void updateParameters() {

    }

    private JComponent createPanel(){
        final JPanel contentPane = new JPanel(new GridBagLayout());
        final GridBagConstraints gbc = DialogUtils.createGridBagConstraints();




        return null;
    }
}
