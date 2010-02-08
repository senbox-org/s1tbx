package org.esa.beam.framework.ui.crs;

import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.ui.AppContext;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import javax.swing.JComponent;
import javax.swing.JRadioButton;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;

/**
 * @author Marco Peters
 * @version $ Revision $ Date $
 * @since BEAM 4.7
 */
public abstract class CrsForm {

    private AppContext appContext;
    private Product referenceProduct;
    private ArrayList<PropertyChangeListener> changeListeners;
    private JComponent crsComponent;
    private JRadioButton radioButton;


    protected CrsForm(AppContext appContext) {
        this.appContext = appContext;
        changeListeners = new ArrayList<PropertyChangeListener>();
    }

    protected abstract String getLabelText();

    boolean wrapAfterButton() {
        return false;
    }

    public final JRadioButton getRadioButton(){
        if(radioButton == null) {
            radioButton = createRadioButton();
        }
        return radioButton;
    }

    protected JRadioButton createRadioButton() {
        return new JRadioButton(getLabelText());
    }


    public abstract CoordinateReferenceSystem getCRS(GeoPos referencePos) throws FactoryException;

    public final JComponent getCrsUI() {
        if(crsComponent == null) {
            crsComponent = createCrsComponent();
        }
        return crsComponent;
    }

    protected abstract JComponent createCrsComponent();

    public void setReferenceProduct(Product product) {
        referenceProduct = product;
    }


    protected Product getReferenceProduct() {
        return referenceProduct;
    }

    protected AppContext getAppContext() {
        return appContext;
    }

    protected void fireCrsChanged() {
        for (PropertyChangeListener listener : changeListeners) {
            listener.propertyChange(new PropertyChangeEvent(this, "crs", null, null));
        }
    }

    protected boolean addCrsChangeListener(PropertyChangeListener listener) {
        if (!changeListeners.contains(listener)) {
            return changeListeners.add(listener);
        }
        return false;
    }

    protected boolean removeCrsChangeListener(PropertyChangeListener listener) {
        if (changeListeners.contains(listener)) {
            return changeListeners.remove(listener);
        }
        return false;
    }

    public abstract void prepareShow();

    public abstract void prepareHide();
}
