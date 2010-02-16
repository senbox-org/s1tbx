package org.esa.beam.visat.toolviews.placemark.gcp;

import org.esa.beam.framework.datamodel.GcpDescriptor;
import org.esa.beam.visat.toolviews.placemark.InsertPlacemarkInteractor;

/**
 * A tool used to create ground control points (single click), select (single click on a GCP) or
 * edit (double click on a GCP) the GCPs displayed in product scene view.
 */
public class InsertGcpInteractor extends InsertPlacemarkInteractor {

    public InsertGcpInteractor() {
        super(GcpDescriptor.INSTANCE);
    }
}