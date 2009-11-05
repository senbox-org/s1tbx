package org.esa.beam.gpf.common.reproject.ui;

import com.bc.ceres.swing.TableLayout;
import com.bc.ceres.swing.TableLayout.Fill;

import org.esa.beam.framework.datamodel.CrsGeoCoding;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductFilter;
import org.esa.beam.framework.gpf.ui.SourceProductSelector;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.ModalDialog;
import org.esa.beam.framework.ui.application.SelectionChangeEvent;
import org.esa.beam.framework.ui.application.SelectionChangeListener;
import org.esa.beam.gpf.common.reproject.ui.projdef.CustomCrsForm;
import org.esa.beam.util.ProductUtils;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTextField;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.geom.GeneralPath;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * @author Marco Peters
 * @author Marco Zühlke
 * @version $ Revision $ Date $
 * @since BEAM 4.7
 */
class CrsForm extends JPanel {

    private SourceProductSelector collocateProductSelector;
    private ButtonModel projButtonModel;
    private ButtonModel crsButtonModel;
    private ButtonModel collocateButtonModel;
    private CustomCrsForm projDefPanel;
    private JPanel collocationPanel;
    private JTextField collocationCrsCodeField;
    private JPanel crsSelectionPanel;

    private CrsInfo selectedCrsInfo;

    private final AppContext appContext;
    private final SourceProductSelector sourceProductSelector;

    CrsForm(AppContext appContext, SourceProductSelector sourceProductSelector) {
        this.appContext = appContext;
        this.sourceProductSelector = sourceProductSelector;
        createUI();
    }

    CoordinateReferenceSystem getCrs(Product sourceProduct) throws FactoryException {
        if (crsButtonModel.isSelected() && selectedCrsInfo != null) {
            return selectedCrsInfo.getCrs(sourceProduct);
        }
        if (projButtonModel.isSelected()) {
            return projDefPanel.getProcjetedCRS(sourceProduct);
        }
        if (isCollocate()) {
            return getCollocationProduct().getGeoCoding().getMapCRS();
        }
        return null;
    }

    boolean isCollocate() {
        return collocateButtonModel.isSelected();
    }

    Product getCollocationProduct() {
        return isCollocate() ? collocateProductSelector.getSelectedProduct() : null;
    }

    void prepareShow() {
        collocateProductSelector.initProducts();
    }

    void prepareHide() {
        collocateProductSelector.releaseProducts();
    }

    private void updateUIState() {
        final boolean collocate = isCollocate();
        firePropertyChange("collocate", !collocate, collocate);
        collocateProductSelector.getProductNameComboBox().setEnabled(collocate);
        collocateProductSelector.getProductFileChooserButton().setEnabled(collocate);
        projDefPanel.setEnabled(projButtonModel.isSelected());
        crsSelectionPanel.setEnabled(crsButtonModel.isSelected());
        collocationPanel.setEnabled(collocateButtonModel.isSelected());
        String crsInfoText = "";
        Product coProduct = collocateProductSelector.getSelectedProduct();
        if (coProduct != null) {
            crsInfoText = coProduct.getGeoCoding().getMapCRS().getName().getCode();
        }
        collocationCrsCodeField.setText(crsInfoText);
    }

    private void createUI() {
        collocationPanel = createCollocationPanel();
        crsSelectionPanel = createCrsSelectionPanel();
        projDefPanel = createCustomCrsPanel();
        JRadioButton projectionRadioButton = new JRadioButton("Custom CRS", true);
        projectionRadioButton.addActionListener(new UpdateStateListener());
        JRadioButton crsRadioButton = new JRadioButton("Predefined CRS");
        crsRadioButton.addActionListener(new UpdateStateListener());
        JRadioButton collocateRadioButton = new JRadioButton("Use CRS of");
        collocateRadioButton.addActionListener(new UpdateStateListener());
        projButtonModel = projectionRadioButton.getModel();
        crsButtonModel = crsRadioButton.getModel();
        collocateButtonModel = collocateRadioButton.getModel();

        ButtonGroup buttonGroup = new ButtonGroup();
        buttonGroup.add(projectionRadioButton);
        buttonGroup.add(crsRadioButton);
        buttonGroup.add(collocateRadioButton);

        final TableLayout tableLayout = new TableLayout(2);
        tableLayout.setTablePadding(4, 4);
        tableLayout.setTableFill(TableLayout.Fill.BOTH);
        tableLayout.setTableAnchor(TableLayout.Anchor.WEST);
        tableLayout.setTableWeightX(1.0);
        tableLayout.setTableWeightY(0.0);
        tableLayout.setCellColspan(0, 0, 2);
        tableLayout.setCellColspan(1, 0, 2);
        tableLayout.setCellWeightX(2, 0, 0.0);
        tableLayout.setCellWeightX(3, 0, 0.0);
        tableLayout.setCellPadding(1, 0, new Insets(4, 24, 4, 4));

        setLayout(tableLayout);
        setBorder(BorderFactory.createTitledBorder("Coordinate Reference System (CRS)"));
        add(projectionRadioButton);
        add(projDefPanel);
        add(crsRadioButton);
        add(crsSelectionPanel);
        add(collocateRadioButton);
        add(collocationPanel);
    }

    private JPanel createCollocationPanel() {
        final TableLayout tableLayout = new TableLayout(2);
        final JPanel panel = new JPanel(tableLayout);
        tableLayout.setTableAnchor(TableLayout.Anchor.WEST);
        tableLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
        tableLayout.setColumnWeightX(0, 1.0);
        tableLayout.setColumnWeightX(1, 0.0);
        tableLayout.setCellFill(0, 1, Fill.BOTH);

        collocationCrsCodeField = new JTextField();
        collocationCrsCodeField.setEditable(false);
        collocateProductSelector = new SourceProductSelector(appContext, "Product:");
        collocateProductSelector.setProductFilter(new CollocateProductFilter());
        collocateProductSelector.addSelectionChangeListener(new SelectionChangeListener() {
            @Override
            public void selectionChanged(SelectionChangeEvent event) {
                updateUIState();
            }
        });

        panel.add(collocateProductSelector.getProductNameComboBox());
        panel.add(collocateProductSelector.getProductFileChooserButton());
//        final JPanel panel = new JPanel(new BorderLayout(2, 2));
//        panel.add(collocateProductSelector.getProductNameComboBox(), BorderLayout.CENTER);
//        panel.add(collocateProductSelector.getProductFileChooserButton(), BorderLayout.EAST);
        panel.addPropertyChangeListener("enabled", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                collocateProductSelector.getProductNameComboBox().setEnabled(panel.isEnabled());
                collocateProductSelector.getProductFileChooserButton().setEnabled(panel.isEnabled());
            }
        });
        panel.add(collocationCrsCodeField);
        return panel;
    }

    private JPanel createCrsSelectionPanel() {
        final TableLayout tableLayout = new TableLayout(2);
        final JPanel panel = new JPanel(tableLayout);
        tableLayout.setTableAnchor(TableLayout.Anchor.WEST);
        tableLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
        tableLayout.setColumnWeightX(0, 1.0);
        tableLayout.setColumnWeightX(1, 0.0);

        final JTextField crsCodeField = new JTextField();
        crsCodeField.setEditable(false);
        final JButton crsButton = new JButton("Select...");
        crsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final PredefinedCrsForm predefinedCrsForm = new PredefinedCrsForm(
                        new CrsInfoListModel(CrsInfo.generateCRSList()));
                final ModalDialog dialog = new ModalDialog(appContext.getApplicationWindow(),
                                                           "Select Coordinate Reference System",
                                                           predefinedCrsForm,
                                                           ModalDialog.ID_OK_CANCEL, null);
                if (dialog.show() == ModalDialog.ID_OK) {
                    selectedCrsInfo = predefinedCrsForm.getSelectedCrsInfo();
                    crsCodeField.setText(selectedCrsInfo.toString());
                }
            }
        });
        panel.add(crsCodeField);
        panel.add(crsButton);
        panel.addPropertyChangeListener("enabled", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                crsCodeField.setEnabled((Boolean) evt.getNewValue());
                crsButton.setEnabled((Boolean) evt.getNewValue());
            }
        });
        return panel;
    }

    private CustomCrsForm createCustomCrsPanel() {
        return new CustomCrsForm(appContext.getApplicationWindow());
    }

    private class CollocateProductFilter implements ProductFilter {

        @Override
        public boolean accept(Product collocationProduct) {
            if (sourceProductSelector.getSelectedProduct() == collocationProduct ||
                    collocationProduct.getGeoCoding() == null) {
                return false;
            }
            final GeoCoding geoCoding = collocationProduct.getGeoCoding();
            if (geoCoding.canGetGeoPos() && geoCoding.canGetPixelPos() && (geoCoding instanceof CrsGeoCoding)) {
                Product sourceProduct = sourceProductSelector.getSelectedProduct();
                final GeneralPath[] sourcePath = ProductUtils.createGeoBoundaryPaths(sourceProduct);
                final GeneralPath[] collocationPath = ProductUtils.createGeoBoundaryPaths(collocationProduct);
                for (GeneralPath path : sourcePath) {
                    Rectangle bounds = path.getBounds();
                    for (GeneralPath colPath : collocationPath) {
                        if (colPath.getBounds().intersects(bounds)) {
                            return true;
                        }
                    }
                }
            }
            return false;
        }
    }

    private class UpdateStateListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            updateUIState();
        }
    }
}