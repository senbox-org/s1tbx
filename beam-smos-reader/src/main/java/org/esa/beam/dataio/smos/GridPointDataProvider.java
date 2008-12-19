package org.esa.beam.dataio.smos;

import com.bc.ceres.binio.DataFormat;
import com.bc.ceres.binio.CompoundType;
import com.bc.ceres.binio.CompoundData;
import com.bc.ceres.binio.Type;

import java.io.IOException;
import java.awt.geom.Area;

public interface GridPointDataProvider {

    Area getRegion();
    
    int getGridPointIndex(int seqnum);

    CompoundType getGridPointType();

    CompoundData getGridPointData(int gridPointIndex) throws IOException;
}
