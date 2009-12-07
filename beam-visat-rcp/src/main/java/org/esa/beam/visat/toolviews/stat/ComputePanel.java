package org.esa.beam.visat.toolviews.stat;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.ValueSet;
import com.bc.ceres.binding.swing.BindingContext;
import com.bc.ceres.glevel.MultiLevelImage;
import com.bc.ceres.swing.TableLayout;

import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNode;
import org.esa.beam.framework.datamodel.ProductNodeEvent;
import org.esa.beam.framework.datamodel.ProductNodeGroup;
import org.esa.beam.framework.datamodel.ProductNodeListener;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.ui.UIUtils;

import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;

import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;


/**
 * A panel which performs the 'compute' action.
 *
 * @author Marco Peters
 * @author Marco Zuehlke
 */
class ComputePanel extends JPanel {
    
    interface ComputeMasks {
        void compute(Mask[] selectedMasks);
    }
    
    private static final Mask ENTIRE_IMAGE_MASK = new Mask("Entire Image", 1,1, new NoMaskType());
    private static final Mask[] ENTIRE_IMAGE_MASKS = new Mask[] {ENTIRE_IMAGE_MASK};
    private static final String SELECTED_MASK = "selected_mask";

    private final ProductNodeListener productNodeListener;
    private final JButton computeButton;
    private final Property masksPoperty;
    private final JComponent maskUiComponent;
    
    private RasterDataNode raster;
    private Product product;

    ComputePanel(final ComputeMasks method,
                 final boolean multiMaskSelection,
                 final RasterDataNode raster) {
        productNodeListener = new PNL();
        final Icon icon = UIUtils.loadImageIcon("icons/Gears20.gif");

        PropertyContainer model = PropertyContainer.createMapBacked(new HashMap<String, Object>());
        if (multiMaskSelection) {
            masksPoperty = Property.create(SELECTED_MASK, Mask[].class, ENTIRE_IMAGE_MASKS, true);
        } else {
            masksPoperty = Property.create(SELECTED_MASK, Mask.class, ENTIRE_IMAGE_MASK, true);
        }
        model.addProperty(masksPoperty);
        masksPoperty.getDescriptor().setValueSet(new ValueSet(ENTIRE_IMAGE_MASKS));
        
        computeButton = new JButton("Compute");     /*I18N*/
        computeButton.setMnemonic('A');
        computeButton.setEnabled(raster != null);
        computeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                Mask[] selectedMasks;
                if (multiMaskSelection) {
                    selectedMasks = (Mask[]) masksPoperty.getValue();
                } else {
                    selectedMasks = new Mask[] {(Mask) masksPoperty.getValue()};
                }
                if (selectedMasks.length == 0) {
                    selectedMasks = new Mask[] {null};
                } else {
                    for (int i = 0; i < selectedMasks.length; i++) {
                        if (selectedMasks[i] == ENTIRE_IMAGE_MASK) {
                            selectedMasks[i] = null;
                        }
                    }
                }
                method.compute(selectedMasks);
            }
        });
        computeButton.setIcon(icon);

        BindingContext bindingContext = new BindingContext(model);
        String labelText;
        if (multiMaskSelection) {
            JList list = new JList();
            list.setCellRenderer(new MaskListCellRenderer());
            bindingContext.bind(SELECTED_MASK, list, true);
            JScrollPane scrollableList = new JScrollPane(list);
            maskUiComponent = scrollableList;
            labelText = "select ROI-Mask(s):";
        } else {
            JComboBox comboBox = new JComboBox();
            comboBox.setRenderer(new MaskListCellRenderer());
            bindingContext.bind(SELECTED_MASK, comboBox);
            maskUiComponent = comboBox;
            labelText = "select ROI-Mask:";
        }
        
        final TableLayout tableLayout = new TableLayout(1);
        tableLayout.setTableAnchor(TableLayout.Anchor.SOUTHWEST);
        tableLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
        tableLayout.setTableWeightX(1.0);
        setLayout(tableLayout);

        add(computeButton);
        add(new JLabel(labelText));
        add(maskUiComponent);

        setRaster(raster);
    }

    public void setRaster(final RasterDataNode raster) {
        if (this.raster != raster) {
            this.raster = raster;
            computeButton.setEnabled(this.raster != null);
            if (raster == null) {
                if (product != null) {
                    product.removeProductNodeListener(productNodeListener);
                }
                product = null;
            } else if (product != raster.getProduct()) {
                product = raster.getProduct();
                if (product != null) {
                    product.addProductNodeListener(productNodeListener);
                }
                updateMaskListState();
            }
        }
    }

    void updateMaskListState() {
        if (raster== null || raster.getProduct() == null) {
            masksPoperty.getDescriptor().setValueSet(new ValueSet(ENTIRE_IMAGE_MASKS));
            return;
        }
        ProductNodeGroup<Mask> maskGroup = raster.getProduct().getMaskGroup();
        boolean enabled = maskGroup.getNodeCount()>0;
        maskUiComponent.setEnabled(enabled);
        final Mask[] masks = new Mask[maskGroup.getNodeCount()+1];
        masks[0] = ENTIRE_IMAGE_MASK;
        
        for (int i = 0; i < maskGroup.getNodeCount(); i++) {
            masks[i+1] = maskGroup.get(i);
        }
        
        ValueSet vs = new ValueSet(masks);
        masksPoperty.getDescriptor().setValueSet(vs);
    }
    
    private static class NoMaskType extends Mask.ImageType {

        protected NoMaskType() {
            super("Entire Image");
        }

        @Override
        public MultiLevelImage createImage(Mask mask) {
            return null;
        }
        
    }
 
    private static class MaskListCellRenderer extends DefaultListCellRenderer {
        
        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index,
                                                      boolean isSelected, boolean cellHasFocus) {

            super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
            if (value != null) {
                Mask mask = (Mask) value;
                setText(mask.getName());
            }
            return this;
        }  
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
