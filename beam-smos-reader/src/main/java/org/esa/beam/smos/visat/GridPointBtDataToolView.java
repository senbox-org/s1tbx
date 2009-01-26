package org.esa.beam.smos.visat;

import org.esa.beam.dataio.smos.L1cSmosFile;
import org.esa.beam.dataio.smos.SmosFile;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.ui.product.ProductSceneView;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.io.IOException;

public abstract class GridPointBtDataToolView extends SmosToolView {
    public static final String ID = GridPointBtDataToolView.class.getName();

    private JLabel infoLabel;
    private JCheckBox snapToSelectedPinCheckBox;
    private GPSL gpsl;

    public GridPointBtDataToolView() {
    }

    @Override
    protected JComponent createClientComponent(ProductSceneView smosView) {
        infoLabel = new JLabel();
        snapToSelectedPinCheckBox = new JCheckBox("Snap to selected pin");
        snapToSelectedPinCheckBox.addItemListener(new IL());

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
    public void componentOpened() {
        super.componentOpened();
        gpsl = new GPSL();
        SmosBox.getInstance().getGridPointSelectionService().addGridPointSelectionListener(gpsl);
        realizeGridPointChange(SmosBox.getInstance().getGridPointSelectionService().getSelectedGridPointId());
    }

    @Override
    public void componentClosed() {
        super.componentClosed();
        SmosBox.getInstance().getGridPointSelectionService().removeGridPointSelectionListener(gpsl);
        realizeGridPointChange(-1);
    }

    private void realizeGridPointChange(int selectedGridPointId) {

        if (selectedGridPointId == -1) {
            setInfoText("No data");
            clearGridPointBtDataComponent();
            return;
        }

        final SmosFile smosFile = getSelectedSmosFile();
        final int gridPointIndex = smosFile.getGridPointIndex(selectedGridPointId);

        if (gridPointIndex >= 0 && smosFile instanceof L1cSmosFile) {
            setInfoText("" +
                    "<html>" +
                    "SEQNUM=<b>" + selectedGridPointId + "</b>, " +
                    "INDEX=<b>" + gridPointIndex + "</b>" +
                    "</html>");

            try {
                GridPointBtDataset ds = GridPointBtDataset.read((L1cSmosFile) smosFile, gridPointIndex);
                updateGridPointBtDataComponent(ds);
            } catch (IOException e) {
                updateGridPointBtDataComponent(e);
            }
        } else {
            setInfoText("No data");
            clearGridPointBtDataComponent();
        }
    }

    protected void setInfoText(String text) {
        infoLabel.setText(text);
    }

    protected abstract JComponent createGridPointComponent();

    protected abstract void updateGridPointBtDataComponent(GridPointBtDataset ds);

    protected abstract void updateGridPointBtDataComponent(IOException e);

    protected abstract void clearGridPointBtDataComponent();

    private class GPSL implements GridPointSelectionService.SelectionListener {
        @Override
        public void handleGridPointSelectionChanged(int oldId, int newId) {
            if (!snapToSelectedPinCheckBox.isSelected()
                    || getSelectedSmosProduct().getPinGroup().getSelectedNode() == null) {
                realizeGridPointChange(newId);
            }
        }
    }

    private class IL implements ItemListener {
        private final ProductNodeListener pnl;

        private IL() {
            pnl = new PNL();
        }

        @Override
        public void itemStateChanged(ItemEvent e) {
            if (e.getStateChange() == ItemEvent.SELECTED) {
                realizeSelectedPin();
                getSelectedSmosProduct().addProductNodeListener(pnl);
            } else {
                getSelectedSmosProduct().removeProductNodeListener(pnl);
            }
        }

        private void realizeSelectedPin() {
            final Pin selectedPin = getSelectedSmosProduct().getPinGroup().getSelectedNode();

            if (selectedPin != null) {
                final PixelPos pixelPos = selectedPin.getPixelPos();
                final int x = (int) Math.floor(pixelPos.getX());
                final int y = (int) Math.floor(pixelPos.getY());
                final int id = SmosBox.getInstance().getSmosViewSelectionService().getGridPointId(x, y);

                realizeGridPointChange(id);
            }
        }

        private class PNL implements ProductNodeListener {
            @Override
            public void nodeChanged(ProductNodeEvent event) {
                if (Pin.PROPERTY_NAME_SELECTED.equals(event.getPropertyName())) {
                    updatePin(event);
                }
            }

            @Override
            public void nodeDataChanged(ProductNodeEvent event) {
                updatePin(event);
            }

            @Override
            public void nodeAdded(ProductNodeEvent event) {
                updatePin(event);
            }

            @Override
            public void nodeRemoved(ProductNodeEvent event) {
                updatePin(event);
            }

            private void updatePin(ProductNodeEvent event) {
                final ProductNode sourceNode = event.getSourceNode();
                if (sourceNode instanceof Pin && sourceNode.isSelected()) {
                    realizeSelectedPin();
                }
            }
        }
    }
}