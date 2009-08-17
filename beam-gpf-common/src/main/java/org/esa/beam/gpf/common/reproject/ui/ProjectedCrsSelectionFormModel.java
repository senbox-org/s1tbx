package org.esa.beam.gpf.common.reproject.ui;

import org.opengis.referencing.crs.ProjectedCRS;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;

/**
 * @author Marco Peters
 * @version $ Revision $ Date $
 * @since BEAM 4.7
 */
class ProjectedCrsSelectionFormModel {
    private final CrsInfoListModel listModel;
    private ProjectedCRS selectedCrs;
    private PropertyChangeSupport changeSupport;

    ProjectedCrsSelectionFormModel(CrsInfoListModel listModel) {
        this.listModel = listModel;
        selectedCrs = listModel.getElementAt(0).getCrs();
    }

    public CrsInfoListModel getListModel() {
        return listModel;
    }

    public ProjectedCRS getSelectedCrs() {
        return selectedCrs;
    }

    public void setSelectedCrs(ProjectedCRS selectedCrs) {
        if (this.selectedCrs != selectedCrs) {
            final ProjectedCRS oldCrs = this.selectedCrs;
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

    private void fireSelectedCrsChanged(ProjectedCRS oldCrs, ProjectedCRS selectedCrs) {
        if(changeSupport != null) {
            changeSupport.firePropertyChange("selectedCrs", oldCrs, selectedCrs);
        }
    }
}
