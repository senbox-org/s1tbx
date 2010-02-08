package org.esa.beam.framework.ui.crs;

import com.bc.ceres.swing.TableLayout;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.ModalDialog;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * @author Marco Peters
 * @version $ Revision $ Date $
 * @since BEAM 4.7
 */
public class PredefinedCrsForm extends CrsForm {

    private CrsInfo selectedCrsInfo;

    public PredefinedCrsForm(AppContext appContext) {
        super(appContext);
    }

    @Override
    protected String getLabelText() {
        return "Predefined CRS";
    }

    @Override
    public CoordinateReferenceSystem getCRS(GeoPos referencePos) throws FactoryException {
        if (selectedCrsInfo != null) {
            return selectedCrsInfo.getCrs(referencePos);
        } else {
            return null;
        }
    }


    @Override
    public void prepareShow() {
    }

    @Override
    public void prepareHide() {
    }

    @Override
    protected JComponent createCrsComponent() {
        final TableLayout tableLayout = new TableLayout(2);
        final JPanel panel = new JPanel(tableLayout);
        tableLayout.setTableAnchor(TableLayout.Anchor.WEST);
        tableLayout.setTableFill(TableLayout.Fill.HORIZONTAL);
        tableLayout.setColumnWeightX(0, 1.0);
        tableLayout.setColumnWeightX(1, 0.0);

        final JTextField crsCodeField = new JTextField();
        crsCodeField.setEditable(false);
        final JButton crsButton = new JButton("Select...");
        final PredefinedCrsPanel predefinedCrsForm = new PredefinedCrsPanel(
                new CrsInfoListModel(CrsInfo.generateCRSList()));
        crsButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                final ModalDialog dialog = new ModalDialog(getAppContext().getApplicationWindow(),
                                                           "Select Coordinate Reference System",
                                                           predefinedCrsForm,
                                                           ModalDialog.ID_OK_CANCEL, null);
                if (dialog.show() == ModalDialog.ID_OK) {
                    selectedCrsInfo = predefinedCrsForm.getSelectedCrsInfo();
                    crsCodeField.setText(selectedCrsInfo.toString());
                    fireCrsChanged();
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

}
