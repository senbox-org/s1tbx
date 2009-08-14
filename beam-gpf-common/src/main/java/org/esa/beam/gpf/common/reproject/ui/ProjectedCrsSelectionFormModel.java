package org.esa.beam.gpf.common.reproject.ui;

import org.opengis.referencing.crs.ProjectedCRS;

/**
 * @author Marco Peters
 * @version $ Revision $ Date $
 * @since BEAM 4.6
 */
class ProjectedCrsSelectionFormModel {
    private final CrsInfoListModel listModel;
    private ProjectedCRS selectedCrs;

    ProjectedCrsSelectionFormModel(CrsInfoListModel listModel) {
        this.listModel = listModel;
    }

    public CrsInfoListModel getListModel() {
        return listModel;
    }

    public ProjectedCRS getSelectedCrs() {
        return selectedCrs;
    }

    public void setSelectedCrs(ProjectedCRS selectedCrs) {
        this.selectedCrs = selectedCrs;
    }
}
