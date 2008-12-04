package org.esa.beam.dataio.smos;

import com.bc.ceres.binio.DataFormat;
import com.bc.ceres.binio.CompoundType;
import com.bc.ceres.binio.CompoundData;
import com.bc.ceres.binio.Type;

import java.io.IOException;

/**
 * todo - add API doc
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
interface GridPointDataProvider {
    
    int getGridPointIndex(int seqnum);

    CompoundType getGridPointType();

    CompoundData getGridPointData(int gridPointIndex) throws IOException;
}
