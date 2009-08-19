package org.esa.beam.gpf.common.reproject.ui;

import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 * @author Marco Peters
 * @version $ Revision $ Date $
 * @since BEAM 4.7
 */
class CrsSelectionFormModel {
    private final CrsInfoListModel listModel;
    private CoordinateReferenceSystem selectedCrs;
    private PropertyChangeSupport changeSupport;

    CrsSelectionFormModel(CrsInfoListModel listModel, CoordinateReferenceSystem selectedCrs) {
        this.listModel = listModel;
        this.selectedCrs = selectedCrs;
    }

    public CrsInfoListModel getListModel() {
        return listModel;
    }

    public CoordinateReferenceSystem getSelectedCrs() {
        return selectedCrs;
    }

    public void setSelectedCrs(CoordinateReferenceSystem selectedCrs) {
        if (this.selectedCrs != selectedCrs) {
            final CoordinateReferenceSystem oldCrs = this.selectedCrs;
            this.selectedCrs = selectedCrs;
            fireSelectedCrsChanged(oldCrs, selectedCrs);
        }
    }

    public void addPropertyChangeListener(PropertyChangeListener listener){
        if (changeSupport == null) {
            changeSupport = new PropertyChangeSupport(this);
        }
        changeSupport.addPropertyChangeListener(listener);
    }

    public void removePropertyChangeListener(PropertyChangeListener listener){
        if (changeSupport != null) {
            changeSupport.removePropertyChangeListener(listener);
        }
    }

    private void fireSelectedCrsChanged(CoordinateReferenceSystem oldCrs, CoordinateReferenceSystem selectedCrs) {
        if(changeSupport != null) {
            changeSupport.firePropertyChange("selectedCrs", oldCrs, selectedCrs);
        }
    }
}
