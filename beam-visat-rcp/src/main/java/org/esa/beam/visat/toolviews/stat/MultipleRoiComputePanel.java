package org.esa.beam.visat.toolviews.stat;

import com.bc.ceres.swing.TableLayout;

import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.ProductNodeEvent;
import org.esa.beam.framework.datamodel.ProductNodeGroup;
import org.esa.beam.framework.datamodel.ProductNodeListener;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.ui.UIUtils;

import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.ButtonGroup;
import javax.swing.ComboBoxModel;
import javax.swing.DefaultComboBoxModel;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JRadioButton;


/**
 * A panel which performs the 'compute' action.
 *
 * @author Marco Zuehlke
 */
class MultipleRoiComputePanel extends JPanel {
    
    interface ComputeMasks {
        void compute(Mask[] selectedMasks);
    }
    
    private final ProductNodeListener productNodeListener;
    
    private final JButton computeButton;
    private final JCheckBox useRoiCheckBox;
    private final JComboBox maskNameComboBox;
    private final JRadioButton iterateButton;
    private final JRadioButton singleButton;
    
    private RasterDataNode raster;
    private Product product;

    MultipleRoiComputePanel(final ComputeMasks method, final RasterDataNode rasterDataNode) {
        productNodeListener = new PNL();
        final Icon icon = UIUtils.loadImageIcon("icons/Gears20.gif");

        computeButton = new JButton("Compute");     /*I18N*/
        computeButton.setMnemonic('A');
        computeButton.setEnabled(rasterDataNode != null);
        computeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                boolean useRoi = useRoiCheckBox.isSelected();
                Mask[] selectedMasks;
                if (useRoi) {
                    if (iterateButton.isEnabled() && iterateButton.isSelected()) {
                        ProductNodeGroup<Mask> roiMaskGroup = raster.getRoiMaskGroup();
                        selectedMasks = roiMaskGroup.toArray(new Mask[roiMaskGroup.getNodeCount()]);
                    } else {
                        String maskName = (String) maskNameComboBox.getSelectedItem();
                        ProductNodeGroup<Mask> roiMaskGroup = raster.getRoiMaskGroup();
                        Mask mask = roiMaskGroup.get(maskName);
                        selectedMasks = new Mask[] {mask};
                    }
                } else {
                    selectedMasks = new Mask[] {null}; 
                }
                method.compute(selectedMasks);
            }
        });
        computeButton.setIcon(icon);

        useRoiCheckBox = new JCheckBox("Use ROI-Mask");
        useRoiCheckBox.setMnemonic('R');
        useRoiCheckBox.addActionListener(new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                updateEnablement();
            }
        });
        
        maskNameComboBox = new JComboBox();
        
        final TableLayout tableLayoutRoi = new TableLayout(1);
        tableLayoutRoi.setTableAnchor(TableLayout.Anchor.SOUTHWEST);
        tableLayoutRoi.setTableFill(TableLayout.Fill.HORIZONTAL);
        tableLayoutRoi.setTableWeightX(1.0);
        tableLayoutRoi.setCellPadding(2, 0, new Insets(4, 20, 4, 0));
        
        JPanel roiPanel = new JPanel();
        roiPanel.setLayout(tableLayoutRoi);
        iterateButton = new JRadioButton("Iterate");
        singleButton = new JRadioButton("Single");
        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(iterateButton);
        buttonGroup.add(singleButton);
        roiPanel.add(iterateButton);
        roiPanel.add(singleButton);
        roiPanel.add(maskNameComboBox);

        ActionListener actionListener = new ActionListener() {
            
            @Override
            public void actionPerformed(ActionEvent e) {
                updateEnablement();
            }
        };
        iterateButton.addActionListener(actionListener);
        singleButton.addActionListener(actionListener);
        
        final TableLayout tableLayout = new TableLayout(1);
        tableLayout.setTableAnchor(TableLayout.Anchor.SOUTHWEST);
        tableLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
        tableLayout.setTableWeightX(1.0);
        tableLayout.setCellPadding(2, 0, new Insets(4, 20, 4, 0));
        setLayout(tableLayout);

        add(computeButton);
        add(useRoiCheckBox);
        add(roiPanel);

        setRaster(rasterDataNode);
    }

    void setRaster(final RasterDataNode raster) {
        if (this.raster != raster) {
            this.raster = raster;
            if (raster == null) {
                if (product != null) {
                    product.removeProductNodeListener(productNodeListener);
                }
                product = null;
            } else if (product != raster.getProduct()) {
                if (product != null) {
                    product.removeProductNodeListener(productNodeListener);
                }
                product = raster.getProduct();
                if (product != null) {
                    product.addProductNodeListener(productNodeListener);
                }
            }
            updateMaskListState();
        }
    }

    private void updateMaskListState() {
        boolean hasRaster = (raster != null);
        computeButton.setEnabled(hasRaster);
        int roiCount = raster.getRoiMaskGroup().getNodeCount();
        boolean hasRois = (hasRaster && roiCount > 0);
        useRoiCheckBox.setEnabled(hasRois);
        if (hasRois) {
            String[] nodeNames = raster.getRoiMaskGroup().getNodeNames();
            maskNameComboBox.setModel(new DefaultComboBoxModel(nodeNames));
            maskNameComboBox.setSelectedIndex(0);
        } else {
            maskNameComboBox.setModel(new DefaultComboBoxModel());
            useRoiCheckBox.setSelected(false);
        }
        updateEnablement();
    }
    
    private void updateEnablement() {
        boolean useRoi = useRoiCheckBox.isSelected() && useRoiCheckBox.isEnabled();
        int numRoiMasks = maskNameComboBox.getModel().getSize();
        boolean hasMultipleRois = numRoiMasks > 1;
        boolean singleRoi = singleButton.isSelected();
        
        singleButton.setEnabled(hasMultipleRois && useRoi);
        iterateButton.setEnabled(hasMultipleRois && useRoi);   
        maskNameComboBox.setEnabled(hasMultipleRois && useRoi && singleRoi);
    }
    
    private class PNL implements ProductNodeListener {

        @Override
        public void nodeAdded(ProductNodeEvent event) {
            handleEvent(event);
        }

        @Override
        public void nodeChanged(ProductNodeEvent event) {
            handleEvent(event);
        }

        @Override
        public void nodeDataChanged(ProductNodeEvent event) {
            handleEvent(event);
        }

        @Override
        public void nodeRemoved(ProductNodeEvent event) {
            handleEvent(event);
        }
        
        private void handleEvent(ProductNodeEvent event) {
            ProductNode sourceNode = event.getSourceNode();
            if (sourceNode instanceof Mask) {
                updateMaskListState();
            }
        }
    }
}
