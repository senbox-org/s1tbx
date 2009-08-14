package org.esa.beam.gpf.common.reproject.ui;

import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.binding.swing.BindingContext;
import com.bc.ceres.swing.TableLayout;

import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.Container;
import java.awt.Insets;


/**
 * @author Marco Peters
 * @version $ Revision $ Date $
 * @since BEAM 4.6
 */
public class GridDefinitionForm extends JPanel{
    private GridDefinitionFormModel model;

    // for testing the UI
    public static void main(String[] args) {
        final JFrame jFrame = new JFrame("Grid Definition Form");
        Container contentPane = jFrame.getContentPane();
        GridDefinitionFormModel model = new GridDefinitionFormModel(0, 0, 0.003, 0.003, "°");
        GridDefinitionForm projectedCRSSelectionForm = new GridDefinitionForm(model);
        contentPane.add(projectedCRSSelectionForm);
        jFrame.setSize(300, 150);
        jFrame.setLocationRelativeTo(null);
        jFrame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                jFrame.setVisible(true);
            }
        });
    }

    public GridDefinitionForm(GridDefinitionFormModel model) {
        this.model = model;
        creatUI();
    }

    private void creatUI() {
        // row 0
        final JLabel pixelSizeXLabel = new JLabel("Pixel Size X:");
        final JTextField pixelSizeXField = new JTextField();
        final JLabel pixelSizeXUnitLabel = new JLabel(model.getUnit());
        final JLabel pixelSizeYLabel = new JLabel("Pixel Size Y:");
        final JTextField pixelSizeYField = new JTextField();
        final JLabel pixelSizeYUnitLabel = new JLabel(model.getUnit());
        // row 1
        final JCheckBox adjustGrid = new JCheckBox("Adjust grid size to source region");
        // row 2
        final JLabel gridWidthLabel = new JLabel("Grid Width:");
        final JTextField gridWidthField = new JTextField();
        final JLabel gridHeightLabel = new JLabel("Grid Height:");
        final JTextField gridHeightField = new JTextField();


        final TableLayout tableLayout = new TableLayout(6);
        setLayout(tableLayout);
        tableLayout.setTableAnchor(TableLayout.Anchor.NORTHWEST);
        tableLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
        tableLayout.setTablePadding(4, 4);
        tableLayout.setColumnPadding(3, new Insets(4, 10, 4, 4));
        tableLayout.setCellWeightX(0, 1, 1.0);
        tableLayout.setCellWeightX(0, 4, 1.0);
        tableLayout.setCellColspan(1, 0, 6);
        tableLayout.setCellWeightX(2, 1, 1.0);
        tableLayout.setCellWeightX(2, 4, 1.0);

        add(pixelSizeXLabel);
        add(pixelSizeXField);
        add(pixelSizeXUnitLabel);
        add(pixelSizeYLabel);
        add(pixelSizeYField);
        add(pixelSizeYUnitLabel);

        add(adjustGrid);

        add(gridWidthLabel);
        add(gridWidthField);
        add(gridHeightLabel, new TableLayout.Cell(2, 3));   // jump over unit column
        add(gridHeightField);

        final ValueContainer valueContainer = ValueContainer.createObjectBacked(model);
        final BindingContext context = new BindingContext(valueContainer);
        context.bind(GridDefinitionFormModel.PIXEL_SIZE_X_NAME, pixelSizeXField);
        context.bind(GridDefinitionFormModel.PIXEL_SIZE_Y_NAME, pixelSizeYField);
        context.bind(GridDefinitionFormModel.ADJUST_SIZE_TO_SOURCE_REGION_NAME, adjustGrid);
        context.bind(GridDefinitionFormModel.GRID_WIDTH_NAME, gridWidthField);
        context.bind(GridDefinitionFormModel.GRID_HEIGHT_NAME, gridHeightField);
        context.bindEnabledState(GridDefinitionFormModel.GRID_WIDTH_NAME, false,
                                 GridDefinitionFormModel.ADJUST_SIZE_TO_SOURCE_REGION_NAME, true);
        context.bindEnabledState(GridDefinitionFormModel.GRID_HEIGHT_NAME, false,
                                 GridDefinitionFormModel.ADJUST_SIZE_TO_SOURCE_REGION_NAME, true);

    }
}
