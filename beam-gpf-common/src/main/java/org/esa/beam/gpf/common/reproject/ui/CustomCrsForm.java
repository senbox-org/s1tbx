package org.esa.beam.gpf.common.reproject.ui;

import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.gpf.common.reproject.ui.projdef.CustomCrsPanel;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import javax.swing.JComponent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * @author Marco Peters
 * @version $ Revision $ Date $
 * @since BEAM 4.7
 */
public class CustomCrsForm extends CrsForm {

    private CustomCrsPanel customCrsPanel;

    protected CustomCrsForm(AppContext appContext) {
        super(appContext);
        customCrsPanel = new CustomCrsPanel(appContext.getApplicationWindow());
        customCrsPanel.addPropertyChangeListener("crs", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                fireCrsChanged();
            }
        });
    }

    @Override
    public CoordinateReferenceSystem getCRS(GeoPos referencePos) throws FactoryException {
        return customCrsPanel.getCRS(referencePos);
    }

    @Override
    public JComponent getCrsUI() {
        return customCrsPanel;
    }

    @Override
    public void prepareShow() {
    }

    @Override
    public void prepareHide() {
    }
}
