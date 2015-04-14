/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.statistics.output;

import java.io.IOException;
import java.util.Map;

/**
 * Output interface for statistics.
 *
 * @author Thomas Storm
 */
public interface StatisticsOutputter {

    /**
     * Takes preparing steps for outputting the statistics.
     *
     * @param statisticsOutputContext A context providing meta-information about the statistics.
     */
    void initialiseOutput(StatisticsOutputContext statisticsOutputContext);

    /**
     * Adds the provided statistics to the output.
     *
     * @param bandName   The name of the band the statistics have been computed for.
     * @param regionId   The id of the region the statistics have been computed for.
     * @param statistics The actual statistics as map. Keys are the algorithm names, values are the actual statistical values.
     */
    void addToOutput(String bandName, String regionId, Map<String, Number> statistics);

    /**
     * Performs finalising steps.
     *
     * @throws IOException If IO fails.
     */
    void finaliseOutput() throws IOException;
}
