package org.esa.beam.gpf.common.reproject.ui;

import com.bc.ceres.swing.TableLayout;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.ui.SourceProductSelector;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.application.SelectionChangeEvent;
import org.esa.beam.framework.ui.application.SelectionChangeListener;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.ButtonModel;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * @author Marco Peters
 * @author Marco Zühlke
 * @version $ Revision $ Date $
 * @since BEAM 4.7
 */
class CrsSelectionForm extends JPanel {

    private ButtonModel customCrsButtonModel;
    private ButtonModel predefinedCrsButtonModel;
    private ButtonModel collocationButtonModel;
    private JComponent customCrsComponent;
    private JComponent collocationComponent;
    private JComponent predefinedCrsComponent;

    private CollocationCrsForm collocationCrsUI;
    private PredefinedCrsForm predefinedCrsUI;
    private CustomCrsForm customCrsUI;
    private CrsChangeListener crsChangeListener;

    CrsSelectionForm(AppContext appContext, SourceProductSelector sourceProductSelector) {
        customCrsUI = new CustomCrsForm(appContext);
        predefinedCrsUI = new PredefinedCrsForm(appContext);
        collocationCrsUI = new CollocationCrsForm(appContext);

        sourceProductSelector.addSelectionChangeListener(new SelectionChangeListener() {
            @Override
            public void selectionChanged(SelectionChangeEvent event) {
                final Product product = (Product) event.getSelection().getFirstElement();
                customCrsUI.setReferenceProduct(product);
                predefinedCrsUI.setReferenceProduct(product);
                collocationCrsUI.setReferenceProduct(product);
            }
        });
        createUI();
        crsChangeListener = new CrsChangeListener();
    }

    CoordinateReferenceSystem getCrs(Product sourceProduct) throws FactoryException {
        if (customCrsButtonModel.isSelected()) {
            return customCrsUI.getCRS(sourceProduct);
        }
        if (predefinedCrsButtonModel.isSelected() ) {
            return predefinedCrsUI.getCRS(sourceProduct);
        }
        if (collocationButtonModel.isSelected()) {
            return collocationCrsUI.getCRS(sourceProduct);
        }
        return null;
    }

    Product getCollocationProduct(){
        return collocationCrsUI.getCollocationProduct();
    }

    boolean isCollocate() {
        return collocationButtonModel.isSelected();
    }

    void prepareShow() {
        customCrsUI.prepareShow();
        predefinedCrsUI.prepareShow();
        collocationCrsUI.prepareShow();
        customCrsUI.addCrsChangeListener(crsChangeListener);
        predefinedCrsUI.addCrsChangeListener(crsChangeListener);
        collocationCrsUI.addCrsChangeListener(crsChangeListener);

        updateUIState();
    }

    void prepareHide() {
        collocationCrsUI.prepareHide();
        predefinedCrsUI.prepareHide();
        collocationCrsUI.prepareHide();
        customCrsUI.removeCrsChangeListener(crsChangeListener);
        predefinedCrsUI.removeCrsChangeListener(crsChangeListener);
        collocationCrsUI.removeCrsChangeListener(crsChangeListener);
    }

    private void updateUIState() {
        final boolean collocate = isCollocate();
        firePropertyChange("collocate", !collocate, collocate);
        customCrsComponent.setEnabled(customCrsButtonModel.isSelected());
        predefinedCrsComponent.setEnabled(predefinedCrsButtonModel.isSelected());
        collocationComponent.setEnabled(collocationButtonModel.isSelected());
    }

    private void createUI() {
        customCrsComponent = customCrsUI.getCrsUI();
        predefinedCrsComponent = predefinedCrsUI.getCrsUI();
        collocationComponent = collocationCrsUI.getCrsUI();

        JRadioButton projectionRadioButton = new JRadioButton("Custom CRS", true);
        projectionRadioButton.addActionListener(new UpdateStateListener());
        JRadioButton crsRadioButton = new JRadioButton("Predefined CRS");
        crsRadioButton.addActionListener(new UpdateStateListener());
        JRadioButton collocateRadioButton = new JRadioButton("Use CRS of");
        collocateRadioButton.addActionListener(new UpdateStateListener());
        customCrsButtonModel = projectionRadioButton.getModel();
        predefinedCrsButtonModel = crsRadioButton.getModel();
        collocationButtonModel = collocateRadioButton.getModel();

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
        add(customCrsComponent);
        add(crsRadioButton);
        add(predefinedCrsComponent);
        add(collocateRadioButton);
        add(collocationComponent);
    }

    private class UpdateStateListener implements ActionListener {

        @Override
        public void actionPerformed(ActionEvent e) {
            updateUIState();
            fireCrsChanged();
        }
    }

    private class CrsChangeListener implements PropertyChangeListener {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            fireCrsChanged();
        }
    }

    private void fireCrsChanged() {
        firePropertyChange("crs", null, null);
    }


}