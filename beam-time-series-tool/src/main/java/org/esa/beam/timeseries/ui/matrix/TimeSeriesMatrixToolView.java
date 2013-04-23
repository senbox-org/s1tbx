/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 * 
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 * 
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.beam.timeseries.ui.matrix;

import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.glayer.swing.LayerCanvas;
import com.bc.ceres.swing.TableLayout;
import com.jidesoft.grid.JideTable;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.ProductNodeEvent;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.help.HelpSys;
import org.esa.beam.framework.ui.PixelPositionListener;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.framework.ui.application.PageComponentDescriptor;
import org.esa.beam.framework.ui.application.support.AbstractToolView;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.esa.beam.framework.ui.tool.ToolButtonFactory;
import org.esa.beam.timeseries.core.TimeSeriesMapper;
import org.esa.beam.timeseries.core.timeseries.datamodel.AbstractTimeSeries;
import org.esa.beam.timeseries.core.timeseries.datamodel.TimeCoding;
import org.esa.beam.timeseries.core.timeseries.datamodel.TimeSeriesListener;
import org.esa.beam.util.math.MathUtils;
import org.esa.beam.visat.VisatApp;

import javax.swing.AbstractButton;
import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.InternalFrameAdapter;
import javax.swing.event.InternalFrameEvent;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Container;
import java.awt.Insets;
import java.awt.event.MouseEvent;
import java.awt.event.MouseWheelEvent;
import java.awt.event.MouseWheelListener;
import java.awt.geom.AffineTransform;
import java.awt.geom.Point2D;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Main class for the matrix tool.
 *
 * @author Marco Peters
 * @author Thomas Storm
 */
public class TimeSeriesMatrixToolView extends AbstractToolView {

    private static final int MATRIX_MINIMUM = 3;
    private static final int MATRIX_DEFAULT_VALUE = MATRIX_MINIMUM;
    private static final int MATRIX_MAXIMUM = 15;
    private static final int MATRIX_STEP_SIZE = 2;

    private JSpinner matrixSizeSpinner;
    private JLabel dateLabel;
    private ProductSceneView currentView;
    private AbstractTimeSeries timeSeries;
    private final SceneViewListener sceneViewListener;
    private final TimeSeriesPPL pixelPosListener;
    private final MatrixMouseWheelListener mouseWheelListener;
    private final TimeSeriesListener timeSeriesMatrixTSL;

    private static final String DATE_PREFIX = "Date: ";
    private MatrixTableModel matrixModel;
    private final SimpleDateFormat dateFormat;
    private MatrixCellRenderer matrixCellRenderer;

    public TimeSeriesMatrixToolView() {
        pixelPosListener = new TimeSeriesPPL();
        sceneViewListener = new SceneViewListener();
        mouseWheelListener = new MatrixMouseWheelListener();
        timeSeriesMatrixTSL = new TimeSeriesMatrixTSL();
        dateFormat = new SimpleDateFormat("dd-MMM-yyyy HH:mm:ss", Locale.getDefault());
    }

    @Override
    protected JComponent createControl() {
        VisatApp.getApp().addInternalFrameListener(sceneViewListener);

        dateLabel = new JLabel(String.format(DATE_PREFIX + " %s", getStartDateString()));
        matrixSizeSpinner = new JSpinner(new SpinnerNumberModel(MATRIX_DEFAULT_VALUE,
                                                                MATRIX_MINIMUM, MATRIX_MAXIMUM,
                                                                MATRIX_STEP_SIZE));
        final JComponent editor = matrixSizeSpinner.getEditor();
        if (editor instanceof JSpinner.DefaultEditor) {
            ((JSpinner.DefaultEditor) editor).getTextField().setEditable(false);
        }
        matrixSizeSpinner.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                matrixModel.setMatrixSize((Integer) matrixSizeSpinner.getModel().getValue());
            }
        });

        final TableLayout tableLayout = new TableLayout(2);
        tableLayout.setTablePadding(4, 4);
        tableLayout.setTableFill(TableLayout.Fill.BOTH);
        tableLayout.setTableAnchor(TableLayout.Anchor.NORTHWEST);
        tableLayout.setTableWeightX(0.0);
        tableLayout.setTableWeightY(0.0);
        tableLayout.setColumnWeightX(0, 1.0);
        tableLayout.setRowWeightY(1, 1.0);
        tableLayout.setCellColspan(0, 0, 2);

        JPanel panel = new JPanel(tableLayout);
        panel.setBorder(BorderFactory.createEmptyBorder(4, 4, 4, 4));
        JPanel buttonPanel = createButtonPanel();
        JPanel tablePanel = createTablePanel();
        panel.add(dateLabel);
        panel.add(tablePanel);
        panel.add(buttonPanel);

        setCurrentView(VisatApp.getApp().getSelectedProductSceneView());
        return panel;
    }

    @Override
    public void componentShown() {
        addMouseWheelListener();
    }

    @Override
    public void componentOpened() {
        addMouseWheelListener();
    }

    @Override
    public void componentClosed() {
        removeMouseWheelListener();
    }

    @Override
    public void componentHidden() {
        removeMouseWheelListener();
    }

    private JPanel createTablePanel() {
        JPanel mainPanel = new JPanel(new BorderLayout(4, 4));
        matrixModel = new MatrixTableModel();
        JideTable matrixTable = new JideTable(matrixModel);
        matrixCellRenderer = new MatrixCellRenderer(matrixModel);
        matrixTable.setDefaultRenderer(Double.class, matrixCellRenderer);
        matrixTable.setBorder(BorderFactory.createLineBorder(Color.BLACK, 1));
        mainPanel.add(BorderLayout.CENTER, matrixTable);
        return mainPanel;
    }

    private String getStartDateString() {
        String startDateString = "";
        if (currentView != null && timeSeries != null) {
            final TimeCoding timeCoding = timeSeries.getRasterTimeMap().get(currentView.getRaster());
            Date startDate = timeCoding.getStartTime().getAsDate();
            startDateString = dateFormat.format(startDate);
        }
        return startDateString;
    }

    private JPanel createButtonPanel() {
        final TableLayout tableLayout = new TableLayout(1);
        tableLayout.setTablePadding(4, 4);
        tableLayout.setRowPadding(0, new Insets(0, 4, 4, 4));
        tableLayout.setTableAnchor(TableLayout.Anchor.NORTHWEST);
        tableLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
        tableLayout.setTableWeightX(1.0);
        tableLayout.setTableWeightY(0.0);
        JPanel buttonPanel = new JPanel(tableLayout);

        AbstractButton helpButton = ToolButtonFactory.createButton(UIUtils.loadImageIcon("icons/Help22.png"), false);
        helpButton.setToolTipText("Help");
        buttonPanel.add(matrixSizeSpinner);
        buttonPanel.add(tableLayout.createVerticalSpacer());
        buttonPanel.add(helpButton);

        final PageComponentDescriptor descriptor = getDescriptor();
        if (descriptor.getHelpId() != null) {
            HelpSys.enableHelpOnButton(helpButton, descriptor.getHelpId());
            HelpSys.enableHelpKey(buttonPanel, descriptor.getHelpId());
        }

        return buttonPanel;
    }

    /*
     * Checks if the view displays a timeseries product.
     * If so it is set as the current view.
     */

    private void setCurrentView(ProductSceneView newView) {
        if (currentView == newView) {
            return;
        }
        if (currentView != null) {
            currentView.removePixelPositionListener(pixelPosListener);
            removeMouseWheelListener();
            if (timeSeries != null) {
                timeSeries.removeTimeSeriesListener(timeSeriesMatrixTSL);
            }
        }
        currentView = newView;
        if (isTimeSeriesView(currentView)) {
            currentView.addPixelPositionListener(pixelPosListener);
            timeSeries = TimeSeriesMapper.getInstance().getTimeSeries(currentView.getProduct());
            timeSeries.addTimeSeriesListener(timeSeriesMatrixTSL);
            addMouseWheelListener();
            final RasterDataNode raster = currentView.getRaster();
            if (raster instanceof Band) {
                matrixModel.setBand((Band) raster);
                matrixModel.setMatrixSize((Integer) matrixSizeSpinner.getValue());
                matrixCellRenderer.setInvalidColor(currentView.getLayerCanvas().getBackground());
                updateDateLabel((Band) currentView.getRaster());
            }
        } else {
            timeSeries = null;
            matrixModel.setMatrixSize(0);
        }
    }

    private void updateDateLabel(Band band) {
        String dateString = "";
        if (band != null) {
            final TimeCoding timeCoding = timeSeries.getRasterTimeMap().get(band);
            final Date startTime = timeCoding.getStartTime().getAsDate();
            dateString = dateFormat.format(startTime);
        }
        dateLabel.setText(String.format(DATE_PREFIX + " %s", dateString));
    }

    // Depending on the direction value this method returns the next
    // band in the list of available bands in the time series.
    // Negative value of direction means previous band.
    // If there is no next band the current band is returned.

    private Band getNextBand(Band currentBand, int direction) {
        final String varName = AbstractTimeSeries.rasterToVariableName(currentBand.getName());
        final List<Band> bandList = timeSeries.getBandsForVariable(varName);
        final int currentIndex = bandList.indexOf(currentBand);

        if (direction < 0) {
            if (currentIndex > 0) {
                return bandList.get(currentIndex - 1);
            }
        } else {
            if (currentIndex + 1 < bandList.size()) {
                return bandList.get(currentIndex + 1);
            }
        }
        return currentBand;
    }

    private boolean isTimeSeriesView(ProductSceneView view) {
        if (view != null) {
            final RasterDataNode viewRaster = view.getRaster();
            final String viewProductType = viewRaster.getProduct().getProductType();
            return !view.isRGB() &&
                   viewProductType.equals(AbstractTimeSeries.TIME_SERIES_PRODUCT_TYPE) &&
                   TimeSeriesMapper.getInstance().getTimeSeries(view.getProduct()) != null;
        }
        return false;
    }


    private void addMouseWheelListener() {
        if (currentView != null) {
            final LayerCanvas layerCanvas = currentView.getLayerCanvas();
            final List<MouseWheelListener> listeners = Arrays.asList(layerCanvas.getMouseWheelListeners());
            if (!listeners.contains(mouseWheelListener)) {
                layerCanvas.addMouseWheelListener(mouseWheelListener);
            }
        }
    }

    private void removeMouseWheelListener() {
        if (currentView != null) {
            currentView.getLayerCanvas().removeMouseWheelListener(mouseWheelListener);
        }
    }

    private class SceneViewListener extends InternalFrameAdapter {

        @Override
        public void internalFrameActivated(InternalFrameEvent e) {
            final Container contentPane = e.getInternalFrame().getContentPane();
            if (contentPane instanceof ProductSceneView) {
                setCurrentView((ProductSceneView) contentPane);
            }
        }

        @Override
        public void internalFrameDeactivated(InternalFrameEvent e) {
            final Container contentPane = e.getInternalFrame().getContentPane();
            if (currentView == contentPane) {
                setCurrentView(null);
            }
        }
    }

    private class TimeSeriesPPL implements PixelPositionListener {

        @Override
        public void pixelPosChanged(ImageLayer imageLayer, int pixelX, int pixelY,
                                    int currentLevel, boolean pixelPosValid, MouseEvent e) {
            if (isVisible() && currentView != null) {
                AffineTransform i2mTransform = imageLayer.getImageToModelTransform(currentLevel);
                Point2D modelP = i2mTransform.transform(new Point2D.Double(pixelX + 0.5, pixelY + 0.5), null);
                AffineTransform m2iTransform = imageLayer.getModelToImageTransform();
                Point2D levelZeroP = m2iTransform.transform(modelP, null);
                matrixModel.setCenterPixel(MathUtils.floorInt(levelZeroP.getX()),
                                           MathUtils.floorInt(levelZeroP.getY()));
            }
        }

        @Override
        public void pixelPosNotAvailable() {
            matrixModel.clearMatrix();
        }
    }

    private class MatrixMouseWheelListener implements MouseWheelListener {

        @Override
        public void mouseWheelMoved(MouseWheelEvent e) {
            if (e.isAltDown()) {
                Band nextBand = getNextBand(matrixModel.getBand(), e.getWheelRotation());
                if (nextBand != null) {
                    matrixModel.setBand(nextBand);
                    updateDateLabel(nextBand);
                }
            }
        }
    }

    private class TimeSeriesMatrixTSL extends TimeSeriesListener {

        @Override
        public void nodeRemoved(ProductNodeEvent event) {
            final ProductNode node = event.getSourceNode();
            if (node == matrixModel.getBand()) {
                final Band band = matrixModel.getBand();
                Band nextBand = getNextBand(band, 1);
                if (nextBand == band) {
                    nextBand = getNextBand(band, -1);
                }
                if (nextBand == band) {
                    nextBand = null;
                }
                updateDateLabel(nextBand);
            }
        }
    }

}
