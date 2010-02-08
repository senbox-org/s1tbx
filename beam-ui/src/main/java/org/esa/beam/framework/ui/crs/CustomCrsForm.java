package org.esa.beam.framework.ui.crs;

import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.ui.AppContext;
import org.esa.beam.framework.ui.crs.projdef.CustomCrsPanel;
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

    public CustomCrsForm(AppContext appContext) {
        super(appContext);
    }

    @Override
    protected String getLabelText() {
        return "Custom CRS";
    }

    @Override
    boolean wrapAfterButton() {
        return true;
    }

    @Override
    public CoordinateReferenceSystem getCRS(GeoPos referencePos) throws FactoryException {
        return ((CustomCrsPanel)getCrsUI()).getCRS(referencePos);
    }

    @Override
    protected JComponent createCrsComponent() {
        final CustomCrsPanel panel = new CustomCrsPanel(getAppContext().getApplicationWindow());
        panel.addPropertyChangeListener("crs", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                fireCrsChanged();
            }
        });
        return panel;
    }

    @Override
    public void prepareShow() {
    }

    @Override
    public void prepareHide() {
    }
}
