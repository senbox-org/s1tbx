/* 
 * Copyright (C) 2002-2008 by Brockmann Consult
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.dataio.smos;

import com.bc.ceres.binio.CompoundData;
import com.bc.ceres.binio.CompoundType;
import com.bc.ceres.binio.DataFormat;
import com.bc.ceres.binio.Type;

import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;

/**
 * todo - add API doc
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
public class L2SmosFile extends SmosFile {
    public static final String BT_DATA_LIST_NAME = "Retrieval_Results_Data";

    private final int retrievalResultsDataIndex;
    private final CompoundType retrievalResultsDataType;

    public L2SmosFile(File file, DataFormat format) throws IOException {
        super(file, format);

        retrievalResultsDataIndex = getGridPointType().getMemberIndex(BT_DATA_LIST_NAME);
        if (retrievalResultsDataIndex == -1) {
            throw new IOException("Grid point type does not include retrieval results data.");
        }

        final Type memberType = getGridPointType().getMemberType(retrievalResultsDataIndex);
        if (!memberType.isCompoundType()) {
            throw new IOException(MessageFormat.format(
                    "Data type ''{0}'' is not of appropriate type", memberType.getName()));
        }

        retrievalResultsDataType = (CompoundType) memberType;
    }

    public final CompoundType getRetrievalResultsDataType() {
        return retrievalResultsDataType;
    }

    public final CompoundData getRetrievalResultsData(int gridPointIndex) throws IOException {
        return getGridPointData(gridPointIndex).getCompound(retrievalResultsDataIndex);
    }
}
