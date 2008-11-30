package org.esa.beam.smos.visat;

import com.bc.ceres.binio.CompoundData;
import com.bc.ceres.binio.CompoundType;
import com.bc.ceres.binio.SequenceData;
import com.bc.ceres.binio.SimpleType;
import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.glevel.MultiLevelImage;
import org.esa.beam.dataio.smos.SmosFile;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.ui.product.ProductSceneView;
import org.jfree.chart.ChartFactory;
import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.plot.PlotOrientation;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYSeries;
import org.jfree.data.xy.XYSeriesCollection;
import org.jfree.ui.RectangleInsets;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.IOException;

public class SmosGridPointInfoToolView extends AbstractSmosToolView {
    public static final String ID = SmosGridPointInfoToolView.class.getName();

    private static final String[] MEMBER_NAMES = new String[]{
            "BT_Value",
            "BT_Value_Real",
            "BT_Value_Imag",
            "Pixel_Radiometric_Accuracy",
            "Incidence_Angle",
            "Azimuth_Angle",
            "Faraday_Rotation_Angle",
            "Geometric_Rotation_Angle",
            "Snapshot_ID_of_Pixel",
            "Footprint_Axis1",
            "Footprint_Axis2",
    };

    private JLabel tableLabel;
    private JTable table;
    private DefaultTableModel nullModel;
    private JTabbedPane tabbedPane;
    private JFreeChart chart;
    private XYSeriesCollection dataset;
    private JLabel chartLabel;

    public SmosGridPointInfoToolView() {
        nullModel = new DefaultTableModel();
    }


    @Override
    protected JComponent createSmosControl() {
        tabbedPane = new JTabbedPane();
        tabbedPane.addTab("Table", createTablePanel());
        tabbedPane.addTab("Chart", createChartPanel());
        tabbedPane.setSelectedIndex(0);
        return tabbedPane;
    }

    private Component createChartPanel() {
        chartLabel = new JLabel();
        dataset = new XYSeriesCollection();
        chart = ChartFactory.createXYLineChart(null,
                                               null,
                                               null,
                                               dataset,
                                               PlotOrientation.VERTICAL,
                                               true, // Legend?
                                               true,
                                               false);

        final XYPlot plot = chart.getXYPlot();
        plot.setNoDataMessage("No data.");
        plot.setAxisOffset(new RectangleInsets(5, 5, 5, 5));
        final JPanel chartPanel = new JPanel(new BorderLayout(2, 2));
        chartPanel.add(chartLabel, BorderLayout.NORTH);
        chartPanel.add(new ChartPanel(chart), BorderLayout.CENTER);
        return chartPanel;
    }

    private JPanel createTablePanel() {
        tableLabel = new JLabel();
        table = new JTable();

        final JPanel tablePanel = new JPanel(new BorderLayout(2, 2));
        tablePanel.add(tableLabel, BorderLayout.NORTH);
        tablePanel.add(new JScrollPane(table), BorderLayout.CENTER);
        return tablePanel;
    }


    @Override
    protected void handleProductSceneViewChanged(ProductSceneView oldView, ProductSceneView newView) {
        tableLabel.setText(newView != null ? "View: " + newView.getName() : "No view selected");
    }

    @Override
    protected void handlePixelPosChanged(ImageLayer baseImageLayer,
                                         int pixelX,
                                         int pixelY,
                                         int currentLevel,
                                         boolean pixelPosValid) {

        if (!pixelPosValid) {
            tableLabel.setText("Pixel not valid");
            return;
        }

        Band gridPointIdBand = getSmosProduct().getBandAt(0); // Convention! Grid_Point_ID is always first!
        final MultiLevelImage levelImage = (MultiLevelImage) gridPointIdBand.getSourceImage();
        final RenderedImage image = levelImage.getImage(currentLevel);
        final Raster data = image.getData(new Rectangle(pixelX, pixelY, 1, 1));
        final int seqnum = data.getSample(pixelX, pixelY, 0);

        // final int seqnum = SmosDgg.smosGridPointIdToDggridSeqnum(gridPointId);
        final SmosFile smosFile = getSmosProductReader().getSmosFile();
        final int gridPointIndex = smosFile.getGridPointIndex(seqnum);

        if (tabbedPane.getSelectedIndex() == 0) {
            updateLabel(tableLabel, seqnum, gridPointIndex);
            updateTable(smosFile, gridPointIndex);
        } else if (tabbedPane.getSelectedIndex() == 1) {
            updateLabel(chartLabel, seqnum, gridPointIndex);
            updateChart(smosFile, gridPointIndex);
        }
    }

    private void updateChart(SmosFile smosFile, int gridPointIndex) {

        dataset.removeAllSeries();
        if (gridPointIndex >= 0) {
            try {
                DS ds = readDS(smosFile, gridPointIndex);

                int ix = smosFile.getBtDataType().getMemberIndex("Incidence_Angle");
                if (ix != -1) {
                    ix++;
                    int iy = smosFile.getBtDataType().getMemberIndex("BT_Value");
                    if (iy != -1) {
                        iy++;
                        XYSeries series = new XYSeries("BT_Value");
                        int length = ds.data.length;
                        for (int i = 0; i < length; i++) {
                            series.add(ds.data[i][ix], ds.data[i][iy]);
                        }
                        dataset.addSeries(series);
                    } else {
                        int iy1 = smosFile.getBtDataType().getMemberIndex("BT_Value_Real");
                        int iy2 = smosFile.getBtDataType().getMemberIndex("BT_Value_Imag");
                        if (iy1 != -1 && iy2 != -1) {
                            iy1++;
                            iy2++;
                            XYSeries series1 = new XYSeries("BT_Value_Real");
                            XYSeries series2 = new XYSeries("BT_Value_Imag");
                            int length = ds.data.length;
                            for (int i = 0; i < length; i++) {
                                series1.add(ds.data[i][ix], ds.data[i][iy1]);
                                series2.add(ds.data[i][ix], ds.data[i][iy2]);
                            }
                            dataset.addSeries(series1);
                            dataset.addSeries(series2);
                        }
                    }
                }
                chart.fireChartChanged();
            } catch (IOException e) {
                // todo - signal error
            }
        }
    }

    private void updateTable(SmosFile smosFile, int gridPointIndex) {
        if (gridPointIndex >= 0) {
            try {
                table.setModel(new SmosTableModel(readDS(smosFile, gridPointIndex)));
            } catch (IOException e) {
                // todo - signal error
            }
        } else {
            table.setModel(nullModel);
        }
    }

    private void updateLabel(JLabel jLabel, int seqnum, int gridPointIndex) {
        jLabel.setText("" +
                "<html>" +
                "SEQNUM=<b>" + seqnum + "</b>, " +
                "INDEX=<b>" + gridPointIndex + "</b>" +
                "</html>");
    }

    private DS readDS(SmosFile smosFile, int gridPointIndex) throws IOException {
        SequenceData btDataList = smosFile.getBtDataList(gridPointIndex);

        CompoundType type = (CompoundType) btDataList.getSequenceType().getElementType();
        int memberCount = type.getMemberCount();

        int btDataListCount = btDataList.getElementCount();

        String[] columnNames = new String[1 + memberCount];
        Class[] columnClasses = new Class[1 + memberCount];
        columnNames[0] = "Index";
        columnClasses[0] = Integer.class;
        for (int j = 0; j < memberCount; j++) {
            columnNames[1 + j] = type.getMemberName(j);
            if (type.getMemberType(j) == SimpleType.FLOAT) {
                columnClasses[1 + j] = Float.class;
            } else if (type.getMemberType(j) == SimpleType.DOUBLE) {
                columnClasses[1 + j] = Double.class;
            } else {
                columnClasses[1 + j] = Long.class;
            }
        }

        Number[][] tableData = new Number[btDataListCount][1 + memberCount];
        for (int i = 0; i < btDataListCount; i++) {
            CompoundData btData = btDataList.getCompound(i);
            tableData[i][0] = i;
            for (int j = 0; j < memberCount; j++) {
                if (type.getMemberType(j) == SimpleType.FLOAT) {
                    tableData[i][1 + j] = btData.getFloat(j);
                } else if (type.getMemberType(j) == SimpleType.DOUBLE) {
                    tableData[i][1 + j] = btData.getDouble(j);
                } else {
                    tableData[i][1 + j] = btData.getLong(j);
                }
            }
        }

        return new DS(columnNames, columnClasses, tableData);
    }

    private static class DS {
        String[] columnNames;
        Class[] columnClasses;
        Number[][] data;

        private DS(String[] columnNames, Class[] columnClasses, Number[][] data) {
            this.columnNames = columnNames;
            this.columnClasses = columnClasses;
            this.data = data;
        }

        int getColumnIndex(String name) {
            for (int i = 0; i < columnNames.length; i++) {
                String columnName = columnNames[i];
                if (columnName.equalsIgnoreCase(name)) {
                    return i;
                }
            }
            return -1;
        }
    }

    @Override
    protected void handlePixelPosNotAvailable() {
        tableLabel.setText("Pixel not available");
    }

    private static class SmosTableModel extends AbstractTableModel {
        private final DS ds;

        public SmosTableModel(DS ds) {
            this.ds = ds;
        }

        @Override
        public int getRowCount() {
            return ds.data.length;
        }

        @Override
        public int getColumnCount() {
            return ds.columnNames.length;
        }

        @Override
        public Object getValueAt(int rowIndex, int columnIndex) {
            return ds.data[rowIndex][columnIndex];
        }

        @Override
        public String getColumnName(int columnIndex) {
            return ds.columnNames[columnIndex];
        }

        @Override
        public Class<?> getColumnClass(int columnIndex) {
            return ds.columnClasses[columnIndex];
        }
    }
}
