/*
 * $Id: Algorithm.java,v 1.1 2006/09/11 10:47:31 norman Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
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
package org.esa.beam.processor.binning.algorithm;

import org.esa.beam.framework.processor.ProcessorException;
import org.esa.beam.processor.binning.database.Bin;

//@todo 1 se/tb - class dokumentation

public interface Algorithm {

    /**
     * Initialize the algorithm given an algorithm parameter string
     */
    public void init(String algorithmParams) throws ProcessorException;

    /**
     * Accumulate the geophysical value passed in to the bin.(spatial binning)
     */
    public void accumulateSpatial(float val, Bin bin);

    /**
     * Process the final spatial binning algorithm
     */
    public void finishSpatial(Bin bin);

    /**
     * Returns whether the algorithm needs to process a finishing stage for the spatial binning - or not
     */
    public boolean needsFinishSpatial();

    /**
     * Accumulate the source bin to the target bin (temporal binning)
     */
    public void accumulateTemporal(Bin source, Bin target);

    /**
     * Interprete the bin
     */
    public void interprete(Bin accumulated, Bin interpreted);

    /**
     * Returns whether the algorithm needs to process an interpretation stage for the temporal binning - or not
     */
    public boolean needsInterpretation();

    /**
     * Retrieves the number of variables needed for accumulation.
     */
    public int getNumberOfAccumulatedVariables();

    /**
     * Retrieves a name for the accumulated variable at the given index
     */
    public String getAccumulatedVariableNameAt(int index);

    /**
     * Retrieves the number of variables of the interpreted bin, i.e. in the final product
     */
    public int getNumberOfInterpretedVariables();

    /**
     * Retrieves a name for the interprested variable at the given index
     */
    public String getInterpretedVariableNameAt(int index);

    /**
     * Retrieves the short description string for the algorithm (used by algorithm factory)
     */
    public String getTypeString();
}
