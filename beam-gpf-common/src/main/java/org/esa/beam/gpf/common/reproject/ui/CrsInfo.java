package org.esa.beam.gpf.common.reproject.ui;

import org.opengis.referencing.crs.ProjectedCRS;

/**
 * @author Marco Peters
* @version $ Revision $ Date $
* @since BEAM 4.6
*/
class CrsInfo {

    private final String epsgCode;
    private final ProjectedCRS crs;

    CrsInfo(String epsgCode, ProjectedCRS crs) {
        this.epsgCode = epsgCode;
        this.crs = crs;
    }

    public String getEpsgCode() {
        return epsgCode;
    }

    public ProjectedCRS getCrs() {
        return crs;
    }

    @Override
    public String toString() {
        return epsgCode + " : " + crs.getName().getCode();
    }
}
