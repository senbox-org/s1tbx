package org.esa.beam.gpf.common.reproject.ui;

import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.binding.swing.BindingContext;
import com.bc.ceres.swing.TableLayout;
import static org.esa.beam.gpf.common.reproject.ui.GridDefinitionFormModel.*;
import org.esa.beam.util.math.MathUtils;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.metadata.extent.GeographicBoundingBox;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import javax.swing.JCheckBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.Container;
import java.awt.Insets;
import java.awt.Rectangle;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;


/**
 * @author Marco Peters
 * @version $ Revision $ Date $
 * @since BEAM 4.6
 */
public class GridDefinitionForm extends JPanel {
    private GridDefinitionFormModel model;
    private ValueContainer valueContainer;

    // for testing the UI
    public static void main(String[] args) throws FactoryException {
        final JFrame jFrame = new JFrame("Grid Definition Form");
        Container contentPane = jFrame.getContentPane();
        Rectangle sourceDimension = new Rectangle(200, 300);
        CoordinateReferenceSystem sourceCrs = DefaultGeographicCRS.WGS84;
        CoordinateReferenceSystem targetCrs = CRS.decode("EPSG:32632");
        GridDefinitionFormModel model = new GridDefinitionFormModel(sourceDimension, sourceCrs, targetCrs,
                                                                    "°");
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
        updateGridDiemension();
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

        valueContainer = ValueContainer.createObjectBacked(model);
        final BindingContext context = new BindingContext(valueContainer);
        context.bind(PIXEL_SIZE_X_NAME, pixelSizeXField);
        context.bind(PIXEL_SIZE_Y_NAME, pixelSizeYField);
        context.bind(ADJUST_SIZE_TO_SOURCE_REGION_NAME, adjustGrid);
        context.bind(GRID_WIDTH_NAME, gridWidthField);
        context.bind(GRID_HEIGHT_NAME, gridHeightField);
        context.bindEnabledState(GRID_WIDTH_NAME, false,
                                 ADJUST_SIZE_TO_SOURCE_REGION_NAME, true);
        context.bindEnabledState(GRID_HEIGHT_NAME, false,
                                 ADJUST_SIZE_TO_SOURCE_REGION_NAME, true);

        context.addPropertyChangeListener(new PixelSizeChangedListener());
    }

    private void updateGridDiemension() {
        final CoordinateReferenceSystem sourceCrs = model.getSourceCrs();
        if (sourceCrs != null) {
            final int sourceW = model.getSourceDimension().width;
            final int sourceH = model.getSourceDimension().height;

            GeographicBoundingBox geoBBox = CRS.getGeographicBoundingBox(sourceCrs);
            double mapW = geoBBox.getNorthBoundLatitude() - geoBBox.getSouthBoundLatitude();
            double mapH = geoBBox.getEastBoundLongitude() - geoBBox.getWestBoundLongitude();

            float pixelSize = (float) Math.min(mapW / sourceW, mapH / sourceH);
            if (MathUtils.equalValues(pixelSize, 0.0f)) {
                pixelSize = 1.0f;
            }
            final int targetW = 1 + (int) Math.floor(mapW / pixelSize);
            final int targetH = 1 + (int) Math.floor(mapH / pixelSize);
            valueContainer.setValue(GRID_WIDTH_NAME, targetW);
            valueContainer.setValue(GRID_HEIGHT_NAME, targetH);
        }
    }

    private class PixelSizeChangedListener implements PropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            String propertyName = evt.getPropertyName();
            boolean pixelSizeChanged = PIXEL_SIZE_X_NAME.equals(propertyName) ||
                    PIXEL_SIZE_Y_NAME.equals(propertyName);
            boolean adjustGridEnabled = Boolean.TRUE.equals(valueContainer.getValue(ADJUST_SIZE_TO_SOURCE_REGION_NAME));
            if (pixelSizeChanged && adjustGridEnabled) {
                updateGridDiemension();
            }
        }
    }
}
