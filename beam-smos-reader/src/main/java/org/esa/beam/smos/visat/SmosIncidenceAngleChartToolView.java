package org.esa.beam.smos.visat;

import com.bc.ceres.binio.*;
import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.glevel.MultiLevelImage;
import org.esa.beam.dataio.smos.SmosDgg;
import org.esa.beam.dataio.smos.SmosFile;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.ui.product.ProductSceneView;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableModel;
import java.awt.Rectangle;
import java.awt.BorderLayout;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.IOException;

public class SmosIncidenceAngleChartToolView extends AbstractSmosToolView {
    public static final String ID = SmosIncidenceAngleChartToolView.class.getName();
    private JLabel label;
    private JTable table;
    private DefaultTableModel nullModel;

    public SmosIncidenceAngleChartToolView() {
        nullModel = new DefaultTableModel();
    }


    @Override
    protected JComponent createSmosControl() {
        final JPanel panel = new JPanel(new BorderLayout(2,2));
        label = new JLabel();
        table = new JTable();
        panel.add(label, BorderLayout.NORTH);
        panel.add(new JScrollPane(table), BorderLayout.CENTER);
        return panel;
    }


    @Override
    protected void handleProductSceneViewChanged(ProductSceneView oldView, ProductSceneView newView) {
        label.setText(newView != null ? "View" + newView.getName() : "No view selected");
    }

    @Override
    protected void handlePixelPosChanged(ImageLayer baseImageLayer,
                                         int pixelX,
                                         int pixelY,
                                         int currentLevel,
                                         boolean pixelPosValid) {

        if (!pixelPosValid) {
            label.setText("Pixel not valid");
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

        label.setText("" +
                "<html>" +
                "SEQNUM=<b>" + seqnum + "</b>, " +
                "INDEX=<b>" + gridPointIndex + "</b>" +
                "</html>");

        if (gridPointIndex >= 0) {
            try {
                final TableModel tableModel = rallala(smosFile, gridPointIndex);
                table.setModel(tableModel);
            } catch (IOException e) {
                // ok
            }
        } else {
            table.setModel(nullModel);
        }
    }

    private TableModel rallala(SmosFile smosFile, int gridPointIndex) throws IOException {
        SequenceData btDataList = smosFile.getBtDataList(gridPointIndex);

        CompoundType type = (CompoundType) btDataList.getSequenceType().getElementType();
        int memberCount = type.getMemberCount();

        int btDataListCount = btDataList.getElementCount();

        String[] columnNames = new String[1 + memberCount];
        columnNames[0] = "Rec#";
        for (int j = 0; j < memberCount; j++) {
            columnNames[1 + j] = type.getMemberName(j);
        }

        Object[][] tableData = new Object[btDataListCount][1 + memberCount];
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

        return new DefaultTableModel(tableData, columnNames);
    }

    @Override
    protected void handlePixelPosNotAvailable() {
        label.setText("Pixel not available");
    }
}
