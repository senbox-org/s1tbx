package org.esa.beam.visat.toolviews.layermanager.editors;

import com.bc.ceres.binding.ValueDescriptor;
import com.bc.ceres.binding.swing.BindingContext;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.swing.TableLayout;
import com.jidesoft.combobox.ColorComboBox;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNodeEvent;
import org.esa.beam.framework.datamodel.ProductNodeListenerAdapter;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.glayer.PlacemarkLayer;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * @author Marco Peters
 * @version $ Revision $ Date $
 * @since BEAM 4.6
 */
public class PlacemarkLayerEditor extends AbstractBindingLayerEditor {

    private Product currentProduct;
    private ProductNodeListenerAdapter placemarkGroupListener = new PlacemarkGroupListener();
    private ColorComboBox outlColorComboBox;
    private ColorComboBox fillColorComboBox;

    @Override
    public JComponent createControl(final AppContext appContext, Layer layer) {
        adjustListener(appContext.getSelectedProduct());

        final JPanel control = new JPanel(new BorderLayout());

        control.add(super.createControl(appContext, layer), BorderLayout.NORTH);
        final PlacemarkLayer placemarkLayer = (PlacemarkLayer) getLayer();
        if ("pin".equals(placemarkLayer.getPlacemarkDescriptor().getRoleName())) {
            control.add(createPinPanel(), BorderLayout.CENTER);
        }

        return control;
    }

    @Override
    public void updateControl() {
        super.updateControl();

        final PlacemarkLayer placemarkLayer = (PlacemarkLayer) getLayer();
        if ("pin".equals(placemarkLayer.getPlacemarkDescriptor().getRoleName())) {
            updatePinPanel(placemarkLayer);
        }
    }

    private void updatePinPanel(PlacemarkLayer placemarkLayer) {
        outlColorComboBox.setSelectedColor(placemarkLayer.getOutlineColor());
        fillColorComboBox.setSelectedColor(placemarkLayer.getFillColor());
    }

    private JPanel createPinPanel() {
        TableLayout layout = new TableLayout(2);
        layout.setTableAnchor(TableLayout.Anchor.WEST);
        layout.setTableFill(TableLayout.Fill.HORIZONTAL);
        layout.setTablePadding(3, 3);
        layout.setColumnWeightX(0, 0.0);
        layout.setColumnWeightX(1, 1.0);

        final JPanel panel = new JPanel(layout);
        fillColorComboBox = new ColorComboBox();
        fillColorComboBox.setColorValueVisible(true);
        fillColorComboBox.setAllowDefaultColor(true);
        fillColorComboBox.setAllowMoreColors(true);
        fillColorComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ((PlacemarkLayer) getLayer()).setFillColor(fillColorComboBox.getSelectedColor());
            }
        });

        final JLabel fillLabel = new JLabel("Symbol fill colour:");
        fillLabel.setLabelFor(fillColorComboBox);
        panel.add(fillLabel);
        panel.add(fillColorComboBox);

        outlColorComboBox = new ColorComboBox();
        outlColorComboBox.setColorValueVisible(true);
        outlColorComboBox.setAllowDefaultColor(true);
        outlColorComboBox.setAllowMoreColors(true);
        outlColorComboBox.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                ((PlacemarkLayer) getLayer()).setOutlineColor(outlColorComboBox.getSelectedColor());
            }
        });
        final JLabel outlLabel = new JLabel("Symbol outline colour:");
        outlLabel.setLabelFor(outlColorComboBox);
        panel.add(outlLabel);
        panel.add(outlColorComboBox);
        return panel;
    }

    private void adjustListener(Product newProduct) {
        if (currentProduct != null) {
            currentProduct.removeProductNodeListener(placemarkGroupListener);
        }
        currentProduct = newProduct;
        currentProduct.addProductNodeListener(placemarkGroupListener);
    }

    @Override
    protected void initializeBinding(AppContext appContext, BindingContext bindingContext) {
        // initializes the general properties
        ValueDescriptor vd0 = new ValueDescriptor(PlacemarkLayer.PROPERTY_NAME_TEXT_ENABLED, Boolean.class);
        vd0.setDefaultValue(PlacemarkLayer.DEFAULT_TEXT_ENABLED);
        vd0.setDisplayName("Text enabled");
        addValueDescriptor(vd0);

        ValueDescriptor vd1 = new ValueDescriptor(PlacemarkLayer.PROPERTY_NAME_TEXT_FG_COLOR, Color.class);
        vd1.setDefaultValue(PlacemarkLayer.DEFAULT_TEXT_FG_COLOR);
        vd1.setDisplayName("Text foreground colour");
        addValueDescriptor(vd1);

        ValueDescriptor vd2 = new ValueDescriptor(PlacemarkLayer.PROPERTY_NAME_TEXT_BG_COLOR, Color.class);
        vd2.setDefaultValue(PlacemarkLayer.DEFAULT_TEXT_BG_COLOR);
        vd2.setDisplayName("Text background colour");
        addValueDescriptor(vd2);
    }

    private class PlacemarkGroupListener extends ProductNodeListenerAdapter {

        @Override
        public void nodeChanged(ProductNodeEvent event) {
            final Product product = event.getSourceNode().getProduct();
            if (event.getSourceNode() == product.getGcpGroup() ||
                event.getSourceNode() == product.getPinGroup()) {
                updateControl();
            }
        }
    }

}
