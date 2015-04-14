/*
 * Copyright (C) 2013 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.snap.binning.operator;

import org.esa.snap.binning.BinningContext;
import org.esa.snap.binning.TemporalBin;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * This interface can be implemented to write binned data.
 *
 * @author Marco Peters
 *
 * TODO mz/mp/nf 2013-11-06 move to formatting or io (together with reader) package, refactor
 * TODO mz/mp/nf 2013-11-06 rename to e.g. BinnedFileWriter
 */
public interface BinWriter {

    void write(Map<String, String> metadataProperties, List<TemporalBin> temporalBins) throws IOException;

    void setBinningContext(BinningContext binningContext);

    void setTargetFileTemplatePath(String targetFileTemplatePath);

    String getTargetFilePath();

    void setLogger(Logger logger);
}
