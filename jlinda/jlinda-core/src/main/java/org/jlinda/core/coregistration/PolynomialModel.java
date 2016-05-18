package org.jlinda.core.coregistration;

import org.esa.snap.core.datamodel.Placemark;

import javax.media.jai.WarpPolynomial;
import java.util.List;

/**
 * Created by luis on 15/02/2016.
 */
public interface PolynomialModel {

    boolean isValid();

    WarpPolynomial getJAIWarp();

    int getNumObservations();

    double getRMS(int index);

    double getXMasterCoord(int index);

    double getYMasterCoord(int index);

    double getXSlaveCoord(int index);

    double getYSlaveCoord(int index);

    List<Placemark> getSlaveGCPList();

    double getRMSStd();
    double getRMSMean();
    double getRowResidualStd();
    double getRowResidualMean();
    double getColResidualStd();
    double getColResidualMean();
}
