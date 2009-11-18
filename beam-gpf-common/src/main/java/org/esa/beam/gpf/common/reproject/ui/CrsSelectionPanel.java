package org.esa.beam.gpf.common.reproject.ui;

import com.bc.ceres.swing.TableLayout;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.AppContext;
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
public class CrsSelectionPanel extends JPanel {

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
    private final boolean showCollocation;

    public CrsSelectionPanel(AppContext appContext) {
        this(appContext, true);
    }

    public CrsSelectionPanel(AppContext appContext, final boolean showCollocation) {
        this.showCollocation = showCollocation;
        customCrsUI = new CustomCrsForm(appContext);
        predefinedCrsUI = new PredefinedCrsForm(appContext);
        if (showCollocation) {
            collocationCrsUI = new CollocationCrsForm(appContext);
        }

        createUI();
        crsChangeListener = new CrsChangeListener();
        addPropertyChangeListener("enabled", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                final Boolean enabled = (Boolean) evt.getNewValue();
                customCrsButtonModel.setEnabled(enabled);
                predefinedCrsButtonModel.setEnabled(enabled);
                customCrsComponent.setEnabled(enabled);
                predefinedCrsComponent.setEnabled(enabled);
                if (showCollocation) {
                    collocationButtonModel.setEnabled(enabled);
                    collocationComponent.setEnabled(enabled);
                }
            }
        });
    }

    public void setReferenceProduct(Product product) {
        customCrsUI.setReferenceProduct(product);
        predefinedCrsUI.setReferenceProduct(product);
        if (this.showCollocation) {
            collocationCrsUI.setReferenceProduct(product);
        }
    }

    public CoordinateReferenceSystem getCrs(GeoPos referencePos) throws FactoryException {
        if (customCrsButtonModel.isSelected()) {
            return customCrsUI.getCRS(referencePos);
        }
        if (predefinedCrsButtonModel.isSelected() ) {
            return predefinedCrsUI.getCRS(referencePos);
        }
        if (showCollocation && collocationButtonModel.isSelected()) {
            return collocationCrsUI.getCRS(referencePos);
        }
        return null;
    }

    public Product getCollocationProduct(){
        if(showCollocation) {
        return collocationCrsUI.getCollocationProduct();
        }else {
            return null;
        }
    }

    boolean isCollocate() {
        return showCollocation && collocationButtonModel.isSelected();
    }

    public void prepareShow() {
        customCrsUI.prepareShow();
        customCrsUI.addCrsChangeListener(crsChangeListener);
        predefinedCrsUI.prepareShow();
        predefinedCrsUI.addCrsChangeListener(crsChangeListener);
        if (showCollocation) {
            collocationCrsUI.prepareShow();
            collocationCrsUI.addCrsChangeListener(crsChangeListener);
        }

        updateUIState();
    }

    public void prepareHide() {
        customCrsUI.prepareHide();
        customCrsUI.removeCrsChangeListener(crsChangeListener);
        predefinedCrsUI.prepareHide();
        predefinedCrsUI.removeCrsChangeListener(crsChangeListener);
        if (showCollocation) {
            collocationCrsUI.prepareHide();
            collocationCrsUI.removeCrsChangeListener(crsChangeListener);
        }
    }

    private void updateUIState() {
        customCrsComponent.setEnabled(customCrsButtonModel.isSelected());
        predefinedCrsComponent.setEnabled(predefinedCrsButtonModel.isSelected());
        if (showCollocation) {
            final boolean collocate = isCollocate();
            firePropertyChange("collocate", !collocate, collocate);
            collocationComponent.setEnabled(collocationButtonModel.isSelected());
        }
    }

    private void createUI() {
        ButtonGroup buttonGroup = new ButtonGroup();


        final TableLayout tableLayout = new TableLayout(2);
        tableLayout.setTablePadding(4, 4);
        tableLayout.setTableFill(TableLayout.Fill.BOTH);
        tableLayout.setTableAnchor(TableLayout.Anchor.WEST);
        tableLayout.setTableWeightX(1.0);
        tableLayout.setTableWeightY(0.0);
        tableLayout.setCellColspan(0, 0, 2);
        tableLayout.setCellColspan(1, 0, 2);
        tableLayout.setCellWeightX(2, 0, 0.0);
        tableLayout.setCellPadding(1, 0, new Insets(4, 24, 4, 4));

        setLayout(tableLayout);
        setBorder(BorderFactory.createTitledBorder("Coordinate Reference System (CRS)"));

        JRadioButton customCrsRadioButton = new JRadioButton("Custom CRS", true);
        customCrsRadioButton.addActionListener(new UpdateStateListener());
        customCrsComponent = customCrsUI.getCrsUI();
        customCrsComponent.setEnabled(true);
        customCrsButtonModel = customCrsRadioButton.getModel();
        buttonGroup.add(customCrsRadioButton);
        add(customCrsRadioButton);
        add(customCrsComponent);

        JRadioButton predefinedCrsRadioButton = new JRadioButton("Predefined CRS");
        predefinedCrsRadioButton.addActionListener(new UpdateStateListener());
        predefinedCrsComponent = predefinedCrsUI.getCrsUI();
        predefinedCrsComponent.setEnabled(false);
        predefinedCrsButtonModel = predefinedCrsRadioButton.getModel();
        buttonGroup.add(predefinedCrsRadioButton);
        add(predefinedCrsRadioButton);
        add(predefinedCrsComponent);

        if (showCollocation) {
            JRadioButton collocationRadioButton = new JRadioButton("Use CRS of");
            collocationRadioButton.addActionListener(new UpdateStateListener());
            collocationComponent = collocationCrsUI.getCrsUI();
            collocationComponent.setEnabled(false);
            collocationButtonModel = collocationRadioButton.getModel();
            buttonGroup.add(collocationRadioButton);
            tableLayout.setCellWeightX(3, 0, 0.0);
            add(collocationRadioButton);
            add(collocationComponent);
        }
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