package org.esa.beam.smos.visat;

import com.bc.ceres.glayer.support.ImageLayer;
import com.bc.ceres.glevel.MultiLevelImage;
import org.esa.beam.dataio.smos.SmosFile;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.ui.product.ProductSceneView;

import javax.swing.*;
import java.awt.BorderLayout;
import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.IOException;

public abstract class SmosGridPointInfoToolView extends SmosToolView {
    public static final String ID = SmosGridPointInfoToolView.class.getName();

//    private static final String[] MEMBER_NAMES = new String[]{
//            "BT_Value",
//            "BT_Value_Real",
//            "BT_Value_Imag",
//            "Pixel_Radiometric_Accuracy",
//            "Incidence_Angle",
//            "Azimuth_Angle",
//            "Faraday_Rotation_Angle",
//            "Geometric_Rotation_Angle",
//            "Snapshot_ID_of_Pixel",
//            "Footprint_Axis1",
//            "Footprint_Axis2",
//    };

    private JLabel infoLabel;
    private JCheckBox snapToSelectedPinCheckBox;

    public SmosGridPointInfoToolView() {
    }

    @Override
    protected JComponent createSmosComponent(ProductSceneView smosView) {
        infoLabel = new JLabel();
        snapToSelectedPinCheckBox = new JCheckBox("Snap to selected pin");

        final JPanel optionsPanel = new JPanel(new BorderLayout(6, 0));
        optionsPanel.add(snapToSelectedPinCheckBox, BorderLayout.WEST);
        optionsPanel.add(createGridPointComponentOptionsComponent(), BorderLayout.CENTER);

        JPanel panel = new JPanel(new BorderLayout(2, 2));
        panel.add(infoLabel, BorderLayout.CENTER);
        panel.add(createGridPointComponent(), BorderLayout.CENTER);
        panel.add(optionsPanel, BorderLayout.SOUTH);
        return panel;
    }

    protected JComponent createGridPointComponentOptionsComponent() {
        return new JPanel();
    }

    boolean isSnappedToPin() {
        return snapToSelectedPinCheckBox.isSelected();
    }


    @Override
    protected final  void handlePixelPosChanged(ImageLayer baseImageLayer,
                                         int pixelX,
                                         int pixelY,
                                         int currentLevel,
                                         boolean pixelPosValid) {

        if (!pixelPosValid) {
            setInfoText("No data");
            clearGridPointComponent();
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

        if (gridPointIndex >= 0) {
            setInfoText("" +
                    "<html>" +
                    "SEQNUM=<b>" + seqnum + "</b>, " +
                    "INDEX=<b>" + gridPointIndex + "</b>" +
                    "</html>");

            try {
                GridPointDataset ds = GridPointDataset.read(smosFile, gridPointIndex);
                updateGridPointComponent(ds);
            } catch (IOException e) {
                updateGridPointComponent(e);
            }
        } else {
            setInfoText("No data");
            clearGridPointComponent();
        }
    }

    @Override
    protected final void handlePixelPosNotAvailable() {
        setInfoText("Pixel not available");
        clearGridPointComponent();
    }

    protected void setInfoText(String text) {
        infoLabel.setText(text);
    }

    protected abstract JComponent createGridPointComponent();

    protected abstract void updateGridPointComponent(GridPointDataset ds);

    protected abstract void updateGridPointComponent(IOException e);

    protected abstract void clearGridPointComponent();

}