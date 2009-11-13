package org.esa.beam.gpf.common.reproject.ui;

import org.esa.beam.framework.datamodel.Product;
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

    private CustomCrsPanel customCrsForm;

    protected CustomCrsForm(AppContext appContext) {
        super(appContext);
        customCrsForm = new CustomCrsPanel(appContext.getApplicationWindow());
        customCrsForm.addPropertyChangeListener("crs", new PropertyChangeListener() {
            @Override
            public void propertyChange(PropertyChangeEvent evt) {
                fireCrsChanged();
            }
        });
    }

    @Override
    public CoordinateReferenceSystem getCRS(Product product) throws FactoryException {
        return customCrsForm.getCRS(product);
    }

    @Override
    public JComponent getCrsUI() {
        return customCrsForm;
    }

    @Override
    public void prepareShow() {
    }

    @Override
    public void prepareHide() {
    }
}
